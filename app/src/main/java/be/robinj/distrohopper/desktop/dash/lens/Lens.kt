package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.FrostedGlass
import be.robinj.distrohopper.preferences.FontPreference
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

/**
 * A dash search "lens": a pluggable search provider (installed apps, local
 * files, DuckDuckGo, GitHub, app stores, …).
 *
 * Every lens streams its results progressively: [search] emits each result the
 * moment it is fully ready (its icon already resolved — no placeholders) through
 * a [LensResultEmitter], rather than returning the whole batch at once. The
 * runner (home/SearchLoader) calls [search] on a background dispatcher inside a
 * cancellable job and marshals each emit onto the UI thread.
 *
 * Created by robin on 4/11/14.
 */
abstract class Lens(protected val context: Context) {

    @JvmField protected var icon: Drawable? = null

    /**
     * How expensive this lens is to search, which drives scheduling in
     * SearchLoader (LOCAL lenses run on every keystroke; IO and NETWORK lenses
     * are debounced).
     */
    abstract val type: LensType

    abstract fun getName(): String

    abstract fun getDescription(): String

    /**
     * Searches and emits each result through [emitter] as it becomes ready, up
     * to [maxResults] per section. Runs on a background dispatcher inside the
     * runner's cancellable job; implementations doing long work should honour
     * coroutine cancellation (the emitter checks it before touching the UI).
     */
    abstract suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter)

    /** Convenience for direct callers (and tests): searches with the default cap. */
    suspend fun search(query: String, emitter: LensResultEmitter) =
        this.search(query, DEFAULT_MAX_RESULTS, emitter)

    open fun getIcon(): Drawable? = this.icon

    open fun getMinSDKVersion(): Int = -1

    /**
     * Runtime permissions this lens needs to deliver results. Lenses missing
     * any of these are disabled by default; enabling one re-requests them.
     */
    open fun requiredPermissions(): Array<String> = emptyArray()

    // Click handling lives within each lens: the base does nothing by default,
    // and lenses override to launch an app, open a file, a store page, or a web
    // link (via the openInBrowser helper). //

    open fun onClick(url: String) {}

    open fun onClick(url: String, obj: Any?) {
        if (obj == null) {
            this.onClick(url)
        }
    }

    open fun onLongClick(url: String, obj: Any?, view: View?) {
        if (obj == null) {
            this.onClick(url)
        }
    }

    /** Opens an http(s) link in the browser. Shared plumbing for the web lenses. */
    protected fun openInBrowser(url: String) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this.context.startActivity(intent)
        }
    }

    /** Shows the failure dialog for a synthetic error result (see CollectionGridAdapter). */
    fun showError(message: String?) = this.showDialog(message ?: "", true)

    protected fun showDialog(message: String, error: Boolean = false) {
        val dlg = AlertDialog.Builder(this.context, R.style.ModernDialogTheme)
        dlg.setMessage(message)
        dlg.setCancelable(true)
        dlg.setNeutralButton(android.R.string.ok, null)

        val dialog = dlg.create()
        dialog.setOnShowListener {
            // Keep the surface legible where cross-window blur isn't available (e.g. Samsung). //
            dialog.window?.let(FrostedGlass::applyDialogFallback)
            // Dialog chrome inflates through the dialog window's own inflater,
            // which doesn't carry the activity's font factory. //
            FontPreference.applyTo(dialog)
        }
        dialog.show()
    }

    protected open fun openConnection(url: String): URLConnection {
        val connection = URL(url).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        return connection
    }

    protected open fun downloadStr(url: String): String =
        BufferedReader(InputStreamReader(this.openConnection(url).getInputStream())).use { reader ->
            val str = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                str.append(line)
            }

            str.toString()
        }

    protected open fun downloadImage(url: String): Drawable =
        BufferedInputStream(this.openConnection(url).getInputStream()).use { input ->
            ByteArrayOutputStream().use { out ->
                val buffer = ByteArray(1024)
                var x: Int

                while (input.read(buffer).also { x = it } != -1) {
                    out.write(buffer, 0, x)
                }

                val imageBytes = out.toByteArray()
                BitmapDrawable(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size))
            }
        }

    companion object {
        private const val DEFAULT_MAX_RESULTS = 20
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
    }
}
