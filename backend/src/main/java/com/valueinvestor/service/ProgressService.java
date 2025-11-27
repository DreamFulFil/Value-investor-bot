package com.valueinvestor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for streaming real-time progress updates via Server-Sent Events (SSE)
 */
@Service
public class ProgressService {

    private static final Logger logger = LoggerFactory.getLogger(ProgressService.class);
    
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    
    public enum ProgressType {
        DEPOSIT("deposit"),
        SCREENING("screening"),
        FETCHING_PRICES("fetching_prices"),
        BUYING("buying"),
        GENERATING_INSIGHTS("generating_insights"),
        COMPLETE("complete"),
        ERROR("error");
        
        private final String value;
        
        ProgressType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Register a new SSE emitter for a client
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(60000L); // 60 second timeout
        
        emitter.onCompletion(() -> {
            logger.debug("SSE emitter completed");
            emitters.remove(emitter);
        });
        
        emitter.onTimeout(() -> {
            logger.debug("SSE emitter timed out");
            emitters.remove(emitter);
        });
        
        emitter.onError((e) -> {
            logger.debug("SSE emitter error: {}", e.getMessage());
            emitters.remove(emitter);
        });
        
        emitters.add(emitter);
        logger.info("New SSE emitter registered, total: {}", emitters.size());
        
        return emitter;
    }
    
    /**
     * Send progress update to all connected clients
     */
    public void sendProgress(ProgressType type, String message, int percentage) {
        sendProgress(type, message, percentage, null);
    }
    
    /**
     * Send progress update with additional data
     */
    public void sendProgress(ProgressType type, String message, int percentage, Map<String, Object> data) {
        Map<String, Object> event = new ConcurrentHashMap<>();
        event.put("type", type.getValue());
        event.put("message", message);
        event.put("percentage", percentage);
        event.put("timestamp", System.currentTimeMillis());
        
        if (data != null) {
            event.putAll(data);
        }
        
        logger.info("Progress: [{}] {} ({}%)", type.getValue(), message, percentage);
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(event));
            } catch (IOException e) {
                logger.debug("Failed to send SSE event: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
    
    /**
     * Send error event
     */
    public void sendError(String errorMessage) {
        sendProgress(ProgressType.ERROR, errorMessage, 0);
    }
    
    /**
     * Send completion event and close all emitters
     */
    public void sendComplete(String message) {
        sendProgress(ProgressType.COMPLETE, message, 100);
        
        // Complete all emitters
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.debug("Error completing emitter: {}", e.getMessage());
            }
        }
        emitters.clear();
    }
    
    /**
     * Get number of connected clients
     */
    public int getConnectedClients() {
        return emitters.size();
    }
}
