package org.example.zkteco.adapter.pull;

import org.example.zkteco.core.DeviceEndpoint;
import org.example.zkteco.core.DeviceProtocol;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZkPullAdapterTest {

    @Test
    void reportsReachableTcpTransport() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Void> accept = CompletableFuture.runAsync(() -> {
                try (var ignored = server.accept()) {
                    // Reachability is enough for the starter probe.
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            DeviceEndpoint endpoint = new DeviceEndpoint(UUID.randomUUID(), "test clock",
                    DeviceProtocol.ZK_PULL, "127.0.0.1", server.getLocalPort(), null, null,
                    Map.of("connectTimeoutMillis", "1000"));

            assertTrue(new ZkPullAdapter().probe(endpoint).online());
            accept.join();
        }
    }
}
