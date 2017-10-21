package jukebot;

import jukebot.utils.Bot;

import java.sql.*;
import java.util.HashMap;

import static jukebot.utils.Bot.LOG;

public class DatabaseHandler {

    public String getPrefix(long id) {

        try(Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM prefixes WHERE id = ?");
            state.setLong(1, id);

            ResultSet prefix = state.executeQuery();

            if (prefix.next())
                return prefix.getString("prefix");

            return Bot.defaultPrefix;

        } catch (Exception e) {

            LOG.error("Failed to retrieve server prefix");
            return Bot.defaultPrefix;

        }

    }

    public boolean setPrefix(long id, String newPrefix) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM prefixes WHERE id = ?");
            state.setLong(1, id);

            boolean entryExists = state.executeQuery().next();

            if (entryExists) {
                PreparedStatement update = con.prepareStatement("UPDATE prefixes SET prefix = ? WHERE id = ?");
                update.setString(1, newPrefix);
                update.setLong(2, id);
                return update.executeUpdate() == 1;
            } else {
                PreparedStatement update = con.prepareStatement("INSERT INTO prefixes VALUES (?, ?);");
                update.setLong(1, id);
                update.setString(2, newPrefix);
                return update.executeUpdate() == 1;
            }

        } catch (Exception e) {
            LOG.error("Failed to update server prefix");
            return false;

        }

    }

    public boolean setTier(long id, int newTier) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM donators WHERE id = ?");
            state.setLong(1, id);

            boolean entryExists = state.executeQuery().next();

            if (newTier == 0) {
                if (!entryExists)
                    return true;
                PreparedStatement update = con.prepareStatement("DELETE FROM donators WHERE id = ?");
                update.setLong(1, id);
                return update.executeUpdate() == 1;
            }

            if (entryExists) {
                PreparedStatement update = con.prepareStatement("UPDATE donators SET tier = ? WHERE id = ?");
                update.setInt(1, newTier);
                update.setLong(2, id);
                return update.executeUpdate() == 1;
            } else {
                PreparedStatement update = con.prepareStatement("INSERT INTO donators VALUES (?, ?);");
                update.setLong(1, id);
                update.setInt(2, newTier);
                return update.executeUpdate() == 1;
            }

        } catch (Exception e) {

            LOG.error("Failed to update user tier");
            return false;

        }

    }

    public int getTier(long id) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM donators WHERE id = ?");
            state.setLong(1, id);

            ResultSet tier = state.executeQuery();

            if (tier.next())
                return tier.getInt("tier");

            return 0;

        } catch (Exception e) {

            LOG.error("Failed to retrieve user tier");
            return 0;

        }

    }

    public HashMap<Long, Integer> getAllDonators() {

        HashMap<Long, Integer> donators = new HashMap<>();

        try (Connection con = connect()) {

            Statement state = con.createStatement();
            ResultSet results = state.executeQuery("SELECT * FROM donators");

            while (results.next())
                donators.put(results.getLong(1), results.getInt(2));

            return donators;

        } catch (Exception e) {

            LOG.error("Failed to retrieve all donators from table");
            return donators;

        }

    }

    public String getPropertyFromConfig(String prop) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM config WHERE prop = ?");
            state.setString(1, prop);

            ResultSet property = state.executeQuery();

            if (property.next())
                return property.getString("content");

            return null;

        } catch (Exception e) {
            LOG.error("Failed to retrieve config property");
            return null;

        }

    }

    private Connection connect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:jukebot.db");
    }
}
