/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole replacement for bonede tree-sitter-ng `org.treesitter.utils.NativeUtils`.
 *
 * Why: the bonede 0.22.6 JAR's NativeUtils:
 *   - resolves library file names from os.name/os.arch in a way that picks
 *     `aarch64-linux-gnu-tree-sitter.so` on Android (because os.name reports
 *     "Linux" there). That binary is glibc-based and dlopen rejects it on
 *     bionic.
 *   - extracts the resource into `${user.home}/.tree-sitter/` by default,
 *     which is unwritable from Android app processes.
 *   - CRC-compares the extracted file with classpath bytes and overwrites
 *     on mismatch — so even pre-placing an Android NDK build does not
 *     survive bonede's flow.
 *
 * This replacement class:
 *   - keeps the same FQCN (org.treesitter.utils.NativeUtils) and public
 *     `loadLib(String)` signature so the bonede-generated TSParser /
 *     TreeSitterMarkdown static initialisers see no behavioural change.
 *   - detects Android via System.getProperty("java.vm.vendor")/"java.vendor"
 *     == "The Android Project". On Android, it loads the per-ABI .so via
 *     System.loadLibrary, which routes through the Android runtime's
 *     native-lib search path that knows about <apk>/lib/<abi>/lib*.so.
 *   - falls back to the original bonede logic (extract from classpath,
 *     System.load) on Desktop / Server JVMs.
 *
 * The `shared/native/android-tree-sitter/<abi>/lib{tree-sitter,tree-sitter-markdown}.so`
 * files are packaged into the APK by the androidApp's jniLibs convention
 * (the project's shared/build.gradle.kts copies them into the merged native
 * libs directory at build time). Bionic resolves `System.loadLibrary("tree-sitter")`
 * to `<nativeLibraryDir>/libtree-sitter.so` automatically.
 *
 *########################################################*/
package org.treesitter.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;

public abstract class NativeUtils {

    private static final boolean ANDROID;
    private static final Set<String> LOADED = new HashSet<>();

    static {
        String vendor = System.getProperty("java.vm.vendor", "");
        String runtime = System.getProperty("java.runtime.name", "");
        String vmName = System.getProperty("java.vm.name", "");
        ANDROID = vendor.contains("Android")
                || runtime.contains("Android")
                || vmName.contains("Dalvik")
                || vmName.contains("ART");
    }

    private NativeUtils() {
        // utility
    }

    /**
     * Public entry point retained for ABI compatibility with bonede
     * tree-sitter-ng. Argument is the bonede-style "lib/tree-sitter" or
     * "lib/tree-sitter-markdown" prefix; the rest is computed.
     */
    public static synchronized void loadLib(String name) {
        if (LOADED.contains(name)) {
            return;
        }
        if (ANDROID) {
            loadOnAndroid(name);
        } else {
            loadFromClasspath(name);
        }
        LOADED.add(name);
    }

    /* ------------------------- Android path ------------------------- */

    private static void loadOnAndroid(String name) {
        // name is "lib/tree-sitter" or "lib/tree-sitter-markdown".
        // System.loadLibrary expects the un-prefixed soname, e.g.
        // "tree-sitter" → looks for lib/<abi>/libtree-sitter.so.
        String soname = androidSonameFromName(name);
        try {
            System.loadLibrary(soname);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                    "Yole NativeUtils: System.loadLibrary(\"" + soname
                            + "\") failed on Android. Verify that "
                            + "shared/native/android-tree-sitter/<abi>/lib"
                            + soname + ".so is packaged into the APK. "
                            + "Original error: " + e.getMessage(),
                    e);
        }
    }

    private static String androidSonameFromName(String name) {
        // Bonede passes "lib/tree-sitter" or "lib/tree-sitter-markdown".
        int slash = name.lastIndexOf('/');
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    /* ----------------------- Desktop / Server ----------------------- */

    private static void loadFromClasspath(String name) {
        // Re-implements bonede 0.22.6's loadLib flow verbatim so Desktop
        // unit tests continue to pass when our replacement NativeUtils is
        // patched into the JAR. Reads the bonede-packaged
        // lib/<arch>-<os>-<base>.<ext> resource from the classpath,
        // writes it to ${tree-sitter-lib:-$HOME/.tree-sitter}/, then
        // System.load() the absolute path.
        try {
            String full = getFullLibName(name);
            Path storeDir = getLibStorePath();
            File target = storeDir.resolve(full).toFile();
            File parent = target.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            byte[] classpath = readLib(name);
            boolean shouldWrite = !target.exists()
                    || crc32(readFile(target)) != crc32(classpath);
            if (shouldWrite) {
                try (OutputStream out = new FileOutputStream(target);
                        InputStream in = new ByteArrayInputStream(classpath)) {
                    in.transferTo(out);
                }
            }
            System.load(target.getAbsolutePath());
        } catch (IOException ioe) {
            throw new RuntimeException(
                    "NativeUtils: failed to extract " + name + ": " + ioe,
                    ioe);
        }
    }

    private static String getFullLibName(String name) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        String ext;
        String os;
        if (osName.contains("windows")) {
            ext = "dll";
            os = "windows";
        } else if (osName.contains("mac")) {
            ext = "dylib";
            os = "macos";
        } else if (osName.contains("linux")) {
            ext = "so";
            os = "linux-gnu";
        } else {
            throw new RuntimeException("Does not support OS: " + osName);
        }
        String arch;
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            arch = "x86_64";
        } else if (osArch.contains("aarch64")) {
            arch = "aarch64";
        } else {
            throw new RuntimeException("Does not support arch: " + osArch);
        }
        // name is "lib/tree-sitter" or "lib/tree-sitter-markdown".
        // Reconstruct "lib/<arch>-<os>-tree-sitter.<ext>" identical to
        // bonede 0.22.6's getFullLibName.
        int slash = name.lastIndexOf('/');
        String dir = slash >= 0 ? name.substring(0, slash) : "";
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        String full = String.format("%s-%s-%s.%s", arch, os, base, ext);
        return dir.isEmpty() ? full : dir + "/" + full;
    }

    private static Path getLibStorePath() {
        String prop = System.getProperty("tree-sitter-lib");
        if (prop != null) {
            return Path.of(prop);
        }
        return Path.of(System.getProperty("user.home", "."), ".tree-sitter");
    }

    private static byte[] readLib(String name) throws IOException {
        String full = getFullLibName(name);
        InputStream is = NativeUtils.class.getClassLoader()
                .getResourceAsStream(full);
        if (is == null) {
            throw new RuntimeException("Can't open " + full);
        }
        try (InputStream s = is) {
            return s.readAllBytes();
        }
    }

    private static byte[] readFile(File f) throws IOException {
        try (InputStream is = new java.io.FileInputStream(f)) {
            return is.readAllBytes();
        }
    }

    private static long crc32(byte[] data) {
        CRC32 c = new CRC32();
        c.update(data);
        return c.getValue();
    }
}
