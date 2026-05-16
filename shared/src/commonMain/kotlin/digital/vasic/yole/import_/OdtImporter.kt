/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 6: OdtImporter — expect class declaration.
 *
 * Desktop (JVM) actual uses Apache ODFDOM (org.odftoolkit:odfdom-java) to
 * open the ODT file and walk the OdfElement tree, emitting Markdown.
 *
 * Android (JVM) actual reads the same ODT bytes as a raw ZipInputStream,
 * extracts content.xml, and parses it with the Android-native XmlPullParser.
 * This avoids the Xerces2 dependency that ODFDOM pulls in, which conflicts
 * with Android's built-in XML parser (Phase 0 §5 finding).
 *
 * iOS and Wasm stubs return ImportError.NotSupported per CONST-035
 * honest-degradation policy.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Imports OpenDocument Text (.odt) files and converts them to Markdown.
 *
 * Platform support:
 * - Desktop (JVM): full implementation via Apache ODFDOM OdfTextDocument.
 * - Android (JVM): full implementation via ZipInputStream + XmlPullParser
 *                  (avoids Xerces conflict — see Phase 0 §5 research finding).
 * - iOS:  returns [ImportError.NotSupported] (no JVM / App Store sandbox).
 * - Web:  returns [ImportError.NotSupported] (Wasm browser sandbox / no JVM).
 */
expect class OdtImporter() : DocumentImporter
