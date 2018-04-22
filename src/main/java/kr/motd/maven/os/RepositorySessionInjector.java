package kr.motd.maven.os;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;

final class RepositorySessionInjector {
    @SuppressWarnings("unchecked")
    static void injectRepositorySession(
            Logger logger, MavenSession session, Map<String, String> dict) {
        // Inject repository session properties.
        try {
            Map<String, String> repoSessionProps;
            // Due to repackaging of Aether in Maven 3.1, session.getRepositorySession()
            // will return either org.eclipse.aether.RepositorySystemSession (Maven 3.1+)
            // or org.sonatype.aether.RepositorySystemSession (Maven 3.0.x)
            // depending on the version of Maven that executes the project.
            // Both interfaces have getSystemProperties() accessor method that returns Map<String, String>.
            final Object repoSession = session.getRepositorySession();
            final Class<?> cls = repoSession.getClass();
            final Method getSystemPropertiesMethod = cls.getDeclaredMethod("getSystemProperties");
            repoSessionProps = (Map<String, String>) getSystemPropertiesMethod.invoke(repoSession);
            try {
                for (Map.Entry<String, String> e : dict.entrySet()) {
                    repoSessionProps.put(e.getKey(), e.getValue());
                }
            } catch (Exception ex) {
                // Time to hack: RepositorySystemSession.getSystemProperties() returned an immutable map.
                final Field f = cls.getDeclaredField("systemProperties");
                f.setAccessible(true);
                repoSessionProps = (Map<String, String>) f.get(repoSession);
                for (Map.Entry<String, String> e : dict.entrySet()) {
                    repoSessionProps.put(e.getKey(), e.getValue());
                }
            }
        } catch (Throwable t) {
            logger.warn("Failed to inject repository session properties.", t);
        }
    }

    private RepositorySessionInjector() {}
}
