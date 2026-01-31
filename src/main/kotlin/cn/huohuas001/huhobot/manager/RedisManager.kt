package cn.huohuas001.huhobot.manager

import org.bukkit.plugin.java.JavaPlugin
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisManager(private val plugin: JavaPlugin) {
    private var jedisPool: JedisPool? = null
    private var connected = false

    fun connect() {
        try {
            val redisConfig = plugin.config.getConfigurationSection("redis")
            if (redisConfig == null) {
                plugin.logger.severe("Redis configuration section not found in config.yml!")
                plugin.logger.severe("Please check your configuration file.")
                return
            }

            val host = redisConfig.getString("host", "localhost")!!
            val port = redisConfig.getInt("port", 6379)
            val password = redisConfig.getString("password", "")!!
            val database = redisConfig.getInt("database", 0)
            val timeout = redisConfig.getInt("timeout", 2000)

            plugin.logger.info("Attempting to connect to Redis at $host:$port (database: $database)...")

            val poolConfig = JedisPoolConfig().apply {
                val poolSection = redisConfig.getConfigurationSection("pool")
                if (poolSection != null) {
                    maxTotal = poolSection.getInt("max-total", 8)
                    maxIdle = poolSection.getInt("max-idle", 8)
                    minIdle = poolSection.getInt("min-idle", 0)
                }
            }

            jedisPool = if (password.isEmpty()) {
                JedisPool(poolConfig, host, port, timeout, null, database)
            } else {
                JedisPool(poolConfig, host, port, timeout, password, database)
            }

            // Test connection
            jedisPool?.resource?.use { jedis ->
                jedis.ping()
                connected = true
                plugin.logger.info("Successfully connected to Redis at $host:$port")
            }
        } catch (e: redis.clients.jedis.exceptions.JedisConnectionException) {
            connected = false
            val host = plugin.config.getString("redis.host", "localhost")
            val port = plugin.config.getInt("redis.port", 6379)
            plugin.logger.severe("================================================")
            plugin.logger.severe("Failed to connect to Redis server!")
            plugin.logger.severe("Host: $host:$port")
            plugin.logger.severe("Reason: ${e.message ?: "Connection refused"}")
            plugin.logger.severe("================================================")
            plugin.logger.severe("Please check:")
            plugin.logger.severe("  1. Redis server is running")
            plugin.logger.severe("  2. Host and port are correct in config.yml")
            plugin.logger.severe("  3. Firewall allows connection to Redis")
            plugin.logger.severe("================================================")
        } catch (e: redis.clients.jedis.exceptions.JedisDataException) {
            connected = false
            plugin.logger.severe("================================================")
            plugin.logger.severe("Redis authentication failed!")
            plugin.logger.severe("Reason: ${e.message}")
            plugin.logger.severe("================================================")
            plugin.logger.severe("Please check the password in config.yml")
            plugin.logger.severe("================================================")
        } catch (e: Exception) {
            connected = false
            plugin.logger.severe("================================================")
            plugin.logger.severe("Unexpected error while connecting to Redis!")
            plugin.logger.severe("Error: ${e.javaClass.simpleName}")
            plugin.logger.severe("Message: ${e.message}")
            plugin.logger.severe("================================================")
            if (plugin.config.getBoolean("debug", false)) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        jedisPool?.takeIf { !it.isClosed }?.let {
            it.close()
            connected = false
            plugin.logger.info("Disconnected from Redis")
        }
    }

    fun reconnect() {
        plugin.logger.info("Reconnecting to Redis...")
        disconnect()
        connect()
    }

    fun getJedisPool(): JedisPool? = jedisPool

    fun isConnected(): Boolean {
        if (!connected || jedisPool == null || jedisPool?.isClosed == true) {
            return false
        }

        return try {
            jedisPool?.resource?.use { it.ping() }
            true
        } catch (e: Exception) {
            connected = false
            false
        }
    }

    val serverName: String
        get() = plugin.config.getString("server-name", "Unknown")!!
}
