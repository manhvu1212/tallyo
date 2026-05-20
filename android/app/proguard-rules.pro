# Room generates classes via KSP; keep entity members.
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
-keep class * extends androidx.room.RoomDatabase

# Compose handles its own keep rules via consumer-proguard.
