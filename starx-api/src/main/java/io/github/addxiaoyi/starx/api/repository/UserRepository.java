package io.github.addxiaoyi.starx.api.repository;

import io.github.addxiaoyi.starx.api.dto.UserDto;
import java.util.Optional;
import java.util.UUID;

/** 玩家账户仓库契约。Velocity 与 Paper 共享此接口，具体实现在 starx-common。 */
public interface UserRepository {

  Optional<UserDto> findByUuid(UUID uuid);

  Optional<UserDto> findByUsername(String username);

  Optional<UserDto> findByEmail(String email);

  boolean existsByUsername(String username);

  void save(UserDto user);

  void delete(UUID uuid);
}
