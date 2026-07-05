package io.github.addxiaoyi.starx.common.auth;

import io.github.addxiaoyi.starx.api.event.EventBus;
import io.github.addxiaoyi.starx.api.event.EventTypes;
import io.github.addxiaoyi.starx.common.crypto.PasswordHasher;
import io.github.addxiaoyi.starx.common.crypto.TotpGenerator;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 认证服务：注册、登录、登出、密码修改、TOTP、可信设备。 */
public final class AuthService {

  private final JdbiUserRepository userRepository;
  private final EventBus eventBus;
  private final SessionManager sessionManager;

  public AuthService(
      JdbiUserRepository userRepository, EventBus eventBus, SessionManager sessionManager) {
    this.userRepository = userRepository;
    this.eventBus = eventBus;
    this.sessionManager = sessionManager;
  }

  public AuthResult autoLogin(UUID uuid, String username, InetAddress address) {
    AuthSession session = sessionManager.getOrCreate(uuid, username, address);
    if (session == null) {
      return AuthResult.failure("服务器繁忙，请稍后再试");
    }
    session.setState(AuthSession.State.AUTHENTICATED);
    userRepository.updateLastLogin(uuid, Instant.now());
    userRepository.updatePremium(uuid, true);
    eventBus.publish(EventTypes.PLAYER_LOGIN_SUCCESS, Map.of("uuid", uuid, "username", username));
    return AuthResult.success("正版自动登录成功", AuthSession.State.AUTHENTICATED);
  }

  public AuthResult register(UUID uuid, String username, String password, String email) {
    if (userRepository.existsByUsernameOrUuid(username, uuid)) {
      return AuthResult.failure("用户名已被占用或已注册");
    }
    String normalizedEmail = normalizeEmail(email);
    StarxUser user =
        new StarxUser(
            uuid,
            username,
            normalizedEmail,
            PasswordHasher.hash(password),
            null,
            false,
            Instant.now(),
            null,
            null,
            List.of());
    userRepository.saveUser(user);
    eventBus.publish(
        EventTypes.PLAYER_REGISTER,
        Map.of(
            "uuid",
            uuid,
            "username",
            username,
            "email",
            normalizedEmail == null ? "" : normalizedEmail));
    return AuthResult.success("注册成功");
  }

