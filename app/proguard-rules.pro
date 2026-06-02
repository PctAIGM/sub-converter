-keepattributes *Annotation*
-keep class com.subconverter.data.** { *; }
-dontwarn org.yaml.snakeyaml.**
-keepclassmembers class org.yaml.snakeyaml.** { *; }
-keepclassmembers class * {
    @org.yaml.snakeyaml.YamlProperty* *;
}
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
