# kotlinx.serialization находит сериализаторы через Companion-объекты.
# R8 в full mode вырезает их как недостижимые, и конфигурация перестаёт
# читаться — но только в релизной сборке, где это тяжелее всего заметить.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class io.jancher.launcher.data.** {
    *** Companion;
}
-keepclasseswithmembers class io.jancher.launcher.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.jancher.launcher.data.**$$serializer { *; }
