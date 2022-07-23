import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;

public class Tracker extends ListenerAdapter {


    //Map stores when the user joins the call
    //userID, timeInMS
    public static Map<Long, Long> joinTracker = new HashMap<>();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e){
        System.out.println("User has joined the vc");

        long userID = Long.parseLong(e.getMember().getId());
        long currentTime = System.currentTimeMillis();

        //Saves the time for when the user first joins the VC
        joinTracker.put(userID, currentTime);
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e){

        String username = e.getMember().getEffectiveName();
        long userID = Long.parseLong(e.getMember().getId());
        long serverID = Long.parseLong(e.getGuild().getId());

        //If for some reason the user joined the VC when the bot is down, we handle it
        if(!joinTracker.containsKey(userID)){
            return;
        }

        //Math stuff to find time elapsed
        long leaveTime = System.currentTimeMillis();
        long timeDifference = leaveTime - joinTracker.get(userID);


        /*
        Saving the data
         */

        Sqlite sqlite = new Sqlite();

        //If the user exists in the database, we update their values
        if(sqlite.exists(userID, serverID)){
            long previousTime = sqlite.getTime(userID, serverID);
            long newTime = previousTime + timeDifference;
            sqlite.update(userID, newTime, serverID);
        }
        //If the user isn't in the database matching the guild, we add them to it
        else{
            System.out.println(username + " does not exists in the database associated with this server: " + e.getGuild().getName() + "... adding!");
            sqlite.insert(userID, timeDifference, serverID);
        }

        //Remove the user from the cache
        joinTracker.remove(userID);

        System.out.println(username + " has left the vc. In vc for " + timeDifference + "ms");
    }
}
