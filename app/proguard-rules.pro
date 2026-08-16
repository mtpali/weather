# Release hardening for weather.
# R8/AGP automatically supplies Android component and Compose consumer rules.

-allowaccessmodification
-repackageclasses 'w'
-renamesourcefileattribute Source

# Remove logging calls from release output if introduced later.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
