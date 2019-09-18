package jukebot;

import com.zaxxer.hikari.HikariDataSource;
import jukebot.entities.CustomPlaylist;
import jukebot.entities.PremiumGuild;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;

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
            statement.addBatch("CREATE TABLE IF NOT EXISTS colours (id INTEGER PRIMARY KEY, rgb INTEGER NOT NULL)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS musicnick (id INTEGER PRIMARY KEY)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS customplaylists (title TEXT NOT NULL, creator INTEGER, tracks TEXT)");
            statement.addBatch("CREATE TABLE IF NOT EXISTS premiumservers (guildid INTEGER PRIMARY KEY, userid INTEGER, added INTEGER)");
            statement.executeBatch();
        } catch (SQLException e) {
            JukeBot.LOG.error("There was an error setting up the SQL database!", e);
        }
    }

    public static LinkedList<String> getPlaylists(final long creator) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM customplaylists WHERE creator = ?");
            statement.setLong(1, creator);
            ResultSet results = statement.executeQuery();

            LinkedList<String> playlists = new LinkedList<>();

            while (results.next()) {
                playlists.add(results.getString("title"));
            }

            return playlists;
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return null;
        }
    }

    @Nullable
    public static CustomPlaylist getPlaylist(final long creator, final String title) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM customplaylists WHERE creator = ? AND title = ?");
            statement.setLong(1, creator);
            statement.setString(2, title);

            ResultSet results = statement.executeQuery();

            return results.next()
                    ? new CustomPlaylist(results.getString("title"), creator, results.getString("tracks"))
                    : null;
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return null;
        }
    }

    public static boolean createPlaylist(final long creator, final String title) {
        try (Connection connection = getConnection()) {
            PreparedStatement update = connection.prepareStatement("INSERT INTO customplaylists VALUES (?, ?, ?)");
            update.setString(1, title);
            update.setLong(2, creator);
            update.setString(3, "");

            return update.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updatePlaylist(final long creator, final String title, final String tracks) {
        try (Connection connection = getConnection()) {
            PreparedStatement update = connection.prepareStatement("UPDATE customplaylists SET tracks = ? WHERE creator = ? AND title = ?");
            update.setString(1, tracks);
            update.setLong(2, creator);
            update.setString(3, title);

            return update.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean deletePlaylist(final long creator, final String title) {
        try (Connection connection = getConnection()) {
            PreparedStatement update = connection.prepareStatement("DELETE FROM customplaylists WHERE creator = ? AND title = ?");
            update.setLong(1, creator);
            update.setString(2, title);

            return update.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getPrefix(final long id) {
        String prefix = getFromDatabase("prefixes", id, "prefix");
        return prefix == null ? JukeBot.config.getDefaultPrefix() : prefix;
    }

    public static boolean setPrefix(final long id, final String newPrefix) {
        try (Connection connection = getConnection()) {
            final boolean shouldUpdate = tableContains("prefixes", id);
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
            final boolean shouldUpdate = tableContains("donators", id);

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
        String result = getFromDatabase("donators", id, "tier");
        return result != null ? Integer.parseInt(result) : 0;
    }

    public static boolean setDjRole(final long guildId, final Long roleId) {
        try (Connection connection = getConnection()) {

            final boolean shouldUpdate = tableContains("djroles", guildId);
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
        String result = getFromDatabase("djroles", guildId, "roleid");
        return result != null ? Long.parseLong(result) : null;
    }

    public static boolean setSkipThreshold(final long guildId, double newThreshold) {
        try (Connection connection = getConnection()) {
            final boolean shouldUpdate = tableContains("skipthres", guildId);
            PreparedStatement statement;

            if (shouldUpdate) {
                statement = connection.prepareStatement("UPDATE skipthres SET threshold = ? WHERE id = ?");
                statement.setDouble(1, newThreshold);
                statement.setLong(2, guildId);
            } else {
                statement = connection.prepareStatement("INSERT INTO skipthres VALUES (?, ?);");
                statement.setLong(1, guildId);
                statement.setDouble(2, newThreshold);
            }

            return statement.executeUpdate() == 1;
        } catch (SQLException unused) {
            JukeBot.LOG.error("Error updating skip thres", unused);
            return false;
        }
    }

    public static Double getSkipThreshold(final long guildId) {
        String result = getFromDatabase("skipthres", guildId, "threshold");
        return result == null ? 0.5 : Double.parseDouble(result);
    }

    public static ArrayList<Long> getDonorIds() {
        ArrayList<Long> donors = new ArrayList<>();

        try (Connection connection = getConnection()) {
            Statement state = connection.createStatement();
            ResultSet results = state.executeQuery("SELECT * FROM donators");

            while (results.next()) {
                donors.add(results.getLong(1));
            }
        } catch (SQLException ignored) {
        }

        return donors;
    }

    public static void blockUser(long id) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO blocked VALUES (?);");
            statement.setLong(1, id);
            statement.execute();
        } catch (SQLException ignored) {
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
        return tableContains("blocked", id);
    }

    public static void setMusicNickEnabled(long id, boolean enable) {
        try (Connection connection = getConnection()) {
            boolean currentlyEnabled = getIsMusicNickEnabled(id);

            if (!enable && !currentlyEnabled || enable && currentlyEnabled) {
                return;
            }

            PreparedStatement statement = enable
                    ? connection.prepareStatement("INSERT INTO musicnick VALUES (?);")
                    : connection.prepareStatement("DELETE FROM musicnick WHERE id = ?");

            statement.setLong(1, id);
            statement.execute();
        } catch (SQLException ignored) {
        }
    }

    public static boolean getIsMusicNickEnabled(long id) {
        return tableContains("musicnick", id);
    }

    public static boolean setColour(long id, int rgb) {
        try (Connection connection = getConnection()) {
            final boolean shouldUpdate = tableContains("colours", id);
            PreparedStatement statement;

            if (shouldUpdate) {
                statement = connection.prepareStatement("UPDATE colours SET rgb = ? WHERE id = ?");
                statement.setInt(1, rgb);
                statement.setLong(2, id);
            } else {
                statement = connection.prepareStatement("INSERT INTO colours VALUES (?, ?);");
                statement.setLong(1, id);
                statement.setInt(2, rgb);
            }

            return statement.executeUpdate() == 1;
        } catch (SQLException unused) {
            JukeBot.LOG.error("Error updating colour", unused);
            return false;
        }
    }

    public static int getColour(long id) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM colours WHERE id = ?");
            statement.setLong(1, id);
            ResultSet results = statement.executeQuery();
            return results.next() ? results.getInt("rgb") : JukeBot.config.getEmbedColour().getRGB();
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return JukeBot.config.getEmbedColour().getRGB();
        }
    }

    public static boolean isPremiumServer(long guildId) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM premiumservers WHERE guildid = ?");
            statement.setLong(1, guildId);
            ResultSet results = statement.executeQuery();
            return results.next();
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return false;
        }
    }

    public static void setPremiumServer(long userId, long guildId) {
        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO premiumservers VALUES (?, ?, ?);");
            statement.setLong(1, guildId);
            statement.setLong(2, userId);
            statement.setLong(3, Instant.now().toEpochMilli());
            statement.execute();
        } catch (SQLException ignored) {
        }
    }

    public static boolean removePremiumServer(final long guildId) {
        try (Connection connection = getConnection()) {
            PreparedStatement update = connection.prepareStatement("DELETE FROM premiumservers WHERE guildid = ?");
            update.setLong(1, guildId);

            return update.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean removePremiumServersOf(final long userId) {
        try (Connection connection = getConnection()) {
            PreparedStatement update = connection.prepareStatement("DELETE FROM premiumservers WHERE userid = ?");
            update.setLong(1, userId);

            return update.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static ArrayList<PremiumGuild> getPremiumServersOf(long userId) {
        ArrayList<PremiumGuild> premiumGuilds = new ArrayList<>();

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM premiumservers WHERE userid = ?");
            statement.setLong(1, userId);
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                premiumGuilds.add(new PremiumGuild(
                        results.getLong("guildid"),
                        results.getLong("added")
                ));
            }
        } catch (SQLException ignored) {
        }

        return premiumGuilds;
    }



    /*
     * +=================================================+
     * |                IGNORE BELOW THIS                |
     * +=================================================+
     */

    @SuppressWarnings("unchecked")
    private static String getFromDatabase(String table, long id, String columnId) {
        final String idColumn = table.equals("djroles") ? "guildid" : "id"; // I'm an actual idiot I stg

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE " + idColumn + " = ?");
            statement.setLong(1, id);
            ResultSet results = statement.executeQuery();
            return results.next() ? results.getString(columnId) : null;
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return null;
        }
    }

    private static boolean tableContains(String table, long id) {
        final String idColumn = table.equals("djroles") ? "guildid" : "id";

        try (Connection connection = getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + " WHERE " + idColumn + " = ?");
            statement.setLong(1, id);
            ResultSet results = statement.executeQuery();
            return results.next();
        } catch (SQLException e) {
            JukeBot.LOG.error("An error occurred while trying to retrieve from the database", e);
            return false;
        }
    }

}
