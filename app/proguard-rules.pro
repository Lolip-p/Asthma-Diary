# Room
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class * extends androidx.work.Worker { <init>(...); }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Compose
-dontwarn androidx.compose.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**
