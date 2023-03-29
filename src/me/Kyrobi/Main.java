package me.Kyrobi;

import me.Kyrobi.Commands.helpCommand;
import me.Kyrobi.Commands.leaderboardCommand;
import me.Kyrobi.Commands.statsCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

public class Main extends ListenerAdapter {

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

    }

    public static void sendToDiscord(String message){
        TextChannel textChannel = jda.getTextChannelById("1000785699219439637");
        textChannel.sendMessage(message).queue();
    }

    public static void sendToDiscordCommand(String message){
        TextChannel textChannel = jda.getTextChannelById("1025861800383758346");
        textChannel.sendMessage(message).queue();
    }

    public static String millisecondsToTimeStamp(long durationInMillis) {

        //Reference: https://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java

        final long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        final long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(durationInMillis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(durationInMillis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02dh %02dm", hr, min);
    }

}
