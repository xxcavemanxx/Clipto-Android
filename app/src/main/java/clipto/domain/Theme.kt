package clipto.domain

import com.wb.clipboard.R

enum class Theme(
        val id: Int,
        val themeId: Int,
        val authThemeId: Int,
        val translucentThemeId: Int,
        val titleRes: Int,
        val dark: Boolean = false,
        val colorListItemSelected: String,
        val colorPrimaryInverse: String,
        val colorAccent: String
) {

    DEFAULT(
            id = 1,
            themeId = R.style.MyTheme_Light,
            authThemeId = R.style.MyTheme_Light_Auth,
            translucentThemeId = R.style.MyTheme_Light_Translucent,
            titleRes = R.string.theme_default,
            colorListItemSelected = "#4d344955",
            colorPrimaryInverse = "#ffffff",
            colorAccent = "#f9aa33"
    ),

    WHITE(
            id = 7,
            themeId = R.style.MyTheme_White,
            authThemeId = R.style.MyTheme_White_Auth,
            translucentThemeId = R.style.MyTheme_White_Translucent,
            titleRes = R.string.theme_white,
            dark = false,
            colorListItemSelected = "#80419FD9",
            colorPrimaryInverse = "#ffffff",
            colorAccent = "#74b5e0"
    ),

    SEPIA(
            id = 4,
            themeId = R.style.MyTheme_Sepia,
            authThemeId = R.style.MyTheme_Sepia_Auth,
            translucentThemeId = R.style.MyTheme_Sepia_Translucent,
            titleRes = R.string.theme_sepia,
            colorListItemSelected = "#DBE6E7",
            colorPrimaryInverse = "#F3EDDB",
            colorAccent = "#5E8DC6"
    ),

    GREEN(
            id = 6,
            themeId = R.style.MyTheme_Green,
            authThemeId = R.style.MyTheme_Green_Auth,
            translucentThemeId = R.style.MyTheme_Green_Translucent,
            titleRes = R.string.theme_green,
            colorListItemSelected = "#E2E9CE",
            colorPrimaryInverse = "#fffbe6",
            colorAccent = "#fa5630"
    ),

    PINK(
            id = 9,
            themeId = R.style.MyTheme_Pink,
            authThemeId = R.style.MyTheme_Pink_Auth,
            translucentThemeId = R.style.MyTheme_Pink_Translucent,
            titleRes = R.string.theme_pink,
            dark = false,
            colorListItemSelected = "#4dfedbd0",
            colorPrimaryInverse = "#ffffff",
            colorAccent = "#fddbd1"
    ),

    DARK(
            id = 2,
            themeId = R.style.MyTheme_Dark,
            authThemeId = R.style.MyTheme_Dark_Auth,
            translucentThemeId = R.style.MyTheme_Dark_Translucent,
            titleRes = R.string.theme_dark,
            dark = true,
            colorListItemSelected = "#747778",
            colorPrimaryInverse = "#FF212121",
            colorAccent = "#f9aa33"
    ),

    DARK_BLUE(
            id = 3,
            themeId = R.style.MyTheme_DarkBlue,
            authThemeId = R.style.MyTheme_DarkBlue_Auth,
            translucentThemeId = R.style.MyTheme_DarkBlue_Translucent,
            titleRes = R.string.theme_dark_blue,
            dark = true,
            colorListItemSelected = "#314A61",
            colorPrimaryInverse = "#1D2733",
            colorAccent = "#5E8DC6"
    ),

    DARK_GREEN(
            id = 10,
            themeId = R.style.MyTheme_DarkGreen,
            authThemeId = R.style.MyTheme_DarkGreen_Auth,
            translucentThemeId = R.style.MyTheme_DarkGreen_Translucent,
            titleRes = R.string.theme_dark_green,
            dark = true,
            colorListItemSelected = "#CC94B042",
            colorPrimaryInverse = "#0B1B07",
            colorAccent = "#94B042"
    ),

    DARK_BLURPLE(
            id = 8,
            themeId = R.style.MyTheme_DarkBlurple,
            authThemeId = R.style.MyTheme_DarkBlurple_Auth,
            translucentThemeId = R.style.MyTheme_DarkBlurple_Translucent,
            titleRes = R.string.theme_dark_blurple,
            dark = true,
            colorListItemSelected = "#33FFFFFF",
            colorPrimaryInverse = "#FF212121",
            colorAccent = "#7289DA"
    ),

    BLACK(
            id = 5,
            themeId = R.style.MyTheme_Black,
            authThemeId = R.style.MyTheme_Black_Auth,
            translucentThemeId = R.style.MyTheme_Black_Translucent,
            titleRes = R.string.theme_black,
            dark = true,
            colorListItemSelected = "#2b2b2b",
            colorPrimaryInverse = "#000000",
            colorAccent = "#5E8DC6"
    ),

    ;

    companion object {
        fun valueOf(settings: Settings): Theme =
                when (settings.theme) {
                    1 -> DEFAULT
                    2 -> DARK
                    3 -> DARK_BLUE
                    4 -> SEPIA
                    5 -> BLACK
                    6 -> GREEN
                    7 -> WHITE
                    8 -> DARK_BLURPLE
                    9 -> PINK
                    10 -> DARK_GREEN
                    else -> DEFAULT
                }
    }

}