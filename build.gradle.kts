import org.gradle.kotlin.dsl.annotationProcessor
import org.gradle.kotlin.dsl.implementation

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "pt-final-251022-be"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    // Redis
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // spring doc
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    // WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    annotationProcessor ("com.querydsl:querydsl-apt:5.0.0:jakarta")
    annotationProcessor ("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor ("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // Testing (테스트)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val querydslSrcDir = "src/main/generated"

tasks.clean {
    delete(file(querydslSrcDir))
}

tasks.withType<JavaCompile>().configureEach {
    options.generatedSourceOutputDirectory.set(file(querydslSrcDir))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ✅ JaCoCo 버전을 Java 25를 지원하는 최신 버전으로 업데이트
jacoco {
    toolVersion = "0.8.13" // Java 25 지원
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // ✅ 테스트 끝나면 리포트 생성
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)   // CI/분석툴 연동용
        html.required.set(true)  // 사람이 보는 리포트
        csv.required.set(false)
    }
    // (선택) 생성/부트/DTO 등 제외
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/Q*",                   // ✅ QueryDSL Q-classes
                    "**/*Application*",        // 부트스트랩
                    "**/*Config*",             // 설정(원하면)
                    "**/*Dto*", "**/*Request*", "**/*Response*"
                )
            }
        })
    )
}

// (선택) 임계치 검증 — 기준 미만이면 빌드 실패
tasks.register<JacocoCoverageVerification>("jacocoVerify") {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit { minimum = "0.60".toBigDecimal() } // 60%
        }
    }
}