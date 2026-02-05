package clipto.extensions

import androidx.appcompat.app.AppCompatActivity
import clipto.AppContext
import clipto.common.extensions.setLocale
import clipto.domain.Theme

fun AppCompatActivity.onCreateWithTheme(themeIdProvider: (theme: Theme) -> Int = { it.themeId }) {
    runCatching {
        val appContext = AppContext.get()
        val theme = appContext.appState.getTheme()
        val themeId = theme.themeId
        application.setTheme(themeId)
        setTheme(themeIdProvider.invoke(theme))
        onCreateWithLocale()
    }
}

fun AppCompatActivity.onCreateWithLocale() {
    runCatching {
        val appContext = AppContext.get()
        val locale = appContext.appState.language.requireValue()
        appContext.app.resources.setLocale(locale)
        resources.setLocale(locale)
    }
}