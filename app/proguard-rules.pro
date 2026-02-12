# Add project specific ProGuard rules here.
-keep class org.tensorflow.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
