import org.gradle.api.tasks.SourceSetContainer
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

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
    maven("https://api.modrinth.com/maven")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

abstract class CheckoutPinnedAutoPtuJava : DefaultTask() {
    @get:Input
    abstract val revision: Property<String>

    @get:OutputDirectory
    abstract val checkoutDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun checkout() {
        val directory = checkoutDirectory.get().asFile
        directory.mkdirs()
        fun git(vararg arguments: String): String {
            val output = ByteArrayOutputStream()
            execOperations.exec {
                commandLine(listOf("git", "-C", directory.absolutePath) + arguments)
                standardOutput = output
            }
            return output.toString(Charsets.UTF_8.name()).trim()
        }

        git("init", "-q")
        // Never silently compile local modifications or extra Java files in a cached checkout.
        check(git("status", "--porcelain", "--untracked-files=all").isEmpty()) {
            "The generated AutoPTU-Java checkout contains local changes: $directory. " +
                "Move it aside before rebuilding the pinned dependency."
        }
        git("fetch", "-q", "--depth=1", "https://github.com/Teffa14/AutoPTU-Java.git", revision.get())
        git("checkout", "-q", "--detach", revision.get())
        check(git("rev-parse", "HEAD") == revision.get()) {
            "AutoPTU-Java checkout does not match the required revision ${revision.get()}"
        }
    }
}

val checkoutPinnedAutoPtuJava by tasks.registering(CheckoutPinnedAutoPtuJava::class) {
    description = "Fetches the exact read-only AutoPTU-Java revision."
    revision.set(autoPtuJavaSha)
    checkoutDirectory.set(autoPtuJavaWorkDir.map { it.dir("repo") })
}

val compilePinnedAutoPtuJava by tasks.registering(JavaCompile::class) {
    description = "Compiles the pinned core with the Java 21 toolchain on every supported OS."
    dependsOn(checkoutPinnedAutoPtuJava)
    source(checkoutPinnedAutoPtuJava.flatMap { it.checkoutDirectory }.map { it.dir("src/main/java") })
    include("**/*.java")
    classpath = files()
    destinationDirectory.set(autoPtuJavaWorkDir.map { it.dir("classes") })
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    options.release.set(21)
    options.encoding = "UTF-8"
}

val preparePinnedAutoPtuJava by tasks.registering(Jar::class) {
    description = "Builds the pinned core JAR without shell tools or command-line length limits."
    archiveFileName.set(autoPtuJavaJar.map { it.asFile.name })
    destinationDirectory.set(autoPtuJavaWorkDir)
    from(compilePinnedAutoPtuJava.flatMap { it.destinationDirectory })
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val pinnedAutoPtuJava = files(preparePinnedAutoPtuJava.flatMap { it.archiveFile })

val productionSmokeMods by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val practicePackMods by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    practicePackMods("net.fabricmc.fabric-api:fabric-api:0.116.11+1.21.1")
    practicePackMods("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")
    practicePackMods("com.cobblemon:fabric:1.8.0+1.21.1")
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.18.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.11+1.21.1")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")
    modImplementation("com.cobblemon:fabric:1.8.0+1.21.1")

    // Frozen Ouros Overworld substrate for Minecraft 1.21.1.
    // Keep worldgen-only artifacts out of Loom's development classpath: some are
    // distributed as runtime/datapack-oriented jars and Loom may try to parse
    // metadata such as access wideners that is irrelevant to our adapter build.
    // They are resolved only into the production smoke runtime below.
    productionSmokeMods("net.fabricmc.fabric-api:fabric-api:0.116.11+1.21.1")
    productionSmokeMods("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")
    productionSmokeMods("com.cobblemon:fabric:1.8.0+1.21.1")
    productionSmokeMods("maven.modrinth:XaDC71GB:UrEAYvpA") // Lithostitched 1.7.7
    productionSmokeMods("maven.modrinth:8oi3bsk5:eWDLFabb") // Terralith 2.6.2
    productionSmokeMods("maven.modrinth:lWDHr9jE:WDwMnQJ5") // Tectonic 3.0.1

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

tasks.register<Zip>("packagePracticeBattle") {
    description = "Packages the remapped mod and its three runtime mod dependencies for a separate Fabric 1.21.1 profile."
    dependsOn("remapJar")
    archiveFileName.set("AutoPTU-Batallas-1.21.1.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    into("mods") {
        from(practicePackMods)
        from(tasks.named("remapJar"))
    }
    from(rootProject.file("docs/battle-package/LEEME.txt"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
