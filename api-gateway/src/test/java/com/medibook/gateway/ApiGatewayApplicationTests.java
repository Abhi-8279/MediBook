package com.medibook.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

    private static DisposableServer authServiceStub;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startStubService() {
        authServiceStub = HttpServer.create()
                .port(0)
                .route(routes -> routes.get("/api/v1/auth/ping", (request, response) -> response
                        .header("X-Downstream-Request-Id", request.requestHeaders().get("X-Request-Id"))
                        .sendString(reactor.core.publisher.Mono.just("auth-ok"))))
                .bindNow();
    }

    @AfterAll
    static void stopStubService() {
        if (authServiceStub != null) {
            authServiceStub.disposeNow();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("medibook.services.auth", () -> "http://localhost:" + authServiceStub.port());
    }

    @Test
    void contextLoads() {
    }

    @Test
    void proxiesAuthRequestsAndPropagatesRequestId() {
        String requestId = "req-123";

        webTestClient.get()
                .uri("/api/v1/auth/ping")
                .header("X-Request-Id", requestId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Request-Id", requestId)
                .expectHeader().valueEquals("X-Downstream-Request-Id", requestId)
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("auth-ok"));
    }

    @Test
    void generatesRequestIdWhenClientDoesNotSendOne() {
        webTestClient.get()
                .uri("/api/v1/auth/ping")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Request-Id")
                .expectHeader().exists("X-Downstream-Request-Id")
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("auth-ok"));
    }
}
