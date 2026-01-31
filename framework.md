# HuHoBot Redis 通讯协议架构文档

本文档描述了 HuHoBot Velocity 端与子服务器（Spigot/Bungee 等）之间的 Redis 通讯协议，用于实现跨服命令执行及结果回调。

## 1. 基础配置

### Redis 通道 (Channels)

#### 命令通道 (Command Channel)
- **名称**: 默认为 `HuHoBotChannel` (在配置文件中可修改)
- **用途**: Velocity 向子服发送指令
- **方向**: Velocity → 子服

#### 回调通道 (Callback Channel)
- **名称**: 默认为 `<命令通道名>_callback` (例如 `HuHoBotChannel_callback`)
- **用途**: 子服向 Velocity 返回执行结果（控制台输出、完成状态）
- **方向**: 子服 → Velocity

---

## 2. 消息协议格式

所有消息均为字符串，使用 `|` (竖线) 作为分隔符。

### 2.1 Velocity 发送给子服的消息 (命令通道)

子服务器需要订阅**命令通道**，并解析收到的消息。消息有两种格式：

#### 格式 A：普通命令 (无回调/广播)

用于不需要等待结果的命令或广播。

**格式**:
```
目标服务器名|命令内容
```

**参数说明**:
- **目标服务器名**: 指定执行命令的服务器名称（例如 `survival`）。如果是 `ALL`，则所有子服都应执行。
- **命令内容**: 要在控制台执行的具体指令（不带 `/`）。

**处理逻辑**:
1. 检查 `目标服务器名` 是否匹配当前服务器名称，或者是 `ALL`
2. 如果匹配，直接在控制台执行 `命令内容`
3. 不需要向 Redis 发送任何回复

**示例**:
```
survival|say Hello World
ALL|whitelist reload
```

---

#### 格式 B：带回调的命令 (需返回结果)

用于 `/huhobot redis exec` 或控制台交互，需要实时获取子服控制台输出。

**格式**:
```
目标服务器名|任务ID|命令内容
```

**参数说明**:
- **目标服务器名**: 指定执行命令的服务器名称
- **任务ID (taskId)**: 一个 UUID 字符串，用于标识本次执行请求
- **命令内容**: 要执行的指令

**处理逻辑**:
1. 检查 `目标服务器名` 是否匹配当前服务器名称
2. 如果匹配，开始执行命令
3. 必须捕获命令执行期间产生的日志/控制台输出，并通过**回调通道**发回（见 2.2）

**示例**:
```
survival|uuid-1234-5678-90ab|say Hello
```

---

### 2.2 子服发回给 Velocity 的消息 (回调通道)

仅当收到**格式 B** (带回调) 的命令时，子服才需要向**回调通道**发送消息。

**消息格式**:
```
任务ID|类型|内容
```

**参数说明**:
- **任务ID**: 对应收到的请求中的 `taskId`
- **类型**: 标识消息类别，固定为以下三种之一：
  - `[OUTPUT]`: 一行控制台输出日志
  - `[ERROR]`: 执行出错或异常信息
  - `[COMPLETE]`: 命令执行结束的信号
- **内容**: 具体的日志内容或空字符串

**示例**:
```
uuid-1234|[OUTPUT]|[Server] Hello World
uuid-1234|[OUTPUT]|Command executed successfully
uuid-1234|[COMPLETE]|
```

---

### 交互流程示例

假设 Velocity 发送命令：
```
survival|uuid-1234|say Hello
```

**子服 (survival) 的处理流程**:

1. **收到命令**: 识别出目标是自己，`taskId` 是 `uuid-1234`

2. **执行并捕获输出**:
   - 监听到控制台输出了 `"[Server] Hello"`
   - 发送 Redis 消息:
     ```
     uuid-1234|[OUTPUT]|[Server] Hello
     ```

3. **执行结束**:
   - 命令线程结束
   - 发送 Redis 消息:
     ```
     uuid-1234|[COMPLETE]|Done
     ```

---

## 3. 子服适配器开发建议

如果你正在编写子服插件（如 Spigot 插件），建议遵循以下步骤：

### 步骤 1: Redis 连接
启动时连接 Redis，并订阅**命令通道**。

### 步骤 2: 消息监听
- 解析消息，判断参数数量是 2 个还是 3 个
- 如果是 3 个参数（带 `taskId`），说明需要回调

### 步骤 3: 日志捕获
- 在执行命令前，注册一个 `LogAppender` 或 `Filter` 来拦截控制台日志
- 将拦截到的日志实时通过 `publish` 发送到**回调通道** (类型为 `[OUTPUT]`)

### 步骤 4: 资源清理
- 命令执行完毕后，务必发送 `[COMPLETE]` 类型的消息，否则 Velocity 端会一直等待直到超时
- 移除 `LogAppender`，防止内存泄漏

---

## 4. 示例代码逻辑 (伪代码)

```kotlin
fun onRedisMessage(channel: String, message: String) {
    val parts = message.split("|")

    // 情况1: 无回调 (Format A)
    if (parts.size == 2) {
        val target = parts[0]
        val cmd = parts[1]

        if (target == "ALL" || target == myServerName) {
            server.dispatchCommand(console, cmd)
        }
    }

    // 情况2: 带回调 (Format B)
    else if (parts.size == 3) {
        val target = parts[0]
        val taskId = parts[1]
        val cmd = parts[2]

        if (target == myServerName) {
            // 1. 开始捕获日志
            val loggerListener = { logLine ->
                redis.publish(callbackChannel, "$taskId|[OUTPUT]|$logLine")
            }
            addConsoleLogger(loggerListener)

            try {
                // 2. 执行命令
                server.dispatchCommand(console, cmd)
            } catch (e: Exception) {
                redis.publish(callbackChannel, "$taskId|[ERROR]|${e.message}")
            } finally {
                // 3. 结束并清理
                removeConsoleLogger(loggerListener)
                redis.publish(callbackChannel, "$taskId|[COMPLETE]|")
            }
        }
    }
}
```

---

## 5. 注意事项

### 5.1 线程安全
- 日志捕获需要在多线程环境下工作
- 使用线程安全的集合存储日志

### 5.2 超时处理
- Velocity 端应设置超时时间（建议 30 秒）
- 子服应确保在合理时间内完成命令执行

### 5.3 错误处理
- 所有异常都应捕获并通过 `[ERROR]` 消息返回
- 即使出错也必须发送 `[COMPLETE]` 消息

### 5.4 性能考虑
- 避免在日志捕获中执行耗时操作
- 使用异步方式发送 Redis 消息（如果可能）

---

## 6. 协议版本

**当前版本**: 1.0

**修订历史**:
- 2025-01-31: 初始版本
