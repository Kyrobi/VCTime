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
import java.util.ArrayList;
import java.util.EventListener;
import java.util.concurrent.TimeUnit;

import static me.Kyrobi.Main.*;

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

            ArrayList<String> allMembers;
            ArrayList<String> pagedNames = new ArrayList<>();
            OptionMapping leaderboardOption = e.getOption("page");

            botUtils fileWrite = new botUtils();


            int nameInterval = 10; // Shows X names per page
            long requestedPages = 0; // Which page the user is requesting
            int pagesPossible; // How many pages are possible given the amount of users returned
            long requestedPage_forLog = 0;
            if(leaderboardOption != null){
                requestedPages = leaderboardOption.getAsLong();
                requestedPage_forLog = leaderboardOption.getAsLong();
            }

            String logMessage = "" +
                    "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " + "\n" +
                    "**Command: **" + e.getName().toLowerCase() + " " +  requestedPage_forLog + "\n-";
            Main.logInfo(Main.LogType.LEADERBOARD_COMMAND, logMessage);
            log_actions(e.getMember(), e.getGuild(), LogType.LEADERBOARD_COMMAND, e.getName() + " " + requestedPage_forLog);

            // Get all the members from this guild
            allMembers = getMembers(e.getGuild().getIdLong());
            pagesPossible = (int) Math.ceil((double)allMembers.size() / (double)nameInterval);

            // Send an error message is user is requesting some long ass page
            if(requestedPages > pagesPossible){
                e.reply("Page " + requestedPages + " does not exist. Pages available: " + pagesPossible).queue();
                return;
            }

            // If the want other page than 0, we subtract 1 so that it's easier to index in the loops later in the code
            else if(requestedPages > 0){
                requestedPages = requestedPages - 1;
            }

            // Makes sure people can't request a negative page amount
            else if(requestedPages < 0){
                requestedPages = 0;
            }

            int lengthOfAllMembers = allMembers.size();

            for(long i = (requestedPages * nameInterval); i < (requestedPages + 1) * nameInterval; i++){
                try{
                    allMembers.get((int) i);
                } catch (IndexOutOfBoundsException iofb){
                    break;
                }
                pagedNames.add(allMembers.get((int) i));
            }

            StringBuilder leaderboardNames = new StringBuilder();
            for(String name: pagedNames){
                leaderboardNames.append(name);
            }

            String serverTotal = "**Server total**: " + millisecondsToTimeStampDays(getServerTotalTime(e.getGuild().getIdLong()));
            leaderboardNames.append(serverTotal);
            leaderboardNames.append("\n\n");

            String resultSuffix = "Page (" + (requestedPages + 1) + "/" + pagesPossible + ")";
            String nextPageNotice = "";
            if((requestedPages + 1) < pagesPossible){
                nextPageNotice = "\nDo `/leaderboard " + (requestedPages + 2) + "` for more results.";
            }

            leaderboardNames.append(resultSuffix);
            leaderboardNames.append(nextPageNotice);

            //We take the final string and post it into the field
            EmbedBuilder eb = new EmbedBuilder();
            eb.addField("Voice Call Leaderboard [Top 1000]", leaderboardNames.toString(), true);


            //e.getChannel().sendMessageEmbeds(eb.build()).queue();
            e.replyEmbeds(eb.build()).queue();

        }
    }

    private ArrayList<String> getMembers(long guildID){
        ArrayList<String> users = new ArrayList<>();
        File dbfile = new File("");
        String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;

        int ranking = 1;
        try(Connection conn = DriverManager.getConnection(url)){

            PreparedStatement selectDescOrder = conn.prepareStatement("SELECT * FROM `stats` WHERE serverID = ? ORDER BY `time` DESC LIMIT 1000");
            selectDescOrder.setLong(1, guildID);

            ResultSet rs = selectDescOrder.executeQuery(); // Execute the command


            //We loop through the database. If the userID matches, we break out of the loop
            while(rs.next()){
                long userId = rs.getLong("userID");

                //stringBuilder1.append("\n`#" + ranking++ + "` **" + rs.getInt("amount") + "** - " + (toUser(userId)).getAsMention());
                users.add("**#" + ranking++ + "** " + "<@" + userId + "> " + millisecondsToTimeStamp(rs.getLong("time")) + "\n");
            }
            rs.close();
            conn.close();

        }
        catch(SQLException ev){
            ev.printStackTrace();
            System.out.println("Error code: " + ev.getMessage());
        }
        return users;
    }

    private long getServerTotalTime(long guildID){
        long sum = 0;

        File dbfile = new File("");
        String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;

        try(Connection conn = DriverManager.getConnection(url)){

            PreparedStatement selectDescOrder = conn.prepareStatement("SELECT SUM(`time`) FROM `stats` WHERE serverID = ?");
            selectDescOrder.setLong(1, guildID);

            ResultSet rs = selectDescOrder.executeQuery(); // Execute the command
            rs.next();

            sum = rs.getLong(1);

            rs.close();
            conn.close();

        }
        catch(SQLException ev){
            ev.printStackTrace();
        }
        return sum;
    }
}
