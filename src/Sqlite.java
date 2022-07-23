import org.w3c.dom.ls.LSOutput;

import java.io.File;
import java.sql.*;
import java.util.Objects;

public class Sqlite {

    File dbfile = new File("");
    //String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + "/counting.db";
    //String pathSep = System.getProperty("File.separator");

    String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName; // For linux to work
    //String url = "jdbc:sqlite:/home/kyrobi/Bot/Mio/counting.db";

    // This function will create a new database if one doesn't exist
    public void createNewTable(){
        String sql = "CREATE TABLE IF NOT EXISTS stats ('userID' integer PRIMARY KEY, 'time' integer NOT NULL DEFAULT 0, 'serverID' integer NOT NULL DEFAULT 0)";

        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url); //Tries to open the connection
            Statement stmt = conn.createStatement(); // Formulate the command to execute
            stmt.execute(sql);  //Execute said command
        }
        catch (SQLException | ClassNotFoundException error){
            System.out.println(error.getMessage());
        }

        System.out.println("Database does not exist. Creating a new one at " + url);
    }

    //Insert a new value into the database
    public void insert(long userID, int amount, long serverID){

        String sqlcommand = "INSERT INTO stats(userID, time, serverID) VALUES(?,?,?)";

        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            PreparedStatement stmt = conn.prepareStatement(sqlcommand);
            stmt.setLong(1, userID); // The first column will contain the ID
            stmt.setInt(2, amount); // The second column will contain the amount
            stmt.setLong(3, serverID);
            stmt.executeUpdate();
            conn.close();
        }
        catch(SQLException | ClassNotFoundException error){
            System.out.println(error.getMessage());
        }
    }

    //Updates an existing value in the database
    public void update(long userID, int amount, long serverID){

        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            //PreparedStatement stmt = conn.prepareStatement(updateCommand);
            PreparedStatement update = conn.prepareStatement("UPDATE stats SET time = ? WHERE userID = ? AND serverID = ?");

            update.setInt(1, amount);
            update.setLong(2, userID);
            update.setLong(3, serverID);

            update.executeUpdate();
            conn.close();

        }
        catch(SQLException | ClassNotFoundException e){
            System.out.println(e.getMessage());
        }

    }

    //Checks to see if a user exists in the database
    public Boolean exists(long userID, long serverID){
        // String to get all the values from the database
        int count = 0;

        try{
            //System.out.println("Connecting...");
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url); // Make connection
            PreparedStatement ifexists = conn.prepareStatement("SELECT * FROM stats WHERE userID = ? AND serverID = ?");

            ifexists.setLong(1, userID);
            ifexists.setLong(2, serverID);

            ResultSet rs = ifexists.executeQuery(); // Execute the command


            //We loop through the database. If the userID matches, we break out of the loop
            while(rs.next()){
                //System.out.println("ID: " + rs.getString("userId") + " Amount: " + rs.getInt("amount"));
                if(Objects.equals(rs.getLong("userID"), userID)){
                    ++count;
                    rs.close();
                    conn.close();
                    break; // Breaks out of the loop once the value has been found. No need to loop through the rest of the database
                }
            }
        }
        catch(SQLException | ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("Error code: " + e.getMessage());
        }

        if(count != 0){
            return true;
        }
        else{
            return false;
        }
    }


    public int getTime(long userId, long serverID){
        int amount = 0;

        try {
            //System.out.println("Connecting...");
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url); // Make connection
            PreparedStatement getAmount = conn.prepareStatement("SELECT * FROM stats WHERE userID = ? AND serverID = ?");

            getAmount.setLong(1, userId);
            getAmount.setLong(2, serverID);

            //Statement stmt = conn.createStatement();
            //ResultSet rs = getAmount.executeQuery(selectfrom); // Execute the command
            ResultSet rs = getAmount.executeQuery(); // Used with prepared statement
            amount = rs.getInt("time");
            rs.close();
            conn.close();
        }
        catch(SQLException | ClassNotFoundException se){
            System.out.println(se.getMessage());
        }
        return amount;
    }
}

