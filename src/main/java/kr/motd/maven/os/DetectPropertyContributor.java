package kr.motd.maven.os;

import org.apache.maven.api.spi.PropertyContributor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component(role = PropertyContributor.class)
public class DetectPropertyContributor implements PropertyContributor {

    private final Logger logger;

    @Inject
    DetectPropertyContributor(Logger logger) {
        super();
        this.logger = logger;
    }

    @Override
    public void contribute(Map<String, String> map) {
        DetectExtension.disable();
        Properties props = new Properties();
        props.putAll(map);
        Detector detector = new SimpleDetector(new SimpleSystemPropertyOperations(map), new SimpleFileOperations(), logger);
        detector.detect(props, getClassifierWithLikes(map));
    }

    /**
     * Inspects the session's user and project properties for the {@link
     * DetectMojo#CLASSIFIER_WITH_LIKES_PROPERTY} and separates the property into a list.
     */
    private static List<String> getClassifierWithLikes(Map<String, String> map) {
        // Check to see if the project defined the
        return DetectMojo.getClassifierWithLikes(map.get(DetectMojo.CLASSIFIER_WITH_LIKES_PROPERTY));
    }


    private static class SimpleDetector extends Detector {

        private final Logger logger;

        private SimpleDetector(SystemPropertyOperationProvider systemPropertyOperationProvider, FileOperationProvider fileOperationProvider, Logger logger) {
            super(systemPropertyOperationProvider, fileOperationProvider);
            this.logger = logger;
        }

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

    }

    private static class SimpleSystemPropertyOperations implements SystemPropertyOperationProvider {
        final Map<String, String> map;

        private SimpleSystemPropertyOperations(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public String getSystemProperty(String name) {
            return System.getProperty(name);
        }

        @Override
        public String getSystemProperty(String name, String def) {
            return System.getProperty(name, def);
        }

        @Override
        public String setSystemProperty(String name, String value) {
            map.put(name, value);
            return System.setProperty(name, value);
        }
    }

    private static class SimpleFileOperations implements FileOperationProvider {
        @Override
        public InputStream readFile(String fileName) throws IOException {
            return Files.newInputStream(Paths.get(fileName));
        }
    }

}
