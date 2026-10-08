# CodeMate Proguard Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Markwon / Prism4j
-keep class io.noties.markwon.** { *; }
-keep class io.noties.prism4j.** { *; }
