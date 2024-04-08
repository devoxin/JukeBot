package me.devoxin.jukebot

import com.zaxxer.hikari.HikariDataSource
import io.sentry.Sentry
import me.devoxin.jukebot.extensions.get
import me.devoxin.jukebot.models.CustomPlaylist
import me.devoxin.jukebot.models.PremiumGuild
import me.devoxin.jukebot.models.PremiumUser
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Instant

object Database {
    private val pool = HikariDataSource()
    var calls = 0L
        private set

    val connection: Connection
        get() = pool.connection.also { calls++ }

    init {
        if (!pool.isRunning) {
            pool.jdbcUrl = "jdbc:sqlite:jukebot.db"
        }

        setupTables()
    }

    private fun setupTables() = runSuppressed {
        connection.use {
            it.createStatement().apply {
                // Dev/Bot stuff
                addBatch("CREATE TABLE IF NOT EXISTS blocked (id INTEGER PRIMARY KEY)")
                addBatch("CREATE TABLE IF NOT EXISTS patrons (id INTEGER PRIMARY KEY, tierId INTEGER NOT NULL, pledgeAmount REAL NOT NULL, override INTEGER NOT NULL DEFAULT 0)")
                addBatch("CREATE TABLE IF NOT EXISTS premiumservers (guildid INTEGER PRIMARY KEY, userid INTEGER, added INTEGER)")
                // Guild Settings
                addBatch("CREATE TABLE IF NOT EXISTS prefixes (id INTEGER PRIMARY KEY, prefix TEXT NOT NULL)")
                addBatch("CREATE TABLE IF NOT EXISTS djroles (guildid INTEGER PRIMARY KEY, roleid INTEGER NOT NULL)")
                addBatch("CREATE TABLE IF NOT EXISTS skipthres (id INTEGER PRIMARY KEY, threshold REAL NOT NULL)")
                addBatch("CREATE TABLE IF NOT EXISTS colours (id INTEGER PRIMARY KEY, rgb INTEGER NOT NULL)")
                addBatch("CREATE TABLE IF NOT EXISTS musicnick (id INTEGER PRIMARY KEY)")
                addBatch("CREATE TABLE IF NOT EXISTS autoplay (id INTEGER PRIMARY KEY)")
                addBatch("CREATE TABLE IF NOT EXISTS autodc (id INTEGER PRIMARY KEY)")
                // User Stuff
                addBatch("CREATE TABLE IF NOT EXISTS customplaylists (title TEXT NOT NULL, creator INTEGER, tracks TEXT)")
            }.executeBatch()
        }
    }

    fun getPlaylists(creator: Long): List<String> = suppressedWithConnection({ emptyList() }) {
        val results = buildStatement(it, "SELECT title FROM customplaylists WHERE creator = ?", creator)
            .executeQuery()

        val playlists = mutableListOf<String>()

        while (results.next()) {
            playlists.add(results.getString("title"))
        }

        playlists.toList()
    }

    fun countPlaylists(creator: Long): Int = suppressedWithConnection({ 0 }) {
        val result = buildStatement(it, "SELECT COUNT(title) AS count FROM customplaylists WHERE creator = ?", creator)
            .executeQuery()

        if (result.next()) result.getInt("count") else 0
    }

    fun getPlaylist(creator: Long, title: String): CustomPlaylist? = suppressedWithConnection({ null }) {
        val results = buildStatement(it, "SELECT title, creator, tracks FROM customplaylists WHERE creator = ? AND title = ?", creator, title)
            .executeQuery()

        if (results.next()) CustomPlaylist(results["title"], creator, results["tracks"]) else null
    }

    fun createPlaylist(creator: Long, title: String) = suppressedWithConnection({ null }) {
        buildStatement(it, "INSERT INTO customplaylists VALUES (?, ?, ?)", title, creator, "")
            .executeUpdate()

        CustomPlaylist(title, creator, "")
    }

    fun updatePlaylist(creator: Long, title: String, tracks: String) = runSuppressed {
        connection.use {
            buildStatement(it, "UPDATE customplaylists SET tracks = ? WHERE creator = ? AND title = ?", tracks, creator, title)
                .executeUpdate()
        }
    }

