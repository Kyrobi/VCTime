package me.Kyrobi;

import net.dv8tion.jda.api.entities.TextChannel;

import java.io.*;

public class botUtils {
    public void writeToFile(String string) throws IOException {

        Main.sendToDiscord(string);

    }

    public void writeToFileCommand(String string) throws IOException {

        Main.sendToDiscordCommand(string);

    }

}
