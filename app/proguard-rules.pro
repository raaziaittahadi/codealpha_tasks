# Keep Retrofit service interface method names
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson models — preserve field names for JSON serialization
-keep class com.aitranslator.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Kotlin Parcelize
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
