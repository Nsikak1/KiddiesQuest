package ng.name.version5

import ng.name.version5.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Loads the real bundled KJV index (the unit-test working directory is the app/ module folder). */
private fun loadKjvIndex(): BibleIndex {
    val text = File("src/main/assets/bibles/kjv/index.json").readText()
    val book = Regex("\"n\":\"([^\"]+)\",\"c\":\\[([0-9,]+)]")
    val books = book.findAll(text).mapIndexed { i, m ->
        BookInfo(i + 1, m.groupValues[1], m.groupValues[2].split(",").map { it.toInt() })
    }.toList()
    return BibleIndex("kjv", "King James Version", books)
}

class CoreLogicTest {
    private val kjv = loadKjvIndex()

    @Test fun bundledIndexIsComplete() {
        assertEquals(66, kjv.books.size)
        assertEquals(1189, kjv.books.sumOf { it.chapterCount })
        assertEquals(31102, kjv.totalVerses)
        assertEquals("Genesis", kjv.book(1).name)
        assertEquals("Revelation", kjv.book(66).name)
    }

    @Test fun testamentSplit() {
        assertEquals(Testament.OLD, kjv.book(39).testament)
        assertEquals(Testament.NEW, kjv.book(40).testament)
    }

    @Test fun referenceLabels() {
        assertEquals("John 3:16", kjv.label(VerseRef(43, 3, 16)))
        assertEquals("Psalm 23:1", kjv.label(VerseRef(19, 23, 1)))
        assertEquals("1 Corinthians 13:13", kjv.label(VerseRef(46, 13, 13)))
    }

    @Test fun containsRejectsImpossibleVerses() {
        assertTrue(VerseRef(43, 3, 16) in kjv)
        assertFalse(VerseRef(43, 3, 99) in kjv)
        assertFalse(VerseRef(43, 99, 1) in kjv)
        assertFalse(VerseRef(67, 1, 1) in kjv)
        assertFalse(VerseRef(1, 1, 0) in kjv)
    }

    @Test fun everyFamousVerseExists() {
        for (ref in FamousVerses.list) assertTrue("Missing $ref", ref in kjv)
        assertEquals(FamousVerses.list.size, FamousVerses.list.toSet().size)
        assertTrue(FamousVerses.list.any { kjv.book(it.book).testament == Testament.OLD })
        assertTrue(FamousVerses.list.any { kjv.book(it.book).testament == Testament.NEW })
    }

    @Test fun pickerAlwaysReturnsRealVerses() {
        val picker = VersePicker(kjv, Random(42))
        for (d in Difficulty.entries) repeat(300) { assertTrue(picker.next(d) in kjv) }
    }

    @Test fun easyPicksOnlyFamousVerses() {
        val picker = VersePicker(kjv, Random(1))
        repeat(200) { assertTrue(picker.next(Difficulty.EASY) in FamousVerses.list) }
    }

    @Test fun mediumStaysInPopularBooks() {
        val picker = VersePicker(kjv, Random(2))
        repeat(200) { assertTrue(picker.next(Difficulty.MEDIUM).book in FamousVerses.mediumBooks) }
    }

    @Test fun hardCanReachObscureBooks() {
        val picker = VersePicker(kjv, Random(3))
        val books = (1..3000).map { picker.next(Difficulty.HARD).book }.toSet()
        assertTrue(books.size > 55)
    }

    @Test fun pickerAvoidsImmediateRepeats() {
        val picker = VersePicker(kjv, Random(5))
        val picks = (1..200).map { picker.next(Difficulty.EASY) }
        for (i in 1 until picks.size) assertTrue(picks[i] != picks[i - 1])
    }

