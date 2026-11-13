# Proguard rules for LiquidGlassTV
-keep class com.liquidglass.tv.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
