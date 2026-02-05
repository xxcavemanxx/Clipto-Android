package clipto.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import clipto.AppContext
import clipto.common.extensions.setLocale
import clipto.presentation.common.fragment.attributed.AttributedObjectFragment

fun Fragment.onCreateWithTheme(layoutResId: Int, inflater: LayoutInflater, container: ViewGroup?): View? {
    val appContext = AppContext.get()
    val themeId = appContext.appState.getTheme().themeId
    appContext.app.setTheme(themeId)
    val theme = appContext.app.theme
    theme.applyStyle(themeId, true)

    val contextThemeWrapper = ContextThemeWrapper(appContext.app, theme)

    val locale = appContext.appState.language.requireValue()
    val resources = contextThemeWrapper.resources
    appContext.app.resources.setLocale(locale)
    resources.setLocale(locale)

    val localInflater = inflater.cloneInContext(contextThemeWrapper)
    return localInflater.inflate(layoutResId, container, false)
}

fun Fragment.storeActiveFieldState() {
    if (this is AttributedObjectFragment<*, *, *>) {
        storeActiveFieldState()
    }
}