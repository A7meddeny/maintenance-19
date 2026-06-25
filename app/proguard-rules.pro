# قواعد ProGuard
-keep class com.masar.maintenance.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * { @com.google.gson.annotations.SerializedName <fields>; }
# Retrofit
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
