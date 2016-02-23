`os-maven-plugin` is a [Maven](http://maven.apache.org/) extension/plugin that generates various useful platform-dependent project properties normalized from `${os.name}` and `${os.arch}`.

`${os.name}` and `${os.arch}` are often subtly different between JVM and operating system versions or they sometimes contain machine-unfriendly characters such as whitespaces.  This plugin tries to remove such fragmentation so that you can determine the current operating system and architecture reliably.

### Generated properties

`os-maven-plugin` detects the name of the current operating system and normalizes it into more generic one.

* `os.detected.name`
  * `aix`
  * `hpux`
  * `os400`
  * `linux`
  * `osx`
  * `freebsd`
  * `openbsd`
  * `netbsd`
  * `sunos`
  * `windows`

`os-maven-plugin` also detects the architecture of the current operating system and normalizes it into more generic one.

* `os.detected.arch`
  * `x86_64`
  * `x86_32`
  * `itanium_64`
  * `sparc_32`
  * `sparc_64`
  * `arm_32`
  * `aarch_64`
  * `ppc_32`
  * `ppc_64`
  * `ppcle_64`
  * `s390_32`
  * `s390_64`

* `os.detected.version.*`
  Operating system dependent version number that may indicate the kernel or OS release version.
  * `os.detected.version.major`
  * `os.detected.version.minor`

You can also use the `${os.detected.classifier}` property, which is a shortcut of `${os.detected.name}-${os.detected.arch}`.

### Enabling `os-maven-plugin` on your Maven project

Add the extension to your `pom.xml` like the following:

```xml
<project>
  <build>
    <extensions>
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.4.0.Final</version>
      </extension>
    </extensions>
  </build>
</project>
```

### Using Gradle?

Use [the plugin from Google](https://github.com/google/osdetector-gradle-plugin).

### Adding a platform-dependent dependency

Use `${os.detected.classifier}` as the classifier of the dependency:

```xml
<project>
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>my-native-library</artifactId>
      <version>1.0.0</version>
      <classifier>${os.detected.classifier}</classifier>
    </dependency>
  </dependencies>
</project>
```

### Generating a platform-dependent dependency

Use `${os.detected.classifier}` as the classifier of the produced JAR:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <classifier>${os.detected.classifier}</classifier>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### Customized deployments for specific releases of Linux

If you need to customize your deployment based on a specific release of Linux, a few other variables may
be made available.

* `${os.detected.release}`: provides the ID for the linux release.
* `${os.detected.release.version}`: provides version ID for this linux release. Only available if
`${os.detected.release}` is also available.
* `${os.detected.release.like.<variant>}`: Identifies a linux release that this release is
"like" (for example, `ubuntu` is "like" `debian`). Only available if `${os.detected.release}` is also
available. An entry will always be made for `os.detected.release.like.${os.detected.release}`.

For most Linux distributions, these values are populated from the `ID`, `ID_LIKE`, and `VERSION_ID`
entries in [`/etc/os-release` or `/usr/lib/os-release`](http://www.freedesktop.org/software/systemd/man/os-release.html).

#### Older variants of Red Hat

If `/etc/os-release` and `/usr/lib/os-release` are unavailable, then `/etc/redhat-release` is inspected.
If it contains `CentOS`, `Fedora`, or `Redhat Enterprise Linux` then `${os.detected.release}` will be
set to `centos`, `fedora`, or `rhel` respectively (other variants are unsupported). "Like" entries will
be created for `${os.detected.release}` as well as `rhel` and `fedora`. The `${os.detected.release.version}`
variable is currently not set.

#### Customizing the classifier

You can configure the `os-maven-plugin` to automatically append a particular "like" value to
`${os.detected.classifier}`. This greatly simplifies the deployment for artifacts that are
different across Linux distributions. The plugin looks for a property named
`os.detection.classifierWithLikes`, which is a comma-separated list of "like" values. The first
value found which matches an existing `${os.detected.release.like.<variant>}` property
will be automatically appended to the classifier.

```xml
<project>
  <properties>
    <os.detection.classifierWithLikes>debian,rhel</os.detection.classifierWithLikes>
  </properties>

  <build>
    <extensions>
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.4.0.Final</version>
      </extension>
    </extensions>
  </build>
</project>
```
This will result in a `${os.detected.classifier}` of `linux-<arch>-debian` on debian-like systems,
`linux-<arch>-rhel` on rhel systems, and the default of `<os>-<arch>` on everything else.

### Issues with Eclipse m2e or other IDEs

If you are using IntelliJ IDEA, you should not have any problem.

If you are using Eclipse, you need to install an additional Eclipse plugin because [m2e](https://www.eclipse.org/m2e/) does not evaluate the extension specified in a `pom.xml`.  [Download `os-maven-plugin-1.4.0.Final.jar`](http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.4.0.Final/os-maven-plugin-1.4.0.Final.jar) and put it into the `<ECLIPSE_HOME>/plugins` directory.

(As you might have noticed, `os-maven-plugin` is a Maven extension, a Maven plugin, and an Eclipse plugin.)

If you are using other IDEs such as NetBeans, you need to set the system properties `os-maven-plugin` sets manually when your IDE is launched.  You usually use JVM's `-D` flags like the following:

    -Dos.detected.name=linux -Dos.detected.arch=x86_64 -Dos.detected.classifier=linux-x86_64

### Mapping Rules

#### os.detected.name

The generation of `os.detected.name` is based on the value of the `os.name` system property.

The table below assumes that we have a variable `osName` that is set to `System.getProperty("os.name").toLowerCase()`;

| Rule for `osName` | `os.deteced.name` |
| ----------------- | --------------------------- |
| startsWith(`aix`) | `aix` |
| startsWith(`hpux`) | `hpux` |
| startsWith(`os400`) AND (`osName.length() <= 5` OR `!Character.isDigit(osName.charAt(5))`) | `os400` |
| startsWith(`linux`) | `linux` |
| startsWith(`macosx` OR `osx`) | `osx` |
| startsWith(`freebsd`) | `freebsd` |
| startsWith(`openbsd`) | `openbsd` |
| startsWith(`netbsd`) | `netbsd` |
| startsWith(`solaris` OR `sunos`) | `sunos` |
| startsWith(`windows`) | `windows` |
| default | `unknown` |

#### os.detected.arch

The generation of `os.detected.arch` is based on the value of the `os.arch` system property.

The table below assumes that we have a variable `osArch` that is set to `System.getProperty("os.arch").toLowerCase()`;

| Rule for `osArch` | `os.deteced.arch` |
| ----------------- | --------------------------- |
| matches `^(x8664|amd64|ia32e|em64t|x64)$` | `x86_64` |
| matches `^(x8632|x86|i[3-6]86|ia32|x32)$` | `x86_32` |
| matches `^(ia64|itanium64)$` | `itanium_64` |
| matches `^(sparc|sparc32)$` | `sparc_32` |
| matches `^(sparcv9|sparc64)$` | `sparc_64` |
| matches `^(arm|arm32)$` | `arm_32` |
| `aarch64` | `aarch_64` |
| matches `^(ppc|ppc32)$` | `ppc_32` |
| `ppc64` | `ppc_64` |
| `ppc64le` | `ppcle_64` |
| `s390` | `s390_32` |
| `s390x` | `s390_64` |
| default | `unknown` |
