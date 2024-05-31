# Keep the names of classes, methods, and fields which are used in JSON (de)serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

# Keep classes with Firebase annotations
-keepclassmembers class **.R$* {
    public static final <fields>;
}

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
