package kr.motd.maven.os;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
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
                try {
                    for (Map.Entry<String, String> e : dict.entrySet()) {
                        repoSessionProps.put(e.getKey(), e.getValue());
                    }
                } catch (Exception ex2) {
                    // In Maven 4, DefaultCloseableSession uses an immutable map
                    // but DefaultRepositorySystemSession may also have an immutable map
                    repoSessionProps = new HashMap<>(repoSessionProps);
                    for (Map.Entry<String, String> e : dict.entrySet()) {
                        repoSessionProps.put(e.getKey(), e.getValue());
                    }
                    repoSessionProps = Collections.unmodifiableMap(repoSessionProps);
                    f.set(repoSession, repoSessionProps);
                    try {
                        // This is to support DefaultRepositorySystemSession
                        final Field fv = cls.getDeclaredField("systemPropertiesView");
                        fv.setAccessible(true);
                        fv.set(repoSession, repoSessionProps);
                    } catch (Exception ex3) {
                        // ignore
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn("Failed to inject repository session properties.", t);
        }
    }

    private RepositorySessionInjector() {}
}
