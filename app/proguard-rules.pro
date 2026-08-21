# Budget Notes — R8 / ProGuard (release)

# Keep Room entities and generated DAO implementations
-keep class com.budgetnotes.app.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

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
-keepclassmembers enum com.budgetnotes.app.data.CardType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ML Kit removed — no OCR dependencies
