package com.github.multidestroy.bukkit.events.listeners;

import com.github.multidestroy.bukkit.Config;
import com.github.multidestroy.bukkit.i18n.Messages;
import com.github.multidestroy.bukkit.database.Database;
import com.github.multidestroy.bukkit.events.LoginAttemptEvent;
import com.github.multidestroy.bukkit.events.LoginAttemptType;
import com.github.multidestroy.bukkit.player.PlayerInfo;
import com.github.multidestroy.bukkit.system.PluginSystem;
import com.github.multidestroy.bukkit.system.ThreadSystem;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class OnJoin implements Listener {

    private final PluginSystem system;
    private final Database database;
    private final Config config;

    public OnJoin(PluginSystem system, Database database, Config config, JavaPlugin plugin, ThreadSystem passwordThreadSystem) {
        this.system = system;
        this.database = database;
        this.config = config;
    }

    /**
     * During pre-login event, player is saving into the system
     */

    @EventHandler
    public void OnPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        if (database.isConnected()) {

            switch (database.checkIpBlockade(event.getName(), event.getAddress())) {
                case -1:
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messages.getColoredString("ERROR"));
                    break;
                case 0:
                    if (!system.isPlayerInSystem(playerName)) { //Check if player was ever saved into the system
                        PlayerInfo playerFromDatabase;
                        if ((playerFromDatabase = database.getRegisteredPlayer(playerName)) != null)
                            system.saveNewPlayer(playerName, playerFromDatabase); //player is already registered
                        else
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messages.getColoredString("ERROR"));
                    }
                    break;
                case 1:
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messages.getColoredString("IP_BLOCKADE.JOIN"));
                    break;
            }


        } else
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messages.getColoredString("ERROR"));
    }

    /**
     * When player has got through the pre login event, now has to be checked whether session is ON.
     * If login session if OFF, LoginAttempt event is called.
     * Player's GameMode is set to survival.
     */

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInfo playerInfo = system.getPlayerInfo(player.getName());
        boolean isLoginSessionAvailable = config.get().getBoolean("settings.login_session.allow");
        boolean bungeeCord = config.get().getBoolean("settings.bungeecord");

        if (!isLoginSessionAvailable || !system.isLoginSession(player.getName(), player.getAddress().getAddress())) {
            if (config.get().getBoolean("settings.bungeecord"))
                playerInfo.setLoginStatus(false);


            // Set player's food, health and GameMode according to plugin settings
            if (config.get().getBoolean("settings.join.max_hunger"))
                player.setFoodLevel(20);
            if (config.get().getBoolean("settings.join.max_health"))
                player.setHealth(20);
            if (config.get().getBoolean("settings.join.gamemode.enforce"))
                player.setGameMode(GameMode.valueOf(config.get().getString("settings.join.gamemode.default")));
        }

        if (isLoginSessionAvailable && system.isLoginSession(player.getName(), player.getAddress().getAddress())) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Messages.getColoredString("SESSION.ENABLE")));
        } else {
            LoginAttemptType loginAttemptType = playerInfo.isRegistered() ? LoginAttemptType.LOGIN : LoginAttemptType.REGISTER;
            Bukkit.getPluginManager().callEvent(new LoginAttemptEvent(player, loginAttemptType));
        }
    }

    /**
     * If Player missed pre-login event kick him from the server
     */

    @EventHandler
    public void missedPreLoginEvent(PlayerLoginEvent event) {
        if (!system.isPlayerInSystem(event.getPlayer().getName()))
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Messages.getColoredString("EVENT.PRELOGIN.MISSED"));
    }

}
