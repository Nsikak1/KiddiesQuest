package ng.name.nsikak.kidquest.core

import kotlin.math.ceil
import kotlin.random.Random

/** Pure Kotlin game rules for "Draw Your Sword" (the Bible drill). No Android imports. */

object GameRules {
    const val ROUNDS_PER_MISSION = 5
    const val DEFAULT_TIME_LIMIT_SEC = 120
    const val TIME_LIMIT_MS = DEFAULT_TIME_LIMIT_SEC * 1000L
    const val NO_LIMIT_MS = 1_000_000_000L
    const val WRONG_GUESS_PENALTY_MS = 5_000L

    /** Seconds the grown-up can choose in Settings. 0 means "no limit". */
    val TIME_LIMIT_CHOICES = listOf(60, 90, 120, 180, 0)

    fun limitMs(seconds: Int): Long = if (seconds <= 0) NO_LIMIT_MS else seconds * 1000L
}

enum class PlayMode(val label: String) {
    PHYSICAL("Physical Bible"),
    IN_APP("In-app Bible"),
}

/** What the child has to find. [timeFactor] shrinks the star time limits for the easier targets. */
enum class DrillType(val label: String, val blurb: String, val timeFactor: Double) {
    VERSE("Verse drill", "Find the exact verse.", 1.0),
    CHAPTER("Chapter drill", "Find the book and the chapter.", 0.75),
    BOOK("Book drill", "Find just the book.", 0.5),
    MIXED("Mixed drill", "Book, chapter and verse rounds all mixed up.", 1.0),
    ;

    /** MIXED turns into one real kind for each round. */
    fun concrete(random: Random): DrillType =
        if (this == MIXED) listOf(BOOK, CHAPTER, VERSE).random(random) else this

    fun matches(chosen: VerseRef, target: VerseRef): Boolean = when (this) {
        BOOK -> chosen.book == target.book
        CHAPTER -> chosen.book == target.book && chosen.chapter == target.chapter
        else -> chosen == target
    }
}

enum class HintMode {
    /** The Old/New Testament hint is always visible and free. */
    ALWAYS_SHOWN,

    /** The hint is hidden until tapped, and tapping costs one star. */
    TAP_COSTS_STAR,

    /** No hint at all. */
    NONE,
}

/** A grown-up's override of each level's own hint rule. */
enum class HintRule(val label: String, val blurb: String) {
    STANDARD("Standard", "Easy: free hint. Medium: a hint costs a star. Hard: no hint."),
    FREE("Always free", "Every level shows the hint for free."),
    COSTS_STAR("Costs a star", "Every level hides the hint; tapping it costs a star."),
    OFF("No hints", "Nobody gets a hint."),
    ;

    fun resolve(difficulty: Difficulty): HintMode = when (this) {
        STANDARD -> difficulty.hintMode
        FREE -> HintMode.ALWAYS_SHOWN
        COSTS_STAR -> HintMode.TAP_COSTS_STAR
        OFF -> HintMode.NONE
    }
}

enum class Difficulty(
    val label: String,
    val blurb: String,
    val defaultCountdownSec: Int,
    val hintMode: HintMode,
    val threeStarSec: Int,
    val twoStarSec: Int,
) {
    EASY("Easy", "Famous verses. Get-ready countdown. Free hint.", 10, HintMode.ALWAYS_SHOWN, 30, 60),
    MEDIUM("Medium", "More verses. No countdown. A hint costs a star.", 0, HintMode.TAP_COSTS_STAR, 20, 45),
    HARD("Hard", "Any verse in the Bible. No countdown. No hint.", 0, HintMode.NONE, 15, 40),
}

object Scoring {
    /** Stars for a correctly found target. A correct find is always worth at least one star. */
    fun starsFor(
        difficulty: Difficulty,
        timeMs: Long,
        hintUsed: Boolean,
        hintMode: HintMode = difficulty.hintMode,
        type: DrillType = DrillType.VERSE,
    ): Int {
        val seconds = timeMs / 1000.0
        val factor = type.timeFactor
        var stars = when {
            seconds <= difficulty.threeStarSec * factor -> 3
            seconds <= difficulty.twoStarSec * factor -> 2
            else -> 1
        }
        if (hintUsed && hintMode == HintMode.TAP_COSTS_STAR) stars -= 1
        return stars.coerceAtLeast(1)
    }
}

/**
 * The stopwatch for one round. The caller passes in "now" (e.g. SystemClock.elapsedRealtime()),
 * so the whole thing can be tested without a real clock.
 */
