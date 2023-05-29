package me.Kyrobi;

import me.Kyrobi.botUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static me.Kyrobi.Main.jda;
import static me.Kyrobi.Main.log_actions;
import static me.Kyrobi.StatsTracker.*;

public class Tracker extends ListenerAdapter {


    //Map stores when the user joins the call
    //userID, timeInMS
    public static Map<Long, User> joinTracker = new HashMap<>();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e){
        String guildName = e.getGuild().getName();
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has joined the VC");


        if(e.getChannelJoined().getName().equalsIgnoreCase("AFK")){
            return;
        }
        startStats(e.getMember());

        String logMessage = "" +
                "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " +"\n" +
                "**Joined: **" + e.getChannelJoined() + "\n-";
        Main.logInfo(Main.LogType.JOIN_EVENT,logMessage);
        log_actions(e.getMember(), e.getGuild(), Main.LogType.JOIN_EVENT, "Join Channel");

        ++timesJoined;
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e){
        String guildName = e.getGuild().getName();
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has left the VC");

        if(e.getChannelLeft().getName().equalsIgnoreCase("AFK")){
            return;
        }
        saveStats(e.getMember());

        String logMessage = "" +
                "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " +"\n" +
                "**Left: **" + e.getChannelLeft().getName() + "\n-";
        Main.logInfo(Main.LogType.LEAVE_EVENT,logMessage);
        log_actions(e.getMember(), e.getGuild(), Main.LogType.LEAVE_EVENT, "Leave Channel" );

        ++timesLeft;
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e){
        System.out.println(e.getMember().getEffectiveName() + " from " + e.getGuild().getName() + " has changed from channel " + e.getChannelLeft().getName() + " to " + e.getChannelJoined().getName());

        String username = e.getMember().getEffectiveName();

        //If moved into an AFK channel

        if(e.getChannelJoined().getName().equalsIgnoreCase("AFK")){
            System.out.println(username +  " got moved into an AFK channel. Saving stats ");
            saveStats(e.getMember());
        }

        if(!e.getChannelJoined().getName().equalsIgnoreCase("AFK")){
            startStats(e.getMember());
        }

        String logMessage = "" +
                "Guild: " + e.getGuild().getName() + "  **|**  " + "User: " + e.getMember().getEffectiveName() + " `" + e.getMember().getUser().getName() + "#" + e.getMember().getUser().getDiscriminator() + "` " +"\n" +
                "**Moved: **" + e.getChannelLeft().getName() + "   \uD83E\uDC82   " +  e.getChannelJoined().getName() + "\n-";
        Main.logInfo(Main.LogType.MOVE_EVENT, logMessage);
        log_actions(e.getMember(), e.getGuild(), Main.LogType.MOVE_EVENT, "Move Channel");

        ++timesMove;
    }

    public static void startStats(Member e){


        long userID = Long.parseLong(e.getId());
        long currentTime = System.currentTimeMillis();

        //Saves the time for when the user first joins the VC
        joinTracker.put(userID, new User(e.getGuild().getIdLong(), currentTime));

    }


    public static void saveStats(Member e){
        String username = e.getEffectiveName();
        long userID = Long.parseLong(e.getId());
        long serverID = Long.parseLong(e.getGuild().getId());

        //If for some reason the user joined the VC when the bot is down, we handle it
        if(!joinTracker.containsKey(userID)){
            return;
        }

        //Math stuff to find time elapsed
        long leaveTime = System.currentTimeMillis();
        long timeDifference = leaveTime - joinTracker.get(userID).getTime();


        /*
        Saving the data
         */

        Sqlite sqlite = new Sqlite();

        //If the user exists in the database, we update their values
        if(sqlite.exists(userID, serverID)){
            long previousTime = sqlite.getTime(userID, serverID);
            long newTime = previousTime + timeDifference;
            sqlite.update(userID, newTime, serverID);
            log_actions(e, e.getGuild(), Main.LogType.SAVING_STATS, "Saving Stats", timeDifference);
        }
        //If the user isn't in the database matching the guild, we add them to it
        else{
            System.out.println(username + " does not exists in the database associated with this server: " + e.getGuild().getName() + "... adding!");
            sqlite.insert(userID, timeDifference, serverID);
        }

        //Remove the user from the cache
        joinTracker.remove(userID);


    }

    public static void saveStats(long guildID, long memberID){
        long userID = memberID;
        long serverID = guildID;

        //If for some reason the user joined the VC when the bot is down, we handle it
        if(!joinTracker.containsKey(userID)){
            return;
        }

        //Math stuff to find time elapsed
        long leaveTime = System.currentTimeMillis();
        long timeDifference = leaveTime - joinTracker.get(userID).getTime();


        /*
        Saving the data
         */

        Sqlite sqlite = new Sqlite();

        Guild guild = jda.getGuildById(guildID);
        Member member = (Member)jda.getUserById(memberID);

        //If the user exists in the database, we update their values
        if(sqlite.exists(userID, serverID)){
            long previousTime = sqlite.getTime(userID, serverID);
            long newTime = previousTime + timeDifference;
            sqlite.update(userID, newTime, serverID);
            log_actions(member, guild, Main.LogType.JOIN_EVENT, "Join Channel", timeDifference);
        }
        //If the user isn't in the database matching the guild, we add them to it
        else{
            // System.out.println(username + " does not exists in the database associated with this server: " + e.getGuild().getName() + "... adding!");
            sqlite.insert(userID, timeDifference, serverID);
        }

        //Remove the user from the cache
        joinTracker.remove(userID);

    }

}
