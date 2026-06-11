package be.robinj.distrohopper

import android.content.ClipData
import android.content.ClipDescription
import android.view.DragEvent

/**
 * Builds DragEvent instances for tests via the hidden static
 * DragEvent.obtain(...) factory in Robolectric's android-all jar. The exact
 * signature differs between API levels, so the arguments are mapped by
 * parameter type rather than position.
 */
internal object DragEvents {
    fun obtain(
        action: Int,
        x: Float = 0f,
        y: Float = 0f,
        localState: Any? = null,
        clipDescription: ClipDescription? = null,
        clipData: ClipData? = null,
    ): DragEvent {
        val method = DragEvent::class.java.declaredMethods
            .filter { it.name == "obtain" && it.parameterTypes.size > 1 }
            .maxByOrNull { it.parameterTypes.size }
            ?: throw IllegalStateException("DragEvent.obtain(...) not found")
        method.isAccessible = true

        var floatsUsed = 0
        val args = method.parameterTypes.map { type ->
            when {
                type == Int::class.javaPrimitiveType -> action
                type == Float::class.javaPrimitiveType ->
                    when (floatsUsed++) { 0 -> x; 1 -> y; else -> 0f }
                type == Boolean::class.javaPrimitiveType -> false
                type == ClipDescription::class.java -> clipDescription
                type == ClipData::class.java -> clipData
                type == Any::class.java -> localState
                else -> null
            }
        }.toTypedArray()

        return method.invoke(null, *args) as DragEvent
    }
}
