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
    session.setState(AuthSession.State.AUTHENTICATED);
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isPresent()) {
      StarxUser user = optional.get();
      StarxUser updated =
          new StarxUser(
              user.uuid(),
              user.username(),
              user.email(),
              user.passwordHash(),
              user.totpSecret(),
              true,
              user.createdAt(),
              Instant.now(),
              user.externalUserId(),
              user.trustedDevices());
      userRepository.saveUser(updated);
    }
    eventBus.publish(EventTypes.PLAYER_LOGIN_SUCCESS, Map.of("uuid", uuid, "username", username));
    return AuthResult.success("正版自动登录成功", AuthSession.State.AUTHENTICATED);
  }

  public AuthResult register(UUID uuid, String username, String password, String email) {
    if (userRepository.findFullByUuid(uuid).isPresent()) {
      return AuthResult.failure("用户已存在");
    }
    if (userRepository.existsByUsername(username)) {
      return AuthResult.failure("用户名已被占用");
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
    if (user.totpSecret() != null && !isTrustedDevice(uuid, deviceId)) {
      if (totpCode != null && TotpGenerator.verify(user.totpSecret(), totpCode, Instant.now())) {
        return authenticate(uuid, username, address);
      }
      AuthSession session = sessionManager.getOrCreate(uuid, username, address);
      session.setState(AuthSession.State.AUTHENTICATING);
      return AuthResult.success("请输入二步验证码", AuthSession.State.AUTHENTICATING);
    }
    return authenticate(uuid, username, address);
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
    return authenticate(uuid, user.username(), session.get().address());
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
    StarxUser user = optional.get();
    if (!PasswordHasher.verify(oldPassword, user.passwordHash())) {
      return AuthResult.failure("原密码错误");
    }
    StarxUser updated =
        new StarxUser(
            user.uuid(),
            user.username(),
            user.email(),
            PasswordHasher.hash(newPassword),
            user.totpSecret(),
            user.premium(),
            user.createdAt(),
            user.lastLoginAt(),
            user.externalUserId(),
            user.trustedDevices());
    userRepository.saveUser(updated);
    return AuthResult.success("密码修改成功");
  }

  public AuthResult bindTotp(UUID uuid, String password, String base32Secret) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    StarxUser user = optional.get();
    if (!PasswordHasher.verify(password, user.passwordHash())) {
      return AuthResult.failure("密码错误");
    }
    StarxUser updated =
        new StarxUser(
            user.uuid(),
            user.username(),
            user.email(),
            user.passwordHash(),
            base32Secret,
            user.premium(),
            user.createdAt(),
            user.lastLoginAt(),
            user.externalUserId(),
            user.trustedDevices());
    userRepository.saveUser(updated);
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
    StarxUser user = optional.get();
    if (user.trustedDevices().contains(deviceId)) {
      return AuthResult.success("设备已信任");
    }
    List<String> devices = new ArrayList<>(user.trustedDevices());
    devices.add(deviceId);
    StarxUser updated = cloneWithTrustedDevices(user, devices);
    userRepository.saveUser(updated);
    return AuthResult.success("设备已添加信任");
  }

  public AuthResult removeTrustedDevice(UUID uuid, String deviceId) {
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isEmpty()) {
      return AuthResult.failure("用户未注册");
    }
    StarxUser user = optional.get();
    List<String> devices = new ArrayList<>(user.trustedDevices());
    if (!devices.remove(deviceId)) {
      return AuthResult.failure("设备不在信任列表中");
    }
    userRepository.saveUser(cloneWithTrustedDevices(user, devices));
    return AuthResult.success("设备已取消信任");
  }

  public boolean isTrustedDevice(UUID uuid, String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return false;
    }
    return userRepository
        .findFullByUuid(uuid)
        .map(user -> user.trustedDevices().contains(deviceId))
        .orElse(false);
  }

  private AuthResult authenticate(UUID uuid, String username, InetAddress address) {
    AuthSession session = sessionManager.getOrCreate(uuid, username, address);
    session.setState(AuthSession.State.AUTHENTICATED);
    Optional<StarxUser> optional = userRepository.findFullByUuid(uuid);
    if (optional.isPresent()) {
      StarxUser user = optional.get();
      StarxUser updated =
          new StarxUser(
              user.uuid(),
              user.username(),
              user.email(),
              user.passwordHash(),
              user.totpSecret(),
              user.premium(),
              user.createdAt(),
              Instant.now(),
              user.externalUserId(),
              user.trustedDevices());
      userRepository.saveUser(updated);
    }
    eventBus.publish(EventTypes.PLAYER_LOGIN_SUCCESS, Map.of("uuid", uuid, "username", username));
    return AuthResult.success("登录成功", AuthSession.State.AUTHENTICATED);
  }

  private void publishLoginFailed(UUID uuid, String username, String reason) {
    eventBus.publish(
        EventTypes.PLAYER_LOGIN_FAILED,
        Map.of("uuid", uuid, "username", username, "reason", reason));
  }

  private StarxUser cloneWithTrustedDevices(StarxUser user, List<String> trustedDevices) {
    return new StarxUser(
        user.uuid(),
        user.username(),
        user.email(),
        user.passwordHash(),
        user.totpSecret(),
        user.premium(),
        user.createdAt(),
        user.lastLoginAt(),
        user.externalUserId(),
        trustedDevices);
  }

  private static String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return email.trim();
  }
}
