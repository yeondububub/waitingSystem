tasks.bootJar {
    enabled = true
}

dependencies {
    implementation (project(":common"))
    implementation ("org.springframework.boot:spring-boot-starter-webflux")
    implementation ("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.100.Final:osx-aarch_64")

    testImplementation ("io.projectreactor:reactor-test")
    testImplementation ("com.github.codemonstur:embedded-redis:1.0.0")
}