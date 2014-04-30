package kr.motd.maven.plugin.os;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Locale;
import java.util.Properties;

/**
 * Detects the current operating system and architecture, normalizes them, and sets them to various project
 * properties.
 * <ul>
 * <li>{@code os.detected.name} - normalized {@code os.name} (e.g. {@code linux}, {@code osx})</li>
 * <li>{@code os.detected.arch} - normalized {@code os.arch} (e.g. {@code x86_64}, {@code x86_32})</li>
 * <li>{@code os.detected.classifier} - a shortcut for {@code 'os.detectedName'.'os.detectedArch'}
 *     (e.g. {@code linux-x86_64})</li>
 * </ul>
 */
@Mojo(name = "detect", defaultPhase = LifecyclePhase.VALIDATE)
public class DetectMojo extends AbstractMojo {

    private static final String UNKNOWN = "unknown";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${os.name}", readonly = true)
    private String osName;
    @Parameter(defaultValue = "${os.arch}", readonly = true)
    private String osArch;

    @Parameter(defaultValue = "true")
    private boolean failOnUnknownOS;

    public void execute() throws MojoExecutionException {
        final String detectedName = normalizeOs(osName);
        final String detectedArch = normalizeArch(osArch);
        final String detectedClassifier = detectedName + '-' + detectedArch;

        setProperty("os.detected.name", detectedName);
        setProperty("os.detected.arch", detectedArch);
        setProperty("os.detected.classifier", detectedClassifier);

        if (failOnUnknownOS) {
            if (UNKNOWN.equals(detectedName)) {
                throw new MojoExecutionException("unknown os.name: " + osName);
            }
            if (UNKNOWN.equals(detectedArch)) {
                throw new MojoExecutionException("unknown os.arch: " + osArch);
            }
        }
    }

    private void setProperty(String name, String value) {
        final Log log = getLog();
        final Properties props = project.getProperties();

        props.setProperty(name, value);
        System.setProperty(name, value);
        if (log.isInfoEnabled()) {
            log.info(name + ": " + value);
        }
    }

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
        if (value.startsWith("macosx")) {
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
        if (value.matches("^(amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (value.matches("^(i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (value.equals("ia64")) {
            return "itanium_64";
        }
        if (value.equals("sparc")) {
            return "sparc_32";
        }
        if (value.equals("sparcv9")) {
            return "sparc_64";
        }
        if (value.equals("arm")) {
            return "arm_32";
        }
        if (value.equals("aarch64")) {
            return "aarch_64";
        }
        if (value.equals("ppc")) {
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
}