    @Test fun starsByTime() {
        assertEquals(3, Scoring.starsFor(Difficulty.EASY, 30_000, false))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 30_001, false))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 60_000, false))
        assertEquals(1, Scoring.starsFor(Difficulty.EASY, 60_001, false))
        assertEquals(3, Scoring.starsFor(Difficulty.HARD, 15_000, false))
        assertEquals(1, Scoring.starsFor(Difficulty.HARD, 41_000, false))
    }

    @Test fun hintCostsAStarOnlyOnMedium() {
        assertEquals(3, Scoring.starsFor(Difficulty.EASY, 5_000, true))   // Easy: free hint
        assertEquals(2, Scoring.starsFor(Difficulty.MEDIUM, 5_000, true)) // Medium: costs one star
        assertEquals(3, Scoring.starsFor(Difficulty.MEDIUM, 5_000, false))
        assertEquals(1, Scoring.starsFor(Difficulty.MEDIUM, 90_000, true)) // never below 1 when found
    }

    @Test fun clockWithCountdown() {
        val c = RoundClock(countdownMs = 10_000)
        c.start(1_000)
        assertEquals(RoundClock.State.COUNTDOWN, c.state)
        assertEquals(10, c.countdownSecondsLeft(1_000))
        assertEquals(1, c.countdownSecondsLeft(10_500))
        assertEquals(RoundClock.State.COUNTDOWN, c.tick(10_999))
        assertEquals(0L, c.elapsedMs(10_999))
        assertEquals(RoundClock.State.HUNTING, c.tick(11_000))
        assertEquals(2_500L, c.elapsedMs(13_500))
        assertEquals(4_000L, c.stop(15_000))
        assertEquals(RoundClock.State.STOPPED, c.state)
        assertEquals(4_000L, c.elapsedMs(99_000)) // frozen after stop
    }

    @Test fun clockWithoutCountdownStartsHunting() {
        val c = RoundClock(countdownMs = 0)
        c.start(500)
        assertEquals(RoundClock.State.HUNTING, c.state)
        assertEquals(1_000L, c.elapsedMs(1_500))
    }

    @Test fun penaltiesAddTimeAndTimeLimitEnds() {
        val c = RoundClock(countdownMs = 0, limitMs = 20_000)
        c.start(0)
        c.addPenalty(GameRules.WRONG_GUESS_PENALTY_MS)
        assertEquals(8_000L, c.elapsedMs(3_000))
        assertEquals(RoundClock.State.HUNTING, c.tick(14_999))
        assertEquals(RoundClock.State.TIME_UP, c.tick(15_000))
        assertEquals(20_000L, c.elapsedMs(50_000))
    }

    @Test fun penaltyIgnoredDuringCountdown() {
        val c = RoundClock(countdownMs = 5_000)
        c.start(0)
        c.addPenalty(5_000)
        c.tick(5_000)
        assertEquals(1_000L, c.elapsedMs(6_000))
    }

    @Test fun missionTotals() {
        val m = Mission(Difficulty.EASY, PlayMode.PHYSICAL)
        val ref = VerseRef(43, 3, 16)
        m.record(RoundResult(ref, Outcome.FOUND, 12_000, 3, false, 0))
        m.record(RoundResult(ref, Outcome.FOUND, 40_000, 2, false, 0))
        m.record(RoundResult(ref, Outcome.GAVE_UP, 20_000, 0, false, 0))
        assertEquals(5, m.totalStars)
        assertEquals(15, m.maxStars)
        assertEquals(12_000L, m.bestTimeMs)
        assertFalse(m.isComplete)
        m.record(RoundResult(ref, Outcome.FOUND, 1, 1, false, 0))
        m.record(RoundResult(ref, Outcome.FOUND, 1, 1, false, 0))
        assertTrue(m.isComplete)
    }

    @Test fun ranks() {
        assertEquals(Rank.RECRUIT, Rank.forStars(0))
        assertEquals(Rank.PAGE, Rank.forStars(10))
        assertEquals(Rank.SQUIRE, Rank.forStars(69))
        assertEquals(Rank.KNIGHT, Rank.forStars(70))
        assertEquals(Rank.CHAMPION, Rank.forStars(10_000))
        assertEquals(Rank.PAGE, Rank.RECRUIT.next)
        assertEquals(null, Rank.CHAMPION.next)
    }
}
