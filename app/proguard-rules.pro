# Project-specific R8 rules, applied on top of proguard-android-optimize.txt.
#
# R8 is on because Play requires at least 25% obfuscation. Only library code is
# renamed: our own names stay, so stack traces remain readable and names that
# are persisted (Theme.getName()) or logged keep working. Check the score in
# the bundle's BUNDLE-METADATA/com.android.tools/r8.json before narrowing this.
-keepattributes SourceFile,LineNumberTable
-keep,allowshrinking,allowoptimization class be.robinj.distrohopper.** { *; }

# ACRA leaks AutoService's annotation processor onto the runtime classpath.
-dontwarn javax.annotation.processing.**
