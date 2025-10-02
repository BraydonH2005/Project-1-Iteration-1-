import org.gradle.internal.os.OperatingSystem
import java.io.File
import org.gradle.api.artifacts.ExternalModuleDependency

plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    modularity.inferModulePath.set(false)
}

repositories { mavenCentral() }


val jfx = "22.0.2"


/* JavaFX unclassified here so IntelliJ resolves javafx.* in the editor */
dependencies {
    compileOnly("org.openjfx:javafx-controls:$jfx")
    compileOnly("org.openjfx:javafx-fxml:$jfx")

    implementation("com.jayway.jsonpath:json-path:2.9.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.13")
}


javafx {
    version = jfx
    modules = listOf("javafx.controls", "javafx.fxml")
}


val os = OperatingSystem.current()
val arch = System.getProperty("os.arch").lowercase()
val fxClassifier = when {
    os.isWindows -> "win"
    os.isMacOsX && arch.contains("aarch64") -> "mac-aarch64"
    os.isMacOsX -> "mac"
    os.isLinux && arch.contains("aarch64") -> "linux-aarch64"
    else -> "linux"
}

val javafxRuntime by configurations.creating

dependencies {

    fun dep(noClassifierNotation: String) =
        dependencies.create("$noClassifierNotation:$fxClassifier").also {
            (it as ExternalModuleDependency).isTransitive = false
        }

    add(javafxRuntime.name, dep("org.openjfx:javafx-base:$jfx"))
    add(javafxRuntime.name, dep("org.openjfx:javafx-graphics:$jfx"))
    add(javafxRuntime.name, dep("org.openjfx:javafx-controls:$jfx"))
    add(javafxRuntime.name, dep("org.openjfx:javafx-fxml:$jfx"))
}


application {
    // Iteration 1 (CLI)
    mainClass.set("edu.bsu.cs.revisionreporter.app.RevisionReporterMain")
}


tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    setJvmArgs(emptyList())
    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
    )
}

/* GUI (Iteration 2): JavaFX on module-path, NOT on classpath */
tasks.register<JavaExec>("runGui") {
    group = "application"
    description = "Run the JavaFX GUI"
    mainClass.set("edu.bsu.cs.revisionreporter.gui.RevisionReporterGuiApp")

    // Remove JavaFX jars from the classpath to avoid duplicate presence
    val noFxClasspath = sourceSets.main.get().runtimeClasspath
        .filter { !it.name.startsWith("javafx-") }
    classpath = noFxClasspath

    doFirst {
        val fxPath = javafxRuntime.resolve()
            .joinToString(File.pathSeparator) { it.absolutePath }
        jvmArgs(
            "--module-path", fxPath,
            "--add-modules", "javafx.controls,javafx.fxml"
        )
    }

    javaLauncher.set(
        javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
    )
}


tasks.register("verifyJavaFx") {
    group = "verification"
    doLast {
        val files = javafxRuntime.resolve()
        if (files.isEmpty()) error("JavaFX runtime jars not resolved. Check network/Maven Central.")
        println("JavaFX runtime OK:")
        files.forEach { println(" - ${it.name}") }
    }
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("FAILED", "SKIPPED")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-unchecked"))
}

