package me.Kyrobi.Commands;


import me.Kyrobi.Main;
import me.Kyrobi.botUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.EventListener;
import java.util.concurrent.TimeUnit;

import static me.Kyrobi.Main.millisecondsToTimeStamp;

public class leaderboardCommand extends ListenerAdapter{

    @Override
    public void onSlashCommand(SlashCommandEvent e){
        Member author = e.getMember();
        String authorName = e.getMember().getUser().getName();

        //If bot tries to run commands, nothing will happen
        if(author.getUser().isBot()){
            return;
        }

        if(e.getName().equalsIgnoreCase("leaderboard")){

            OptionMapping leaderboardOption = e.getOption("page");
            long page = leaderboardOption.getAsLong();

            botUtils fileWrite = new botUtils();
            try {
                fileWrite.writeToFile("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: leaderboard`");
                fileWrite.writeToFileCommand("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: leaderboard`");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            File dbfile = new File("");
            String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;
            //String selectDescOrder = "SELECT * FROM `stats` WHERE serverID = ? ORDER BY `time` DESC LIMIT 25";


            StringBuilder stringBuilder1 = new StringBuilder();
            EmbedBuilder eb = new EmbedBuilder();

            //int maxAmount = 20;
            int ranking = 1;
            try{
                Connection conn = DriverManager.getConnection(url); // Make connection

                PreparedStatement selectDescOrder = conn.prepareStatement("SELECT * FROM `stats` WHERE serverID = ? ORDER BY `time` DESC LIMIT 25");
                selectDescOrder.setLong(1, e.getGuild().getIdLong());

                ResultSet rs = selectDescOrder.executeQuery(); // Execute the command


                //We loop through the database. If the userID matches, we break out of the loop
                while(rs.next()){
                    long userId = rs.getLong("userID");

                    //stringBuilder1.append("\n`#" + ranking++ + "` **" + rs.getInt("amount") + "** - " + (toUser(userId)).getAsMention());
                    stringBuilder1.append("\n**#" + ranking++ + "** " + "<@" + userId + "> " + millisecondsToTimeStamp(rs.getLong("time")));
                }
                rs.close();
                conn.close();

            }
            catch(SQLException ev){
                ev.printStackTrace();
                System.out.println("Error code: " + ev.getMessage());
            }

            //We take the final string and post it into the field
            eb.addField("Voice call leaderboard [Top 25]", stringBuilder1.toString(), true);

            //e.getChannel().sendMessageEmbeds(eb.build()).queue();
            e.replyEmbeds(eb.build()).queue();

        }
    }
}
