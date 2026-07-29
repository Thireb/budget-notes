# Budget Notes — R8 / ProGuard (release)

# Keep Room entities and generated DAO implementations
-keep class com.budgetnotes.app.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Compose / Enums used in Room converters
-keepclassmembers enum com.budgetnotes.app.data.BudgetItemType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
