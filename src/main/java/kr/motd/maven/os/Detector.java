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
    public static final String DETECTED_LIKE_PREFIX = "os.detected.like.";

    private static final String ID_PREFIX = "ID=";
    private static final String ID_LIKE_PREFIX = "ID_LIKE=";
    private static final String UNKNOWN = "unknown";
    private static final String[] OS_RELEASE_FILES = {"/etc/os-release", "/usr/lib/os-release"};
    private static final String REDHAT_FILE = "/etc/redhat-release";

    protected void detect(Properties props) throws DetectionException {
        log("------------------------------------------------------------------------");
        log("Detecting the operating system and CPU architecture");
        log("------------------------------------------------------------------------");

        Properties allProps = new Properties(System.getProperties());
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
        if (failOnUnknownOS == null || !failOnUnknownOS.equalsIgnoreCase("false")) {
            if (UNKNOWN.equals(detectedName)) {
                throw new DetectionException("unknown os.name: " + osName);
            }
            if (UNKNOWN.equals(detectedArch)) {
                throw new DetectionException("unknown os.arch: " + osArch);
            }
        }

        // Add properties for all variants that this OS is "like"
        for (String like : getOsLike(detectedName)) {
            String propKey = DETECTED_LIKE_PREFIX + like;
            setProperty(props, propKey, "true");
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
        if (value.equals("aarch64")) {
            return "aarch_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if (value.equals("ppc64")) {
            return "ppc_64";
        }

        return UNKNOWN;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    /**
     * Gets a collection of OSes that the given detected OS is "like".
     */
    private static Collection<String> getOsLike(String detectedName) {
        Set<String> like = new LinkedHashSet<String>();
        if (!"linux".equals(detectedName)) {
            // Currently only Linux is supported.
            return like;
        }

        for (String osReleaseFileName : OS_RELEASE_FILES) {
            File file = new File(osReleaseFileName);
            if (file.exists()) {
                like.addAll(parseLinuxOsReleaseFile(file));
                break;
            }
        }

        // Older versions of redhat don't have /etc/os-release. In this case, just
        // add "rhel" and "fedora" to the list.
        if (like.isEmpty() && isRedHatVariant()) {
            like.add("rhel");
            like.add("fedora");
        }

        return like;
    }

    /**
     * Indicates whether or not the OS is a variant of Redhat.
     */
    private static boolean isRedHatVariant() {
        return new File(REDHAT_FILE).exists();
    }

    /**
     * Parses a file in the format of {@code /etc/os-release} and returns a collection
     * containing lower-case values taken from the {@code ID} and {@code ID_LIKE} entries.
     */
    private static Collection<String> parseLinuxOsReleaseFile(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));

            Set<String> likeSet = new LinkedHashSet<String>();
            String line;
            while((line = reader.readLine()) != null) {
                // Parse the lines ID and ID_LIKE the same way.
                int prefixLength = line.startsWith(ID_LIKE_PREFIX) ? ID_LIKE_PREFIX.length() :
                    line.startsWith(ID_PREFIX) ? ID_PREFIX.length() : -1;
                if (prefixLength < 0) {
                    continue;
                }

                // Get the value after "=".
                line = line.substring(prefixLength);

                // Split the line on any whitespace.
                String[] parts =  line.split("\\s+");
                Collections.addAll(likeSet, parts);
            }
            return likeSet;
        } catch (IOException ignored) {
            // Just absorb. Don't treat failure to read /etc/os-release as an error.
        } finally {
            closeQuietly(reader);
        }
        return Collections.emptyList();
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
}
