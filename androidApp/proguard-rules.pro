# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-64 Phase 3: ProGuard / R8 keep rules for Apache POI poi-ooxml-lite.
#
# Source: centic9/poi-on-android (MIT) — adapted 8-directive set.
# Required because POI uses reflection-heavy XML parsing (XmlBeans) and
# StAX/SAX adapters that R8 would otherwise strip.
#
# Note: isMinifyEnabled = false in androidApp/build.gradle.kts as of v1.6.0,
# so these rules are currently dormant. They are included preemptively so
# enabling minification in a future release does not break .docx import.

# --- Apache POI core ---
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# --- XMLBeans (used internally by poi-ooxml-lite for schema types) ---
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

# --- OpenXML4J (OOXML package handling) ---
-keep class org.apache.poi.openxml4j.** { *; }

# --- StAX / javax.xml.stream (Android ships its own impl; keep the interfaces) ---
-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**

# --- SAX parser references inside POI ---
-keep class org.xml.sax.** { *; }
-dontwarn org.xml.sax.**

# --- Suppress warnings for missing optional dependencies ---
-dontwarn org.bouncycastle.**
-dontwarn org.etsi.**
-dontwarn org.w3c.dom.ElementTraversal
