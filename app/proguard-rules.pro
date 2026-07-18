# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Hilt generated classes
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.DefineComponent class * { *; }
-keep @dagger.hilt.components.SingletonComponent class * { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Navigation Compose type-safe routes
-keep class com.citecircle.app.core.navigation.** { *; }

# Keep model classes for serialization
-keep class com.citecircle.app.core.model.** { *; }

# Keep Coil
-keep class coil.** { *; }
