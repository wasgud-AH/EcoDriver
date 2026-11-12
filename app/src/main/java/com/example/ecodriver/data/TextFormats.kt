package com.example.ecodriver.data

// Pure string helpers. No Android imports, so they run in JVM unit tests.

/** kotlin-obd-api joins trouble codes with commas: "P0300,P0171". Blank or null means none. */
fun parseDtcs(raw: String?): List<String> =
    raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

/** One CSV line. Fields with a comma, quote or line break are quoted, with quotes doubled. */
fun csvRow(fields: List<Any?>): String = fields.joinToString(",") { f ->
    val s = f?.toString() ?: ""
    if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
