package me.Kyrobi;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.sql.*;
import java.util.concurrent.TimeUnit;


public class Commands extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent e){

        System.out.println("Slash command");

        Member author = e.getMember();
        String authorName = e.getName();

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

            botUtils fileWrite = new botUtils();

            try {
                fileWrite.writeToFile("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: stats`");
                fileWrite.writeToFileCommand("`" + e.getGuild().getName() + "`" + " | **" + authorName + "** | `issued command: stats`");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            e.reply(author.getAsMention() + "'s total time spent in vc: " + millisecondsToTimeStamp(sqlite.getTime(authorID, serverID))).queue();
        }

        //Leaderboard command
        if(e.getName().equalsIgnoreCase("leaderboard")){

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

        if(e.getName().equalsIgnoreCase("help")){
            e.reply("" +
                    "/stats - View your call time\n" +
                    "/leaderboard - View the vc leaderboard for your server\n" +
                    "\nUsers in a voice channel called AFK won't have their time counted." +
                    "\nYour stats will update when you leave the voice call."
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


    @Override
    public void onMessageReceived(MessageReceivedEvent e){

        //If bot tries to run commands, nothing will happen
        if(e.getAuthor().isBot()){
            return;
        }

        String[] args = e.getMessage().getContentRaw().split(" ");

        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase("listservers"))){

            //For me to see all the server the bot is in
            if(e.getAuthor().getIdLong() == 559428414709301279L){

                System.out.println("Ordering server list");
                StringBuilder str = new StringBuilder();

                str.append("In ").append(Main.jda.getGuilds().size()).append(" servers!\n");
                for(Guild a: Main.jda.getGuilds()){
                    str.append("`" + a.getName() + " " + a.getMemberCount() + "`" + "\n");
                    //System.out.println("- " + a.getName() + ": " + a.getMemberCount() + " members");
                }

                e.getChannel().sendMessage(String.valueOf(str)).queue();
            }
        }
    }




    public String millisecondsToTimeStamp(long durationInMillis) {

        //Reference: https://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java

        final long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        final long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(durationInMillis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(durationInMillis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02dh %02dm", hr, min);
    }

}
