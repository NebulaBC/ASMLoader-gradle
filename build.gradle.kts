plugins {
    id("java")
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

tasks.test {
    useJUnitPlatform()
}