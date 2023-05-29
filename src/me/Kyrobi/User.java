package me.Kyrobi;

public class User {

    private long time;
    private long guildID;

    User(long guildID, long time){
        this.guildID = guildID;
        this.time = time;
    }

    public long getGuildID(){
        return guildID;
    }

    public long getTime(){
        return time;
    }
}
