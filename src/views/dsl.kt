package views

val newline = "\r?\n".toRegex()

operator fun Boolean.div(s: String) = if (this) s else ""
operator fun Any?.div(s: String) = if (this != null) s else ""

fun String?.escapeHTML() = this?.replace("&", "&amp;")?.replace("<", "&lt;")?.replace("\"", "&quot;") ?: ""
fun String?.escapeJS() = this?.replace("'", "\\'") ?: ""

operator fun String?.unaryPlus() = escapeHTML()

fun <T> Collection<T>.each(itemTemplate: T.() -> String) = joinToString("", transform = itemTemplate)
