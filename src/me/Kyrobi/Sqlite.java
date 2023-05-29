package me.Kyrobi;

import org.w3c.dom.ls.LSOutput;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Objects;

public class Sqlite {

    File dbfile = new File("");
    //String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + "/counting.db";
    //String pathSep = System.getProperty("File.separator");

    public String url = "jdbc:sqlite:" + dbfile.getAbsolutePath() + File.separator + Main.databaseFileName; // For linux to work
    //String url = "jdbc:sqlite:/home/kyrobi/Bot/Mio/counting.db";

    // This function will create a new database if one doesn't exist
    public void createNewTable(){
        String create_stats_table = "CREATE TABLE IF NOT EXISTS stats ('userID' integer NOT NULL DEFAULT 0, 'time' integer NOT NULL DEFAULT 0, 'serverID' integer NOT NULL DEFAULT 0)";

        String create_general_stats_table =
                "CREATE TABLE IF NOT EXISTS general_stats " +
                        "(" +
                        "'id' INTEGER, " +
                        "'time' TEXT, " +
                        "'total_members' INTEGER, " +
                        "'total_servers' INTEGER, " +
                        "'members_in_vc' INTEGER, " +
                        "'time_in_vc' INTEGER, " +
                        "'times_joined_vc' INTEGER, " +
                        "'times_left_vc' INTEGER, " +
                        "'times_moved_vc' INTEGER, " +
                        "PRIMARY KEY('id' AUTOINCREMENT)" +
                        ")";

        String create_actions_table =
                "CREATE TABLE IF NOT EXISTS actions_log " +
                        "(" +
                        "'id' INTEGER, " +
                        "'time' TEXT, " +
                        "'user' TEXT, " +
                        "'name' TEXT, " +
                        "'server' TEXT, " +
                        "'event_type' TEXT, " +
                        "'command' TEXT, " +
                        "'time_in_vc' INTEGER, " +
                        "PRIMARY KEY('id' AUTOINCREMENT)" +
                        ")";


        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            Statement stmt = conn.createStatement(); // Formulate the command to execute
            stmt.execute(create_stats_table);  //Execute said command

            stmt.execute(create_general_stats_table);

            stmt.execute(create_actions_table);
        }
        catch (SQLException | ClassNotFoundException error){
            System.out.println(error.getMessage());
        }

        System.out.println("Database does not exist. Creating a new one at " + url);

        /*
        General Stats table
         */
    }

    //Insert a new value into the database
    public void insert(long userID, long time, long serverID){

        String sqlcommand = "INSERT INTO stats(userID, time, serverID) VALUES(?,?,?)";

        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            PreparedStatement stmt = conn.prepareStatement(sqlcommand);
            stmt.setLong(1, userID); // The first column will contain the ID
            stmt.setLong(2, time); // The second column will contain the amount
            stmt.setLong(3, serverID);
            stmt.executeUpdate();
            conn.close();
        }
        catch(SQLException | ClassNotFoundException error){
            System.out.println(error.getMessage());
        }
    }

    public void bulkInsert(List<Long> userID, List<Long>time, List<Long> serverID){
        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            //PreparedStatement stmt = conn.prepareStatement(updateCommand);
            PreparedStatement update = conn.prepareStatement("UPDATE stats SET time = ? WHERE userID = ? AND serverID = ?");

            // Disable auto-commit to enable batch processing
            conn.setAutoCommit(false);

            for (int i = 0; i < userID.size(); i++){
                update.setLong(1, time.get(i));
                update.setLong(2, userID.get(i));
                update.setLong(3, serverID.get(i));
                update.addBatch();
            }

            // Execute the batch
            update.executeBatch();

            // Commit the changes
            conn.commit();

            // Enable auto-commit again
            conn.setAutoCommit(true);

            conn.close();

        }
        catch(SQLException | ClassNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    //Updates an existing value in the database
    public void update(long userID, long time, long serverID){

        try(Connection conn = DriverManager.getConnection(url)){
            Class.forName("org.sqlite.JDBC");
            //PreparedStatement stmt = conn.prepareStatement(updateCommand);
            PreparedStatement update = conn.prepareStatement("UPDATE stats SET time = ? WHERE userID = ? AND serverID = ?");

            update.setLong(1, time);
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

        try(Connection conn = DriverManager.getConnection(url)){
            //System.out.println("Connecting...");
            Class.forName("org.sqlite.JDBC");
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


    public long getTime(long userId, long serverID){
        long amount = 0;

        try (Connection conn = DriverManager.getConnection(url)){
            //System.out.println("Connecting...");
            Class.forName("org.sqlite.JDBC");
            PreparedStatement getAmount = conn.prepareStatement("SELECT * FROM stats WHERE userID = ? AND serverID = ?");

            getAmount.setLong(1, userId);
            getAmount.setLong(2, serverID);

            //Statement stmt = conn.createStatement();
            //ResultSet rs = getAmount.executeQuery(selectfrom); // Execute the command
            ResultSet rs = getAmount.executeQuery(); // Used with prepared statement
            amount = rs.getLong("time");
            rs.close();
            conn.close();
        }
        catch(SQLException | ClassNotFoundException se){
            System.out.println(se.getMessage());
        }
        return amount;
    }


    /*
    Creating logging table and add stuff to it
     */
}

