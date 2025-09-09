package com.example.sqslistener.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class SqsMessageListener {

    @SqsListener(value = "${sqs.queue-name}")
    public void processMessage(@Payload String messageBody,
                              @Header("MessageId") String messageId,
                              @Header("ApproximateReceiveCount") String receiveCount,
                              Acknowledgement acknowledgement,
                              Message<String> message) {
        
        try {
            log.info("Received message with ID: {} (Attempt: {})", messageId, receiveCount);
            log.info("Message body: {}", messageBody);
            
            // Process your message here
            processBusinessLogic(messageBody);
            
            // Manual acknowledgment - message will be deleted from queue
            acknowledgement.acknowledge();
            log.info("Message {} acknowledged successfully", messageId);
            
        } catch (Exception e) {
            log.error("Error processing message {}: {}", messageId, e.getMessage(), e);
            
            // You can choose to:
            // 1. Not acknowledge (message will become visible again after visibility timeout)
            // 2. Or acknowledge anyway if you want to remove it despite the error
            // 3. Or implement retry logic with acknowledgement.acknowledgeAsync()
            
            // For this example, we're not acknowledging on error
            // The message will be retried after visibility timeout expires
        }
    }

    /**
     * Alternative listener with async acknowledgment
     */
    @SqsListener(value = "${sqs.queue-name-async}", id = "async-listener")
    public void processMessageAsync(@Payload String messageBody,
                                   @Header("MessageId") String messageId,
                                   Acknowledgement acknowledgement) {
        
        log.info("Processing async message: {}", messageId);
        
        // Async acknowledgment example
        CompletableFuture<Void> future = acknowledgement.acknowledgeAsync();
        
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to acknowledge message {}: {}", messageId, throwable.getMessage());
            } else {
                log.info("Message {} acknowledged asynchronously", messageId);
            }
        });
    }

    private void processBusinessLogic(String messageBody) {
        // Simulate processing
        // Add your business logic here
        log.info("Processing business logic for message: {}", messageBody);
        
        // Simulate some work
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
    }
}