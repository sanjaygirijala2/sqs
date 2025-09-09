package com.example.sqslistener.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDto {
    private String id;
    private String type;
    private String content;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    
    // Add your custom fields based on your message structure
}