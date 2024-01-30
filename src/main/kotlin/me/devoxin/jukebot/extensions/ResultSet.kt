package me.devoxin.jukebot.extensions

import java.sql.ResultSet

operator fun ResultSet.get(key: String): String = this.getString(key)
