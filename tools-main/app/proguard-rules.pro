# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve Room database entities, DAOs, and Room database
-keep class com.example.data.** { *; }
-keep class com.example.util.** { *; }
-keep class com.example.ui.** { *; }
-keep class com.example.update.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-dontwarn androidx.room.paging.**

# Preserve annotations and type metadata for serialization/Room/Moshi
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep ML Kit Document Scanner & Text Recognition
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# Keep Compose Runtime Owner
-keepclassmembers class * extends androidx.compose.ui.node.Owner { *; }

# Keep ViewModel classes
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}


