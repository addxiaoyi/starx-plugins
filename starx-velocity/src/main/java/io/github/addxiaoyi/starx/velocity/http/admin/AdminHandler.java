package io.github.addxiaoyi.starx.velocity.http.admin;

import io.github.addxiaoyi.starx.velocity.http.RouteRegistrar;

@FunctionalInterface
public interface AdminHandler {

  void register(RouteRegistrar routes);
}
