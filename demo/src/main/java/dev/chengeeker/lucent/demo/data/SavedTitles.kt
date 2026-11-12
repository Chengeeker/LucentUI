package dev.chengeeker.lucent.demo.data

internal fun toggleSavedTitle(saved: String, title: String): String {
    require(title.isNotBlank() && '\n' !in title && '\r' !in title)
    val titles = saved.lineSequence().filter { it.isNotBlank() }.toMutableSet()
    if (!titles.remove(title)) titles.add(title)
    return titles.joinToString("\n")
}
