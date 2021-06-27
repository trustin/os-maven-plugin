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
}
