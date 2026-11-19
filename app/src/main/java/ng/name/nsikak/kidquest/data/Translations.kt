package ng.name.nsikak.kidquest.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonWriter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** One downloadable translation. The list lives in assets/bibles/catalog.json, so adding one needs no code change. */
data class CatalogEntry(
    val id: String,
    val name: String,
    val language: String,
    val url: String,
    val approxMb: Double,
    val note: String,
)

object TranslationCatalog {
    fun load(context: Context): List<CatalogEntry> = try {
        val text = context.assets.open("bibles/catalog.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogEntry(
                id = o.getString("id"),
                name = o.getString("name"),
                language = o.optString("language", ""),
                url = o.getString("url"),
                approxMb = o.optDouble("approxMb", 0.0),
                note = o.optString("note", ""),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Downloads a translation published as one big JSON file of the form
 *   [ {"name":"Genesis","chapters":[["verse","verse"], ...]}, ... 66 books ]
 * It is read as a stream (never fully in memory) and written out as the small per-book files the app uses.
 */
object TranslationDownloader {

    /** Blocking. Call from a background thread. Throws IOException on any problem and leaves nothing half-installed. */
    fun download(repo: BibleRepository, entry: CatalogEntry, onProgress: (Float) -> Unit) {
        val root = repo.biblesDirectory()
        root.mkdirs()
        val tmp = File(root, entry.id + ".tmp")
        tmp.deleteRecursively()
        tmp.mkdirs()

        val connection = URL(entry.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode != 200) throw IOException("The server answered ${connection.responseCode}.")
            val total = connection.contentLength.toLong()
            val counting = CountingInputStream(connection.inputStream)
            val reader = JsonReader(InputStreamReader(skipBom(counting), Charsets.UTF_8))

            val bookNames = ArrayList<String>()
            val bookCounts = ArrayList<List<Int>>()

            reader.beginArray()
            while (reader.hasNext()) {
                var name = ""
                val chapters = ArrayList<List<String>>()
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "name" -> name = reader.nextString()
                        "chapters" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                val verses = ArrayList<String>()
                                reader.beginArray()
                                while (reader.hasNext()) verses.add(reader.nextString().trim())
                                reader.endArray()
                                chapters.add(verses)
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (chapters.isEmpty()) throw IOException("Book ${bookNames.size + 1} has no chapters.")
                bookNames.add(name)
                bookCounts.add(chapters.map { it.size })
                writeBook(File(tmp, bookFileName(bookNames.size)), chapters)

                val fraction = if (total > 0) counting.count.toFloat() / total else bookNames.size / 66f
                onProgress(fraction.coerceIn(0f, 0.99f))
            }
            reader.endArray()
            reader.close()

            if (bookNames.size != 66) throw IOException("Expected 66 books but found ${bookNames.size}.")

            val books = JSONArray()
            for (i in bookNames.indices) {
                books.put(JSONObject().put("n", bookNames[i]).put("c", JSONArray(bookCounts[i])))
            }
            File(tmp, "index.json").writeText(
                JSONObject().put("id", entry.id).put("name", entry.name).put("books", books).toString(),
                Charsets.UTF_8,
            )

            val dest = File(root, entry.id)
            dest.deleteRecursively()
            if (!tmp.renameTo(dest)) throw IOException("Could not save the translation.")
            repo.forget(entry.id)
            onProgress(1f)
        } catch (e: Exception) {
            tmp.deleteRecursively()
            if (e is IOException) throw e
            throw IOException("Download failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }

    private fun writeBook(file: File, chapters: List<List<String>>) {
        JsonWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)).use { w ->
            w.beginArray()
            for (chapter in chapters) {
                w.beginArray()
                for (verse in chapter) w.value(verse)
                w.endArray()
            }
            w.endArray()
        }
    }

    /** Some JSON files start with a UTF-8 byte-order mark, which the JSON reader would choke on. */
    private fun skipBom(input: InputStream): InputStream {
        val buffered = BufferedInputStream(input)
        buffered.mark(3)
        val head = ByteArray(3)
        val n = buffered.read(head, 0, 3)
        val isBom = n == 3 && head[0] == 0xEF.toByte() && head[1] == 0xBB.toByte() && head[2] == 0xBF.toByte()
        if (!isBom) buffered.reset()
        return buffered
    }

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var count = 0L
            private set

        override fun read(): Int {
            val b = super.read()
            if (b >= 0) count++
            return b
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val n = super.read(buffer, offset, length)
            if (n > 0) count += n
            return n
        }
    }
}

sealed interface DownloadStatus {
    data class Running(val progress: Float) : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
}

/** App-wide, so a download keeps going while the child moves between screens. */
class DownloadTracker(private val repo: BibleRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val status = mutableStateMapOf<String, DownloadStatus>()

    /** Bumps whenever a translation is installed or removed, so lists can refresh. */
    var installedVersion by mutableIntStateOf(0)
        private set

    fun start(entry: CatalogEntry) {
        if (status[entry.id] is DownloadStatus.Running) return
        status[entry.id] = DownloadStatus.Running(0f)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    TranslationDownloader.download(repo, entry) { p -> status[entry.id] = DownloadStatus.Running(p) }
                }
                status.remove(entry.id)
            } catch (e: Exception) {
                status[entry.id] = DownloadStatus.Failed(e.message ?: "Download failed.")
            }
            installedVersion++
        }
    }

    fun delete(id: String) {
        repo.delete(id)
        status.remove(id)
        installedVersion++
    }
}
