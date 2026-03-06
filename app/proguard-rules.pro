# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Domain models (used in Gson serialization)
-keep class com.dompetku.domain.model.** { *; }

# POI (Apache)
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**

# OpenCSV
-dontwarn com.opencsv.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
