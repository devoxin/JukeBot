package me.devoxin.jukebot.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

object Scopes {
    val IO = CoroutineScope(Dispatchers.IO) + SupervisorJob()
}