    fun deletePlaylist(creator: Long, title: String) = runSuppressed {
        connection.use {
            buildStatement(it, "DELETE FROM customplaylists WHERE creator = ? AND title = ?", creator, title)
                .executeUpdate()
        }
    }

    fun getPrefix(guildId: Long) = getFromDatabase("prefixes", guildId, "prefix")
        ?: Launcher.config.defaultPrefix

    fun setPrefix(guildId: Long, newPrefix: String) = runSuppressed {
        connection.use {
            buildStatement(it, "INSERT INTO prefixes(id, prefix) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET prefix = ?", guildId, newPrefix, newPrefix)
                .executeUpdate()
        }
    }

    fun getPatron(userId: Long): PremiumUser? = suppressedWithConnection({ null }) {
        val results = buildStatement(it, "SELECT id, tierId, override FROM patrons WHERE id = ?", userId)
            .executeQuery()

        if (results.next()) PremiumUser(results["id"].toLong(), results["tierId"].toInt(), results["pledgeAmount"].toInt(), results["override"] == "1") else null
    }

    fun setPatron(userId: Long, tierId: Int, pledgeAmountCents: Int, override: Boolean) = runSuppressed {
        connection.use {
            if (tierId == 0) { // failsafe
                buildStatement(it, "DELETE FROM patrons WHERE id = ?", userId).executeUpdate()
                return@runSuppressed
            }

            val overrideInt = if (override) 1 else 0

            buildStatement(
                it,
                "INSERT INTO patrons(id, tierId, pledgeAmount, override) VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET tier = ?, pledgeAmount = ?, override = ?",
                userId, tierId, pledgeAmountCents, overrideInt, tierId, pledgeAmountCents, overrideInt
            ).executeUpdate()
        }
    }

    fun deletePatron(userId: Long) = runSuppressed {
        connection.use {
            buildStatement(it, "DELETE FROM patrons WHERE id = ?", userId).executeUpdate()
        }
    }

    fun getDjRole(guildId: Long) = getFromDatabase("djroles", guildId, "roleid")?.toLong()

    fun setDjRole(guildId: Long, roleId: Long?) = runSuppressed {
        connection.use {
            if (roleId == null) {
                buildStatement(it, "DELETE FROM djroles WHERE guildid = ?", guildId).executeUpdate()
                return@runSuppressed
            }

            buildStatement(it, "INSERT INTO djroles(guildid, roleid) VALUES (?, ?) ON CONFLICT(guildid) DO UPDATE SET roleid = ?", guildId, roleId, roleId)
                .executeUpdate()
        }
    }

    fun getSkipThreshold(guildId: Long) = getFromDatabase("skipthres", guildId, "threshold")?.toDouble()
        ?: 0.5

    fun setSkipThreshold(guildId: Long, threshold: Double) = runSuppressed {
        connection.use {
            buildStatement(it, "INSERT INTO skipthres(id, threshold) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET threshold = ?", guildId, threshold, threshold)
                .executeUpdate()
        }
    }

    fun getPatrons(): List<PremiumUser> = suppressedWithConnection({ emptyList() }) {
        val results = buildStatement(it, "SELECT id, tierId, pledgeAmount, override FROM patrons").executeQuery()
        val list = mutableListOf<PremiumUser>()

        while (results.next()) {
            list.add(PremiumUser(results["id"].toLong(), results["tierId"].toInt(), results["pledgeAmount"].toInt(), results["override"] == "1"))
        }

        list
    }

    fun getColour(guildId: Long) = suppressedWithConnection({ Launcher.config.embedColour.rgb }) {
        val results = buildStatement(it, "SELECT id, rgb FROM colours WHERE id = ?", guildId)
            .executeQuery()

        if (results.next()) results.getInt("rgb") else Launcher.config.embedColour.rgb
    }

    fun setColour(guildId: Long, rgb: Int) = runSuppressed {
        connection.use {
            buildStatement(it, "INSERT INTO colours(id, rgb) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET rgb = ?", guildId, rgb, rgb)
                .executeUpdate()
        }
    }

