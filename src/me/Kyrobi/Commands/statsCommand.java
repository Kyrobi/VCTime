package me.Kyrobi.Commands;

import me.Kyrobi.Sqlite;
import me.Kyrobi.Tracker;
import me.Kyrobi.botUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.util.EventListener;

import static me.Kyrobi.Main.millisecondsToTimeStamp;

public class statsCommand extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent e){

        System.out.println("Slash command");

        Member author = e.getMember();
        String authorName = e.getMember().getUser().getName();

        //If bot tries to run commands, nothing will happen
        if(author.getUser().isBot()){
            return;
        }


        //Command to see your own stats
        if(e.getName().equalsIgnoreCase("stats")){
            //System.out.println("Getting stats");

            System.out.println("Getting status");
            Sqlite sqlite = new Sqlite();
            long authorID = Long.parseLong(author.getId());
            long serverID = Long.parseLong(e.getGuild().getId());

            //Update the stats right as user does /stats to give an illusion of real time update
            if(Tracker.joinTracker.containsKey(e.getMember().getIdLong())){
                Tracker.saveStats(e.getMember());
                Tracker.startStats(e.getMember());
            }


            botUtils fileWrite = new botUtils();

            try {
                fileWrite.writeToFile("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: stats`");
                fileWrite.writeToFileCommand("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: stats`");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            e.reply(author.getAsMention() + "'s total time spent in vc: " + millisecondsToTimeStamp(sqlite.getTime(authorID, serverID))).queue();
        }

    }
}
