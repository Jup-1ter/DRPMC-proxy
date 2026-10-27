plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("kapt") version "2.1.0"
}

group = "ru.darkrpmc"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public")
}

val exposedVersion: String by project
dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.mysql:mysql-connector-j:9.1.0")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks.register<Jar>("fatJar") {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("org/jetbrains/annotations/**")
        exclude("org/intellij/**")
        exclude("org/slf4j/**")
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
    with(tasks.jar.get() as CopySpec)
    finalizedBy("copyJar")
}

tasks.register<Copy>("copyJar") {
    val jarTask = tasks.named<Jar>("jar")
    from(jarTask)
    into("..\\..\\..\\servers\\ProxyServer\\plugins")
}

tasks.jar { finalizedBy("fatJar") }