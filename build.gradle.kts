plugins {
    id("java")
    id("maven-publish")
}

group = "b100.asmloader"
version = "1.7.7.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // ASM Stuff
    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-tree:9.5")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "b100.asmloader"
            artifactId = "ASMLoader"
            version = "1.7.7.0"

            from(components["java"])
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Premain-Class"] = "b100.asmloader.ASMLoaderAgent"
        attributes["Main-Class"] = "b100.asmloader.gui.ASMLoaderGUI"
        attributes["Can-Retransform-Classes"] = true
    }
}

tasks.test {
    useJUnitPlatform()
}