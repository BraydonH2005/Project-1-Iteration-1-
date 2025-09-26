
plugins {
    application
    java
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

repositories { mavenCentral() }

dependencies {
    implementation("com.jayway.jsonpath:json-path:2.9.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.13")
}

application {
    mainClass.set("edu.bsu.cs.revisionreporter.app.RevisionReporterMain")
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
