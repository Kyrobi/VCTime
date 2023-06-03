package me.Kyrobi;

import me.Kyrobi.Commands.helpCommand;
import me.Kyrobi.Commands.leaderboardCommand;
import me.Kyrobi.Commands.statsCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;


import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;
import static me.Kyrobi.StatsTracker.*;
import static me.Kyrobi.Tracker.*;

public class Main extends ListenerAdapter {

    // What type of logging is this?
    public enum LogType{
        JOIN_EVENT,
        LEAVE_EVENT,
        MOVE_EVENT,
        STATS_COMMAND,
        HELP_COMMAND,
        LEADERBOARD_COMMAND,
        SAVING_STATS
    }

    public static JDA jda;
    public static final String databaseFileName = "vcstats.db";


    public static void main(String[] args) throws LoginException, InterruptedException, IOException {

        Path tokenFile;
        String token = null;

        //Read in token from a file
        try{
            tokenFile = Path.of("token.txt");
            token = Files.readString(tokenFile);
            //jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MESSAGES).build().awaitReady();
            jda = JDABuilder.createDefault(token).build().awaitReady();
        }
        catch (IOException | IllegalArgumentException e){
            System.out.println("Cannot open token file! Making a new one. Please configure it");

            PrintWriter writer = new PrintWriter("token.txt", "UTF-8");
            writer.print("1234567890123456");
            writer.close();
            exit(1);
        }



        final int[] memberCount = {0};
        Guild myGuild = jda.getGuildById(1000784443797164136L);
        TextChannel channel = myGuild.getTextChannelById(1041145268873216101L);

        // Auto send user count and server count to channel every 12 hours
        int SECONDS = 43200; // The delay in seconds. This is 12 hours
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                for (Guild a: jda.getGuilds()){
                    memberCount[0] += a.getMemberCount();
                }

                int serverCount = jda.getGuilds().size();

                channel.sendMessage(serverCount + " - " + memberCount[0]).queue();
                jda.getPresence().setActivity(Activity.playing("Spectating " + memberCount[0] + " members!"));
                memberCount[0] = 0; //Resets to 0 or else it will keep stacking

            }
        }, 0, 1000 * SECONDS);


        // See if a database exists already. If not, create a new one
        File tempFile = new File(databaseFileName);
        System.out.println("Check file at " + tempFile.getAbsolutePath());
        boolean exists = tempFile.exists();
        if(!exists){
            //tempFile.createNewFile();
            System.out.println("Making a new database file");
            Sqlite sqlite = new Sqlite();
            sqlite.createNewTable();
        }

        //Registers the event for command tracking and time tracking
        jda.upsertCommand("help", "Shows the available commands").queue();
        jda.addEventListener(new helpCommand());

        jda.upsertCommand("stats", "Shows your total time in voice chat").queue();
        jda.addEventListener(new statsCommand());

        OptionData leaderboardOption = new OptionData(OptionType.INTEGER, "page", "Request a specific leaderboard page", false);
        jda.upsertCommand("leaderboard", "Shows your server's leaderboard").addOptions(leaderboardOption).queue();
        jda.addEventListener(new leaderboardCommand());

        // jda.addEventListener(new Commandsasd());
        jda.addEventListener(new Tracker());


        /*
        When starting, add all users existing in a vc into the tracker
         */
        for(Guild guilds: jda.getGuilds()){
            for(Member member: guilds.getMembers()){
                if(member.getVoiceState().inVoiceChannel() && !(member.getUser().isBot())){
                    joinTracker.put(member.getIdLong(), new User(member.getGuild().getIdLong(), System.currentTimeMillis()));
                }
            }
        }

        Runnable printHowManyInCall = new Runnable() {
            public void run() {
                System.out.println("People in voice calls: " + joinTracker.size());
            }
        };


        Runnable saveAllUsers = new Runnable() {
            public void run() {
                autoSave();
            }
        };

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        scheduler.scheduleAtFixedRate(printHowManyInCall, 0, 10, TimeUnit.SECONDS);


        // Run every minute based on system time
        Calendar calendar = Calendar.getInstance();
        scheduler.scheduleAtFixedRate(new logging_stats(), millisToNextHour(calendar), 60*60*1000, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(saveAllUsers, 0, 10, TimeUnit.MINUTES);

    }

    public static void logInfo(LogType logType, String message){

        TextChannel textChannel = null;
        String logMessage = "";
        if(logType.equals(LogType.JOIN_EVENT) || logType.equals(LogType.LEAVE_EVENT) || logType.equals(LogType.MOVE_EVENT)){
            // #logs channel
            textChannel = jda.getTextChannelById("1000785699219439637");
        }

        else if(logType.equals(LogType.HELP_COMMAND) || logType.equals(LogType.STATS_COMMAND) || logType.equals(LogType.LEADERBOARD_COMMAND)){
            // #commands channel
            textChannel = jda.getTextChannelById("1025861800383758346");
        }


        textChannel.sendMessage(message).queue();
    }

    public static String millisecondsToTimeStamp(long durationInMillis) {

        //Reference: https://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java

        final long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        final long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis - TimeUnit.HOURS.toMillis(hr));
        // final long sec = TimeUnit.MILLISECONDS.toSeconds(durationInMillis - TimeUnit.HOURS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        // final long ms = TimeUnit.MILLISECONDS.toMillis(durationInMillis - - TimeUnit.HOURS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02dh %02dm", hr, min);
    }

    static class logging_stats implements Runnable {
        @Override
        public void run() {
            File dbfile = new File("");
            String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;

            String sqlcommand = "INSERT INTO general_stats" +
                    "(time, total_members, total_servers, members_in_vc, time_in_vc, times_joined_vc, times_left_vc, times_moved_vc) " +
                    "VALUES(?,?,?,?,?,?,?,?)";

            try(Connection conn = DriverManager.getConnection(url)){
                Class.forName("org.sqlite.JDBC");
                PreparedStatement stmt = conn.prepareStatement(sqlcommand);

                stmt.setString(1, getDate());
                stmt.setLong(2, getTotalMembers());
                stmt.setLong(3, getTotalServers());
                stmt.setLong(4, getTotalMembersInVC());
                stmt.setString(5, getTotalCallTimeInLastHourInMS());
                stmt.setLong(6, getTimesJoined());
                stmt.setLong(7, getTimesLeft());
                stmt.setLong(8, getTimesMove());
                stmt.executeUpdate();
                conn.close();
            }
            catch(SQLException | ClassNotFoundException error){
                System.out.println(error.getMessage());
            }

            tempTimeTracker.clear();
            for (Long key: joinTracker.keySet()) {
                tempTimeTracker.put(key, joinTracker.get(key));
            }

        }
    }

    public static void log_actions(Member member, Guild guild, LogType eventType, String command) {
        File dbfile = new File("");
        String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;

        String sqlcommand = "INSERT INTO actions_log" +
                "(time, user, name, server, event_type, command) " +
                "VALUES(?,?,?,?,?,?)";


        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            PreparedStatement stmt = conn.prepareStatement(sqlcommand);

            stmt.setString(1, getDate());
            stmt.setString(2, member.getUser().getName() + "#" + member.getUser().getDiscriminator());
            stmt.setString(3, member.getEffectiveName());
            stmt.setString(4, guild.getName());
            stmt.setString(5, String.valueOf(eventType));
            stmt.setString(6, command);
            stmt.executeUpdate();
            conn.close();
        }
        catch(SQLException | ClassNotFoundException error){
            System.out.println(error.getMessage());
        }
    }

    public static void log_actions(Member member, Guild guild, LogType eventType, String command, long timeInVC) {
        File dbfile = new File("");
        String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName;

        String sqlcommand = "INSERT INTO actions_log" +
                "(time, user, name, server, event_type, command, time_in_vc) " +
                "VALUES(?,?,?,?,?,?,?)";


        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            PreparedStatement stmt = conn.prepareStatement(sqlcommand);

            stmt.setString(1, getDate());
            stmt.setString(2, member.getUser().getName() + "#" + member.getUser().getDiscriminator());
            stmt.setString(3, member.getEffectiveName());
            stmt.setString(4, guild.getName());
            stmt.setString(5, String.valueOf(eventType));
            stmt.setString(6, command);
            stmt.setString(7, millisecondsToTimeStamp(timeInVC));
            stmt.executeUpdate();
            conn.close();
        }
        catch(SQLException | ClassNotFoundException error){
            System.out.println(error.getMessage());
        }
    }

    private static long millisToNextHour(Calendar calendar) {
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        int minutesToNextHour = 60 - minutes;
        int secondsToNextHour = 60 - seconds;
        int millisToNextHour = 1000 - millis;
        return minutesToNextHour*60*1000 + secondsToNextHour*1000 + millisToNextHour;
    }

    private static void autoSave(){
        try{
            long startTime = System.nanoTime();

            System.out.println("-\n\nAuto saving user stats...\n\n-");


            // Save all the user's stats before fully exiting
            for (Long key: joinTracker.keySet()) {
                saveStatsShutdown(joinTracker.get(key).getGuildID(), key);
            }

            long endTime = System.nanoTime();
            long elapsedTime = endTime - startTime;
            double seconds = (double) elapsedTime / 1_000_000_000.0;
            System.out.println("-\n\nAUTOSAVE COMPLETED \n\nSaving took: " + seconds + " seconds for " + joinTracker.size() + " users.");
        }

        catch (Exception e){
            System.out.println("Error: ");
            for(StackTraceElement i: e.getStackTrace()){
                System.out.println(i.toString());
            }
        }
    }
}
