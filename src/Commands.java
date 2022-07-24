import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class Commands extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e){

        //If bot tries to run commands, nothing will happen
        if(e.getAuthor().isBot()){
            return;
        }

        String[] args = e.getMessage().getContentRaw().split(" ");

        //Command to see your own stats
        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase("stats"))){
            //System.out.println("Getting stats");
            Sqlite sqlite = new Sqlite();
            long authorID = Long.parseLong(e.getAuthor().getId());
            long serverID = Long.parseLong(e.getGuild().getId());

            botUtils fileWrite = new botUtils();
            try {
                fileWrite.writeToFile(e.getAuthor().getName() + " issued command: stats");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println(e.getAuthor().getName() + " issued command: stats");

            e.getChannel().sendMessage(e.getMessage().getAuthor().getAsMention() + "'s total time spent in vc: " + millisecondsToTimeStamp(sqlite.getTime(authorID, serverID))).queue();
        }

        //Leaderboard command
        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase("leaderboard"))){

            botUtils fileWrite = new botUtils();
            try {
                fileWrite.writeToFile(e.getAuthor().getName() + " issued command: leaderboard");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println(e.getAuthor().getName() + " issued command: leaderboard");

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

            e.getChannel().sendMessageEmbeds(eb.build()).queue();

        }

        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase("help"))){
            e.getChannel().sendMessage("" +
                    "$vc stats - View your call time\n" +
                    "$vc leaderboard - View the vc leaderboard for your server\n" +
                    "\nUsers in a voice channel called AFK won't have their time counted."
            ).queue();

            botUtils fileWrite = new botUtils();
            try {
                fileWrite.writeToFile(e.getAuthor().getName() + " issued command: help");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println(e.getAuthor().getName() + " issued command: help");
        }

        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase("listservers"))){

            //For me to see all the server the bot is in
            if(e.getAuthor().getIdLong() == 559428414709301279L){

                System.out.println("Ordering server list");
                StringBuilder str = new StringBuilder();
                for(Guild a: Main.jda.getGuilds()){
                    str.append(a.getName() + "\n");
                    System.out.println("- " + a.getName() + ": " + a.getMemberCount() + " members");
                }

                try {
                    botUtils fileWrite = new botUtils();
                    fileWrite.writeToFile(String.valueOf(str));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
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
