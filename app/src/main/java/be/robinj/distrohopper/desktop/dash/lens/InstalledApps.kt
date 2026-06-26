package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.view.View
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.Profiles
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.AppLauncherLongClickListener

/**
 * Created by robin on 5/11/14.
 */
class InstalledApps(context: Context, private val apps: AppManager?) : Lens(context) {

    init {
        this.icon = context.resources.getDrawable(R.mipmap.ic_launcher, null)
    }

    // In-memory scan over the loaded app list — effectively instant //
    override val type = LensType.LOCAL

    override fun getName() = "Installed apps"

    override fun getDescription() = "Search installed apps"

    /**
     * One section per profile, so work-profile apps get their own section —
     * effectively a separate lens per profile, while remaining a single lens in
     * the preferences. A single profile keeps the plain, unsuffixed section.
     */
    override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {
        val apps = this.apps ?: return
        val profiles = apps.getProfiles()

        for (profile in profiles) {
            val appResults = apps.searchProfile(query, maxResults, profile)

            if (appResults.isEmpty()) {
                continue
            }

            val sectionName = if (profiles.size > 1)
                this.context.getString(R.string.lens_profile_section,
                    this.getName(), Profiles.label(this.context, profile))
            else
                this.getName()

            for (app in appResults) {
                emitter.emit(sectionName, LensSearchResult(
                    this.context, app.label, "${app.packageName}:${app.activityName}",
                    app.icon.drawable, app))
            }
        }
    }

    override fun onClick(url: String, obj: Any?) {
        (obj as App).launch()
    }

    override fun onLongClick(url: String, obj: Any?, view: View?) {
        try {
            // Same as a long press on the dash grid: drag to the launcher to
            // pin at the drop position, or to move an already pinned icon //
            AppLauncherLongClickListener.startAppDrag(view, obj as App)
        } catch (ex: Exception) {
            ExceptionHandler(ex).show(this.context)
        }
    }
}
