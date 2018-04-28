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

    public static String getPrefix(final long id) {

        try (Connection connection = getConnection()) {

            PreparedStatement state = connection.prepareStatement("SELECT * FROM prefixes WHERE id = ?");
            state.setLong(1, id);

            ResultSet prefix = state.executeQuery();

            return prefix.next() ? prefix.getString("prefix") : JukeBot.defaultPrefix;

        } catch (SQLException e) {
            return JukeBot.defaultPrefix;
        }

    }

    public static boolean setPrefix(final long id, final String newPrefix) {

        try (Connection connection = getConnection()) {

            PreparedStatement state = connection.prepareStatement("SELECT * FROM prefixes WHERE id = ?");
            state.setLong(1, id);

            PreparedStatement update;

            if (state.executeQuery().next()) {
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

            PreparedStatement state = connection.prepareStatement("SELECT * FROM donators WHERE id = ?");
            state.setLong(1, id);

            final boolean entryExists = state.executeQuery().next();

            if (newTier == 0) {
                if (!entryExists) return true;
                PreparedStatement update = connection.prepareStatement("DELETE FROM donators WHERE id = ?");
                update.setLong(1, id);
                return update.executeUpdate() == 1;
            }

            PreparedStatement update;

            if (entryExists) {
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

        try (Connection connection = getConnection()) {

            PreparedStatement state = connection.prepareStatement("SELECT * FROM donators WHERE id = ?");
            state.setLong(1, id);

            ResultSet tier = state.executeQuery();

            return tier.next() ? tier.getInt("tier") : 0;

        } catch (SQLException e) {
            return 0;
        }

    }

    public static ArrayList<Long> getDonatorIDs() {

        ArrayList<Long> donators = new ArrayList<>();

        try (Connection connection = getConnection()) {

            Statement state = connection.createStatement();
            ResultSet results = state.executeQuery("SELECT * FROM donators");

            while (results.next())
                donators.add(results.getLong(1));

        } catch (SQLException unused) {
        }

        return donators;

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
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM blocked WHERE id = ?");
            statement.setLong(1, id);
            ResultSet results = statement.executeQuery();

            return results.next();
        } catch (SQLException unused) {
            return false;
        }
    }

    static String getPropertyFromConfig(String prop, String def) {

        try (Connection connection = getConnection()) {

            PreparedStatement state = connection.prepareStatement("SELECT * FROM config WHERE prop = ?");
            state.setString(1, prop);

            ResultSet property = state.executeQuery();

            return property.next() ? property.getString("content") : def;

        } catch (SQLException e) {
            return def;
        }

    }
}
