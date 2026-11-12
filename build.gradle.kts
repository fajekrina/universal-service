plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.fajekrina"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val recordBuilderVersion = "53"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    all {
        // logging via Log4j2 (same as the Maverick services)
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("io.soabase.record-builder:record-builder-core:$recordBuilderVersion")
    annotationProcessor("io.soabase.record-builder:record-builder-processor:$recordBuilderVersion")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.networknt:json-schema-validator:1.4.1")
    // H2 is also a compile dependency: DB triggers + view functions are Java classes (org.h2.api.Trigger)
    implementation("com.h2database:h2")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testCompileOnly("io.soabase.record-builder:record-builder-core:$recordBuilderVersion")
    testAnnotationProcessor("io.soabase.record-builder:record-builder-processor:$recordBuilderVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<JavaCompile> {
        // sources contain non-ASCII (e.g. "—"); Windows javac would otherwise read them as cp1252
        options.encoding = "UTF-8"
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("reports/coverage"))
        }
    }
}
