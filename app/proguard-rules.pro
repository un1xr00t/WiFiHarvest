# Keep application class
-keep public class * extends android.app.Application

# Keep WiFiHarvest specific classes
-keep class com.app.wifiharvest.** { *; }

# Keep Google Maps and Location Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep model classes (for JSON serialization)
-keep class com.app.wifiharvest.models.** { *; }
-keep class com.app.wifiharvest.WifiNetwork { *; }

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}