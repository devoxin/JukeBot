package jukebot;

import jukebot.utils.Bot;

import java.sql.*;

public class DatabaseHandler {

    public String getPrefix(long id) {

        try(Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM prefixes WHERE id = ?");
            state.setLong(1, id);

            ResultSet prefix = state.executeQuery();

            if (prefix.next())
                return prefix.getString("prefix");
            else
                return Bot.defaultPrefix;

        } catch (Exception e) {

            System.out.println("Failed to retrieve server prefix");
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

            System.out.println("Failed to update server prefix");
            return false;

        }

    }

    public boolean setTier(long id, String newTier) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM donators WHERE id = ?");
            state.setLong(1, id);

            boolean entryExists = state.executeQuery().next();

            if (entryExists) {
                PreparedStatement update = con.prepareStatement("UPDATE donators SET tier = ? WHERE id = ?");
                update.setString(1, newTier);
                update.setLong(2, id);
                return update.executeUpdate() == 1;
            } else {
                PreparedStatement update = con.prepareStatement("INSERT INTO donators VALUES (?, ?);");
                update.setLong(1, id);
                update.setString(2, newTier);
                return update.executeUpdate() == 1;
            }

        } catch (Exception e) {

            System.out.println("Failed to update user tier");
            return false;

        }

    }

    public String getTier(long id) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM donators WHERE id = ?");
            state.setLong(1, id);

            ResultSet tier = state.executeQuery();

            if (tier.next())
                return tier.getString("tier");
            else
                return "0";

        } catch (Exception e) {

            System.out.println("Failed to retrieve user tier");
            return "0";

        }

    }

    public String getPropertyFromConfig(String prop) {

        try (Connection con = connect()) {

            PreparedStatement state = con.prepareStatement("SELECT * FROM config WHERE prop = ?");
            state.setString(1, prop);

            ResultSet property = state.executeQuery();

            if (property.next())
                return property.getString("content");
            else
                return null;

        } catch (Exception e) {

            System.out.println("Failed to retrieve config property");
            return null;

        }

    }

    private Connection connect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:jukebot.db");
    }
}
