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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.InterpolationFilterReader;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Detects the current operating system and architecture, normalizes them, and sets them to various project
 * properties.
 * <ul>
 * <li>{@code os.detected.name} - normalized {@code os.name} (e.g. {@code linux}, {@code osx})</li>
 * <li>{@code os.detected.arch} - normalized {@code os.arch} (e.g. {@code x86_64}, {@code x86_32})</li>
 * <li>{@code os.detected.version} - {@code os.detected.version.major}.{@code os.detected.version.minor}</li>
 * <li>{@code os.detected.version.major} - major part of {@code os.version} (integer value)</li>
 * <li>{@code os.detected.version.minor} - minor part of {@code os.version} (integer value)</li>
 * <li>{@code os.detected.classifier} - a shortcut for {@code 'os.detectedName'.'os.detectedArch'}
 *     (e.g. {@code linux-x86_64}). If the property {@code ${os.detection.classifierWithLikes}} is set,
 *     the first value for which a corresponding {@code os.detected.release.like.{variant}} property
 *     exists will be appended to the classifier (e.g. building on ubuntu with
 *     {@code os.detection.classifierWithLikes = "debian,rhel"} would result in
 *     {@code os.detected.classifier = "linux-x86_64-debian"}).</li>
 * <li>{@code os.detected.release} - provides the ID for the linux release (if available).</li>
 * <li>{@code os.detected.release.version} - provides version ID for this linux release. Only
 *     available if ${os.detected.release} is also available. </li>
 * <li>{@code os.detected.release.like.{variant}} - Identifies a linux release that this release is
 *     "like" (for example, ubuntu is "like" debian). Only available if ${os.detected.release} is also
 *     available. An entry will always be made for os.detected.release.like.${os.detected.release}. </li>
 * </ul>
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "detect-os")
public class DetectExtension extends AbstractMavenLifecycleParticipant {

    @Requirement
    @SuppressWarnings("UnusedDeclaration")
    private Logger logger;

    private final Detector detector = new Detector() {
        @Override
        protected void log(String message) {
            logger.info(message);
        }

        @Override
        protected void logProperty(String name, String value) {
            if (logger.isInfoEnabled()) {
                logger.info(name + ": " + value);
            }
        }
    };

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        // Detect the OS and CPU architecture.
        Properties sessionProps = new Properties();
        sessionProps.putAll(session.getSystemProperties());
        sessionProps.putAll(session.getUserProperties());
        try {
            detector.detect(sessionProps, getClassifierWithLikes(session));
        } catch (DetectionException e) {
            throw new MavenExecutionException(e.getMessage(), session.getCurrentProject().getFile());
        }

