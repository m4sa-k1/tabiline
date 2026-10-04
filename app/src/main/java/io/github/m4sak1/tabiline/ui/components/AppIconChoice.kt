package io.github.m4sak1.tabiline.ui.components

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color

enum class AppIconChoice(val title: String, val color: Color) {
    GREEN("Green", Color(0xFF32866A)),
    PURPLE("Purple", Color(0xFF675496)),
    BLUE("Blue", Color(0xFF315DA8)),
    TEAL("Teal", Color(0xFF006A70)),
    CORAL("Coral", Color(0xFFA84462)),
    MONO("Mono", Color(0xFF514B5E));

    fun component(context: Context) = ComponentName(context.packageName, "io.github.m4sak1.tabiline.Launcher$name")

    companion object {
        fun current(context: Context): AppIconChoice = entries.firstOrNull {
            context.packageManager.getComponentEnabledSetting(it.component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: GREEN
    }
}

/** Enable the replacement first: there must always be a launchable alias. */
fun changeAppIcon(activity: Activity, choice: AppIconChoice) {
    val manager = activity.packageManager
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        manager.setComponentEnabledSettings(AppIconChoice.entries.map {
            PackageManager.ComponentEnabledSetting(it.component(activity),
                if (it == choice) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        })
    } else {
        manager.setComponentEnabledSetting(choice.component(activity), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        AppIconChoice.entries.filterNot { it == choice }.forEach {
            manager.setComponentEnabledSetting(it.component(activity), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
    // Recreate the task, not an OS process kill (which can lose the restart request).
    activity.startActivity(Intent.makeRestartActivityTask(choice.component(activity)))
    activity.finish()
}
