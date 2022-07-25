import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.exit;

public class Main extends ListenerAdapter {

    public static JDA jda;
    public static String prefix = "$";
    public static final String databaseFileName = "vcstats.db";

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {

        Path tokenFile;
        String token = null;

        //Read in token from a file
        try{
            tokenFile = Path.of("token.txt");
            token = Files.readString(tokenFile);
            jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_PRESENCES).build().awaitReady();
        }
        catch (IOException | IllegalArgumentException e){
            System.out.println("Cannot open token file! Making a new one. Please configure it");

            PrintWriter writer = new PrintWriter("token.txt", "UTF-8");
            writer.print("1234567890123456");
            writer.close();
            exit(1);
        }

        jda.getPresence().setActivity(Activity.playing("In " + jda.getGuilds().size() + " servers!"));

        //Updates presence with stats about the bot

        //Reference: https://stackoverflow.com/questions/1220975/calling-a-function-every-10-minutes
        int SECONDS = 10; // The delay in minutes
        final int[] presenseSwitch = {1}; // Controls which stats so show in presence
        final int[] memberCount = {0};
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() { // Function runs every MINUTES minutes.

                if(presenseSwitch[0] == 1){
                    jda.getPresence().setActivity(Activity.playing("In " + jda.getGuilds().size() + " servers!"));
                    presenseSwitch[0] = 0;
                }
                else if(presenseSwitch[0] == 0){

                    for(Guild a: jda.getGuilds()){
                        memberCount[0] += a.getMemberCount();
                    }
                    jda.getPresence().setActivity(Activity.playing("Spectating " + Arrays.toString(memberCount) + " members!"));
                    presenseSwitch[0] = 1;
                    memberCount[0] = 0; //Resets to 0 or else it will keep stacking
                }
                else{
                    System.out.println("Error in deciding which presence to display");
                }
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
        jda.addEventListener(new Commands());
        jda.addEventListener(new Tracker());


//        Sqlite sqlite = new Sqlite();
//        sqlite.insert(559428414709301279L, 99999999, 793748152355389481L);

    }

    public static void sendToDiscord(String message){
        TextChannel textChannel = jda.getTextChannelById("1000785699219439637");
        textChannel.sendMessage(message).queue();
    }
}
