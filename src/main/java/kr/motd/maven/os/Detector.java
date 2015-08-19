/*
 * Copyright 2014 Trustin Heuiseung Lee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.motd.maven.os;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public abstract class Detector {

    public static final String DETECTED_NAME = "os.detected.name";
    public static final String DETECTED_ARCH = "os.detected.arch";
    public static final String DETECTED_CLASSIFIER = "os.detected.classifier";
    public static final String DETECTED_RELEASE = "os.detected.release";
    public static final String DETECTED_RELEASE_VERSION = DETECTED_RELEASE + ".version";
    public static final String DETECTED_RELEASE_LIKE_PREFIX = DETECTED_RELEASE + ".like.";

    private static final String UNKNOWN = "unknown";
    private static final String LINUX_ID_PREFIX = "ID=";
    private static final String LINUX_ID_LIKE_PREFIX = "ID_LIKE=";
    private static final String LINUX_VERSION_ID_PREFIX = "VERSION_ID=";
    private static final String[] LINUX_OS_RELEASE_FILES = {"/etc/os-release", "/usr/lib/os-release"};
    private static final String REDHAT_RELEASE_FILE = "/etc/redhat-release";
    private static final String[] DEFAULT_REDHAT_VARIANTS = {"rhel", "fedora"};

    protected void detect(Properties props) {
        log("------------------------------------------------------------------------");
        log("Detecting the operating system and CPU architecture");
        log("------------------------------------------------------------------------");

        final Properties allProps = new Properties(System.getProperties());
        allProps.putAll(props);

        final String osName = allProps.getProperty("os.name");
        final String osArch = allProps.getProperty("os.arch");

        final String detectedName = normalizeOs(osName);
        final String detectedArch = normalizeArch(osArch);
        final String detectedClassifier = detectedName + '-' + detectedArch;

        setProperty(props, DETECTED_NAME, detectedName);
        setProperty(props, DETECTED_ARCH, detectedArch);
        setProperty(props, DETECTED_CLASSIFIER, detectedClassifier);

        final String failOnUnknownOS = allProps.getProperty("failOnUnknownOS");
        if (!"false".equalsIgnoreCase(failOnUnknownOS)) {
            if (UNKNOWN.equals(detectedName)) {
                throw new DetectionException("unknown os.name: " + osName);
            }
            if (UNKNOWN.equals(detectedArch)) {
                throw new DetectionException("unknown os.arch: " + osArch);
            }
        }

        LinuxRelease linuxRelease = "linux".equals(detectedName) ? getLinuxRelease() : null;
        if (linuxRelease != null) {
            setProperty(props, DETECTED_RELEASE, linuxRelease.id);
            if (linuxRelease.version != null) {
                setProperty(props, DETECTED_RELEASE_VERSION, linuxRelease.version);
            }
            for (String like : linuxRelease.like) {
                String propKey = DETECTED_RELEASE_LIKE_PREFIX + like;
                setProperty(props, propKey, "true");
            }
        }
    }

    private void setProperty(Properties props, String name, String value) {
        props.setProperty(name, value);
        System.setProperty(name, value);
        logProperty(name, value);
    }

    protected abstract void log(String message);
    protected abstract void logProperty(String name, String value);

    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("aix")) {
            return "aix";
        }
        if (value.startsWith("hpux")) {
            return "hpux";
        }
        if (value.startsWith("os400")) {
            // Avoid the names such as os4000
            if (value.length() <= 5 || !Character.isDigit(value.charAt(5))) {
                return "os400";
            }
        }
        if (value.startsWith("linux")) {
            return "linux";
        }
        if (value.startsWith("macosx") || value.startsWith("osx")) {
            return "osx";
        }
        if (value.startsWith("freebsd")) {
            return "freebsd";
        }
        if (value.startsWith("openbsd")) {
            return "openbsd";
        }
        if (value.startsWith("netbsd")) {
            return "netbsd";
        }
        if (value.startsWith("solaris") || value.startsWith("sunos")) {
            return "sunos";
        }
        if (value.startsWith("windows")) {
            return "windows";
        }

        return UNKNOWN;
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (value.matches("^(ia64|itanium64)$")) {
            return "itanium_64";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(value)) {
            return "aarch_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if ("ppc64".equals(value)) {
            return "ppc_64";
        }
        if (value.equals("ppc64le")) {
            return "ppcle_64";
        }
        if (value.equals("s390")) {
            return "s390_32";
        }
        if (value.equals("s390x")) {
            return "s390_64";
        }

        return UNKNOWN;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private static LinuxRelease getLinuxRelease() {
        // First, look for the os-release file.
        for (String osReleaseFileName : LINUX_OS_RELEASE_FILES) {
            File file = new File(osReleaseFileName);
            if (file.exists()) {
                return parseLinuxOsReleaseFile(file);
            }
        }

        // Older versions of redhat don't have /etc/os-release. In this case, try
        // parsing this file.
        File file = new File(REDHAT_RELEASE_FILE);
        if (file.exists()) {
            return parseLinuxRedhatReleaseFile(file);
        }

        return null;
    }

    /**
     * Parses a file in the format of {@code /etc/os-release} and return a {@link LinuxRelease}
     * based on the {@code ID}, {@code ID_LIKE}, and {@code VERSION_ID} entries.
     */
    private static LinuxRelease parseLinuxOsReleaseFile(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));

            String id = null;
            String version = null;
            Set<String> likeSet = new LinkedHashSet<String>();
            String line;
            while((line = reader.readLine()) != null) {
                // Parse the ID line.
                if (line.startsWith(LINUX_ID_PREFIX)) {
                    // Set the ID for this version.
                    id = line.substring(LINUX_ID_PREFIX.length());

                    // Also add the ID to the "like" set.
                    likeSet.add(id);
                    continue;
                }

                // Parse the VERSION_ID line.
                if (line.startsWith(LINUX_VERSION_ID_PREFIX)) {
                    // Set the ID for this version.
                    version = normalizeReleaseVersion(line.substring(LINUX_VERSION_ID_PREFIX.length()));
                    continue;
                }

                // Parse the ID_LIKE line.
                if (line.startsWith(LINUX_ID_LIKE_PREFIX)) {
                    line = line.substring(LINUX_ID_LIKE_PREFIX.length());

                    // Split the line on any whitespace.
                    String[] parts =  line.split("\\s+");
                    Collections.addAll(likeSet, parts);
                }
            }

            if (id != null) {
                return new LinuxRelease(id, version, likeSet);
            }
        } catch (IOException ignored) {
            // Just absorb. Don't treat failure to read /etc/os-release as an error.
        } finally {
            closeQuietly(reader);
        }
        return null;
    }

    /**
     * Parses the {@code /etc/redhat-release} and returns a {@link LinuxRelease} containing the
     * ID and like ["rhel", "fedora", ID]. Currently only supported for CentOS, Fedora, and RHEL.
     * Other variants will return {@code null}.
     */
    private static LinuxRelease parseLinuxRedhatReleaseFile(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));

            // There is only a single line in this file.
            String line = reader.readLine();
            if (line != null) {
                line = line.toLowerCase(Locale.US);

                String id;
                if (line.contains("centos")) {
                    id = "centos";
                } else if (line.contains("fedora")) {
                    id = "fedora";
                } else if (line.contains("red hat enterprise linux")) {
                    id = "rhel";
                } else {
                    // Other variants are not currently supported.
                    return null;
                }

                Set<String> likeSet = new LinkedHashSet<String>();
                likeSet.addAll(Arrays.asList(DEFAULT_REDHAT_VARIANTS));
                likeSet.add(id);

                return new LinuxRelease(id, null, likeSet);
            }
        } catch (IOException ignored) {
            // Just absorb. Don't treat failure to read /etc/os-release as an error.
        } finally {
            closeQuietly(reader);
        }
        return null;
    }

    private static String normalizeReleaseVersion(String version) {
        return version.trim().replace("\"", "");
    }

    private static void closeQuietly(Closeable obj) {
        try {
            if (obj != null) {
                obj.close();
            }
        } catch (IOException ignored) {
            // Ignore.
        }
    }

    private static class LinuxRelease {
        final String id;
        final String version;
        final Collection<String> like;

        LinuxRelease(String id, String version, Set<String> like) {
            this.id = id;
            this.version = version;
            this.like = Collections.unmodifiableCollection(like);
        }
    }
}
