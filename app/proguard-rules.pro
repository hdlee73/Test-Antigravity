-keepattributes Signature,*Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class io.github.hdlee73.etftracker.data.api.dto.** { *; }
-keep class io.github.hdlee73.etftracker.domain.model.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
