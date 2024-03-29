package me.Kyrobi.Commands;

import me.Kyrobi.Main;
import me.Kyrobi.Sqlite;
import me.Kyrobi.Tracker;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.EventListener;

import static me.Kyrobi.Main.log_actions;
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

            String logMessage = "" +
                    "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + "\n" +
                    "**Command: **" + e.getName().toLowerCase() + "\n-";
            Main.logInfo(Main.LogType.STATS_COMMAND, logMessage);
            log_actions(e.getMember(), e.getGuild(), Main.LogType.STATS_COMMAND, e.getName());

            if(sqlite.exists(authorID, serverID)){
                e.reply(author.getAsMention() + "\nLeaderboard Ranking: **#" + getPlayerLeaderboardPosition(e.getGuild().getIdLong(), e.getMember().getIdLong()) + "**\nTotal Time Spent: **" +  millisecondsToTimeStamp(sqlite.getTime(authorID, serverID)) + "**").queue();
            } else {
                e.reply("You have never been in a voice call before on this server. Please join one to start tracking your time.").queue();
            }
        }

    }

    private long getPlayerLeaderboardPosition(long guildID, long userID){
        File dbfile = new File("");
        String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;

        long rankingCounter = 0;
        try(Connection conn = DriverManager.getConnection(url)){

            PreparedStatement selectDescOrder = conn.prepareStatement(
                    "SELECT rank FROM (SELECT *, RANK() OVER (ORDER BY `time` DESC) AS `rank` FROM `stats` WHERE serverID = ?) WHERE userID = ?");
            selectDescOrder.setLong(1, guildID);
            selectDescOrder.setLong(2, userID);

            ResultSet rs = selectDescOrder.executeQuery(); // Execute the command

            rs.next();

            rankingCounter = rs.getLong(1);

            rs.close();
            conn.close();

        }
        catch(SQLException ev){
            ev.printStackTrace();
        }
        return rankingCounter;
    }
}
