package ng.name.nsikak.kidquest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ng.name.nsikak.kidquest.R

/** Lilita One (SIL Open Font License) - a chunky game-style face, bundled so it works offline. */
val LilitaOne = FontFamily(Font(R.font.lilita_one, FontWeight.Normal))

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
)
