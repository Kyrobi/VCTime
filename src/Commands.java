import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Commands extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e){
        String[] args = e.getMessage().getContentRaw().split(" ");

        //Command to see your own stats
        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase(Main.prefix + "stats"))){
            Sqlite sqlite = new Sqlite();
            long authorID = Long.parseLong(e.getAuthor().getId());
            long serverID = Long.parseLong(e.getGuild().getId());
            e.getChannel().sendMessage(e.getMessage().getAuthor().getAsMention() + "'s vc time: " + millisecondsToTimeStamp(sqlite.getTime(authorID, serverID))).queue();
        }

        //Leaderboard command
        if((args[0].equalsIgnoreCase(Main.prefix + "vc")) && (args[1].equalsIgnoreCase(Main.prefix + "leaderboard"))){

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