        // Generate the dictionary.
        Map<String, String> dict = new LinkedHashMap<String, String>();
        dict.put(Detector.DETECTED_NAME, sessionProps.getProperty(Detector.DETECTED_NAME));
        dict.put(Detector.DETECTED_ARCH, sessionProps.getProperty(Detector.DETECTED_ARCH));
        dict.put(Detector.DETECTED_CLASSIFIER, sessionProps.getProperty(Detector.DETECTED_CLASSIFIER));
        for (Map.Entry<Object, Object> entry : sessionProps.entrySet()) {
            if (entry.getKey().toString().startsWith(Detector.DETECTED_RELEASE)) {
                dict.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        // Inject the current session.
        injectSession(session, dict);

        /// Perform the interpolation for the properties of all dependencies.
        for (MavenProject p: session.getProjects()) {
            interpolate(dict, p);
        }
    }

    /**
     * Inspects the session's user and project properties for the {@link
     * DetectMojo#CLASSIFIER_WITH_LIKES_PROPERTY} and separates the property into a list.
     */
    private static List<String> getClassifierWithLikes(MavenSession session) {
        // Check to see if the project defined the
        Properties props = new Properties();
        props.putAll(session.getUserProperties());
        props.putAll(session.getCurrentProject().getProperties());
        return DetectMojo.getClassifierWithLikes(
            props.getProperty(DetectMojo.CLASSIFIER_WITH_LIKES_PROPERTY));
    }

    private void injectSession(MavenSession session, Map<String, String> dict) {
        Properties sessionExecProps = session.getSystemProperties();
        sessionExecProps.setProperty(Detector.DETECTED_NAME, dict.get(Detector.DETECTED_NAME));
        sessionExecProps.setProperty(Detector.DETECTED_ARCH, dict.get(Detector.DETECTED_ARCH));
        sessionExecProps.setProperty(Detector.DETECTED_CLASSIFIER, dict.get(Detector.DETECTED_CLASSIFIER));
        for (Map.Entry<String, String> entry : dict.entrySet()) {
            if (entry.getKey().startsWith(Detector.DETECTED_RELEASE)) {
                sessionExecProps.setProperty(entry.getKey(), entry.getValue());
            }
        }

        // Work around the 'NoClassDefFoundError' or 'ClassNotFoundException' related with Aether in IntelliJ IDEA.
        for (StackTraceElement e: new Exception().getStackTrace()) {
            if (String.valueOf(e.getClassName()).startsWith("org.jetbrains.idea.maven")) {
                return;
            }
        }

        // Injection of RepositorySession is done in a separate class so that the extension is not impacted by
        // the case where the runtime does not have Aether.
        RepositorySessionInjector.injectRepositorySession(logger, session, dict);
    }

    private static void interpolate(Map<String, String> dict, MavenProject p) {
        if (p == null) {
            return;
        }

        interpolate(dict, p.getParent());
        interpolate(dict, p.getModel());
        for (ModelBase model: p.getActiveProfiles()) {
            interpolate(dict, model);
        }
    }

    private static void interpolate(Map<String, String> dict, ModelBase model) {
        model.getProperties().putAll(dict);
        interpolate(dict, model.getDependencies());

        DependencyManagement depMgmt = model.getDependencyManagement();
        if (depMgmt != null) {
            interpolate(dict, depMgmt.getDependencies());
        }

        if (model instanceof Model) {
            Build build = ((Model) model).getBuild();
            if (build != null) {
                for (Plugin bp: build.getPlugins()) {
                    interpolate(dict, bp.getDependencies());
                }
                if (build.getPluginManagement() != null) {
                    for (Plugin bp: build.getPluginManagement().getPlugins()) {
                        interpolate(dict, bp.getDependencies());
                    }
                }
            }
        }
    }

    private static void interpolate(Map<String, String> dict, Iterable<Dependency> dependencies) {
        if (dependencies == null) {
            return;
        }

        for (Dependency d: dependencies) {
            d.setGroupId(interpolate(dict, d.getGroupId()));
            d.setArtifactId(interpolate(dict, d.getArtifactId()));
            d.setVersion(interpolate(dict, d.getVersion()));
            d.setClassifier(interpolate(dict, d.getClassifier()));
            d.setSystemPath(interpolate(dict, d.getSystemPath()));
            for (Exclusion e: d.getExclusions()) {
                e.setGroupId(interpolate(dict, e.getGroupId()));
                e.setArtifactId(interpolate(dict, e.getArtifactId()));
            }
        }
    }

    private static String interpolate(Map<String, String> dict, String value) {
        if (value == null) {
            return null;
        }

        for (;;) {
            if (!value.contains("${")) {
                // Nothing to interpolate.
                break;
            }

            InterpolationFilterReader reader = new InterpolationFilterReader(new StringReader(value), dict);
            StringWriter writer = new StringWriter(value.length());
            for (;;) {
                int ch;
                try {
                    ch = reader.read();
                } catch (IOException e) {
                    // Should not reach here.
                    throw (Error) new Error().initCause(e);
                }

                if (ch == -1) {
                    break;
                }
                writer.write(ch);
            }

            String newValue = writer.toString();
            if (value.equals(newValue)) {
                // No interpolatable prpoerties left.
                break;
            }

            // Interpolated at least one variable.
            // Try again just in case the interpolation introduced another variable.
            value = newValue;
        }

        return value;
    }
}
