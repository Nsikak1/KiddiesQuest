package ng.name.nsikak.kidquest

import ng.name.nsikak.kidquest.core.*
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
    private val names = kjv.books.map { it.name }

    // ---------- Bible index ----------
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
        assertEquals("Psalm 23", kjv.label(VerseRef(19, 23, 1), DrillType.CHAPTER))
        assertEquals("Psalms", kjv.label(VerseRef(19, 1, 1), DrillType.BOOK))
        assertEquals("Luke 2:1-14", kjv.rangeLabel(42, 2, 1, 14))
        assertEquals("John 3:16", kjv.rangeLabel(43, 3, 16, 16))
    }

    @Test fun spokenLabels() {
        assertEquals("First Samuel, chapter 17, verse 4", kjv.spoken(VerseRef(9, 17, 4)))
        assertEquals("Psalm, chapter 23", kjv.spoken(VerseRef(19, 23, 1), DrillType.CHAPTER))
        assertEquals("Third John", kjv.spoken(VerseRef(64, 1, 1), DrillType.BOOK))
        assertEquals("John, chapter 3, verse 16", kjv.spoken(VerseRef(43, 3, 16)))
    }

    @Test fun containsRejectsImpossibleVerses() {
        assertTrue(VerseRef(43, 3, 16) in kjv)
        assertFalse(VerseRef(43, 3, 99) in kjv)
        assertFalse(VerseRef(43, 99, 1) in kjv)
        assertFalse(VerseRef(67, 1, 1) in kjv)
        assertFalse(VerseRef(1, 1, 0) in kjv)
    }

    // ---------- Pickers ----------
    @Test fun everyFamousVerseExists() {
        for (ref in FamousVerses.list) assertTrue("Missing $ref", ref in kjv)
        assertEquals(FamousVerses.list.size, FamousVerses.list.toSet().size)
        assertTrue(FamousVerses.list.any { kjv.book(it.book).testament == Testament.OLD })
        assertTrue(FamousVerses.list.any { kjv.book(it.book).testament == Testament.NEW })
    }

    @Test fun pickerAlwaysReturnsRealTargets() {
        val picker = VersePicker(kjv, Random(42))
        for (d in Difficulty.entries) for (t in listOf(DrillType.VERSE, DrillType.CHAPTER, DrillType.BOOK)) {
            repeat(200) { assertTrue(picker.next(d, t) in kjv) }
        }
    }

    @Test fun bookAndChapterTargetsAreNormalised() {
        val picker = VersePicker(kjv, Random(9))
        repeat(200) {
            val b = picker.next(Difficulty.MEDIUM, DrillType.BOOK)
            assertTrue(b.chapter == 1 && b.verse == 1)
            val c = picker.next(Difficulty.MEDIUM, DrillType.CHAPTER)
            assertEquals(1, c.verse)
        }
    }

    @Test fun easyPicksOnlyFamousVerses() {
        val picker = VersePicker(kjv, Random(1))
        repeat(200) { assertTrue(picker.next(Difficulty.EASY) in FamousVerses.list) }
        repeat(200) { assertTrue(picker.next(Difficulty.EASY, DrillType.BOOK).book in FamousVerses.books) }
    }

    @Test fun mediumStaysInPopularBooks() {
        val picker = VersePicker(kjv, Random(2))
        repeat(200) { assertTrue(picker.next(Difficulty.MEDIUM).book in FamousVerses.mediumBooks) }
    }

    @Test fun hardCanReachEveryKindOfBook() {
        val picker = VersePicker(kjv, Random(3))
        assertTrue((1..3000).map { picker.next(Difficulty.HARD).book }.toSet().size > 55)
        assertTrue((1..600).map { picker.next(Difficulty.HARD, DrillType.BOOK).book }.toSet().size > 55)
    }

    @Test fun pickerAvoidsImmediateRepeats() {
        val picker = VersePicker(kjv, Random(5))
        val picks = (1..200).map { picker.next(Difficulty.EASY) }
        for (i in 1 until picks.size) assertTrue(picks[i] != picks[i - 1])
    }

    @Test fun drillMatching() {
        val target = VerseRef(19, 23, 1)
        assertTrue(DrillType.BOOK.matches(VerseRef(19, 5, 5), target))
        assertFalse(DrillType.BOOK.matches(VerseRef(20, 23, 1), target))
        assertTrue(DrillType.CHAPTER.matches(VerseRef(19, 23, 9), target))
        assertFalse(DrillType.CHAPTER.matches(VerseRef(19, 24, 1), target))
        assertTrue(DrillType.VERSE.matches(VerseRef(19, 23, 1), target))
        assertFalse(DrillType.VERSE.matches(VerseRef(19, 23, 2), target))
        val kinds = (1..100).map { DrillType.MIXED.concrete(Random(it)) }.toSet()
        assertEquals(setOf(DrillType.BOOK, DrillType.CHAPTER, DrillType.VERSE), kinds)
    }

    // ---------- Scoring, hints, clock ----------
    @Test fun starsByTime() {
        assertEquals(3, Scoring.starsFor(Difficulty.EASY, 30_000, false))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 30_001, false))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 60_000, false))
        assertEquals(1, Scoring.starsFor(Difficulty.EASY, 60_001, false))
        assertEquals(3, Scoring.starsFor(Difficulty.HARD, 15_000, false))
        assertEquals(1, Scoring.starsFor(Difficulty.HARD, 41_000, false))
    }

    @Test fun easierTargetsNeedFasterTimes() {
        assertEquals(3, Scoring.starsFor(Difficulty.EASY, 15_000, false, type = DrillType.BOOK))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 15_001, false, type = DrillType.BOOK))
        assertEquals(3, Scoring.starsFor(Difficulty.EASY, 22_500, false, type = DrillType.CHAPTER))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 22_501, false, type = DrillType.CHAPTER))
    }

    @Test fun hintCostsAStarOnlyWhereTheRuleSaysSo() {
        assertEquals(3, Scoring.starsFor(Difficulty.EASY, 5_000, true))
        assertEquals(2, Scoring.starsFor(Difficulty.MEDIUM, 5_000, true))
        assertEquals(3, Scoring.starsFor(Difficulty.MEDIUM, 5_000, false))
        assertEquals(1, Scoring.starsFor(Difficulty.MEDIUM, 90_000, true))
        assertEquals(2, Scoring.starsFor(Difficulty.EASY, 5_000, true, hintMode = HintRule.COSTS_STAR.resolve(Difficulty.EASY)))
    }

    @Test fun hintRulesResolve() {
        assertEquals(HintMode.ALWAYS_SHOWN, HintRule.STANDARD.resolve(Difficulty.EASY))
        assertEquals(HintMode.TAP_COSTS_STAR, HintRule.STANDARD.resolve(Difficulty.MEDIUM))
        assertEquals(HintMode.NONE, HintRule.STANDARD.resolve(Difficulty.HARD))
        for (d in Difficulty.entries) {
            assertEquals(HintMode.ALWAYS_SHOWN, HintRule.FREE.resolve(d))
            assertEquals(HintMode.TAP_COSTS_STAR, HintRule.COSTS_STAR.resolve(d))
            assertEquals(HintMode.NONE, HintRule.OFF.resolve(d))
        }
    }

    @Test fun timeLimitChoices() {
        assertEquals(60_000L, GameRules.limitMs(60))
        assertEquals(GameRules.NO_LIMIT_MS, GameRules.limitMs(0))
        val c = RoundClock(0, GameRules.limitMs(0))
        c.start(0)
        assertEquals(RoundClock.State.HUNTING, c.tick(3_600_000))
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
        assertEquals(4_000L, c.elapsedMs(99_000))
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

    // ---------- Progress, records, backup ----------
    @Test fun newRecordsAreDetected() {
        val p = ProgressData()
        val ref = VerseRef(43, 3, 16)
        assertFalse(p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, ref, 2, 40_000)) // sets the bar
        assertFalse(p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, ref, 2, 45_000)) // slower
        assertTrue(p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, ref, 3, 20_000))  // faster
        assertEquals(20_000L, p.bestMs(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE))
        assertEquals(null, p.bestMs(Difficulty.EASY, PlayMode.IN_APP, DrillType.VERSE))
        assertFalse(p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, ref, 0, 1_000)) // failed round
        assertEquals(7, p.totalStars)
    }

    @Test fun booksLightUpAndRankBySlowness() {
        val p = ProgressData()
        p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, VerseRef(43, 3, 16), 2, 40_000)
        p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, VerseRef(19, 23, 1), 3, 8_000)
        p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.BOOK, VerseRef(1, 1, 1), 3, 5_000)
        assertEquals(setOf(43, 19, 1), p.litBooks)
        assertEquals(43, p.slowestBooks(1).first().first)
        assertEquals(VerseRef(43, 3, 16), p.slowestVerses(1).first().first)
        assertEquals(2, p.slowestVerses().size) // the book drill is not a "verse"
        assertEquals(10_000L, p.books[1]!!.bestMs)  // 5s at the 0.5 book factor = 10s verse-equivalent
    }

    @Test fun streakLogic() {
        assertEquals(1, Streak.next(-1, 0, 100))
        assertEquals(2, Streak.next(99, 1, 100))
        assertEquals(1, Streak.next(97, 5, 100))
        assertEquals(3, Streak.next(100, 3, 100))
        assertEquals(4, Streak.current(100, 4, 100))
        assertEquals(4, Streak.current(99, 4, 100))
        assertEquals(0, Streak.current(98, 4, 100))
        val p = ProgressData()
        assertEquals(1, p.recordDaily(10)); assertEquals(2, p.recordDaily(11)); assertEquals(2, p.recordDaily(11))
        assertTrue(p.dailyDone(11)); assertFalse(p.dailyDone(12))
        assertEquals(1, p.recordDaily(13)); assertEquals(2, p.bestStreak)
    }

    @Test fun backupRoundTrips() {
        val p = ProgressData()
        p.recordRound(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE, VerseRef(43, 3, 16), 3, 12_300)
        p.recordRound(Difficulty.HARD, PlayMode.IN_APP, DrillType.CHAPTER, VerseRef(19, 23, 1), 2, 30_000)
        p.recordMission(); p.recordDaily(20_000); p.recordTrainer(9, 10); p.recordMemory(1); p.markStoryRead(4)
        val q = ProgressData.fromText(p.toText())!!
        assertEquals(p.toText(), q.toText())
        assertEquals(p.totalStars, q.totalStars)
        assertEquals(12_300L, q.bestMs(Difficulty.EASY, PlayMode.PHYSICAL, DrillType.VERSE))
        assertEquals(setOf(4), q.storiesRead)
        assertEquals(20_000L, q.lastDailyDay)
    }

    @Test fun backupRejectsJunkAndSurvivesDamage() {
        assertEquals(null, ProgressData.fromText(""))
        assertEquals(null, ProgressData.fromText("hello=world"))
        val d = ProgressData.fromText("KIDQUEST-BACKUP 1\nstars=12\nstars=abc\nbook=999,1,2,3\nverse=bad\nnonsense\nstory=2\n")!!
        assertEquals(12, d.totalStars)
        assertTrue(d.books.isEmpty() && d.verses.isEmpty())
        assertEquals(setOf(2), d.storiesRead)
    }

    @Test fun otherRewards() {
        val p = ProgressData()
        assertTrue(p.markStoryRead(1)); assertFalse(p.markStoryRead(1))
        assertEquals(1, p.totalStars)
        assertEquals(3, p.recordTrainer(10, 10)); assertEquals(0, p.recordTrainer(4, 10))
        p.recordMemory(1); p.recordMemory(0)
        assertEquals(1, p.memoryWins)
        assertEquals(5, p.totalStars)
    }

    // ---------- Daily challenge ----------
    @Test fun dailyVerseIsSameForEveryoneAndAlwaysValid() {
        for (day in 20_000L..20_400L) {
            val v = Daily.verseFor(day)
            assertTrue(v in kjv)
            assertEquals(v, Daily.verseFor(day))
            assertTrue(v != Daily.verseFor(day - 1))
        }
        assertTrue((20_000L..20_100L).map { Daily.verseFor(it) }.toSet().size > 30)
    }

    // ---------- Book trainer ----------
    @Test fun trainerQuestionsAreValid() {
        val labels = (1..66).map { "B$it" }
        for (d in Difficulty.entries) for (seed in 1..60) {
            val session = Trainer.session(labels, d, 10, Random(seed))
            assertEquals(10, session.size)
            for (q in session) when (q) {
                is ChoiceQuestion -> {
                    assertEquals(q.options.size, q.options.toSet().size)
                    assertTrue(q.correct in q.options.indices)
                    val m = Regex("Which book comes (AFTER|BEFORE) B(\\d+)\\?").matchEntire(q.prompt)
                    if (m != null) {
                        val s = m.groupValues[2].toInt()
                        val want = if (m.groupValues[1] == "AFTER") s + 1 else s - 1
                        assertEquals("B$want", q.options[q.correct])
                        assertTrue(q.options.size == (if (d == Difficulty.EASY) 3 else 4))
                    } else {
                        assertEquals(2, q.options.size)
                    }
                }
                is OrderQuestion -> {
                    assertEquals(q.answer.sorted(), q.answer)
                    assertEquals(q.answer.toSet(), q.shuffled.toSet())
                    assertTrue(q.shuffled != q.answer)
                    assertEquals(when (d) { Difficulty.EASY -> 4; Difficulty.MEDIUM -> 5; Difficulty.HARD -> 6 }, q.answer.size)
                }
            }
        }
    }

    @Test fun trainerTestamentQuestionsAreCorrect() {
        for (seed in 1..40) for (q in Trainer.session(names, Difficulty.MEDIUM, 10, Random(seed))) {
            if (q is ChoiceQuestion && q.prompt.startsWith("Is ")) {
                val name = q.prompt.removePrefix("Is ").removeSuffix(" in the Old or the New Testament?")
                val book = kjv.books.first { it.name == name }
                assertEquals(book.testament.label, q.options[q.correct])
            }
        }
    }

    @Test fun trainerStars() {
        assertEquals(3, Trainer.starsFor(9, 10)); assertEquals(2, Trainer.starsFor(7, 10))
        assertEquals(1, Trainer.starsFor(5, 10)); assertEquals(0, Trainer.starsFor(4, 10))
    }

    // ---------- Memory step ----------
    private val sample = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life."

    @Test fun wordsKeepPunctuation() {
        val w = Memory.words("“Lord,” (and") 
        assertEquals("Lord", w[0].core); assertEquals("“", w[0].lead); assertEquals(",”", w[0].trail)
        assertEquals("(and", w[1].text)
        assertEquals("loved", Memory.parse("loved").core)
        assertEquals("—", Memory.parse("—").text)
    }

    @Test fun fillPuzzlesAreSolvable() {
        for (d in Difficulty.entries) for (seed in 1..80) {
            val p = Memory.fill(sample, d, Random(seed))!!
            assertTrue(p.blanks.isNotEmpty() && p.blanks.size <= 3)
            for (b in p.blanks) {
                assertEquals(p.words[b.wordIndex].core, b.answer)
                assertTrue(b.answer in b.options)
                assertEquals(3, b.options.toSet().size)
                assertTrue(b.wordIndex > 0)
                val verseWords = p.words.map { it.core.lowercase() }
                assertTrue(b.options.filter { it != b.answer }.none { it.lowercase() in verseWords })
            }
            assertEquals(p.blanks.map { it.wordIndex }.sorted(), p.blanks.map { it.wordIndex })
        }
        assertEquals(null, Memory.fill("Jesus wept.", Difficulty.EASY))
    }

    @Test fun scramblePuzzlesAreSolvable() {
        for (d in Difficulty.entries) for (seed in 1..80) {
            val p = Memory.scramble(sample, d, Random(seed))!!
            assertEquals(p.words.indices.toList(), p.order.sorted())
            assertTrue(p.order.map { p.words[it] } != p.words)
            // tapping words in the right order always succeeds
            var placed = 0
            val remaining = p.order.toMutableList()
            while (placed < p.words.size) {
                val pick = remaining.first { Memory.isNextWord(p, placed, it) }
                remaining.remove(pick); placed++
            }
            assertEquals(0, remaining.size)
        }
        assertEquals(null, Memory.scramble("Jesus wept.", Difficulty.EASY))
        assertEquals(1, Memory.bonusStars(0)); assertEquals(1, Memory.bonusStars(1)); assertEquals(0, Memory.bonusStars(2))
    }

    // ---------- Stories ----------
    @Test fun storiesPointAtRealVerses() {
        assertEquals(Stories.all.size, Stories.all.map { it.id }.toSet().size)
        for (s in Stories.all) {
            assertTrue("${s.title} start", VerseRef(s.book, s.chapter, s.fromVerse) in kjv)
            assertTrue("${s.title} end", VerseRef(s.book, s.chapter, s.toVerse) in kjv)
            assertTrue(s.fromVerse <= s.toVerse && s.toVerse - s.fromVerse < 25)
            assertTrue(s.title.isNotBlank() && s.summary.length > 20)
        }
        assertEquals("David and Goliath", Stories.byId(8)!!.title)
    }
}
