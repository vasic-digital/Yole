# Keep all public classes for testing
-keep public class digital.vasic.opoc.util.** { *; }
-keep public class digital.vasic.opoc.format.** { *; }
-keep public class digital.vasic.opoc.model.** { *; }
-keep public class digital.vasic.opoc.web.** { *; }
-keep public class digital.vasic.opoc.wrapper.** { *; }

# Keep all JUnit test classes
-keep class * extends junit.framework.TestCase { *; }
-keep class * extends org.junit.Test { *; }
-keep class * extends org.junit.Assert { *; }

# Keep AssertJ classes
-keep class org.assertj.** { *; }

# Keep all Kotlin metadata for reflection
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep Android classes used by the library
-keep class android.** { *; }
-keep class androidx.** { *; }

# Keep Java StringConcatFactory for Kotlin string concatenation
-dontwarn java.lang.invoke.StringConcatFactory
-keep class java.lang.invoke.StringConcatFactory { *; }