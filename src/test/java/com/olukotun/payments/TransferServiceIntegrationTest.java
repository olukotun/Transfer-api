package com.olukotun.payments;

import com.olukotun.payments.dto.TransferRequest;
import com.olukotun.payments.dto.TransferResponse;
import com.olukotun.payments.exception.IdempotencyConflictException;
import com.olukotun.payments.respository.TransferRepository;
import com.olukotun.payments.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import com.olukotun.payments.service.TransferProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "payments.worker.enabled=false"
)public class TransferServiceIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void configureTestTokens() {
        transferRepository.deleteAll();
        when(jwtDecoder.decode("test-write-token"))
                .thenReturn(testJwt(
                        "test-write-token",
                        List.of("transfers-read", "transfers-write")
                ));

        when(jwtDecoder.decode("test-read-token"))
                .thenReturn(testJwt(
                        "test-read-token",
                        List.of("transfers-read")
                ));
    }

    private Jwt testJwt(String token, List<String> roles) {
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("test-client")
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }
    @LocalServerPort
    private int port;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("payments_test")
                    .withUsername("test_user")
                    .withPassword("test_password");
    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransferProcessor transferProcessor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void sameKeyAndDetailsReturnSameTransfer() {
        String key = UUID.randomUUID().toString();

        TransferRequest request = new TransferRequest(
                "ACC1001",
                "ACC2002",
                new BigDecimal("125.50"),
                "USD"
        );

        try {
            TransferResponse first =
                    transferService.createTransfer(key, request);

            TransferResponse second =
                    transferService.createTransfer(key, request);

            assertEquals(first.transferId(), second.transferId());

            var stored = transferRepository.findByIdempotencyKey(key);

            assertTrue(stored.isPresent());
            assertEquals(first.transferId(), stored.orElseThrow().getId());
        } finally {
            transferRepository.findByIdempotencyKey(key)
                    .ifPresent(transferRepository::delete);
        }
    }
    @Test
    void sameKeyWithDifferentAmountIsRejected() {
        String key = UUID.randomUUID().toString();

        TransferRequest original = new TransferRequest(
                "ACC1001",
                "ACC2002",
                new BigDecimal("125.50"),
                "USD"
        );

        TransferRequest changed = new TransferRequest(
                "ACC1001",
                "ACC2002",
                new BigDecimal("150.00"),
                "USD"
        );

        try {
            TransferResponse first =
                    transferService.createTransfer(key, original);

            assertThrows(IdempotencyConflictException.class, () -> transferService.createTransfer(key, changed));

            var stored = transferRepository.findByIdempotencyKey(key).orElseThrow();

            assertEquals(first.transferId(), stored.getId());
            assertEquals(0, new BigDecimal("125.50").compareTo(stored.getAmount())
            );
        } finally {
            transferRepository.findByIdempotencyKey(key).ifPresent(transferRepository::delete);
        }
    }
    @Test
    void sameAmountWithDifferentScaleReturnsSameTransfer() {
        String key = UUID.randomUUID().toString();

        TransferRequest original = new TransferRequest(
                "ACC1001",
                "ACC2002",
                new BigDecimal("125.50"),
                "USD"
        );

        TransferRequest retry = new TransferRequest(
                "ACC1001",
                "ACC2002",
                new BigDecimal("125.5"),
                "USD"
        );

        try {
            TransferResponse first =
                    transferService.createTransfer(key, original);

            TransferResponse second =
                    transferService.createTransfer(key, retry);

            assertEquals(first.transferId(), second.transferId());
        } finally {
            transferRepository.findByIdempotencyKey(key)
                    .ifPresent(transferRepository::delete);
        }
    }
    @Test
    void negativeAmountReturns400WithoutSavingTransfer() throws Exception {
        long countBefore = transferRepository.count();

        String body = """
            {
              "sourceAccount": "ACC1001",
              "destinationAccount": "ACC2002",
              "amount": -10.00,
              "currency": "USD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port + "/api/transfers"
                ))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("Authorization", "Bearer test-write-token")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(
                response.body().contains("Amount must be at least 0.01"),
                response.body()
        );
        assertEquals(countBefore, transferRepository.count());
    }
    @Test
    void requestWithoutTokenReturns401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port
                                + "/api/transfers/"
                                + UUID.randomUUID()
                ))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void readOnlyTokenCannotCreateTransfer() throws Exception {
        long countBefore = transferRepository.count();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port + "/api/transfers"
                ))
                .header("Authorization", "Bearer test-read-token")
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {
                      "sourceAccount": "ACC1001",
                      "destinationAccount": "ACC2002",
                      "amount": 25.00,
                      "currency": "USD"
                    }
                    """))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertEquals(countBefore, transferRepository.count());
    }
    private TransferResponse createPendingTransfer() {
        return transferService.createTransfer(
                UUID.randomUUID().toString(),
                new TransferRequest(
                        "ACC1001",
                        "ACC2002",
                        new BigDecimal("200.00"),
                        "USD"
                )
        );
    }

    @Test
    void pendingTransferIsCompleted() {
        TransferResponse created = createPendingTransfer();

        assertEquals("PENDING", created.status());

        assertTrue(transferProcessor.processNextPendingTransfer());

        var stored = transferRepository
                .findById(created.transferId())
                .orElseThrow();

        assertEquals("COMPLETED", stored.getStatus());
    }

    @Test
    void completedTransferIsNotProcessedAgain() {
        TransferResponse created = createPendingTransfer();

        assertTrue(transferProcessor.processNextPendingTransfer());

        // No pending work remains.
        assertFalse(transferProcessor.processNextPendingTransfer());

        var stored = transferRepository
                .findById(created.transferId())
                .orElseThrow();

        assertEquals("COMPLETED", stored.getStatus());
        assertEquals(1L, transferRepository.count());
    }

    @Test
    void anotherWorkerSkipsLockedTransfer() throws Exception {
        TransferResponse created = createPendingTransfer();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        var executor = Executors.newSingleThreadExecutor();

        try {
            transaction.executeWithoutResult(tx -> {
                // Hold the pending row's lock in this transaction.
                var locked = transferRepository
                        .findNextPendingForUpdate()
                        .orElseThrow();

                assertEquals(created.transferId(), locked.getId());

                // A different thread uses a separate transaction.
                var competingWorker = executor.submit(
                        () -> transferProcessor.processNextPendingTransfer()
                );

                try {
                    assertFalse(
                            competingWorker.get(5, TimeUnit.SECONDS),
                            "The competing worker must skip the locked transfer"
                    );
                } catch (Exception exception) {
                    throw new AssertionError(
                            "The competing worker did not finish successfully",
                            exception
                    );
                }

                assertEquals("PENDING", locked.getStatus());
            });

            // The first transaction has ended and released its lock.
            assertTrue(transferProcessor.processNextPendingTransfer());

            var stored = transferRepository
                    .findById(created.transferId())
                    .orElseThrow();

            assertEquals("COMPLETED", stored.getStatus());
        } finally {
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(5, TimeUnit.SECONDS),
                    "Worker thread did not stop"
            );
        }
    }

    @Test
    void rolledBackProcessingLeavesTransferPendingForRetry() {
        TransferResponse created = createPendingTransfer();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () ->
                transaction.executeWithoutResult(tx -> {
                    assertTrue(
                            transferProcessor.processNextPendingTransfer()
                    );

                    // Force failure after the processor changes the status.
                    throw new IllegalStateException("Simulated failure");
                })
        );

        var afterFailure = transferRepository
                .findById(created.transferId())
                .orElseThrow();

        assertEquals("PENDING", afterFailure.getStatus());

        // A fresh transaction can process the transfer successfully.
        assertTrue(transferProcessor.processNextPendingTransfer());

        var afterRetry = transferRepository
                .findById(created.transferId())
                .orElseThrow();

        assertEquals("COMPLETED", afterRetry.getStatus());
    }
}
