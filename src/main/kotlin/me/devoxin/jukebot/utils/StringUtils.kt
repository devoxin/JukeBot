package me.devoxin.jukebot.utils

object StringUtils {
    fun split(content: String, limit: Int = 2000): List<String> {
        if (content.length < limit) {
            return listOf(content)
        }

        val pages = ArrayList<String>()

        val lines = content.trim().split("\n").dropLastWhile { it.isEmpty() }.toTypedArray()
        val chunk = StringBuilder(limit)

        for (line in lines) {
            if (chunk.isNotEmpty() && chunk.length + line.length > limit) {
                pages.add(chunk.toString())
                chunk.setLength(0)
            }

            if (line.length > limit) {
                val lineChunks = line.length / limit

                for (i in 0 until lineChunks) {
                    val start = limit * i
                    val end = start + limit
                    pages.add(line.substring(start, end))
                }
            } else {
                chunk.append(line).append("\n")
            }
        }

        if (chunk.isNotEmpty()) {
            pages.add(chunk.toString())
        }

        return pages
    }

    fun isSubstringWithin(s: String, substring: String): Boolean {
        var lastIndex = 0

        return substring.chars()
            .allMatch { (s.indexOf(it.toChar(), lastIndex).also { i -> lastIndex = i }) != -1 }
    }
}
