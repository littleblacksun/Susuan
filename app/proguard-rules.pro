# Add project specific ProGuard rules here.
# Keep Room database, entities, and DAOs.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep Kotlin Coroutines & DataStore internals.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep FileProvider & Compose stability metadata.
-keep class androidx.core.content.FileProvider { *; }
-dontwarn java.lang.invoke.StringConcatFactory
