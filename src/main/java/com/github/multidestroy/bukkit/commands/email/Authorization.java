package com.github.multidestroy.bukkit.commands.email;

import com.github.multidestroy.bukkit.EmailSender;
import com.github.multidestroy.bukkit.i18n.Messages;
import com.github.multidestroy.bukkit.system.ThreadSystem;
import org.bukkit.entity.Player;

public interface Authorization {

    default Runnable sendEmailAuthorization(Player player, String email, ThreadSystem threadSystem, EmailSender emailSender) {
        return () -> {
            if (threadSystem.tryLock(player.getName())) {
                try {
                    if (emailSender.sendEmail())
                        player.sendMessage(Messages.getColoredString("EMAIL.AUTHORIZATION"));
                    else
                        player.sendMessage(Messages.getColoredString("ERROR"));
                } finally {
                    threadSystem.unlock(player.getName());
                }
            } else
                player.sendMessage(Messages.getColoredString("COMMAND.THREAD.LOCK"));

        };
    }
}
