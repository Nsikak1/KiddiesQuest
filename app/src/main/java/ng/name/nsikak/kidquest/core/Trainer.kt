package ng.name.nsikak.kidquest.core

import kotlin.random.Random

sealed interface BookQuestion {
    val prompt: String
}

/** Pick one answer. [correct] is an index into [options]. */
data class ChoiceQuestion(override val prompt: String, val options: List<String>, val correct: Int) : BookQuestion

/** Tap the books in Bible order. [shuffled] and [answer] hold book numbers (1..66). */
data class OrderQuestion(override val prompt: String, val shuffled: List<Int>, val answer: List<Int>) : BookQuestion

/** Questions for the "Books of the Bible" trainer. */
object Trainer {
    const val QUESTIONS = 10

    fun starsFor(correct: Int, total: Int): Int {
        if (total <= 0) return 0
        val ratio = correct.toDouble() / total
        return when {
            ratio >= 0.9 -> 3
            ratio >= 0.7 -> 2
            ratio >= 0.5 -> 1
            else -> 0
        }
    }

    private enum class Kind { NEXT, BEFORE, TESTAMENT, ORDER }

    private val PATTERN = listOf(
        Kind.NEXT, Kind.ORDER, Kind.BEFORE, Kind.TESTAMENT, Kind.NEXT,
        Kind.ORDER, Kind.BEFORE, Kind.NEXT, Kind.TESTAMENT, Kind.ORDER,
    )

    /** Books children usually know best; used as the starting point of Easy questions. */
    private val EASY_SUBJECTS = listOf(1, 2, 6, 8, 9, 19, 20, 23, 27, 32, 40, 41, 42, 43, 44, 45, 48, 49, 50, 58, 59, 66)

    /** [names] must hold the 66 book names in Bible order. */
    fun session(names: List<String>, difficulty: Difficulty, count: Int = QUESTIONS, random: Random = Random.Default): List<BookQuestion> {
        val used = HashSet<Int>()
        val pool = if (difficulty == Difficulty.EASY) EASY_SUBJECTS else (1..66).toList()
        return (0 until count).map { i ->
            when (PATTERN[i % PATTERN.size]) {
                Kind.NEXT -> neighbour(names, difficulty, pool, used, random, after = true)
                Kind.BEFORE -> neighbour(names, difficulty, pool, used, random, after = false)
                Kind.TESTAMENT -> testament(names, pool, used, random)
                Kind.ORDER -> order(names, difficulty, random)
            }
        }
    }

    private fun fresh(pool: List<Int>, used: MutableSet<Int>, random: Random, ok: (Int) -> Boolean): Int {
        val options = pool.filter { ok(it) }
        val unused = options.filter { it !in used }
        val choice = (if (unused.isNotEmpty()) unused else options).random(random)
        used.add(choice)
        return choice
    }

    private fun neighbour(
        names: List<String>, difficulty: Difficulty, pool: List<Int>, used: MutableSet<Int>, random: Random, after: Boolean,
    ): ChoiceQuestion {
        val subject = fresh(pool, used, random) { if (after) it < 66 else it > 1 }
        val answer = if (after) subject + 1 else subject - 1
        val wanted = if (difficulty == Difficulty.EASY) 3 else 4
        val near = (1..66).filter { it != answer && it != subject && (difficulty == Difficulty.EASY || Math.abs(it - answer) <= 8) }
        val wrong = near.shuffled(random).take(wanted - 1)
        val options = (wrong + answer).shuffled(random)
        val word = if (after) "AFTER" else "BEFORE"
        return ChoiceQuestion("Which book comes $word ${names[subject - 1]}?", options.map { names[it - 1] }, options.indexOf(answer))
    }

    private fun testament(names: List<String>, pool: List<Int>, used: MutableSet<Int>, random: Random): ChoiceQuestion {
        val subject = fresh(pool, used, random) { true }
        val correct = if (subject <= BookInfo.OLD_TESTAMENT_BOOKS) 0 else 1
        return ChoiceQuestion(
            "Is ${names[subject - 1]} in the Old or the New Testament?",
            listOf(Testament.OLD.label, Testament.NEW.label),
            correct,
        )
    }

    private fun order(names: List<String>, difficulty: Difficulty, random: Random): OrderQuestion {
        val n = when (difficulty) { Difficulty.EASY -> 4; Difficulty.MEDIUM -> 5; Difficulty.HARD -> 6 }
        val answer: List<Int> = if (difficulty == Difficulty.EASY) {
            val start = random.nextInt(66 - n + 1) + 1
            (start until start + n).toList()           // books that sit side by side
        } else {
            (1..66).shuffled(random).take(n).sorted()   // books from anywhere
        }
        var shuffled = answer.shuffled(random)
        var tries = 0
        while (shuffled == answer && tries < 20) { shuffled = answer.shuffled(random); tries++ }
        return OrderQuestion("Tap these books in Bible order, first to last.", shuffled, answer)
    }
}
