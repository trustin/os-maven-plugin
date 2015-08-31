package kr.motd.maven.os;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Bundle;

import java.util.Collections;
import java.util.Properties;

public class EclipseStartup extends Detector implements IStartup {

    private static final String ID = EclipseStartup.class.getPackage().getName();

    private ILog logger;

    @Override
    public void earlyStartup() {
        Bundle bundle = Platform.getBundle(ID);
        logger = Platform.getLog(bundle);
        detect(new Properties(), Collections.<String>emptyList());
    }

    @Override
    protected void log(String message) {
        logger.log(new Status(IStatus.INFO, ID, message));
    }

    @Override
    protected void logProperty(String name, String value) {
        logger.log(new Status(IStatus.INFO, ID, name + ": " + value));
    }
}
