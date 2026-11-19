package ng.name.nsikak.kidquest.core

import kotlin.random.Random

/** One verse a day, the same for every child on every phone (it depends only on the date). */
object Daily {
    val difficulty = Difficulty.MEDIUM
    const val BONUS_STARS = 2

    fun verseFor(day: Long): VerseRef {
        val list = FamousVerses.list
        fun pick(d: Long): Int = Random((d * 2654435761L % 2147483647L).toInt()).nextInt(list.size)
        var i = pick(day)
        if (i == pick(day - 1)) i = (i + 1) % list.size // never the same verse two days in a row
        return list[i]
    }
}