    fun getIsPremiumServer(guildId: Long) = suppressedWithConnection({ false }) {
        if (Launcher.isSelfHosted) {
            return@suppressedWithConnection true
        }

        buildStatement(it, "SELECT guildid, userid, added FROM premiumservers WHERE guildid = ?", guildId)
            .executeQuery().next()
    }

    fun setPremiumServer(userId: Long, guildId: Long) = runSuppressed {
        connection.use {
            buildStatement(it, "INSERT INTO premiumservers VALUES (?, ?, ?)", guildId, userId, Instant.now().toEpochMilli())
                .execute()
        }
    }

    fun removePremiumServer(guildId: Long) = runSuppressed {
        connection.use {
            buildStatement(it, "DELETE FROM premiumservers WHERE guildid = ?", guildId)
                .executeUpdate()
        }
    }

    fun removePremiumServersOf(userId: Long) = runSuppressed {
        connection.use {
            buildStatement(it, "DELETE FROM premiumservers WHERE userid = ?", userId)
                .executeUpdate()
        }
    }

    fun getPremiumServersOf(userId: Long): List<PremiumGuild> = suppressedWithConnection({ emptyList() }) {
        val results = buildStatement(it, "SELECT guildid, userid, added FROM premiumservers WHERE userid = ?", userId)
            .executeQuery()

        val list = mutableListOf<PremiumGuild>()

        while (results.next()) {
            list.add(PremiumGuild(results.getLong("guildid"), results.getLong("added")))
        }

        list
    }

    fun countPremiumServersOf(userId: Long): Int = suppressedWithConnection({ 0 }) {
        val result = buildStatement(it, "SELECT COUNT(guildid) AS count FROM premiumservers WHERE userid = ?", userId)
            .executeQuery()

        if (result.next()) result.getInt("count") else 0
    }

    fun getIsBlocked(userId: Long) = getFromDatabase("blocked", userId, "id") != null
    fun setIsBlocked(userId: Long, block: Boolean) = setEnabled("blocked", userId, block)

    fun getIsAutoPlayEnabled(guildId: Long) = getFromDatabase("autoplay", guildId, "id") != null
    fun setAutoPlayEnabled(guildId: Long, enable: Boolean) = setEnabled("autoplay", guildId, enable)

    fun getIsAutoDcDisabled(guildId: Long) = getFromDatabase("autodc", guildId, "id") != null
    fun setAutoDcDisabled(guildId: Long, enable: Boolean) = setEnabled("autodc", guildId, enable)

    fun getIsMusicNickEnabled(guildId: Long) = getFromDatabase("musicnick", guildId, "id") != null
    fun setMusicNickEnabled(guildId: Long, enable: Boolean) = setEnabled("musicnick", guildId, enable)

    /*
     * +=================================================+
     * |                IGNORE BELOW THIS                |
     * +=================================================+
     */
    private fun getFromDatabase(table: String, id: Long, columnId: String): String? =
        suppressedWithConnection({ null }) {
            val idColumn = if (table == "djroles") "guildid" else "id" // I'm an actual idiot I stg

            val results = buildStatement(it, "SELECT * FROM $table WHERE $idColumn = ?", id)
                .executeQuery()

            if (results.next()) results[columnId] else null
        }

    private fun setEnabled(table: String, id: Long, enable: Boolean) = runSuppressed {
        connection.use {
            val stmt = if (!enable) {
                buildStatement(it, "DELETE FROM $table WHERE id = ?", id)
            } else {
                buildStatement(it, "INSERT OR IGNORE INTO $table (id) VALUES (?)", id)
            }

            stmt.execute()
        }
    }

    private fun buildStatement(con: Connection, sql: String, vararg obj: Any): PreparedStatement {
        val statement = con.prepareStatement(sql)

        for ((i, o) in obj.withIndex()) {
            statement.setObject(i + 1, o)
        }

        return statement
    }

    private fun runSuppressed(block: () -> Unit) = runCatching(block).onFailure(Sentry::capture)

    private fun <T> suppressedWithConnection(default: () -> T, block: (Connection) -> T) =
        runCatching { connection.use(block) }.onFailure(Sentry::capture).getOrNull() ?: default()
}
