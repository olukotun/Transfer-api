package com.olukotun.payments;

import com.olukotun.payments.service.TransferProcessor;
import com.olukotun.payments.service.TransferSqsConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferSqsConsumerTest {

    private static final String QUEUE_URL =
            "https://sqs.us-east-2.amazonaws.com/123456789012/test-queue";

    private SqsClient sqsClient;
    private TransferProcessor processor;
    private TransferSqsConsumer consumer;
    private UUID transferId;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        processor = mock(TransferProcessor.class);
        transferId = UUID.randomUUID();

        consumer = new TransferSqsConsumer(
                sqsClient,
                processor,
                JsonMapper.builder().build(),
                QUEUE_URL
        );
    }

    private void receiveBody(String body) {
        Message message = Message.builder()
                .messageId("test-message")
                .receiptHandle("test-receipt")
                .body(body)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(
                        ReceiveMessageResponse.builder()
                                .messages(message)
                                .build()
                );
    }

    private void receiveValidEvent() {
        receiveBody("""
                {
                  "eventId": "%s",
                  "transferId": "%s",
                  "eventType": "TRANSFER_REQUESTED"
                }
                """.formatted(UUID.randomUUID(), transferId));
    }

    @Test
    void successfulProcessingDeletesMessageAfterProcessorReturns() {
        receiveValidEvent();
        when(processor.processTransfer(transferId)).thenReturn(true);

        consumer.consume();

        ArgumentCaptor<DeleteMessageRequest> deletion =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);

        InOrder order = inOrder(processor, sqsClient);
        order.verify(processor).processTransfer(transferId);
        order.verify(sqsClient).deleteMessage(deletion.capture());

        assertEquals(QUEUE_URL, deletion.getValue().queueUrl());
        assertEquals(
                "test-receipt",
                deletion.getValue().receiptHandle()
        );
    }

    @Test
    void alreadyCompletedTransferStillAcknowledgesMessage() {
        receiveValidEvent();
        when(processor.processTransfer(transferId)).thenReturn(false);

        consumer.consume();

        verify(processor).processTransfer(transferId);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void processingFailureDoesNotDeleteMessage() {
        receiveValidEvent();
        when(processor.processTransfer(transferId))
                .thenThrow(new IllegalStateException("Simulated failure"));

        consumer.consume();

        verify(processor).processTransfer(transferId);
        verify(sqsClient, never())
                .deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void malformedMessageIsNotProcessedOrDeleted() {
        receiveBody("not valid JSON");

        consumer.consume();

        verifyNoInteractions(processor);
        verify(sqsClient, never())
                .deleteMessage(any(DeleteMessageRequest.class));
    }
}