package cn.huohuas001.huhobot.redis

import cn.huohuas001.huhobot.RCSpigotAdapter
import redis.clients.jedis.Jedis
import java.util.concurrent.atomic.AtomicBoolean

class RedisSubscriber(private val plugin: RCSpigotAdapter) {

    private val listener = RedisMessageListener(plugin)
    private var subscriberThread: Thread? = null
    private val running = AtomicBoolean(false)

    fun start() {
        if (running.get()) {
            plugin.logger.warning("Redis subscriber is already running!")
            return
        }

        running.set(true)
        subscriberThread = Thread({
            try {
                plugin.logger.info("Starting Redis subscriber thread...")

                while (running.get()) {
                    try {
                        plugin.getRedisManager().getJedisPool()?.resource?.use { jedis ->
                            plugin.logger.info("Subscribing to channel: ${listener.getCommandChannel()}")
                            // 这个调用会阻塞直到取消订阅
                            jedis.subscribe(listener, listener.getCommandChannel())
                        }
                    } catch (e: Exception) {
                        if (running.get()) {
                            plugin.logger.severe("Redis subscriber error: ${e.message}")
                            e.printStackTrace()
                            // 等待后重试
                            Thread.sleep(5000)
                        }
                    }
                }
            } catch (e: InterruptedException) {
                plugin.logger.info("Redis subscriber thread interrupted")
            } finally {
                plugin.logger.info("Redis subscriber thread stopped")
            }
        }, "RedisSubscriber-Thread")

        subscriberThread?.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        if (!running.get()) {
            return
        }

        plugin.logger.info("Stopping Redis subscriber...")
        running.set(false)

        // 取消订阅
        try {
            listener.unsubscribe()
            listener.shutdown()
        } catch (e: Exception) {
            plugin.logger.warning("Error unsubscribing: ${e.message}")
        }

        // 等待线程结束
        subscriberThread?.let {
            try {
                it.interrupt()
                it.join(3000)
            } catch (e: InterruptedException) {
                plugin.logger.warning("Error joining subscriber thread: ${e.message}")
            }
        }

        subscriberThread = null
    }

    fun isRunning(): Boolean = running.get()
}
