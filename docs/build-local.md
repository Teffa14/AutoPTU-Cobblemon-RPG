# Local build

Install Git, a Java 21 JDK, and Gradle 8.14.3. Point `JAVA_HOME` at the JDK and add
Git and Gradle to `PATH`. Use Java 21 to launch Gradle as well as compile the mod.
The project does not currently ship a Gradle wrapper.

From the repository root:

```text
gradle :test :fabric-adapter:test :fabric-adapter:remapJar
```

In PowerShell, when tools are installed outside `PATH`:

```powershell
$env:JAVA_HOME = 'C:\tools\jdk-21'
& 'C:\tools\gradle-8.14.3\bin\gradle.bat' :test :fabric-adapter:test :fabric-adapter:remapJar
```

The production mod JAR is written to `fabric-adapter/build/libs/`. The first build
needs network access for Maven dependencies and the pinned AutoPTU-Java source.
Subsequent unchanged builds reuse Gradle's outputs.

## Pinned AutoPTU-Java dependency

The adapter fetches the exact `autoPtuJavaSha` declared in
`fabric-adapter/build.gradle.kts` into its own build directory. It does not use or
modify a separate working copy of AutoPTU-Java.

```text
gradle :fabric-adapter:preparePinnedAutoPtuJava
```

This task runs Git with separate arguments, compiles all core sources through
Gradle's Java 21 `JavaCompile` task, and packages a reproducible JAR. Windows paths
containing spaces and large source sets are supported; Bash, `find`, `xargs`, and
manual JAR preparation are not required. Adapter compile, test, and packaging
tasks pick up the resulting JAR automatically.

The generated source checkout is read-only build input. If it has local changes
or additional files, preparation fails with its path instead of compiling an
unverified core. Move that generated checkout aside and rerun preparation to
fetch a clean copy. To force a rebuild of the pin without clearing unrelated
build output, use:

```text
gradle :fabric-adapter:preparePinnedAutoPtuJava --rerun-tasks
```
