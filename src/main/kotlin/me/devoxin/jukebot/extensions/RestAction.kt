package me.devoxin.jukebot.extensions

import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.requests.RestAction

suspend fun <T> RestAction<T>.await() = submit().await()
