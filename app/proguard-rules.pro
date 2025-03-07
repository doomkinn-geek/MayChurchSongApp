# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Сохраняем информацию о строках для стек-трейсов
-keepattributes SourceFile,LineNumberTable

# Скрываем имена исходных файлов
-renamesourcefileattribute SourceFile

# Правила для Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Правила для Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class ru.maychurch.maychurchsong.**$$serializer { *; }
-keepclassmembers class ru.maychurch.maychurchsong.** {
    *** Companion;
}
-keepclasseswithmembers class ru.maychurch.maychurchsong.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Правила для Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Дополнительно для fix java.lang.management.ManagementFactory
-dontwarn java.lang.management.**
-keep class java.lang.management.** { *; }

# Для исправления ошибки IntellijIdeaDebugDetector
-keepclassmembers class io.ktor.util.debug.** { *; }
-keep class io.ktor.util.debug.** { *; }

# Правила для JSOup
-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes

# Исправление ошибки отсутствия JSpecify аннотаций (используются в JSoup)
-dontwarn org.jspecify.annotations.**
-keep class org.jspecify.annotations.** { *; }

# Правила для WorkManager
-keep class androidx.work.** { *; }

# Правила для Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Не оптимизировать/удалять наши модели данных
-keep class ru.maychurch.maychurchsong.data.model.** { *; }

# Дополнительные настройки минификации
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Отключаем обработку аннотаций
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# Более специфичные правила для JSoup
-keep public class org.jsoup.** {
    public *;
}
-keep public class org.jsoup.nodes.** {
    public *;
}
-keep public class org.jsoup.select.** {
    public *;
}
-keep public class org.jsoup.helper.** {
    public *;
}
-keep public class org.jsoup.Connection$** {
    public *;
}
-dontwarn org.jsoup.nodes.Element$**
-dontwarn org.jsoup.select.Elements$**