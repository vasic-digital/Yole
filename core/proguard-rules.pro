# ProGuard rules for Yole Core module
# Keep all public APIs
-keep public class digital.vasic.yole.format.** { *; }
-keep public class digital.vasic.yole.model.** { *; }
-keep public class digital.vasic.yole.frontend.** { *; }

# Keep base classes
-keep public class * extends digital.vasic.yole.format.TextConverterBase { *; }
-keep public class * extends digital.vasic.yole.frontend.textview.SyntaxHighlighterBase { *; }
-keep public class * extends digital.vasic.yole.format.ActionButtonBase { *; }