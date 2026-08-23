import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("fabric-loom") version "1.11.8"
}

group = "io.autoptu"
version = "0.1.0-SNAPSHOT"

val autoPtuJavaSha = "967b16237c6ea93a939bd4acbbe67da979885a60"
val autoPtuJavaDependency = "com.github.Teffa14:AutoPTU-Java:$autoPtuJavaSha"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://jitpack.io")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

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

    // AutoPTU-Java stays read-only. The playable vertical pins one inspected upstream commit and
    // embeds that compiled library in the Fabric mod so battle accuracy/damage/HP remain core-owned.
    implementation(autoPtuJavaDependency)
    include(autoPtuJavaDependency)

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

// The production Fabric mod must carry the adapter-neutral integration classes it invokes.
// They contain no Minecraft/Fabric/Cobblemon types and remain owned by the root integration module.
val rootMain = project(":").extensions.getByType<SourceSetContainer>().named("main")
tasks.jar {
    dependsOn(":classes")
    from(rootMain.map { it.output })
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
