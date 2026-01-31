package cn.huohuas001.huhobot.redis

import cn.huohuas001.huhobot.RCSpigotAdapter
import cn.huohuas001.huhobot.spigot.commands.HybridCommandExecutor
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPubSub
import java.util.concurrent.ConcurrentHashMap

class RedisMessageListener(private val plugin: RCSpigotAdapter) : JedisPubSub() {

    private val commandChannel: String
    private val callbackChannel: String
    private val serverName: String
    private val commandExecutor = HybridCommandExecutor(plugin)
    private val activeTasks = ConcurrentHashMap<String, Boolean>()

    init {
        commandChannel = plugin.config.getString("redis.command-channel", "HuHoBotChannel")!!
        callbackChannel = "${commandChannel}_callback"
        serverName = plugin.config.getString("server-name", "Unknown")!!
    }

    override fun onMessage(channel: String, message: String) {
        if (channel != commandChannel) return

        try {
            val parts = message.split("|")

            when (parts.size) {
                2 -> handleFormatA(parts)  // 无回调命令
                3 -> handleFormatB(parts)  // 带回调命令
                else -> plugin.logger.warning("Invalid message format: $message")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error processing Redis message: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 处理格式A：普通命令（无回调/广播）
     * 格式: 目标服务器名|命令内容
     */
    private fun handleFormatA(parts: List<String>) {
        val target = parts[0]
        val command = parts[1]

        if (target == "ALL" || target == serverName) {
            plugin.logger.info("Executing broadcast command: $command")
            plugin.submit {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
        }
    }

    /**
     * 处理格式B：带回调的命令
     * 格式: 目标服务器名|任务ID|命令内容
     */
    private fun handleFormatB(parts: List<String>) {
        val target = parts[0]
        val taskId = parts[1]
        val command = parts[2]

        if (target != serverName) return

        plugin.logger.info("Executing command with callback - TaskID: $taskId, Command: $command")

        activeTasks[taskId] = true

        // 使用 HybridCommandExecutor 执行命令
        commandExecutor.execute(command).whenComplete { result, error ->
            try {
                if (error != null) {
                    // 发送错误信息
                    publishCallback(taskId, "[ERROR]", error.message ?: "Unknown error")
                    plugin.logger.severe("Error executing command: ${error.message}")
                    error.printStackTrace()
                } else {
                    // 发送所有输出
                    val output = result.getRawString()
                    if (output.isNotEmpty()) {
                        // 逐行发送输出
                        output.split("\n").forEach { line ->
                            if (line.isNotEmpty()) {
                                publishCallback(taskId, "[OUTPUT]", line)
                            }
                        }
                    }
                }

                // 发送完成信号
                publishCallback(taskId, "[COMPLETE]", "")
            } finally {
                activeTasks.remove(taskId)
            }
        }
    }

    /**
     * 发送回调消息到 Redis
     */
    private fun publishCallback(taskId: String, type: String, content: String) {
        try {
            plugin.getRedisManager().getJedisPool()?.resource?.use { jedis ->
                val message = "$taskId|$type|$content"
                jedis.publish(callbackChannel, message)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to publish callback: ${e.message}")
        }
    }

    override fun onSubscribe(channel: String, subscribedChannels: Int) {
        plugin.logger.info("Subscribed to Redis channel: $channel")
    }

    override fun onUnsubscribe(channel: String, subscribedChannels: Int) {
        plugin.logger.info("Unsubscribed from Redis channel: $channel")
    }

    fun getCommandChannel(): String = commandChannel
    fun getCallbackChannel(): String = callbackChannel

    fun shutdown() {
        activeTasks.clear()
        commandExecutor.cleanup()
    }
}
