plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.pavelfatin"
version = "2.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21.0.2"
    modules("javafx.controls", "javafx.fxml", "javafx.web")
}

repositories {
    mavenCentral()
}

dependencies {
    // Serial communication (replaces RXTX)
    implementation("com.fazecast:jSerialComm:2.10.4")

    // XML binding (replaces javax.xml.bind removed in JDK 11+)
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.5")

    // CSV (replaces javacsv-2.0)
    implementation("org.apache.commons:commons-csv:1.11.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.pavelfatin.sleeparchiver.SleepArchiver")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.pavelfatin.sleeparchiver.SleepArchiver",
            "Implementation-Title" to "SleepArchiver",
            "Implementation-Version" to project.version
        )
    }
}

tasks.register<Copy>("prepareJpackageInput") {
    dependsOn("jar")
    from(tasks.jar.get().archiveFile)
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("jpackage-input"))
}

tasks.register<Exec>("jpackage") {
    dependsOn("prepareJpackageInput")
    val inputDir = layout.buildDirectory.dir("jpackage-input").get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage-output").get().asFile
    val iconsDir = file("src/main/resources/icons")

    val os = System.getProperty("os.name").lowercase()
    val installerType = when {
        os.contains("mac") -> "dmg"
        os.contains("win") -> "msi"
        else -> "deb"
    }
    val iconFile = when {
        os.contains("mac") -> File(iconsDir, "icon.icns")
        os.contains("win") -> File(iconsDir, "icon.ico")
        else -> File(iconsDir, "icon.png")
    }

    val args = mutableListOf(
        "jpackage",
        "--input", inputDir.absolutePath,
        "--dest", outputDir.absolutePath,
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "com.pavelfatin.sleeparchiver.Launcher",
        "--name", "SleepArchiver",
        "--app-version", project.version.toString(),
        "--type", installerType,
        "--vendor", "Evgen Tamarovsky"
    )
    if (iconFile.exists()) {
        args.addAll(listOf("--icon", iconFile.absolutePath))
    }

    commandLine(args)
}
