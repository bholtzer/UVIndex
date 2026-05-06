# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Retrofit & OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.squareup.okhttp3.** { *; }

# Keep Gson models
-keep class com.bihstudio.uvindex.data.remote.dto.** { *; }
-keep class com.bihstudio.uvindex.domain.model.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Firebase
-keep class com.google.firebase.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }
