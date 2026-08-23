import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("fabric-loom") version "1.11.8"
}

group = "io.autoptu"
version = "0.1.0-SNAPSHOT"

val autoPtuJavaSha = "aefc058328a9217d634477835a4851d521aaeccb"
val autoPtuJavaWorkDir = layout.buildDirectory.dir("pinned-autoptu-java/$autoPtuJavaSha")
val autoPtuJavaJar = layout.buildDirectory.file("pinned-autoptu-java/$autoPtuJavaSha/autoptu-java-core.jar")

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val preparePinnedAutoPtuJava by tasks.registering(Exec::class) {
    outputs.file(autoPtuJavaJar)
    doFirst {
        delete(autoPtuJavaWorkDir)
    }
    val workDirPath = autoPtuJavaWorkDir.get().asFile.absolutePath
    val jarPath = autoPtuJavaJar.get().asFile.absolutePath
    commandLine(
        "bash", "-c", """
        set -euo pipefail
        mkdir -p '$workDirPath/repo' '$workDirPath/classes'
        git -C '$workDirPath/repo' init -q
        git -C '$workDirPath/repo' remote add origin https://github.com/Teffa14/AutoPTU-Java.git
        git -C '$workDirPath/repo' fetch -q --depth=1 origin '$autoPtuJavaSha'
        git -C '$workDirPath/repo' checkout -q --detach FETCH_HEAD
        find '$workDirPath/repo/src/main/java' -type f -name '*.java' -print0 \
          | sort -z \
          | xargs -0 javac --release 21 -d '$workDirPath/classes'
        jar --create --file '$jarPath' -C '$workDirPath/classes' .
        """.trimIndent()
    )
}

val pinnedAutoPtuJava = files(autoPtuJavaJar)

val productionSmokeMods by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.17.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.11+1.21.1")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")
    modImplementation("com.cobblemon:fabric:1.7.3+1.21.1")

    productionSmokeMods("net.fabricmc.fabric-api:fabric-api:0.116.11+1.21.1")
    productionSmokeMods("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")
    productionSmokeMods("com.cobblemon:fabric:1.7.3+1.21.1")

    implementation(project(":"))

    // AutoPTU-Java stays read-only. The exact inspected commit is fetched as source and compiled
    // with javac. Its classes are copied into this mod jar below before Loom remaps the artifact.
    implementation(pinnedAutoPtuJava)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.named("compileJava") {
    dependsOn(preparePinnedAutoPtuJava)
}
tasks.named("remapJar") {
    dependsOn(preparePinnedAutoPtuJava)
}

// The production Fabric mod carries both the adapter-neutral integration classes and the exact
// compiled AutoPTU-Java pin. io.autoptu.core has no Minecraft mappings, so these classes remain
// unchanged by Loom while the Fabric/Cobblemon-facing classes are remapped normally.
val rootMain = project(":").extensions.getByType<SourceSetContainer>().named("main")
tasks.jar {
    dependsOn(":classes", preparePinnedAutoPtuJava)
    from(rootMain.map { it.output })
    from({ zipTree(autoPtuJavaJar.get().asFile) }) {
        exclude("META-INF/MANIFEST.MF")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.register<Copy>("prepareProductionSmokeMods") {
    dependsOn("remapJar")
    into(layout.buildDirectory.dir("production-smoke/mods"))
    from(productionSmokeMods)
    from(tasks.named("remapJar"))
}

tasks.test {
    useJUnitPlatform()
}
