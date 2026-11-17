# Keep our own names: only library code is renamed.
-keepattributes SourceFile,LineNumberTable
-keep,allowshrinking,allowoptimization class be.robinj.distrohopper.** { *; }

# ACRA leaks AutoService's annotation processor onto the runtime classpath.
-dontwarn javax.annotation.processing.**
