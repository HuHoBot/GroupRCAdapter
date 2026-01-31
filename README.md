# HuHoBot GroupRCAdapter

[![GitHub Release](https://img.shields.io/github/v/release/HuHoBot/GroupRCAdapter?style=for-the-badge)](https://github.com/HuHoBot/GroupRCAdapter/releases)
[![License](https://img.shields.io/github/license/HuHoBot/GroupRCAdapter?style=for-the-badge)](https://github.com/HuHoBot/GroupRCAdapter/blob/main/LICENSE)
[![Build Status](https://img.shields.io/github/actions/workflow/status/HuHoBot/GroupRCAdapter/build.yml?style=for-the-badge)](https://github.com/HuHoBot/GroupRCAdapter/actions)

HuHoBot 的 群组子服适配器，通过 Redis 实现 Velocity 与子服之间的跨服命令通讯。

## 功能特性

- ✅ **Redis 通讯**: 基于 Redis 的跨服消息传递
- ✅ **命令执行**: 支持远程执行控制台命令
- ✅ **实时反馈**: 实时捕获并返回命令执行输出
- ✅ **广播支持**: 支持向所有子服广播命令
- ✅ **日志捕获**: 自动捕获控制台日志输出
- ✅ **跨版本兼容**: 支持 Spigot 1.16+ 所有版本

## 快速开始

### 1. 下载插件

从 [Releases](https://github.com/huohuas001/RCHuHoBot/releases) 下载最新版本的 `RCHuHoBot-x.x.x-Spigot.jar`

### 2. 安装插件

将 jar 文件放入服务器的 `plugins` 目录，然后启动服务器。

### 3. 配置 Redis

首次启动后，插件会在 `plugins/RCSpigotAdapter/config.yml` 生成配置文件。

**配置请参考文档**: https://huhobot.txssb.cn/

基础配置示例：
```yaml
server-name: "survival"  # 当前服务器名称

debug: false  # 是否开启调试模式

redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0
  timeout: 2000
  command-channel: "HuHoBotChannel"
  pool:
    max-total: 8
    max-idle: 8
    min-idle: 0
```

### 4. 重启服务器

修改配置后，执行 `/huhobot reload` 或重启服务器使配置生效。

## 命令使用

### 主命令

```
/huhobot [status|reconnect|reload|help]
```

**别名**: `/hb`, `/huho`

### 子命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/huhobot status` | 查看 Redis 连接状态和插件信息 | `huhobot.status` |
| `/huhobot reconnect` | 重新连接 Redis 服务器 | `huhobot.admin` |
| `/huhobot reload` | 重载配置文件 | `huhobot.admin` |
| `/huhobot help` | 显示帮助信息 | 所有人 |

## 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `huhobot.status` | 允许查看状态 | 所有人 |
| `huhobot.admin` | 允许使用管理命令 | OP |

## 工作原理

RCHuHoBot 使用 Redis 的发布/订阅（Pub/Sub）机制实现 Velocity/BungeeCord 与 Spigot 子服之间的通讯：

1. **命令通道**: Velocity 通过 Redis 发送命令到指定子服
2. **回调通道**: 子服捕获命令执行的日志并实时返回给 Velocity
3. **广播支持**: 支持向所有子服同时发送命令
- **最低 Java 版本**: Java 8

## 常见问题

### 1. 插件无法连接 Redis

**解决方案**:
- 检查 Redis 服务是否运行
- 确认配置文件中的 host 和 port 正确
- 检查防火墙是否允许连接
- 设置 `debug: true` 查看详细错误信息

### 2. 命令执行没有反馈

**解决方案**:
- 检查服务器名称是否正确配置
- 确认 Velocity 端和子服的通道名称一致
- 使用 `/huhobot status` 检查订阅器状态

### 3. 日志输出不完整

**解决方案**:
- 插件使用 Log4j2 Appender 捕获日志
- 确保服务器使用 Paper/Purpur 等支持 Log4j2 的核心

## 构建项目

```bash
# 克隆仓库
git clone https://github.com/huohuas001/RCHuHoBot.git
cd RCHuHoBot

# 构建插件
./gradlew clean build

# 生成的 jar 在 build/libs/ 目录
```

#### 查看开发文档

如果你需要更详细的开发指南和高级功能，请查阅[开发文档](framework.md)。

## 📄 开源协议

[GNU General Public License v3.0](LICENSE) - 自由使用、修改和分发，但需遵守以下条款：

- **开源义务**：任何衍生作品必须保持开源
- **相同许可**：修改后的版本必须使用相同许可证
- **版权声明**：必须保留原始版权声明

完整协议文本请查看 [LICENSE](LICENSE) 文件

## 🤝 参与贡献

欢迎提交PR或通过[Discussions](https://github.com/HuHoBot/GroupRCAdapter/discussions)提出建议

