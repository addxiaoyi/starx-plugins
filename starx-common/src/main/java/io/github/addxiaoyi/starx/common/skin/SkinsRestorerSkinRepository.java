package io.github.addxiaoyi.starx.common.skin;

import io.github.addxiaoyi.starx.api.dto.SkinDto;
import io.github.addxiaoyi.starx.api.repository.SkinRepository;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基于反射的 SkinsRestorer 皮肤仓库实现。
 *
 * <p>项目当前未引入 SkinsRestorer 依赖，因此通过反射调用 {@code SkinsRestorerProvider.get()} 获取 API，并在类不存在时提供降级。
 */
public final class SkinsRestorerSkinRepository implements SkinRepository {

  private static final Logger LOGGER =
      Logger.getLogger(SkinsRestorerSkinRepository.class.getName());

  private static final String PROVIDER_CLASS = "net.skinsrestorer.api.SkinsRestorerProvider";

  private final boolean available;
  private final Object playerStorage;
  private final Object skinStorage;

  /** 尝试连接 SkinsRestorer API；不可用时不抛出异常。 */
  public SkinsRestorerSkinRepository() {
    Object api = null;
    Object playerStorageTmp = null;
    Object skinStorageTmp = null;
    boolean ok = false;
    try {
      Class<?> providerClass = Class.forName(PROVIDER_CLASS);
      Method get = providerClass.getMethod("get");
      api = get.invoke(null);
      playerStorageTmp = invoke(api, "getPlayerStorage");
      skinStorageTmp = invoke(api, "getSkinStorage");
      ok = true;
    } catch (ClassNotFoundException e) {
      LOGGER.fine("SkinsRestorer not available, skin bridge will degrade gracefully.");
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.WARNING, "Failed to initialize SkinsRestorer API", e);
    }

    this.available = ok;
    this.playerStorage = playerStorageTmp;
    this.skinStorage = skinStorageTmp;
  }

  @Override
  public Optional<SkinDto> findByPlayer(UUID uuid, String name) {
    if (!available) {
      return Optional.empty();
    }

    try {
      Optional<String> skinId = invokeOptional(playerStorage, "getSkinIdOfPlayer", uuid);
      if (skinId.isEmpty()) {
        return Optional.empty();
      }

      Optional<?> skinData = invokeOptional(skinStorage, "getSkinDataByIdentifier", skinId.get());
      if (skinData.isEmpty()) {
        return Optional.of(new SkinDto(uuid, name, skinId.get(), null, null, null));
      }

      Object data = skinData.get();
      String value = (String) invoke(data, "getValue");
      String signature = (String) invoke(data, "getSignature");
      return Optional.of(new SkinDto(uuid, name, skinId.get(), value, signature, null));
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.WARNING, "Failed to read skin for " + uuid, e);
      return Optional.empty();
    }
  }

  @Override
  public void setSkinId(UUID uuid, String skinId) {
    if (!available) {
      return;
    }
    try {
      invokeVoid(playerStorage, "setSkinIdOfPlayer", uuid, skinId);
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.WARNING, "Failed to set skin id for " + uuid, e);
    }
  }

  @Override
  public void setSkinData(UUID uuid, String value, String signature) {
    if (!available) {
      return;
    }
    try {
      Optional<String> existing = invokeOptional(playerStorage, "getSkinIdOfPlayer", uuid);
      String skinId = existing.orElseGet(() -> "starx-" + uuid.toString().replace("-", ""));
      invokeVoid(skinStorage, "setSkinData", skinId, value, signature);
      invokeVoid(playerStorage, "setSkinIdOfPlayer", uuid, skinId);
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.WARNING, "Failed to set skin data for " + uuid, e);
    }
  }

  @Override
  public void clearSkin(UUID uuid) {
    if (!available) {
      return;
    }
    try {
      invokeVoid(playerStorage, "removeSkinIdOfPlayer", uuid);
    } catch (ReflectiveOperationException e) {
      LOGGER.log(Level.WARNING, "Failed to clear skin for " + uuid, e);
    }
  }

  private static Object invoke(Object target, String methodName, Object... args)
      throws ReflectiveOperationException {
    Method method = findMethod(target.getClass(), methodName, args);
    return method.invoke(target, args);
  }

  private static void invokeVoid(Object target, String methodName, Object... args)
      throws ReflectiveOperationException {
    Method method = findMethod(target.getClass(), methodName, args);
    method.invoke(target, args);
  }

  @SuppressWarnings("unchecked")
  private static <T> Optional<T> invokeOptional(Object target, String methodName, Object... args)
      throws ReflectiveOperationException {
    Method method = findMethod(target.getClass(), methodName, args);
    return (Optional<T>) method.invoke(target, args);
  }

  private static Method findMethod(Class<?> clazz, String name, Object... args)
      throws NoSuchMethodException {
    for (Method method : clazz.getMethods()) {
      if (!method.getName().equals(name)) {
        continue;
      }
      Class<?>[] paramTypes = method.getParameterTypes();
      if (paramTypes.length != args.length) {
        continue;
      }
      boolean matches = true;
      for (int i = 0; i < paramTypes.length; i++) {
        if (args[i] != null && !wrap(paramTypes[i]).isInstance(args[i])) {
          matches = false;
          break;
        }
      }
      if (matches) {
        return method;
      }
    }
    throw new NoSuchMethodException(name + " in " + clazz);
  }

  private static Class<?> wrap(Class<?> type) {
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    return type;
  }
}
