package cn.huohuas001.huhobot.command

import cn.huohuas001.huhobot.RCSpigotAdapter
import cn.huohuas001.huhobot.manager.RedisManager
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class HuhobotCommand(
    private val plugin: RCSpigotAdapter,
    private val redisManager: RedisManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "status" -> handleStatus(sender)
            "reconnect" -> handleReconnect(sender)
            "reload" -> handleReload(sender)
            "help" -> sendHelp(sender)
            else -> sender.sendMessage("${ChatColor.RED}Unknown subcommand. Use /huhobot help for help.")
        }
        return true
    }

    private fun handleStatus(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== HuHoBot Status ===")
        sender.sendMessage("${ChatColor.YELLOW}Server Name: ${ChatColor.WHITE}${redisManager.serverName}")

        val connected = redisManager.isConnected()
        val status = if (connected) "${ChatColor.GREEN}Connected" else "${ChatColor.RED}Disconnected"
        sender.sendMessage("${ChatColor.YELLOW}Redis Status: $status")

        if (connected) {
            val host = plugin.config.getString("redis.host")
            val port = plugin.config.getInt("redis.port")
            sender.sendMessage("${ChatColor.YELLOW}Redis Info: ${ChatColor.WHITE}$host:$port")

            val subscriber = plugin.getRedisSubscriber()
            val subStatus = if (subscriber?.isRunning() == true) {
                "${ChatColor.GREEN}Running"
            } else {
                "${ChatColor.RED}Not Running"
            }
            sender.sendMessage("${ChatColor.YELLOW}Subscriber Status: $subStatus")

            val commandChannel = plugin.config.getString("redis.command-channel", "HuHoBotChannel")
            val callbackChannel = plugin.config.getString("redis.callback-channel", "${commandChannel}_callback")
            sender.sendMessage("${ChatColor.YELLOW}Command Channel: ${ChatColor.WHITE}$commandChannel")
            sender.sendMessage("${ChatColor.YELLOW}Callback Channel: ${ChatColor.WHITE}$callbackChannel")
        }
    }

    private fun handleReconnect(sender: CommandSender) {
        if (!sender.hasPermission("huhobot.admin")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        sender.sendMessage("${ChatColor.YELLOW}Reconnecting to Redis...")

        // Stop subscriber first
        plugin.getRedisSubscriber()?.stop()

        // Reconnect to Redis
        redisManager.reconnect()

        if (redisManager.isConnected()) {
            sender.sendMessage("${ChatColor.GREEN}Successfully reconnected to Redis!")

            // Restart subscriber
            plugin.getRedisSubscriber()?.start() ?: run {
                sender.sendMessage("${ChatColor.YELLOW}Restarting subscriber...")
            }
        } else {
            sender.sendMessage("${ChatColor.RED}Failed to reconnect to Redis. Check console for errors.")
        }
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("huhobot.admin")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        sender.sendMessage("${ChatColor.YELLOW}Reloading configuration...")
        plugin.reloadConfig()
        redisManager.reconnect()
        sender.sendMessage("${ChatColor.GREEN}Configuration reloaded!")
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== HuHoBot Commands ===")
        sender.sendMessage("${ChatColor.YELLOW}/huhobot status${ChatColor.WHITE} - Show Redis connection status")
        sender.sendMessage("${ChatColor.YELLOW}/huhobot reconnect${ChatColor.WHITE} - Reconnect to Redis")
        sender.sendMessage("${ChatColor.YELLOW}/huhobot reload${ChatColor.WHITE} - Reload configuration")
        sender.sendMessage("${ChatColor.YELLOW}/huhobot help${ChatColor.WHITE} - Show this help message")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val completions = mutableListOf("status", "help")
            if (sender.hasPermission("huhobot.admin")) {
                completions.addAll(listOf("reconnect", "reload"))
            }

            val input = args[0].lowercase()
            return completions.filter { it.startsWith(input) }
        }
        return emptyList()
    }
}
