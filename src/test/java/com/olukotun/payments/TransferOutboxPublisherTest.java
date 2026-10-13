package com.olukotun.payments;

import com.olukotun.payments.entity.TransferOutbox;
import com.olukotun.payments.respository.TransferOutboxRepository;
import com.olukotun.payments.service.TransferOutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferOutboxPublisherTest {

    private static final String QUEUE_URL =
            "https://sqs.us-east-2.amazonaws.com/123456789012/test-queue";

    private TransferOutboxRepository repository;
    private SqsClient sqsClient;
    private TransferOutboxPublisher publisher;
    private TransferOutbox event;

    @BeforeEach
    void setUp() {
        repository = mock(TransferOutboxRepository.class);
        sqsClient = mock(SqsClient.class);

        publisher = new TransferOutboxPublisher(
                repository, sqsClient, QUEUE_URL
        );

        event = new TransferOutbox(UUID.randomUUID());

        when(repository.findNextUnpublishedForUpdate())
                .thenReturn(Optional.of(event));
    }

    @Test
    void successfulSendMarksEventPublished() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenAnswer(invocation -> {
                    // Publication must not be recorded before sending.
                    assertNull(event.getPublishedAt());

                    return SendMessageResponse.builder()
                            .messageId("test-message")
                            .build();
                });

        assertTrue(publisher.publishNextEvent());
        assertNotNull(event.getPublishedAt());

        var request = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(request.capture());

        assertEquals(QUEUE_URL, request.getValue().queueUrl());

        Map<?, ?> body = JsonMapper.builder().build()
                .readValue(request.getValue().messageBody(), Map.class);

        assertEquals(event.getId().toString(), body.get("eventId"));
        assertEquals(
                event.getTransferId().toString(),
                body.get("transferId")
        );
        assertEquals("TRANSFER_REQUESTED", body.get("eventType"));
    }

    @Test
    void failedSendLeavesEventUnpublished() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(
                        SqsException.builder()
                                .message("Simulated SQS failure")
                                .statusCode(503)
                                .build()
                );

        assertThrows(
                SqsException.class,
                () -> publisher.publishNextEvent()
        );

        assertNull(event.getPublishedAt());
    }
}