package ng.name.nsikak.kidquest.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ng.name.nsikak.kidquest.core.BibleIndex
import ng.name.nsikak.kidquest.core.BookInfo
import ng.name.nsikak.kidquest.core.VerseRef
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

/** The KJV ships inside the app (assets/bibles/kjv). Everything else is downloaded into filesDir/bibles/<id>. */
const val BUNDLED_ID = "kjv"

/** "01.json" .. "66.json". Built by hand (not String.format) so it never depends on the phone's locale digits. */
fun bookFileName(book: Int): String = (if (book < 10) "0$book" else "$book") + ".json"

data class InstalledBible(val id: String, val name: String, val bundled: Boolean)

/**
 * Both the bundled and the downloaded translations use the same tiny layout, so one reader handles both:
 *   index.json  -> {"id","name","books":[{"n":"Genesis","c":[31,25,...]}, ...]}
 *   01.json..66.json -> one book each: [[verse, verse, ...], [..], ...]  (chapters of verses)
 * Only one book is ever parsed at a time, which keeps memory low on older phones.
 */
class BibleRepository(private val context: Context) {
    private val indexCache = HashMap<String, BibleIndex>()
    private var cachedKey: String? = null
    private var cachedBook: JSONArray? = null

    fun biblesDirectory(): File = File(context.filesDir, "bibles")

    private fun open(id: String, file: String): InputStream =
        if (id == BUNDLED_ID) {
            context.assets.open("bibles/$id/$file")
        } else {
            File(File(biblesDirectory(), id), file).inputStream()
        }

    fun installed(): List<InstalledBible> {
        val result = ArrayList<InstalledBible>()
        result.add(InstalledBible(BUNDLED_ID, index(BUNDLED_ID).name, true))
        val dirs = biblesDirectory().listFiles()?.sortedBy { it.name } ?: emptyList()
        for (dir in dirs) {
            if (!dir.isDirectory || dir.name.endsWith(".tmp") || dir.name == BUNDLED_ID) continue
            if (!File(dir, "index.json").exists()) continue
            try {
                result.add(InstalledBible(dir.name, index(dir.name).name, false))
            } catch (e: Exception) {
                // A damaged download is simply not listed.
            }
        }
        return result
    }

    fun isInstalled(id: String): Boolean = installed().any { it.id == id }

    fun index(id: String): BibleIndex = synchronized(this) {
        indexCache.getOrPut(id) {
            val text = open(id, "index.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val arr = root.getJSONArray("books")
            val books = (0 until arr.length()).map { i ->
                val b = arr.getJSONObject(i)
                val counts = b.getJSONArray("c")
                BookInfo(i + 1, b.getString("n"), (0 until counts.length()).map { counts.getInt(it) })
            }
            BibleIndex(id, root.getString("name"), books)
        }
    }

    /** All verses of one chapter (verse 1 is element 0). Parsed off the main thread. */
    suspend fun chapter(id: String, book: Int, chapter: Int): List<String> = withContext(Dispatchers.IO) {
        val arr: JSONArray = synchronized(this@BibleRepository) {
            val key = "$id/$book"
            if (cachedKey != key || cachedBook == null) {
                val text = open(id, bookFileName(book)).bufferedReader().use { it.readText() }
                cachedBook = JSONArray(text)
                cachedKey = key
            }
            cachedBook!!
        }
        val verses = arr.getJSONArray(chapter - 1)
        (0 until verses.length()).map { verses.getString(it) }
    }

    suspend fun verse(id: String, ref: VerseRef): String =
        chapter(id, ref.book, ref.chapter).getOrNull(ref.verse - 1) ?: ""

    fun delete(id: String) {
        if (id == BUNDLED_ID) return
        synchronized(this) {
            File(biblesDirectory(), id).deleteRecursively()
            indexCache.remove(id)
            cachedKey = null
            cachedBook = null
        }
    }

    /** Called after a download finishes so a fresh index is read from disk. */
    fun forget(id: String) {
        synchronized(this) {
            indexCache.remove(id)
            cachedKey = null
            cachedBook = null
        }
    }
}
