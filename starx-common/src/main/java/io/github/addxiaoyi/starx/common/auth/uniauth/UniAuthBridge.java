package io.github.addxiaoyi.starx.common.auth.uniauth;

import io.github.addxiaoyi.starx.common.crypto.PasswordHasher;
import io.github.addxiaoyi.starx.common.database.JdbiUserRepository;
import io.github.addxiaoyi.starx.common.model.StarxUser;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UniAuth 桥接服务。
 *
 * <p>在桥接模式下，首登用户的密码会先通过 UniAuth 验证，验证成功后存储到本地数据库，后续登录直接使用本地密码。
 */
public final class UniAuthBridge {

  private static final Logger logger = Logger.getLogger(UniAuthBridge.class.getName());
  private static final String SOURCE_SYSTEM_STARVC = "starvc";

  private final UniAuthConfig config;
  private final UniAuthClient client;
  private final JdbiUserRepository userRepository;

  public UniAuthBridge(
      UniAuthConfig config, UniAuthClient client, JdbiUserRepository userRepository) {
    this.config = Objects.requireNonNull(config, "config");
    this.client = Objects.requireNonNull(client, "client");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
  }

  /**
   * 尝试验证用户，优先使用本地密码，失败时回退到 UniAuth。
   *
   * @param uuid 用户 UUID
   * @param username 用户名
   * @param password 密码
   * @return 异步验证结果
   */
  public CompletableFuture<BridgeResult> authenticate(UUID uuid, String username, String password) {
    Optional<StarxUser> userOpt = userRepository.findFullByUsername(username);

    if (userOpt.isPresent()) {
      StarxUser user = userOpt.get();
      if ("completed".equals(user.migrationState()) && user.passwordHash() != null) {
        return authenticateLocally(user, password);
      } else {
        return authenticateWithUniAuthAndMigrate(uuid, username, password, user);
      }
    } else {
      return authenticateWithUniAuthAndCreate(uuid, username, password);
    }
  }

  private CompletableFuture<BridgeResult> authenticateLocally(StarxUser user, String password) {
    if (PasswordHasher.verify(password, user.passwordHash())) {
      return CompletableFuture.completedFuture(
          new BridgeResult(true, "Login successful (local)", user));
    } else {
      return CompletableFuture.completedFuture(new BridgeResult(false, "Invalid password", null));
    }
  }

  private CompletableFuture<BridgeResult> authenticateWithUniAuthAndMigrate(
      UUID uuid, String username, String password, StarxUser existingUser) {
    return client
        .login(username, password)
        .thenApply(
            response -> {
              if (response.success()) {
                try {
                  String hashedPassword = PasswordHasher.hash(password);
                  userRepository.updatePassword(uuid, hashedPassword);
                  userRepository.updateMigrationState(uuid, "completed");
                  userRepository.updatePasswordMigratedAt(uuid, Instant.now());

                  Optional<StarxUser> updatedUserOpt = userRepository.findFullByUsername(username);
                  StarxUser updatedUser = updatedUserOpt.orElse(null);
                  logger.log(Level.INFO, "User {0} migrated from StarVC to local auth", username);
                  return new BridgeResult(
                      true, "Login successful (migrated from StarVC)", updatedUser);
                } catch (Exception e) {
                  logger.log(
                      Level.WARNING, "Failed to migrate user " + username + " to local auth", e);
                  return new BridgeResult(
                      true, "Login successful (from StarVC, migration failed)", existingUser);
                }
              } else {
                return new BridgeResult(
                    false,
                    response.message() != null ? response.message() : "Authentication failed",
                    null);
              }
            });
  }

  private CompletableFuture<BridgeResult> authenticateWithUniAuthAndCreate(
      UUID uuid, String username, String password) {
    return client
        .login(username, password)
        .thenApply(
            response -> {
              if (response.success()) {
                try {
                  String hashedPassword = PasswordHasher.hash(password);
                  String email = response.email();

                  StarxUser newUser =
                      new StarxUser(
                          uuid,
                          username,
                          email,
                          hashedPassword,
                          null,
                          false,
                          Instant.now(),
                          null,
                          null,
                          null,
                          null,
                          SOURCE_SYSTEM_STARVC,
                          "completed",
                          Instant.now());

                  userRepository.create(newUser);

                  logger.log(Level.INFO, "User {0} created from StarVC", username);
                  return new BridgeResult(true, "Login successful (created from StarVC)", newUser);
                } catch (Exception e) {
                  logger.log(
                      Level.WARNING, "Failed to create user " + username + " from StarVC", e);
                  return new BridgeResult(
                      true, "Login successful (from StarVC, user creation failed)", null);
                }
              } else {
                return new BridgeResult(
                    false,
                    response.message() != null ? response.message() : "Authentication failed",
                    null);
              }
            });
  }

  /**
   * 桥接验证结果。
   *
   * @param success 是否成功
   * @param message 消息
   * @param user 用户对象（如果成功）
   */
  public record BridgeResult(boolean success, String message, StarxUser user) {}
}
