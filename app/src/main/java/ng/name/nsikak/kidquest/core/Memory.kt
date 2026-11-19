package ng.name.nsikak.kidquest.core

import kotlin.random.Random

/** A word split into leading punctuation, the word itself, and trailing punctuation. */
data class Word(val lead: String, val core: String, val trail: String) {
    val text: String get() = lead + core + trail
}

/** A blank in a sentence: which word, the right answer, and the choices (right one included). */
data class Blank(val wordIndex: Int, val answer: String, val options: List<String>)

data class FillPuzzle(val words: List<Word>, val blanks: List<Blank>)

/** [words] are in the right order; [order] is the shuffled order they are shown in (indices into [words]). */
data class ScramblePuzzle(val words: List<String>, val order: List<Int>)

/** The "memory step" after a verse is found: fill in missing words, or put scrambled words in order. */
object Memory {
    private val WORD = Regex("^([^\\p{L}\\p{N}]*)([\\p{L}\\p{N}'’-]*[\\p{L}\\p{N}])(.*)$")

    private val DISTRACTORS = listOf(
        "love", "heart", "great", "give", "good", "people", "earth", "heaven", "faith", "peace",
        "king", "spirit", "light", "life", "name", "hope", "power", "truth", "world", "blessed",
        "mercy", "child", "father", "house", "water", "word", "glory", "praise", "strength", "kingdom",
    )

    fun parse(raw: String): Word {
        val m = WORD.matchEntire(raw) ?: return Word("", raw, "")
        return Word(m.groupValues[1], m.groupValues[2], m.groupValues[3])
    }

    fun words(text: String): List<Word> =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { parse(it) }

    /** Null when the verse is too short to make a puzzle. */
    fun fill(text: String, difficulty: Difficulty, random: Random = Random.Default): FillPuzzle? {
        val words = words(text)
        if (words.size < 4) return null
        val want = when (difficulty) { Difficulty.EASY -> 1; Difficulty.MEDIUM -> 2; Difficulty.HARD -> 3 }
        val candidates = words.indices.filter { it > 0 && words[it].core.length >= 4 && words[it].core.all { c -> c.isLetter() } }
        if (candidates.isEmpty()) return null
        val count = minOf(want, maxOf(1, candidates.size / 2))
        val chosen = candidates.shuffled(random).take(count).sorted()
        val inVerse = words.map { it.core.lowercase() }.toSet()
        val blanks = chosen.map { i ->
            val answer = words[i].core
            val others = DISTRACTORS.filter { it !in inVerse }.shuffled(random).take(2).map { matchCase(it, answer) }
            Blank(i, answer, (listOf(answer) + others).shuffled(random))
        }
        return FillPuzzle(words, blanks)
    }

    private fun matchCase(word: String, like: String): String =
        if (like.first().isUpperCase()) word.replaceFirstChar { it.uppercase() } else word

    /** Null when there are fewer than three words. Long verses use just their opening words. */
    fun scramble(text: String, difficulty: Difficulty, random: Random = Random.Default): ScramblePuzzle? {
        val all = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val take = when (difficulty) { Difficulty.EASY -> 5; Difficulty.MEDIUM -> 7; Difficulty.HARD -> 9 }
        val words = if (all.size <= take + 2) all else all.take(take)
        if (words.size < 3) return null
        var order = words.indices.toList().shuffled(random)
        var tries = 0
        while (order.map { words[it] } == words && tries < 20) { order = words.indices.toList().shuffled(random); tries++ }
        return ScramblePuzzle(words, order)
    }

    /** True if tapping the word at [wordIndex] is right when [placed] words are already in place. */
    fun isNextWord(p: ScramblePuzzle, placed: Int, wordIndex: Int): Boolean = p.words[wordIndex] == p.words[placed]

    /** One bonus star for finishing with at most one slip. */
    fun bonusStars(mistakes: Int): Int = if (mistakes <= 1) 1 else 0
}