class RoundClock(
    private val countdownMs: Long,
    private val limitMs: Long = GameRules.TIME_LIMIT_MS,
) {
    enum class State { READY, COUNTDOWN, HUNTING, STOPPED, TIME_UP }

    var state: State = State.READY
        private set

    private var huntStartAt = 0L
    private var penaltyMs = 0L
    private var finalElapsedMs = 0L

    fun start(now: Long) {
        huntStartAt = now + countdownMs
        penaltyMs = 0L
        finalElapsedMs = 0L
        state = if (countdownMs > 0) State.COUNTDOWN else State.HUNTING
    }

    fun tick(now: Long): State {
        if (state == State.COUNTDOWN && now >= huntStartAt) state = State.HUNTING
        if (state == State.HUNTING && rawElapsed(now) + penaltyMs >= limitMs) {
            finalElapsedMs = limitMs
            state = State.TIME_UP
        }
        return state
    }

    fun countdownSecondsLeft(now: Long): Int {
        if (state != State.COUNTDOWN) return 0
        return ceil((huntStartAt - now) / 1000.0).toInt().coerceAtLeast(0)
    }

    fun elapsedMs(now: Long): Long = when (state) {
        State.READY, State.COUNTDOWN -> 0L
        State.HUNTING -> (rawElapsed(now) + penaltyMs).coerceAtMost(limitMs)
        State.STOPPED, State.TIME_UP -> finalElapsedMs
    }

    fun addPenalty(ms: Long) {
        if (state == State.HUNTING) penaltyMs += ms
    }

    /** Stops the clock and returns the final time (including penalties). */
    fun stop(now: Long): Long {
        if (state == State.HUNTING) {
            finalElapsedMs = (rawElapsed(now) + penaltyMs).coerceAtMost(limitMs)
            state = State.STOPPED
        }
        return finalElapsedMs
    }

    private fun rawElapsed(now: Long): Long = (now - huntStartAt).coerceAtLeast(0L)
}

enum class Outcome { FOUND, WRONG_PLACE, GAVE_UP, TIME_UP }

data class RoundResult(
    val ref: VerseRef,
    val outcome: Outcome,
    val timeMs: Long,
    val stars: Int,
    val hintUsed: Boolean,
    val wrongGuesses: Int,
    val type: DrillType = DrillType.VERSE,
)

class Mission(
    val difficulty: Difficulty,
    val mode: PlayMode,
    val rounds: Int = GameRules.ROUNDS_PER_MISSION,
    val drill: DrillType = DrillType.VERSE,
) {
    private val list = mutableListOf<RoundResult>()
    val results: List<RoundResult> get() = list

    fun record(result: RoundResult) {
        list.add(result)
    }

    val isComplete: Boolean get() = list.size >= rounds
    val totalStars: Int get() = list.sumOf { it.stars }
    val maxStars: Int get() = rounds * 3
    val bestTimeMs: Long? get() = list.filter { it.stars > 0 }.minOfOrNull { it.timeMs }
}

enum class Rank(val title: String, val minStars: Int) {
    RECRUIT("Recruit", 0),
    PAGE("Page", 10),
    SQUIRE("Squire", 30),
    KNIGHT("Knight", 70),
    CAPTAIN("Captain", 130),
    CHAMPION("Champion", 250),
    ;

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forStars(stars: Int): Rank = entries.last { stars >= it.minStars }
    }
}

/** Picks a random target for a difficulty and drill type, avoiding the last few picks. Works from the index only. */
class VersePicker(
    private val index: BibleIndex,
    private val random: Random = Random.Default,
) {
    private val recent = ArrayDeque<VerseRef>()

    /** For BOOK drills the result is (book, 1, 1); for CHAPTER drills it is (book, chapter, 1). */
    fun next(difficulty: Difficulty, type: DrillType = DrillType.VERSE): VerseRef {
        var ref = pick(difficulty, type)
        var tries = 0
        while ((ref in recent || ref !in index) && tries < 50) {
            ref = pick(difficulty, type)
            tries++
        }
        if (ref !in index) ref = normalize(anyVerse(), type)
        recent.addLast(ref)
        if (recent.size > RECENT_MEMORY) recent.removeFirst()
        return ref
    }

    private fun pick(difficulty: Difficulty, type: DrillType): VerseRef {
        if (type == DrillType.BOOK && difficulty == Difficulty.HARD) {
            return VerseRef(random.nextInt(index.books.size) + 1, 1, 1) // every book equally likely
        }
        return normalize(candidate(difficulty), type)
    }

    private fun normalize(ref: VerseRef, type: DrillType): VerseRef = when (type) {
        DrillType.BOOK -> VerseRef(ref.book, 1, 1)
        DrillType.CHAPTER -> VerseRef(ref.book, ref.chapter, 1)
        else -> ref
    }

    private fun candidate(difficulty: Difficulty): VerseRef = when (difficulty) {
        Difficulty.EASY -> FamousVerses.list.random(random)
        Difficulty.MEDIUM -> fromBooks(FamousVerses.mediumBooks)
        Difficulty.HARD -> anyVerse()
    }

    private fun fromBooks(books: List<Int>): VerseRef {
        val book = index.book(books.random(random))
        val chapter = random.nextInt(book.chapterCount) + 1
        val verse = random.nextInt(book.versesIn(chapter)) + 1
        return VerseRef(book.number, chapter, verse)
    }

    /** Every verse in the Bible is equally likely. */
    private fun anyVerse(): VerseRef {
        var pick = random.nextInt(index.totalVerses)
        for (book in index.books) {
            for (chapter in 1..book.chapterCount) {
                val count = book.versesIn(chapter)
                if (pick < count) return VerseRef(book.number, chapter, pick + 1)
                pick -= count
            }
        }
        return VerseRef(1, 1, 1)
    }

    private companion object {
        const val RECENT_MEMORY = 10
    }
}
