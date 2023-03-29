package me.Kyrobi.Commands;

import me.Kyrobi.botUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;

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
                    "\nUsers in a voice channel named `AFK` won't have their time counted."
            ).queue();

            botUtils fileWrite = new botUtils();
            try {
                fileWrite.writeToFile("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: help`");
                fileWrite.writeToFileCommand("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: help`");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
