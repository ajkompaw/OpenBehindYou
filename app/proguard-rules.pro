# The bundled ML Kit face detector ships consumer ProGuard rules.
# Keep ML Kit entry points defensively for release builds.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin coroutines / Compose are covered by their own consumer rules.
