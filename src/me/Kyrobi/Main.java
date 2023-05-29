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
import static java.lang.System.setOut;
import static me.Kyrobi.StatsTracker.*;
import static me.Kyrobi.Tracker.joinTracker;
import static me.Kyrobi.Tracker.saveStats;

public class Main extends ListenerAdapter {

    // What type of logging is this?
    public enum LogType{
        JOIN_EVENT,
        LEAVE_EVENT,
        MOVE_EVENT,
        STATS_COMMAND,
        HELP_COMMAND,
        LEADERBOARD_COMMAND
    }

    public static JDA jda;
    public static final String databaseFileName = "vcstats.db";


    public static void main(String[] args) throws LoginException, InterruptedException, IOException {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Shutting down ... saving user stats");

            // Create a copy of the key set to avoid ConcurrentModificationException
            Map<Long, User> copiedJointracker = new HashMap<Long, User>(joinTracker);

            // Save all the user's stats before fully exiting
            for (Long key: copiedJointracker.keySet()) {
                tempTimeTracker.put(key, copiedJointracker.get(key));
                saveStats(copiedJointracker.get(key).getGuildID(), key);
            }
        }));


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

        // jda.getPresence().setActivity(Activity.playing("In " + jda.getGuilds().size() + " servers!"));

        //Updates presence with stats about the bot

        //Reference: https://stackoverflow.com/questions/1220975/calling-a-function-every-10-minutes
//        int SECONDS = 20; // The delay in seconds
//
//        final int[] memberCount = {0};
//        final int[] presenseSwitch = {1}; // Controls which stats so show in presence
//
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() { // Function runs every MINUTES minutes.
//
//                if(presenseSwitch[0] == 1){
//                    jda.getPresence().setActivity(Activity.playing("In " + jda.getGuilds().size() + " servers!"));
//                    presenseSwitch[0] = 0;
//                }
//
//                else if(presenseSwitch[0] == 0){
//                    for(Guild a: jda.getGuilds()){
//                        memberCount[0] += a.getMemberCount();
//                    }
//                    jda.getPresence().setActivity(Activity.playing("Spectating " + Arrays.toString(memberCount) + " members!"));
//                    presenseSwitch[0] = 1;
//                    memberCount[0] = 0; //Resets to 0 or else it will keep stacking
//                }
//
//            }
//        }, 0, 1000 * SECONDS);


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


//        me.Kyrobi.Sqlite sqlite = new me.Kyrobi.Sqlite();
//        sqlite.insert(559428414709301279L, 99999999, 793748152355389481L);


        /*
        Run every minute based on system time
         */
        Calendar calendar = Calendar.getInstance();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new logging_stats(), millisToNextHour(calendar), 60*60*1000, TimeUnit.MILLISECONDS);


        /*
        When starting, add all users existing in a vc into the tracker
         */
        for(Guild guilds: jda.getGuilds()){
            for(Member member: guilds.getMembers()){
                if(member.getVoiceState().inVoiceChannel()){
                    joinTracker.put(member.getIdLong(), new User(member.getGuild().getIdLong(), System.currentTimeMillis()));
                }
            }
        }







//        Runnable helloRunnable = new Runnable() {
//            public void run() {
//                System.out.println("JoinHashMap   Size:" + joinTracker.size());
//
//                System.out.println("Users");
//                for (Long key: joinTracker.keySet()) {
//                    System.out.println("UserID:" + key + " GuilID" + joinTracker.get(key).getGuildID() + " Time:" + joinTracker.get(key).getTime() + " Current Time:" + System.currentTimeMillis());;
//                }
//            }
//        };
//
//        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
//        executor.scheduleAtFixedRate(helloRunnable, 0, 1, TimeUnit.SECONDS);

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

    public static String millisecondsToTimeStampDays(long durationInMillis) {

        //Reference: https://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java

        final long day = TimeUnit.MILLISECONDS.toDays(durationInMillis);
        final long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis - TimeUnit.DAYS.toMillis(day));
        final long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis - TimeUnit.DAYS.toMillis(day) - TimeUnit.HOURS.toMillis(hr));
        // final long sec = TimeUnit.MILLISECONDS.toSeconds(durationInMillis - TimeUnit.HOURS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        // final long ms = TimeUnit.MILLISECONDS.toMillis(durationInMillis - - TimeUnit.HOURS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02dd %02dh %02dm", day, hr, min);
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
                "(time, user, name, server, event_type, command, time_in_vc) " +
                "VALUES(?,?,?,?,?,?,?)";

        long currentTime = System.currentTimeMillis();
        long timeDifference = 0;
        if(joinTracker.containsKey(member.getIdLong())){
            timeDifference = currentTime - joinTracker.get(member.getIdLong()).getTime();
        }


        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            PreparedStatement stmt = conn.prepareStatement(sqlcommand);

            stmt.setString(1, getDate());
            stmt.setString(2, member.getUser().getName() + "#" + member.getUser().getDiscriminator());
            stmt.setString(3, member.getEffectiveName());
            stmt.setString(4, guild.getName());
            stmt.setString(5, String.valueOf(eventType));
            stmt.setString(6, command);
            stmt.setString(7, millisecondsToTimeStampDays(timeDifference));
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

    private static long millisToNextHour(Calendar calendar) {
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        int minutesToNextHour = 60 - minutes;
        int secondsToNextHour = 60 - seconds;
        int millisToNextHour = 1000 - millis;
        return minutesToNextHour*60*1000 + secondsToNextHour*1000 + millisToNextHour;
    }
}
