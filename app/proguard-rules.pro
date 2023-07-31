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

-printmapping out.mapping

-keepclasseswithmembers class * {
    native <methods>;
}

-keep public class * extends net.fred.lua.common.activity.BaseActivity {}
-keep public class * extends android.app.Application {}
-keep public class * extends android.view.View {}

-keep class net.fred.lua.foreign.types.Type {}
-keep class net.fred.lua.foreign.Pointer {}
-keep class net.fred.lua.foreign.NativeMethodException {}

-keep class net.fred.lua.foreign.ffi.FunctionDescriber {
   long requestMemory(long);
}

-keep class net.fred.lua.foreign.ffi.FunctionCaller {
    long evalParamsTotalSize(java.lang.Object[]);
}