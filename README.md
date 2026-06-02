# Sub Converter

Android 端 mihomo (Clash Meta) 订阅转换与管理工具。在手机本地运行 HTTP 服务，将多个订阅源聚合、筛选后输出统一的 mihomo 配置。

## 功能

- **订阅源管理** — 添加多个 mihomo 订阅 URL，查看流量、到期信息，支持自动定时刷新
- **正则筛选** — 对节点名称设置保留/排除正则，带实时预览匹配结果
- **模板系统** — 管理多个 YAML 模板，支持远程 URL 自动拉取更新
- **输出配置** — 将多个订阅源 + 模板组合为输出订阅，其他客户端直接订阅
- **本地 HTTP 服务** — 在手机上启动 HTTP Server，局域网内其他设备可直接拉取配置
- **自动识别** — 从订阅响应头自动获取名称 (`profile-title`)、官网地址 (`profile-web-page-url`)，输出时携带标准头

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Room (数据库) + DataStore (设置)
- OkHttp (网络请求) + SnakeYAML (YAML 解析)
- WorkManager (定时刷新) + Coroutines/Flow

## 构建

```bash
./gradlew assembleDebug
```

Debug APK 输出路径: `app/build/outputs/apk/debug/`

## 使用

1. 添加订阅源（URL），首次刷新自动获取名称和流量信息
2. 创建 YAML 模板（或使用默认 mihomo 模板）
3. 创建输出配置，选择订阅源和模板
4. 启动 HTTP 服务，用其他客户端订阅输出的 URL

## 输出示例

```
http://<IP>:9876/subscriptions/1.yaml?token=xxx
```

响应头包含:
- `Profile-Title` — 订阅名称
- `Profile-Web-Page-Url` — 官网地址
- `Profile-Update-Interval` — 更新间隔（小时）
- `Subscription-Userinfo` — 流量信息
- `Content-Disposition` — 文件名

## 许可

MIT
