# 安装步骤

## 构建

```bash
./gradlew build
```

产物：

- `starx-velocity/build/libs/starx-velocity-*-all.jar`
- `starx-paper/build/libs/starx-paper-*-all.jar`

## Velocity 端

1. 复制 jar 到 `plugins/`。
2. 可选安装 LimboAPI。
3. 启动 Velocity，生成默认配置。
4. 编辑 `plugins/starx-velocity/config.yml`。
5. 重启。

## Paper/Folia 端

1. 复制 jar 到 `plugins/`。
2. 确认 `server.properties`：

   ```properties
   online-mode=false
   velocity.enabled=true
   velocity.secret=<与 velocity.toml 一致>
   ```

3. 启动服务端，生成默认配置。
4. 按需编辑 `plugins/starx-paper/config.yml`。
5. 重启。

## 验证

1. 查看启动日志无报错。
2. 访问 `http://127.0.0.1:8788/health`。
3. 使用客户端连接并登录。

## 完整文档

详见仓库 `docs/setup/installation.md`。
