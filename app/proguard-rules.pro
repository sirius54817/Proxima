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

# Keep useful debug metadata in release stack traces.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature
-renamesourcefileattribute SourceFile

# WorkManager workers are created reflectively by class name.
-keep class * extends androidx.work.ListenableWorker {
	public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep backup payload models stable for Gson serialization/deserialization.
-keep class com.sirius.proxima.backup.BackupData { *; }
-keep class com.sirius.proxima.backup.BackupPdfBlob { *; }
-keepclassmembers class com.sirius.proxima.data.model.** {
	<fields>;
}

# Keep methods exposed to JavaScript in WebView bridges.
-keepclassmembers class * {
	@android.webkit.JavascriptInterface <methods>;
}

# Google API client often references optional javax annotations.
-dontwarn javax.annotation.**

# Google Sign-In/Auth + Google API Drive client classes used at runtime.
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.api.** { *; }
-keep class com.google.http.** { *; }
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }
-keep class com.google.api.client.http.ByteArrayContent { *; }
-keep class com.google.api.client.http.javanet.NetHttpTransport { *; }
-keep class com.google.api.client.json.gson.GsonFactory { *; }
-keep class com.google.api.services.drive.Drive { *; }
-keep class com.google.api.services.drive.Drive$Builder { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.**

# Keep Gson adapters/types used by backup payload serialization.
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.TypeAdapter
-dontwarn com.google.gson.**

# Network stack references (safe keeps for release reflection edge-cases).
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Ignore optional desktop/JVM-only classes pulled by transitive Apache auth paths.
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**

