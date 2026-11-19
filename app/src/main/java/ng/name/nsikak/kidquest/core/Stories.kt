package ng.name.nsikak.kidquest.core

/** A famous Bible story: a short kid-friendly summary and the passage to read. Verse text comes from the chosen Bible. */
data class Story(
    val id: Int,
    val title: String,
    val summary: String,
    val book: Int,
    val chapter: Int,
    val fromVerse: Int,
    val toVerse: Int,
)

object Stories {
    val all: List<Story> = listOf(
        Story(1, "God Makes the World", "In the beginning God made light, sky, land and sea, one day at a time.", 1, 1, 1, 19),
        Story(2, "Noah Builds a Boat", "God tells Noah to build a big ark to keep his family and the animals safe.", 1, 6, 13, 22),
        Story(3, "The Rainbow Promise", "After the flood God puts a rainbow in the sky to promise it will never happen again.", 1, 9, 8, 17),
        Story(4, "Joseph and the Colourful Coat", "Joseph's father loves him and gives him a special coat, and his brothers get jealous.", 1, 37, 3, 11),
        Story(5, "Baby Moses", "A mother hides her baby in a basket on the river, and a princess finds him.", 2, 2, 1, 10),
        Story(6, "Crossing the Red Sea", "God makes a path through the sea so His people can walk across on dry ground.", 2, 14, 13, 22),
        Story(7, "Ruth Stays with Naomi", "Ruth promises to stay with her sad mother-in-law wherever she goes.", 8, 1, 14, 18),
        Story(8, "David and Goliath", "A young shepherd boy trusts God and beats the giant with a sling and a stone.", 9, 17, 38, 50),
        Story(9, "Daniel and the Lions", "Daniel keeps praying to God, and God keeps him safe in a den of lions.", 27, 6, 16, 23),
        Story(10, "Jonah and the Big Fish", "Jonah runs away from God, but a great fish swallows him and brings him back.", 32, 1, 11, 17),
        Story(11, "Baby Jesus is Born", "Mary and Joseph have no room at the inn, so baby Jesus is born in a stable.", 42, 2, 1, 14),
        Story(12, "Jesus Calms the Storm", "A huge storm scares the friends of Jesus, but He tells the wind and waves to be still.", 41, 4, 35, 41),
        Story(13, "Feeding Five Thousand", "A boy shares his small lunch and Jesus feeds a huge crowd.", 43, 6, 5, 13),
        Story(14, "The Good Samaritan", "A stranger stops to help a hurt man when everyone else walks past.", 42, 10, 30, 37),
        Story(15, "Zacchaeus Up a Tree", "A short man climbs a tree to see Jesus, and Jesus visits his house.", 42, 19, 1, 10),
        Story(16, "The Lost Son Comes Home", "A son comes home after making bad choices, and his father welcomes him with a party.", 42, 15, 17, 24),
        Story(17, "Jesus is Alive!", "On Sunday morning the tomb is empty, and an angel says Jesus has risen.", 40, 28, 1, 10),
    )

    fun byId(id: Int): Story? = all.firstOrNull { it.id == id }
}
