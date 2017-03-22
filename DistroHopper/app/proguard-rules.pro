# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# SnappyDB
-dontwarn sun.reflect.**
-dontwarn java.beans.**
-dontwarn sun.nio.ch.**
-dontwarn sun.misc.**

-keep class com.esotericsoftware.** {*;}

-keep class java.beans.** { *; }
-keep class sun.reflect.** { *; }
-keep class sun.nio.ch.** { *; }

-keep class com.snappydb.** { *; }
-dontwarn com.snappydb.**

# Kryo
-dontwarn sun.reflect.**
-dontwarn java.beans.**
-keep,allowshrinking class com.esotericsoftware.** {
   <fields>;
   <methods>;
}
-keep,allowshrinking class java.beans.** { *; }
-keep,allowshrinking class sun.reflect.** { *; }
-keep,allowshrinking class com.esotericsoftware.kryo.** { *; }
-keep,allowshrinking class com.esotericsoftware.kryo.io.** { *; }
-keep,allowshrinking class sun.nio.ch.** { *; }
-dontwarn sun.nio.ch.**
-keep public class * extends com.google.protobuf.GeneratedMessage { *; }
-keep class de.javakaffee.kryoserializers.** { *; }
-dontwarn de.javakaffee.kryoserializers.**
-keep class com.esotericsoftware.kryo.serializers.** { *; }
-dontwarn com.esotericsoftware.kryo.serializers.**