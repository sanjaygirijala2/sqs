package com.example.sqslistener.listener;

import com.example.sqslistener.model.MessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StructuredMessageListener {
    
    private final ObjectMapper objectMapper;

    /**
     * Listener for JSON messages with automatic deserialization
     */
    @SqsListener(value = "${sqs.queue-name}", id = "structured-listener")
    public void processStructuredMessage(@Payload MessageDto message,
                                        @Header("MessageId") String messageId,
                                        Acknowledgement acknowledgement) {
        try {
            log.info("Received structured message: ID={}, Type={}", message.getId(), message.getType());
            
            // Process based on message type
            switch (message.getType()) {
                case "ORDER":
                    processOrder(message);
                    break;
                case "NOTIFICATION":
                    processNotification(message);
                    break;
                default:
                    log.warn("Unknown message type: {}", message.getType());
            }
            
            // Acknowledge after successful processing
            acknowledgement.acknowledge();
            log.info("Message {} processed and acknowledged", messageId);
            
        } catch (Exception e) {
            log.error("Failed to process structured message {}: {}", messageId, e.getMessage(), e);
            // Message will not be acknowledged and will be retried
        }
    }

    /**
     * Listener for raw JSON that needs manual parsing
     */
    @SqsListener(value = "${sqs.queue-name-raw}", id = "raw-json-listener")
    public void processRawJsonMessage(@Payload String jsonMessage,
                                     @Header("MessageId") String messageId,
                                     Acknowledgement acknowledgement) {
        try {
            // Manual JSON parsing
            MessageDto message = objectMapper.readValue(jsonMessage, MessageDto.class);
            log.info("Parsed message: {}", message);
            
            // Process the message
            processBusinessLogic(message);
            
            // Manual acknowledgment
            acknowledgement.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to parse or process message {}: {}", messageId, e.getMessage(), e);
            // Not acknowledging - message will be retried
        }
    }

    private void processOrder(MessageDto message) {
        log.info("Processing order: {}", message.getContent());
        // Add order processing logic
    }

    private void processNotification(MessageDto message) {
        log.info("Processing notification: {}", message.getContent());
        // Add notification processing logic
    }

    private void processBusinessLogic(MessageDto message) {
        log.info("Processing message with custom logic: {}", message);
        // Add your business logic here
    }
}