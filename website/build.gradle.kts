tasks.bootJar {
    enabled = true
}

dependencies {
    implementation (project(":common"))
    implementation ("org.springframework.boot:spring-boot-starter-web")
    implementation ("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation ("org.springframework.boot:spring-boot-starter-webflux")
}