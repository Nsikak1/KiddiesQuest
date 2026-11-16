package ng.name.version5.core

/** Pure Kotlin (no Android imports) so it can be unit-tested on a normal JVM. */

enum class Testament(val label: String) {
    OLD("Old Testament"),
    NEW("New Testament"),
}

/** One book. [number] is 1..66 in the standard Protestant order; [verseCounts] has one entry per chapter. */
class BookInfo(val number: Int, val name: String, val verseCounts: List<Int>) {
    val chapterCount: Int get() = verseCounts.size
    val testament: Testament get() = if (number <= OLD_TESTAMENT_BOOKS) Testament.OLD else Testament.NEW

    /** "Psalms" reads better as "Psalm 23:1" in a reference. */
    val displayName: String get() = if (name == "Psalms") "Psalm" else name

    fun versesIn(chapter: Int): Int = verseCounts[chapter - 1]

    companion object {
        const val OLD_TESTAMENT_BOOKS = 39
    }
}

data class VerseRef(val book: Int, val chapter: Int, val verse: Int)

/** The shape of a translation (book names and verse counts), without the verse text. */
class BibleIndex(val id: String, val name: String, val books: List<BookInfo>) {
    fun book(number: Int): BookInfo = books[number - 1]

    val totalVerses: Int by lazy { books.sumOf { b -> b.verseCounts.sum() } }

    operator fun contains(ref: VerseRef): Boolean {
        if (ref.book !in 1..books.size) return false
        val b = book(ref.book)
        if (ref.chapter !in 1..b.chapterCount) return false
        return ref.verse in 1..b.versesIn(ref.chapter)
    }

    fun label(ref: VerseRef): String = "${book(ref.book).displayName} ${ref.chapter}:${ref.verse}"
}
