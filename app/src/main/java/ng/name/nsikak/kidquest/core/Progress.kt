package ng.name.nsikak.kidquest.core

/** Everything the app remembers about a child. Pure Kotlin, so saving/restoring is unit-tested. */

data class BookStat(val found: Int, val totalMs: Long, val bestMs: Long) {
    val avgMs: Long get() = if (found == 0) 0L else totalMs / found
}

data class VerseStat(val count: Int, val totalMs: Long) {
    val avgMs: Long get() = if (count == 0) 0L else totalMs / count
}

object Streak {
    /** The streak after finishing today's challenge. */
    fun next(lastDay: Long, streak: Int, today: Long): Int = when (lastDay) {
        today -> streak
        today - 1 -> streak + 1
        else -> 1
    }

    /** The streak to show right now (it has lapsed if yesterday was missed). */
    fun current(lastDay: Long, streak: Int, today: Long): Int =
        if (lastDay == today || lastDay == today - 1) streak else 0
}

class ProgressData {
    var totalStars = 0
    var missions = 0
    val bests = LinkedHashMap<String, Long>()
    val books = LinkedHashMap<Int, BookStat>()
    val verses = LinkedHashMap<String, VerseStat>()
    var streak = 0
    var bestStreak = 0
    var lastDailyDay = -1L
    var trainerBest = 0
    var trainerRuns = 0
    var memoryWins = 0
    val storiesRead = LinkedHashSet<Int>()

    private fun bestKey(d: Difficulty, m: PlayMode, t: DrillType) = "${d.name}_${m.name}_${t.name}"

    fun bestMs(d: Difficulty, m: PlayMode, t: DrillType): Long? = bests[bestKey(d, m, t)]

    /**
     * Records one finished round. Returns true only when it beat an earlier personal best
     * (the very first result sets the bar, it is not a "record").
     */
    fun recordRound(d: Difficulty, m: PlayMode, t: DrillType, ref: VerseRef, stars: Int, timeMs: Long): Boolean {
        totalStars += stars
        if (stars <= 0 || timeMs <= 0) return false

        val key = bestKey(d, m, t)
        val old = bests[key]
        val isRecord = old != null && timeMs < old
        if (old == null || timeMs < old) bests[key] = timeMs

        // Times are scaled so a Book drill and a Verse drill can be compared fairly.
        val fair = (timeMs / t.timeFactor).toLong()
        val b = books[ref.book]
        books[ref.book] =
            if (b == null) BookStat(1, fair, fair) else BookStat(b.found + 1, b.totalMs + fair, minOf(b.bestMs, fair))

        if (t == DrillType.VERSE) {
            val k = "${ref.book}:${ref.chapter}:${ref.verse}"
            val v = verses[k]
            verses[k] = if (v == null) VerseStat(1, timeMs) else VerseStat(v.count + 1, v.totalMs + timeMs)
            trimVerses()
        }
        return isRecord
    }

    private fun trimVerses() {
        if (verses.size <= MAX_VERSES) return
        val keep = verses.entries.sortedByDescending { it.value.avgMs }.take(KEEP_VERSES).map { it.key }.toSet()
        verses.keys.retainAll(keep)
    }

    val litBooks: Set<Int> get() = books.keys

    fun slowestBooks(limit: Int = 5): List<Pair<Int, BookStat>> =
        books.entries.sortedByDescending { it.value.avgMs }.take(limit).map { it.key to it.value }

    fun slowestVerses(limit: Int = 5): List<Pair<VerseRef, VerseStat>> =
        verses.entries
            .mapNotNull { e -> parseRef(e.key)?.let { it to e.value } }
            .sortedByDescending { it.second.avgMs }
            .take(limit)

    fun recordMission() {
        missions++
    }

    fun recordDaily(today: Long): Int {
        streak = Streak.next(lastDailyDay, streak, today)
        lastDailyDay = today
        if (streak > bestStreak) bestStreak = streak
        return streak
    }

    fun dailyDone(today: Long): Boolean = lastDailyDay == today

    fun currentStreak(today: Long): Int = Streak.current(lastDailyDay, streak, today)

    fun recordTrainer(correct: Int, total: Int): Int {
        trainerRuns++
        if (correct > trainerBest) trainerBest = correct
        val stars = Trainer.starsFor(correct, total)
        totalStars += stars
        return stars
    }

