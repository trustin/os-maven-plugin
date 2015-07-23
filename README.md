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
        <version>1.2.3.Final</version>
      </extension>
    </extensions>
  </build>
</project>

```

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

### Custom classifiers for OS variants

If you need to customize your deployment based on a variant of an OS, you can check the existence of `${os.detected.like.<variant>}`.

The snippet below deploys an artifact with a different classifier if on an OS that is like `debian`.

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-antrun-plugin</artifactId>
        <executions>
          <execution>
            <phase>initialize</phase>
            <configuration>
              <exportAntProperties>true</exportAntProperties>
              <target>
                <condition property="deploy.classifier"
                           value="${os.detected.classifier}-debian"
                           else="${os.detected.classifier}">
                  <isset property="os.detected.like.debian"/>
                </condition>
              </target>
            </configuration>
            <goals>
              <goal>run</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
        <plugin>
          <artifactId>maven-jar-plugin</artifactId>
          <configuration>
            <classifier>${deploy.classifier}</classifier>
          </configuration>
        </plugin>
    </plugins>
  </build>
</project>
```

For all platforms, there will be at least one property for `os.detected.like.${os.detected.name}`. On Linux,
additional properties will be populated from the `ID` and `ID_LIKE` entries in
[`/etc/os-release` or `/usr/lib/os-release`](http://www.freedesktop.org/software/systemd/man/os-release.html). If unavailable
and the file `/etc/redhat-release` exists, `rhel` and `fedora` are assumed.

### Issues with Eclipse m2e or other IDEs

If you are using IntelliJ IDEA, you should not have any problem.

If you are using Eclipse, you need to install an additional Eclipse plugin because [m2e](https://www.eclipse.org/m2e/) does not evaluate the extension specified in a `pom.xml`.  [Download `os-maven-plugin-1.2.3.Final.jar`](http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.2.3.Final/os-maven-plugin-1.2.3.Final.jar) and put it into the `<ECLIPSE_HOME>/plugins` directory.

(As you might have noticed, `os-maven-plugin` is a Maven extension, a Maven plugin, and an Eclipse plugin.)

If you are using other IDEs such as NetBeans, you need to set the system properties `os-maven-plugin` sets manually when your IDE is launched.  You usually use JVM's `-D` flags like the following:

    -Dos.detected.name=linux -Dos.detected.arch=x86_64 -Dos.detected.classifier=linux-x86_64




