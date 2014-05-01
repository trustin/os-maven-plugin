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

    public void execute() throws MojoExecutionException {
        try {
            detector.detect(project.getProperties());
        } catch (DetectionException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
