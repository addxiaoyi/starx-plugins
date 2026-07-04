package io.github.addxiaoyi.starx.velocity.http.admin;

import io.javalin.Javalin;

/** 管理后台 HTTP 路由处理器契约。 */
public interface AdminHandler {

  /** 在 Javalin 应用中注册本处理器负责的路由。 */
  void register(Javalin app);
}
