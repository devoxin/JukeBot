package jukebot;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;

public class Database {

    public static int calls = 0;
    private final static HikariDataSource pool = new HikariDataSource();

    private static Connection getConnection() throws SQLException {
        if (!pool.isRunning())
            pool.setJdbcUrl("jdbc:sqlite:jukebot.db");

        calls++;
        return pool.getConnection();
    }

    public static void setupDatabase() {
        try (Connection connection = getConnection()) {

            Statement statement = connection.createStatement();
            statement.addBatch("CREATE TABLE IF NOT EXISTS blocked (id INTEGER PRIMARY KEY)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS donators (id INTEGER PRIMARY KEY, tier TEXT NOT NULL)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS prefixes (id INTEGER PRIMARY KEY, prefix TEXT NOT NULL)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS djroles (guildid INTEGER PRIMARY KEY, roleid INTEGER NOT NULL)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS skipthres (id INTEGER PRIMARY KEY, threshold REAL NOT NULL)");
            statement.executeBatch();

        } catch (SQLException e) {
            JukeBot.LOG.error("There was an error setting up the SQL database!", e);
        }
    }

    public static String getPrefix(final long id) {
        try {
            ResultSet result = getFromDatabase("prefixes", id);
            return result != null && result.next() ? result.getString("prefix") : JukeBot.getDefaultPrefix();
        } catch (SQLException e) {
            JukeBot.LOG.error("Error accessing results from ResultSet", e);
            return JukeBot.getDefaultPrefix();
        }
    }

    public static boolean setPrefix(final long id, final String newPrefix) {

        try (Connection connection = getConnection()) {

            final boolean shouldUpdate = entryExists("prefixes", id);
            PreparedStatement update;

            if (shouldUpdate) {
                update = connection.prepareStatement("UPDATE prefixes SET prefix = ? WHERE id = ?");
                update.setString(1, newPrefix);
                update.setLong(2, id);
            } else {
                update = connection.prepareStatement("INSERT INTO prefixes VALUES (?, ?);");
                update.setLong(1, id);
                update.setString(2, newPrefix);
            }

            return update.executeUpdate() == 1;

        } catch (SQLException e) {
            return false;
        }

    }

    public static boolean setTier(final long id, final int newTier) {

        try (Connection connection = getConnection()) {

            final boolean shouldUpdate = entryExists("donators", id);

            if (newTier == 0) {
                if (!shouldUpdate) return true;
                PreparedStatement update = connection.prepareStatement("DELETE FROM donators WHERE id = ?");
                update.setLong(1, id);
                return update.executeUpdate() == 1;
            }

            PreparedStatement update;

            if (shouldUpdate) {
                update = connection.prepareStatement("UPDATE donators SET tier = ? WHERE id = ?");
                update.setInt(1, newTier);
                update.setLong(2, id);
            } else {
                update = connection.prepareStatement("INSERT INTO donators VALUES (?, ?);");
                update.setLong(1, id);
                update.setInt(2, newTier);
            }

            return update.executeUpdate() == 1;

        } catch (SQLException e) {
            return false;
        }

    }

    public static int getTier(long id) {
        try {
            ResultSet result = getFromDatabase("donators", id);
            return result != null && result.next() ? result.getInt("tier") : 0;
        } catch (SQLException e) {
            JukeBot.LOG.error("Error accessing results from ResultSet", e);
            return 0;
        }
    }

    public static boolean setDjRole(final long guildId, final Long roleId) {
        try (Connection connection = getConnection()) {

            final boolean shouldUpdate = entryExists("djroles", guildId);
            PreparedStatement update;

            if (shouldUpdate) {
                if (roleId == null) {
                    update = connection.prepareStatement("DELETE FROM djroles WHERE guildid = ?");
                    update.setLong(1, guildId);
                } else {
                    update = connection.prepareStatement("UPDATE djroles SET roleid = ? WHERE guildid = ?");
                    update.setLong(1, roleId);
                    update.setLong(2, guildId);
                }
            } else {
                update = connection.prepareStatement("INSERT INTO djroles VALUES (?, ?);");
                update.setLong(1, guildId);
                update.setLong(2, roleId);
            }

            return update.executeUpdate() == 1;

        } catch (SQLException e) {
            return false;
        }
    }

    public static Long getDjRole(final long guildId) {
        try {
            ResultSet result = getFromDatabase("djroles", guildId);
            return result != null && result.next() ? result.getLong("roleid") : null;
        } catch (SQLException e) {
            JukeBot.LOG.error("Error accessing results from ResultSet", e);
            return null;
        }
    }

    public static Double getSkipThreshold(final long guildId) {
        try {
            ResultSet result = getFromDatabase("skipthres", guildId);
            return result != null && result.next() ? result.getDouble("threshold"): 0.5;
        } catch (SQLException e) {
            JukeBot.LOG.error("Error accessing results from ResultSet", e);
            return 0.5;
        }
    }

    public static ArrayList<Long> getDonorIds() {

        ArrayList<Long> donors = new ArrayList<>();

        try (Connection connection = getConnection()) {

            Statement state = connection.createStatement();
            ResultSet results = state.executeQuery("SELECT * FROM donators");

            while (results.next())
                donors.add(results.getLong(1));

        } catch (SQLException unused) {
        }

        return donors;

    }

    public static void blockUser(long id) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO blocked VALUES (?);");
            statement.setLong(1, id);
            statement.execute();
        } catch (SQLException unused) {
        }
    }

    public static void unblockUser(long id) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM blocked WHERE id = ?");
            statement.setLong(1, id);
            statement.execute();
        } catch (SQLException unused) {
        }
    }

    public static boolean isBlocked(long id) {
        try {
            ResultSet result = getFromDatabase("blocked", id);
            return result != null && result.next();
        } catch (SQLException e) {
            JukeBot.LOG.error("Error accessing results from ResultSet", e);
            return false;
        }
    }

    private static ResultSet getFromDatabase(String table, long id) {
        final String idColumn = table.equals("djroles") ? "guildid" : "id"; // I'm an actual idiot I stg

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE " + idColumn + " = ?");
            statement.setLong(1, id);
            return statement.executeQuery();
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return null;
        }
    }

    private static boolean entryExists(String table, long id) { // Same principle as above except this takes out some more work
        try {
            ResultSet results = getFromDatabase(table, id);
            return results != null && results.next();
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while checking entry existence in the database", e);
            return false;
        }
    }

}
