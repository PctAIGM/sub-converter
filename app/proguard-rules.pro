-keepattributes *Annotation*
-keep class com.subconverter.data.** { *; }
-dontwarn org.yaml.snakeyaml.**
-keep class * implements org.yaml.snakeyaml.constructor.Constructor$Construct { *; }
-keepclassmembers class * {
    @org.yaml.snakeyaml.YamlProperty* *;
}
-dontwarn com.google.zxing.**
-keep class com.google.zxing.qrcode.QRCodeWriter { *; }
-keep class com.google.zxing.MultiFormatReader { *; }
