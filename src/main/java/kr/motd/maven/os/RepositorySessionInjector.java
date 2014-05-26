package kr.motd.maven.os;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;

import java.lang.reflect.Field;
import java.util.Map;

final class RepositorySessionInjector {
    @SuppressWarnings("unchecked")
    static void injectRepositorySession(
            Logger logger, MavenSession session, Map<String, String> dict) throws MavenExecutionException {
        // Inject repository session properties.
        try {
            RepositorySystemSession repoSession = session.getRepositorySession();
            Map<String, String> repoSessionProps = repoSession.getSystemProperties();
            try {
                repoSessionProps.putAll(dict);
            } catch (Exception e) {
                // Time to hack: RepositorySystemSession.getRepositorySession() returned an immutable map.
                Class<?> cls = session.getRepositorySession().getClass();
                Field f = cls.getDeclaredField("systemProperties");
                f.setAccessible(true);
                repoSessionProps = (Map<String, String>) f.get(repoSession);
                repoSessionProps.putAll(dict);
            }
        } catch (Throwable t) {
            logger.warn("Failed to inject repository session properties.", t);
        }
    }
}
