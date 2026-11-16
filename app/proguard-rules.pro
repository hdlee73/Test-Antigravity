-keepattributes Signature,*Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class io.github.hdlee73.etftracker.data.api.dto.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
