package cn.huohuas001.huhobot

import cn.huohuas001.huhobot.command.HuhobotCommand
import cn.huohuas001.huhobot.manager.RedisManager
import cn.huohuas001.huhobot.redis.RedisSubscriber
import org.bukkit.plugin.java.JavaPlugin
import com.github.Anon8281.universalScheduler.UniversalScheduler
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask

class RCSpigotAdapter : JavaPlugin() {
    private lateinit var redisManager: RedisManager
    private lateinit var redisSubscriber: RedisSubscriber
    lateinit var scheduler: TaskScheduler

    fun submit(task: Runnable): MyScheduledTask? {
        return scheduler.runTask(task)
    }

    override fun onEnable() {
        // Save default config
        saveDefaultConfig()

        logger.info("================================================")
        logger.info("  RCSpigotAdapter - Redis Communication Plugin")
        logger.info("  Version: ${description.version}")
        logger.info("================================================")

        // Initialize scheduler
        scheduler = UniversalScheduler.getScheduler(this)
        logger.info("Task scheduler initialized")

        // Initialize Redis Manager
        redisManager = RedisManager(this)
        redisManager.connect()

        // Initialize Redis Subscriber
        if (redisManager.isConnected()) {
            redisSubscriber = RedisSubscriber(this)
            redisSubscriber.start()
            logger.info("Redis subscriber started successfully")
        } else {
            logger.warning("================================================")
            logger.warning("Plugin enabled in LIMITED MODE")
            logger.warning("Redis connection failed - Commands will not work!")
            logger.warning("Use /huhobot reconnect to retry connection")
            logger.warning("================================================")
        }

        // Register commands
        val huhobotCommand = HuhobotCommand(this, redisManager)
        getCommand("huhobot")?.apply {
            setExecutor(huhobotCommand)
            tabCompleter = huhobotCommand
        }

        logger.info("RCSpigotAdapter enabled successfully!")
        logger.info("================================================")
    }

    override fun onDisable() {
        // Stop Redis Subscriber
        if (::redisSubscriber.isInitialized) {
            redisSubscriber.stop()
        }

        // Disconnect from Redis
        if (::redisManager.isInitialized) {
            redisManager.disconnect()
        }

        logger.info("RCSpigotAdapter has been disabled!")
    }

    fun getRedisManager(): RedisManager = redisManager

    fun getRedisSubscriber(): RedisSubscriber? {
        return if (::redisSubscriber.isInitialized) redisSubscriber else null
    }
}
