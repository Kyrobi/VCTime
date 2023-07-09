package me.Kyrobi.Commands;

import me.Kyrobi.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;

import static me.Kyrobi.Main.log_actions;

public class helpCommand extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent e){
        Member author = e.getMember();
        String authorName = e.getMember().getUser().getName();

        //If bot tries to run commands, nothing will happen
        if(author.getUser().isBot()){
            return;
        }

        if(e.getName().equalsIgnoreCase("help")){
            e.reply("" +
                    "/stats - View your call time\n" +
                    "/leaderboard - View the vc leaderboard for your server\n" +
                    "\nUsers in an AFK voice channel won't have their time counted."
            ).queue();

            String logMessage = "" +
                    "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + "\n" +
                    "**Command: **" + e.getName().toLowerCase() + "\n-";
            Main.logInfo(Main.LogType.HELP_COMMAND, logMessage);

            log_actions(e.getMember(), e.getGuild(), Main.LogType.HELP_COMMAND, e.getName());
        }
    }
}
