import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import android.content.SharedPreferences

object ThemeHelper {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_setting"

    const val LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK_MODE = AppCompatDelegate.MODE_NIGHT_YES
    const val DEFAULT_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // System default

    fun applyTheme(theme: Int) {
        AppCompatDelegate.setDefaultNightMode(theme)
    }

    fun saveTheme(context: Context, theme: Int) {
        val editor = getPreferences(context).edit()
        editor.putInt(KEY_THEME, theme)
        editor.apply()
    }

    fun getTheme(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME, DEFAULT_MODE)
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}