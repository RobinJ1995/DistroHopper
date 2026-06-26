package be.robinj.distrohopper.cache

import android.content.Context

internal class TestStringCache(context: Context, name: String) : StringCache(context, name)
internal class TestLongCache(context: Context, name: String) : LongCache(context, name)
internal class TestDrawableCache(context: Context, name: String) : DrawableCache(context, name)
