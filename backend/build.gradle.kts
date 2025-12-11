import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Generate

val jooqVersion = "3.20.5"
ext["jooq.version"] = jooqVersion

plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.monosoul.jooq-docker") version "8.0.6"
    jacoco
}
val springAiVersion by extra("1.1.0")

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
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.ai:spring-ai-starter-model-openai") // Chat
    implementation("org.springframework.ai:spring-ai-openai")               // Embedding
    implementation("org.springframework.ai:spring-ai-rag")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-mariadb")
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.redisson:redisson-spring-boot-starter:3.41.0")

    // JOOQ
    implementation("org.jooq:jooq:${jooqVersion}")
    jooqCodegen("org.jooq:jooq-meta:${jooqVersion}")
    jooqCodegen("org.jooq:jooq-meta-extensions:${jooqVersion}")
    jooqCodegen("org.jooq:jooq-codegen:${jooqVersion}")
    compileOnly("org.jooq:jooq-codegen:${jooqVersion}")
    compileOnly("org.jooq:jooq-meta:${jooqVersion}")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    jooqCodegen("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    jooqCodegen("org.flywaydb:flyway-mysql:10.20.1")
    // flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
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
    annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")
    // 이미지 리사이징
    implementation("net.coobird:thumbnailator:0.4.20")

    // Testing (테스트)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:mariadb")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.25.10")

    // STMP
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Quartz
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // OAUTH2
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // 모니터링
    implementation("io.micrometer:micrometer-registry-prometheus")
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

val dbUser: String? = System.getenv("SPRING__DATASOURCE__USERNAME")
val dbPasswd: String? = System.getenv("SPRING__DATASOURCE__PASSWORD")
val dbHost: String = System.getenv("DB_HOST") ?: "localhost"

jooq {
    version = jooqVersion

    if (dbUser != null && dbPasswd != null) {
        withoutContainer {
            db {
                username = dbUser
                password = dbPasswd
                name = "chwimeet"
                host = dbHost
                port = 3306
                jdbc {
                    schema = "jdbc:mariadb"
                    driverClassName = "org.mariadb.jdbc.Driver"
                }
            }
        }
    }
}

tasks {
    generateJooqClasses {
        onlyIf { dbUser != null && dbPasswd != null }
        schemas = listOf("chwimeet")
        basePackageName = "com.back.jooq"
        outputDirectory = project.layout.projectDirectory.dir("src/generated")
        includeFlywayTable = false

        usingJavaConfig {
            generate = Generate()
                .withJavaTimeTypes(true)
                .withDeprecated(false)
                .withDaos(true)
                .withFluentSetters(true)
                .withRecords(true)

            database.withForcedTypes(
                ForcedType()
                    .withUserType("java.lang.Long")
                    .withTypes("int unsigned"),
                ForcedType()
                    .withUserType("java.lang.Integer")
                    .withTypes("tinyint unsigned"),
                ForcedType()
                    .withUserType("java.lang.Integer")
                    .withTypes("smallint unsigned")
            )
        }
    }
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
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
    }
}