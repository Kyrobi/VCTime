import net.dv8tion.jda.api.entities.TextChannel;

import java.io.*;

public class botUtils {
    public void writeToFile(String string) throws IOException {


        System.out.println(string);
        File file = new File("out.txt");

        /* This logic is to create the file if the
         * file is not already present
         */
        if(!file.exists()){
            file.createNewFile();
        }

        //Here true is to append the content to file
        FileWriter fw = new FileWriter(file,true);
        //BufferedWriter writer give better performance
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(string);
        //Closing BufferedWriter Stream
        bw.newLine();
        bw.close();

        Main.sendToDiscord(string);

    }

}
