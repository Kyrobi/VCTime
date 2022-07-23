import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;

public class Tracker extends ListenerAdapter {


    //Map stores when the user joins the call
    //userID, timeInMS
    public static Map<Long, Long> joinTracker = new HashMap<>();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e){
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has joined the VC");
        if(e.getChannelJoined().getName().equalsIgnoreCase("AFK")){
            return;
        }
        startStats(e.getMember());
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e){
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has left the VC");
        if(e.getChannelLeft().getName().equalsIgnoreCase("AFK")){
            return;
        }
        saveStats(e.getMember());
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e){
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has changed from channel " + e.getChannelLeft().getName() + " to " + e.getChannelJoined().getName());
        String username = e.getMember().getEffectiveName();

        //If moved into an AFK channel
        if(e.getChannelJoined().getName().equalsIgnoreCase("AFK")){
            System.out.println(username +  " got moved into an AFK channel. Saving stats    ");
            saveStats(e.getMember());
        }

        if(!e.getChannelJoined().getName().equalsIgnoreCase("AFK")){
            startStats(e.getMember());
        }
    }

    public void startStats(Member e){
        String username = e.getEffectiveName();
        System.out.println(username + " has joined the vc");

        long userID = Long.parseLong(e.getId());
        long currentTime = System.currentTimeMillis();

        //Saves the time for when the user first joins the VC
        joinTracker.put(userID, currentTime);
    }

    public void saveStats(Member e){
        String username = e.getEffectiveName();
        long userID = Long.parseLong(e.getId());
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
