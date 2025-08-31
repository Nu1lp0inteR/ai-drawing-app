// 文件路径: src/main/java/com/aidrawing/backend/service/ComfyUIWebSocketClient.java

package com.aidrawing.backend.client;

import com.aidrawing.backend.dto.ComfyUIWSMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ComfyUIWebSocketClient extends WebSocketClient {

    private final String promptId;
    private final CompletableFuture<Void> completionSignal;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor for the WebSocket client.
     * * WebSocket 客户端的构造函数。
     *
     * @param serverUri       The WebSocket server URI.
     * @param promptId        The specific prompt ID this client is listening for.
     * @param completionSignal A future that will be completed when the task is done.
     */
    public ComfyUIWebSocketClient(URI serverUri, String promptId, CompletableFuture<Void> completionSignal) {
        super(serverUri);
        this.promptId = promptId;
        this.completionSignal = completionSignal;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WebSocket] Connected to ComfyUI for prompt_id: " + promptId);
    }

    @Override
    public void onMessage(String message) {
        // System.out.println("[WebSocket] Received message: " + message); // Uncomment for verbose logging
        try {
            ComfyUIWSMessage msg = objectMapper.readValue(message, ComfyUIWSMessage.class);
            if ("status".equals(msg.getType()) && msg.getData().getStatus() != null) {
                if (msg.getData().getStatus().getExecInfo().getQueueRemaining() == 0) {
                    System.out.println("[WebSocket] Queue is empty. Task is considered complete. Completing signal.");
                    completionSignal.complete(null);
                }
            }
        } catch (Exception e) {
             System.err.println("[WebSocket] Failed to parse message: " + message + " | Error: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WebSocket] Connection closed. Code: " + code + ", Reason: " + reason);
        if (!completionSignal.isDone()) {
             // If connection closes unexpectedly, complete exceptionally.
            completionSignal.completeExceptionally(new RuntimeException("WebSocket closed before task completion. Reason: " + reason));
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[WebSocket] An error occurred: " + ex.getMessage());
        completionSignal.completeExceptionally(ex);
    }
}
