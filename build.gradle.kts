plugins {
    java
    kotlin("jvm") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cn.huohuas001"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    implementation("com.github.Anon8281:UniversalScheduler:0.1.6")
    implementation("redis.clients:jedis:5.0.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-jdk14:1.7.36")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
    compileOnly("org.apache.logging.log4j:log4j-api:2.17.1")
}

val targetJavaVersion = 8
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("RCHuHoBot-${project.version}-Spigot.jar")

    // Relocate dependencies to avoid conflicts
    relocate("kotlin", "cn.huohuas001.huhobot.libs.kotlin")
    relocate("com.github.Anon8281.universalScheduler", "cn.huohuas001.huhobot.libs.universalScheduler")
    relocate("redis.clients.jedis", "cn.huohuas001.huhobot.libs.jedis")
    relocate("org.apache.commons.pool2", "cn.huohuas001.huhobot.libs.pool2")
    relocate("org.json", "cn.huohuas001.huhobot.libs.json")
    relocate("com.google.gson", "cn.huohuas001.huhobot.libs.gson")
    relocate("org.slf4j", "cn.huohuas001.huhobot.libs.slf4j")

    // Exclude unnecessary files
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Disable the default jar task to avoid generating original jar
tasks.jar {
    enabled = false
}

