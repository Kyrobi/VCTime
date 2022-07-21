import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends ListenerAdapter {

    public static JDA jda;

    public static void main(String[] args) throws LoginException, InterruptedException {

        String token = "OTk5NTUzNzA3MDIyNzQ1Njgx.GBLhK6.hIMZr1E6CimWTurldAEX3ifSVWtZ1dWrpVHMR0";
        jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_PRESENCES).build().awaitReady();
        jda.getPresence().setActivity(Activity.playing("In " + jda.getGuilds().size() + " servers!"));

        //Updates presence with stats about the bot

        //Reference: https://stackoverflow.com/questions/1220975/calling-a-function-every-10-minutes
        int SECONDS = 5; // The delay in minutes
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
                    jda.getPresence().setActivity(Activity.playing("Spectating " + Arrays.toString(memberCount) + " servers!"));
                    presenseSwitch[0] = 1;
                    memberCount[0] = 0; //Resets to 0 or else it will keep stacking
                }
                else{
                    System.out.println("Error in deciding which presence to display");
                }
            }
        }, 0, 1000 * SECONDS);

        //Check if a database exists. If not, create a new one
        Sqlite sqlite = new Sqlite();

        // See if a database exists already. If not, create a new one
        File tempFile = new File("counting.db");
        boolean exists = tempFile.exists();
        if(!exists){
            sqlite.createNewTable();
        }

        //Registers the event for command tracking and time tracking
        jda.addEventListener(new Tracker());
    }
}
