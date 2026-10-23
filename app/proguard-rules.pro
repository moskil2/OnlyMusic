# Media3 and Compose ship their own consumer rules; nothing extra is needed.
# Drop debug and verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
