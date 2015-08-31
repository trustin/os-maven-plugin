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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Detects the current operating system and architecture, normalizes them, and sets them to various project
 * properties.
 * <ul>
 * <li>{@code os.detected.name} - normalized {@code os.name} (e.g. {@code linux}, {@code osx})</li>
 * <li>{@code os.detected.arch} - normalized {@code os.arch} (e.g. {@code x86_64}, {@code x86_32})</li>
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
@Mojo(name = "detect", defaultPhase = LifecyclePhase.VALIDATE)
public class DetectMojo extends AbstractMojo {
    static final String CLASSIFIER_WITH_LIKES_PROPERTY = "os.detection.classifierWithLikes";

    @Parameter(defaultValue = "${project}", readonly = true)
    @SuppressWarnings("UnusedDeclaration")
    private MavenProject project;

    @Parameter(property = CLASSIFIER_WITH_LIKES_PROPERTY, defaultValue = "${" + CLASSIFIER_WITH_LIKES_PROPERTY + "}")
    @SuppressWarnings("UnusedDeclaration")
    private String classifierWithLikes;

    private final Detector detector = new Detector() {
        @Override
        protected void log(String message) {
            getLog().info(message);
        }

        @Override
        protected void logProperty(String name, String value) {
            Log log = getLog();
            if (log.isInfoEnabled()) {
                log.info(name + ": " + value);
            }
        }
    };

    @Override
    public void execute() throws MojoExecutionException {
        try {
            detector.detect(project.getProperties(), getClassifierWithLikes(classifierWithLikes));
        } catch (DetectionException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Takes a comma-separated value of os "likes" to be included in the generated classifier and
     * returns them as a list.
     *
     * @param propertyValue the value of the {@link #CLASSIFIER_WITH_LIKES_PROPERTY} property.
     * @return the value as a list of entries.
     */
    public static List<String> getClassifierWithLikes(@Nullable String propertyValue) {
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        String[] parts = propertyValue.split("\\,");
        List<String> likes = new ArrayList<String>(parts.length);
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                likes.add(part);
            }
        }
        return likes;
    }
}