    fun addBonus(stars: Int) {
        totalStars += stars
    }

    fun recordMemory(bonusStars: Int) {
        if (bonusStars > 0) memoryWins++
        totalStars += bonusStars
    }

    /** Marks a story as read. Returns true (and gives one star) only the first time. */
    fun markStoryRead(id: Int): Boolean {
        if (!storiesRead.add(id)) return false
        totalStars += 1
        return true
    }

    // ---- backup file (plain text, one "key=value" per line) ----

    fun toText(): String = buildString {
        appendLine(HEADER)
        appendLine("stars=$totalStars")
        appendLine("missions=$missions")
        appendLine("streak=$streak")
        appendLine("bestStreak=$bestStreak")
        appendLine("lastDaily=$lastDailyDay")
        appendLine("trainerBest=$trainerBest")
        appendLine("trainerRuns=$trainerRuns")
        appendLine("memoryWins=$memoryWins")
        for ((k, v) in bests) appendLine("best=$k:$v")
        for ((k, v) in books) appendLine("book=$k,${v.found},${v.totalMs},${v.bestMs}")
        for ((k, v) in verses) appendLine("verse=$k,${v.count},${v.totalMs}")
        for (id in storiesRead) appendLine("story=$id")
    }

    companion object {
        const val HEADER = "KIDQUEST-BACKUP 1"
        private const val MAX_VERSES = 300
        private const val KEEP_VERSES = 250

        fun parseRef(text: String): VerseRef? {
            val p = text.split(":")
            if (p.size != 3) return null
            val b = p[0].toIntOrNull() ?: return null
            val c = p[1].toIntOrNull() ?: return null
            val v = p[2].toIntOrNull() ?: return null
            return VerseRef(b, c, v)
        }

        /** Returns null if the text is not a Kid Quest backup. Unknown or damaged lines are skipped. */
        fun fromText(text: String): ProgressData? {
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty() || lines[0] != HEADER) return null
            val d = ProgressData()
            for (line in lines.drop(1)) {
                val eq = line.indexOf('=')
                if (eq <= 0) continue
                val key = line.substring(0, eq)
                val value = line.substring(eq + 1)
                when (key) {
                    "stars" -> value.toIntOrNull()?.let { if (it >= 0) d.totalStars = it }
                    "missions" -> value.toIntOrNull()?.let { if (it >= 0) d.missions = it }
                    "streak" -> value.toIntOrNull()?.let { if (it >= 0) d.streak = it }
                    "bestStreak" -> value.toIntOrNull()?.let { if (it >= 0) d.bestStreak = it }
                    "lastDaily" -> value.toLongOrNull()?.let { d.lastDailyDay = it }
                    "trainerBest" -> value.toIntOrNull()?.let { if (it >= 0) d.trainerBest = it }
                    "trainerRuns" -> value.toIntOrNull()?.let { if (it >= 0) d.trainerRuns = it }
                    "memoryWins" -> value.toIntOrNull()?.let { if (it >= 0) d.memoryWins = it }
                    "best" -> {
                        val i = value.lastIndexOf(':')
                        val ms = if (i > 0) value.substring(i + 1).toLongOrNull() else null
                        if (ms != null && ms > 0) d.bests[value.substring(0, i)] = ms
                    }
                    "book" -> {
                        val p = value.split(",")
                        val n = p.getOrNull(0)?.toIntOrNull()
                        val f = p.getOrNull(1)?.toIntOrNull()
                        val t = p.getOrNull(2)?.toLongOrNull()
                        val b = p.getOrNull(3)?.toLongOrNull()
                        if (p.size == 4 && n != null && f != null && t != null && b != null && n in 1..66 && f > 0) {
                            d.books[n] = BookStat(f, t, b)
                        }
                    }
                    "verse" -> {
                        val p = value.split(",")
                        val ref = p.getOrNull(0)?.let { parseRef(it) }
                        val c = p.getOrNull(1)?.toIntOrNull()
                        val t = p.getOrNull(2)?.toLongOrNull()
                        if (p.size == 3 && ref != null && c != null && t != null && c > 0) {
                            d.verses[p[0]] = VerseStat(c, t)
                        }
                    }
                    "story" -> value.toIntOrNull()?.let { d.storiesRead.add(it) }
                }
            }
            return d
        }
    }
}
