`os-maven-plugin` is a [Maven](http://maven.apache.org/) extension/plugin that generates various useful platform-dependent project properties normalized from `${os.name}` and `${os.arch}`.

`${os.name}` and `${os.arch}` are often subtly different between JVM and operating system versions or they sometimes contain machine-unfriendly characters such as whitespaces.  This plugin tries to remove such fragmentation so that you can determine the current operating system and architecture reliably.

### Generated properties

`os-maven-plugin` detects the information about the current operating system and normalize it into more portable one.

#### Property: `os.detected.name`

`os.detected.name` is set to one of the following values, based on the lower-cased value of the `os.name` Java system property, whose non-alphanumeric characters are stripped out. e.g. `OS_400` -> `os400`

* `aix` - if the value starts with `aix`
* `hpux` - if the value starts with `hpux`
* `os400` - if the value starts with `os400` and its following character is *not* a digit (e.g. `os4000`)
* `linux` - if the value starts with `linux`
* `osx` - if the value starts with `mac` or `osx`
* `freebsd` - if the value starts with `freebsd`
* `openbsd` - if the value starts with `openbsd`
* `netbsd` - if the value starts with `netbsd`
* `sunos` - if the value starts with `solaris` or `sunos`
* `windows` - if the value starts with `windows`
* `zos` - if the value starts with `zos`

#### Property: `os.detected.arch`

`os.detected.arch` is set to one of the following values, based on the lower-cased value of the `os.arch` Java system property, whose non-alphanumeric characters are stripped out. e.g. `x86_64` -> `x8664`

* `x86_32` - if the value is one of: `x8632`, `x86`, `i386`, `i486`, `i586`, `i686`, `ia32`, `x32`
* `x86_64` - if the value is one of: `x8664`, `amd64`, `ia32e`, `em64t`, `x64`
* `itanium_32` - if the value is `ia64n`
* `itanium_64` - if the value is one of: `ia64`, `ia64w`, `itanium64`
* `sparc_32` - if the value is one of: `sparc`, `sparc32`
* `sparc_64` - if the value is one of: `sparcv9`, `sparc64`
* `arm_32` - if the value is one of: `arm`, `arm32`
* `aarch_64` - if the value is `aarch64`
* `mips_32` - if the value is one of: `mips`, `mips32`
* `mips_64` - if the value is `mips64`
* `mipsel_32` - if the value is one of: `mipsel`, `mips32el`
* `mipsel_64` - if the value is `mips64el`
* `ppc_32` - if the value is one of: `ppc`, `ppc32`
* `ppc_64` - if the value is `ppc64`
* `ppcle_32` - if the value is one of: `ppcle`, `ppc32le`
* `ppcle_64` - if the value is `ppc64le`
* `s390_32` - if the value is `s390`
* `s390_64` if the value is `s390x`
* `riscv` if the value is `riscv` or `riscv32`
* `riscv64` if the value is `riscv64`
* `e2k` if the value is `e2k`
* `loongarch_64` if the value is `loongarch64`

Note: The bitness part of this property relies on the bitness of the JVM binary, e.g. You'll get the property that ends with `_32` if you run a 32-bit JVM on a 64-bit OS.

#### Property: `os.detected.bitness`

The bitness from whether `sun.arch.data.model`, `com.ibm.vm.bitmode` or `os.arch`, e.g. `64`, `32`. May report `31` for zOS legacy systems.

Note: This property signifies the bitness of the JVM binary, e.g. You'll get `32` if you run a 32-bit JVM on a 64-bit OS.

#### Property: `os.detected.version.*`

`os.detected.version` and its sub-properties are operation system dependent version number that may indicate the kernel or OS release version. They are generated from the `os.version` Java system property. `os-maven-plugin` find the version number using the following regular expression:

    ((\\d+)\\.(\\d+)).*

* `os.detected.version.major` - the first matching digits
* `os.detected.version.minor` - the second matching digits
* `os.detected.version` - `<os.detected.version.major>.<os.detected.version.minor>` e.g. `3.1`

#### Property: `os.detected.classifier`

You can also use the `${os.detected.classifier}` property, which is a shortcut of `${os.detected.name}-${os.detected.arch}`.

#### Property: `os.detected.release.*` (Linux-only)

See the section 'Customized deployments for specific releases of Linux' below.

### Enabling `os-maven-plugin` on your Maven project

Add the extension to your `pom.xml` like the following:

```xml
<project>
  <build>
    <extensions>
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.7.0</version>
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
        <version>1.7.0</version>
      </extension>
    </extensions>
  </build>
</project>
```
This will result in a `${os.detected.classifier}` of `linux-<arch>-debian` on debian-like systems,
`linux-<arch>-rhel` on rhel systems, and the default of `<os>-<arch>` on everything else.

### Issues with Eclipse m2e or other IDEs

If you are using IntelliJ IDEA, you should not have any problem.

If you are using Eclipse, you need to install an additional Eclipse plugin because [m2e](https://www.eclipse.org/m2e/) does not evaluate the extension specified in a `pom.xml`.  [Download `os-maven-plugin-1.7.0.jar`](http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.7.0/os-maven-plugin-1.7.0.jar) and put it into the `<ECLIPSE_HOME>/dropins` directory.

(As you might have noticed, `os-maven-plugin` is a Maven extension, a Maven plugin, and an Eclipse plugin.)

Alternatively, in some projects it may be possible to add the plugin to the build lifecycle instead of using it as an extension. Remove the plugin from the `<extensions>` section of the POM and place it into the `<build><plugins>` section instead:

```xml
<plugin>
  <groupId>kr.motd.maven</groupId>
  <artifactId>os-maven-plugin</artifactId>
  <version>1.7.0</version>
  <executions>
    <execution>
      <phase>initialize</phase>
      <goals>
        <goal>detect</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

If you are using other IDEs such as NetBeans, you need to set the system properties `os-maven-plugin` sets manually when your IDE is launched.  You usually use JVM's `-D` flags like the following:

    -Dos.detected.name=linux -Dos.detected.arch=x86_64 -Dos.detected.classifier=linux-x86_64

Alternatively, you can hardcode the properties in the local Maven settings. Add the following sections to `settings.xml` (specify property values according to your OS configuration):

```xml
<profiles>
  <profile>
    <id>os-properties</id>
    <properties>
      <os.detected.name>linux</os.detected.name>
      <os.detected.arch>x86_64</os.detected.arch>
      <os.detected.classifier>linux-x86_64</os.detected.classifier>
    </properties>
  </profile>
</profiles>

<activeProfiles>
  <activeProfile>os-properties</activeProfile>
</activeProfiles>
```
