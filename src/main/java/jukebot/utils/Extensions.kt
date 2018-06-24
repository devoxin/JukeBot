package jukebot.utils

import okhttp3.Response
import org.json.JSONObject

fun Response.json(): JSONObject? {
    val body = body()
    val ret: JSONObject? = if (isSuccessful && body != null) JSONObject(body()!!.string()) else null

    close()
    return ret
}
