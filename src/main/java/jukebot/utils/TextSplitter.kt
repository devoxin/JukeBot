package jukebot.utils

object TextSplitter {
    fun split(content: String, limit: Int = 2000): Array<String> {
        if (content.length < limit) {
            return arrayOf(content)
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

        if (chunk.isNotEmpty())
            pages.add(chunk.toString())

        return pages.toTypedArray()
    }
}
