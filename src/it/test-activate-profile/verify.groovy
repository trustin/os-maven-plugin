import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils

def projectPropertiesFile = new File(basedir, "target/project.properties")

assert projectPropertiesFile.exists() : "project.properties not found"
assert projectPropertiesFile.canRead() : "project.properties cannot be read"

Properties projectProperties = new Properties()
projectPropertiesFile.withInputStream {
    projectProperties.load(it)
}

if (SystemUtils.IS_OS_WINDOWS) {
    assert projectProperties."active.profile" == 'detected-windows'
} else if (SystemUtils.IS_OS_LINUX) {
    String osReleaseContent = FileUtils.readFileToString(new File("/etc/os-release"))

    if (osReleaseContent.contains("ID=ubuntu")) {
        assert projectProperties."active.profile" == 'detected-ubuntu'
    }
} else if (SystemUtils.IS_OS_MAC) {
    assert projectProperties."active.profile" == 'detected-osx'
}
