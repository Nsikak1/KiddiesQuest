package ng.name.version5.core

/**
 * Book numbers: Genesis=1 ... Malachi=39, Matthew=40, John=43, Acts=44, Romans=45, Revelation=66.
 * Only references are stored here, never the verse text. The text always comes from the chosen translation.
 */
object FamousVerses {

    private fun v(book: Int, chapter: Int, verse: Int) = VerseRef(book, chapter, verse)

    /** Easy: well-known verses from both testaments. */
    val list: List<VerseRef> = listOf(
        v(1, 1, 1), v(1, 1, 27), v(1, 28, 15),
        v(2, 20, 3), v(2, 20, 12),
        v(4, 6, 24),
        v(5, 6, 5), v(5, 31, 6),
        v(6, 1, 9), v(6, 24, 15),
        v(8, 1, 16),
        v(9, 16, 7),
        v(13, 16, 34),
        v(19, 23, 1), v(19, 23, 4), v(19, 27, 1), v(19, 34, 8), v(19, 46, 1), v(19, 46, 10),
        v(19, 56, 3), v(19, 100, 1), v(19, 118, 24), v(19, 119, 105), v(19, 121, 1), v(19, 139, 14),
        v(20, 3, 5), v(20, 3, 6), v(20, 15, 1), v(20, 17, 17), v(20, 22, 6),
        v(21, 3, 1),
        v(23, 9, 6), v(23, 40, 31), v(23, 41, 10),
        v(24, 29, 11),
        v(25, 3, 23),
        v(33, 6, 8),
        v(40, 5, 16), v(40, 6, 33), v(40, 7, 7), v(40, 11, 28), v(40, 19, 14), v(40, 22, 37),
        v(40, 28, 19), v(40, 28, 20),
        v(41, 10, 14), v(41, 16, 15),
        v(42, 2, 11), v(42, 6, 31), v(42, 18, 16),
        v(43, 1, 1), v(43, 3, 16), v(43, 8, 12), v(43, 10, 11), v(43, 11, 35), v(43, 14, 6), v(43, 15, 12),
        v(44, 1, 8), v(44, 16, 31),
        v(45, 3, 23), v(45, 6, 23), v(45, 8, 28),
        v(46, 13, 13),
        v(47, 5, 17),
        v(48, 5, 22),
        v(49, 2, 8), v(49, 4, 32), v(49, 6, 1),
        v(50, 4, 6), v(50, 4, 13),
        v(51, 3, 20),
        v(52, 5, 16), v(52, 5, 17),
        v(55, 1, 7), v(55, 3, 16),
        v(58, 11, 1), v(58, 13, 8),
        v(59, 1, 5), v(59, 4, 7),
        v(60, 5, 7),
        v(62, 4, 8), v(62, 4, 19),
        v(66, 3, 20), v(66, 21, 4),
    )

    /** Medium: any verse from these better-known books. */
    val mediumBooks: List<Int> = listOf(
        1, 2, 5, 6, 8, 9, 10, 11, 18, 19, 20, 21, 23, 24, 27, 32,
        40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54, 55, 58, 59, 60, 62, 66,
    )
}
