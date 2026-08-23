import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("fabric-loom") version "1.11.8"
}

group = "io.autoptu"
version = "0.1.0-SNAPSHOT"

val autoPtuJavaSha = "3ede4a8493738ddc70b2f0eb3959973488f78db9"
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
    commandLine(
        "bash", "-lc",
        "git clone --quiet https://github.com/Teffa14/AutoPTU-Java.git ${autoPtuJavaWorkDir.get().asFile} && " +
                "cd ${autoPtuJavaWorkDir.get().asFile} && git checkout --quiet $autoPtuJavaSha && " +
                "gradle clean jar --no-daemon && cp build/libs/*.jar ${autoPtuJavaJar.get().asFile}"
    )
}

val sourceSets = the<SourceSetContainer>()
sourceSets.named("main") {
    compileClasspath += files(autoPtuJavaJar)
    runtimeClasspath += files(autoPtuJavaJar)
}
sourceSets.named("test") {
    compileClasspath += files(autoPtuJavaJar)
    runtimeClasspath += files(autoPtuJavaJar)
}

tasks.named("compileJava") {
    dependsOn(preparePinnedAutoPtuJava)
}
tasks.named("compileTestJava") {
    dependsOn(preparePinnedAutoPtuJava)
}
tasks.named("runClient") {
    dependsOn(preparePinnedAutoPtuJava)
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.14")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.4+1.21.1")
    modImplementation("com.cobblemon:mod:1.6.1+1.21.1")
    modImplementation("dev.architectury:architectury-fabric:13.0.8")
    modImplementation("software.bernie.geckolib:geckolib-fabric-1.21.1:4.7.6")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.test {
    useJUnitPlatform()
}