  public AuthResult login(
      UUID uuid,
      String username,
      String password,
      String totpCode,
      InetAddress address,
      String deviceId) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    StarxUser user = optional.get();
    if (!PasswordHasher.verify(password, user.passwordHash())) {
      publishLoginFailed(uuid, username, "密码错误");
      return AuthResult.failure("密码错误");
    }
    if (user.totpSecret() != null && !isTrustedDevice(user.trustedDevices(), deviceId)) {
      if (totpCode != null && TotpGenerator.verify(user.totpSecret(), totpCode, Instant.now())) {
        return authenticate(user);
      }
      AuthSession session = sessionManager.getOrCreate(uuid, username, address);
      if (session == null) {
        return AuthResult.failure("服务器繁忙，请稍后再试");
      }
      session.setState(AuthSession.State.AUTHENTICATING);
      return AuthResult.success("请输入二步验证码", AuthSession.State.AUTHENTICATING);
    }
    return authenticate(user);
  }

  public AuthResult verifyTotp(UUID uuid, String code) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    StarxUser user = optional.get();
    if (user.totpSecret() == null) {
      return AuthResult.failure("未开启二步验证");
    }
    Optional<AuthSession> session = sessionManager.get(uuid);
    if (session.isEmpty() || session.get().state() != AuthSession.State.AUTHENTICATING) {
      return AuthResult.failure("请先登录");
    }
    if (!TotpGenerator.verify(user.totpSecret(), code, Instant.now())) {
      publishLoginFailed(uuid, user.username(), "二步验证码错误");
      return AuthResult.failure("二步验证码错误");
    }
    return authenticate(user);
  }

  public AuthResult logout(UUID uuid) {
    Optional<AuthSession> session = sessionManager.get(uuid);
    String username = session.map(AuthSession::username).orElse("");
    sessionManager.remove(uuid);
    eventBus.publish(EventTypes.PLAYER_LOGOUT, Map.of("uuid", uuid, "username", username));
    return AuthResult.success("已登出");
  }

  public AuthResult changePassword(UUID uuid, String oldPassword, String newPassword) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    if (!PasswordHasher.verify(oldPassword, optional.get().passwordHash())) {
      return AuthResult.failure("原密码错误");
    }
    userRepository.updatePasswordHash(uuid, PasswordHasher.hash(newPassword));
    return AuthResult.success("密码修改成功");
  }

  public AuthResult bindTotp(UUID uuid, String password, String base32Secret) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    if (!PasswordHasher.verify(password, optional.get().passwordHash())) {
      return AuthResult.failure("密码错误");
    }
    userRepository.updateTotpSecret(uuid, base32Secret);
    return AuthResult.success("二步验证已绑定");
  }

  public AuthResult addTrustedDevice(UUID uuid, String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return AuthResult.failure("设备标识不能为空");
    }
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    List<String> devices = new ArrayList<>(optional.get().trustedDevices());
    if (devices.contains(deviceId)) {
      return AuthResult.success("设备已信任");
    }
    devices.add(deviceId);
    userRepository.updateTrustedDevices(uuid, devices);
    return AuthResult.success("设备已添加信任");
  }

  public AuthResult removeTrustedDevice(UUID uuid, String deviceId) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    List<String> devices = new ArrayList<>(optional.get().trustedDevices());
    if (!devices.remove(deviceId)) {
      return AuthResult.failure("设备不在信任列表中");
    }
    userRepository.updateTrustedDevices(uuid, devices);
    return AuthResult.success("设备已取消信任");
  }

  public boolean isTrustedDevice(UUID uuid, String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return false;
    }
    return userRepository
        .findTrustedDevicesByUuid(uuid)
        .map(
            json -> {
              List<String> devices = parseTrustedDevices(json);
              return devices.contains(deviceId);
            })
        .orElse(false);
  }

  public boolean isUserRegistered(UUID uuid) {
    return userRepository.existsByUuid(uuid);
  }

  public Optional<AuthSession.State> getSessionState(UUID uuid) {
    return sessionManager.get(uuid).map(AuthSession::state);
  }

  public boolean isTotpEnabled(UUID uuid) {
    return userRepository.findTotpSecretByUuid(uuid).isPresent();
  }

  public AuthResult enableTotp(UUID uuid, String password) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    StarxUser user = optional.get();
    if (!PasswordHasher.verify(password, user.passwordHash())) {
      return AuthResult.failure("密码错误");
    }
    if (user.totpSecret() != null) {
      return AuthResult.failure("二步验证已开启，请先关闭后再重新开启");
    }
    String secret = TotpGenerator.generateSecret();
    String uri = TotpGenerator.provisioningUri("StarX", user.username(), secret);
    userRepository.updateTotpSecret(uuid, secret);
    eventBus.publish(
        EventTypes.PLAYER_TOTP_ENABLED, Map.of("uuid", uuid, "username", user.username()));
    return AuthResult.success("二步验证已开启！密钥: " + secret + " | " + uri);
  }

  public AuthResult disableTotp(UUID uuid, String password) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    StarxUser user = optional.get();
    if (!PasswordHasher.verify(password, user.passwordHash())) {
      return AuthResult.failure("密码错误");
    }
    if (user.totpSecret() == null) {
      return AuthResult.failure("二步验证未开启");
    }
    userRepository.updateTotpSecret(uuid, null);
    eventBus.publish(
        EventTypes.PLAYER_TOTP_DISABLED, Map.of("uuid", uuid, "username", user.username()));
    return AuthResult.success("二步验证已关闭");
  }

  public AuthResult resetPassword(String username, String newPassword) {
    Optional<StarxUser> optional = userRepository.findFullByUsername(username);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户不存在");
    }
    userRepository.updatePasswordHash(optional.get().uuid(), PasswordHasher.hash(newPassword));
    return AuthResult.success("密码已重置");
  }

  public AuthResult bindEmail(String username, String email) {
    Optional<StarxUser> optional = userRepository.findFullByUsername(username);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户不存在");
    }
    String normalized = email == null || email.isBlank() ? null : email.trim();
    userRepository.updateEmail(optional.get().uuid(), normalized);
    return AuthResult.success("邮箱已绑定");
  }

  public AuthResult deleteUser(String username) {
    Optional<StarxUser> optional = userRepository.findFullByUsername(username);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户不存在");
    }
    userRepository.delete(optional.get().uuid());
    sessionManager.remove(optional.get().uuid());
    return AuthResult.success("用户已删除");
  }

  private AuthResult authenticate(StarxUser user) {
    AuthSession session = sessionManager.getOrCreate(user.uuid(), user.username(), null);
    if (session == null) {
      return AuthResult.failure("服务器繁忙，请稍后再试");
    }
    session.setState(AuthSession.State.AUTHENTICATED);
    userRepository.updateLastLogin(user.uuid(), Instant.now());
    eventBus.publish(
        EventTypes.PLAYER_LOGIN_SUCCESS, Map.of("uuid", user.uuid(), "username", user.username()));
    return AuthResult.success("登录成功", AuthSession.State.AUTHENTICATED);
  }

  private boolean isTrustedDevice(List<String> trustedDevices, String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return false;
    }
    return trustedDevices.contains(deviceId);
  }

  private void publishLoginFailed(UUID uuid, String username, String reason) {
    eventBus.publish(
        EventTypes.PLAYER_LOGIN_FAILED,
        Map.of("uuid", uuid, "username", username, "reason", reason));
  }

  private static String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return email.trim();
  }

  private static List<String> parseTrustedDevices(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> parsed =
          new com.google.gson.Gson()
              .fromJson(json, new com.google.gson.reflect.TypeToken<List<String>>() {}.getType());
      return parsed == null ? List.of() : List.copyOf(parsed);
    } catch (Exception e) {
      return List.of();
    }
  }
}
