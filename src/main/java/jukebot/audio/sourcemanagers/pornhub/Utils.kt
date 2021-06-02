package jukebot.audio.sourcemanagers.pornhub

object Utils {
    private val formatPattern = "(var\\s+(?:media|quality)_.+)".toPattern()
    private val mediaStringPattern = "(var.+?mediastring[^<]+)".toPattern()
    private val cleanRegex = "/\\*(?:(?!\\*/).)*?\\*/".toRegex()
    private val cleanVarRegex = "var\\s+".toRegex()
    private val links = "https.+?(?=,upgrade)".toRegex()


    fun extractMediaString(page: String): String {
        val vars = hashMapOf<String, String>()
        val assignments = extractAssignments(page)

        for (assignment in assignments) {
            val trimmed = assignment.trim()

            if (trimmed.isEmpty()) {
                continue
            }

            val noVar = trimmed.replace(cleanVarRegex, "")
            val (name, value) = noVar.split('=', limit = 2)

            vars[name] = parseSegment(value, vars)
        }

        val formats = vars.filter { it.key.startsWith("media") || it.key.startsWith("qualityItems_") }

        val validFormats = formats.filter { it.key.startsWith("qualityItems")}
        if(validFormats.isEmpty()) {
            throw IllegalStateException("Failed to parse valid formats.")
        } else if (validFormats.size > 1) {
            throw IllegalStateException("Contains too many entries from parse.")
        }

        val newLink = validFormats.iterator().next().value.replace("\\", "", ignoreCase = true)

        val values = links.findAll(newLink)
        return values.iterator().next().value
    }

    fun extractAssignments(script: String): List<String> {
        val formats = formatPattern.matcher(script)

        if (formats.find()) {
            return formats.group(1).split(';')
        }

        val assignments = mediaStringPattern.matcher(script)

        if (!assignments.find()) {
            throw IllegalStateException("No assignments or formats found within the script!")
        }

        return assignments.group(1).split(';')
    }

    fun parseSegment(segment: String, v: HashMap<String, String>): String {
        val cleaned = segment.replace(cleanRegex, "").trim()

        if (cleaned.contains('+')) {
            val subSegments = cleaned.split('+')
            return subSegments.joinToString("") { parseSegment(it, v) }
        }

        return v[cleaned]
            ?: cleaned.replace("'", "").replace("\"", "")
    }
}
