package br.com.immersionhub.generator.desktop.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HttpGroqClientIntegrationTest {
    @Test
    void retrievesRuntimeModelBudgetAndRetries429UsingServerHeaders() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ObjectMapper mapper = new ObjectMapper();
        AtomicInteger completions = new AtomicInteger();
        List<JsonNode> requestBodies = new ArrayList<>();

        server.createContext("/models/test-model", exchange -> {
            byte[] body = """
                {"id":"test-model","context_window":4096,"max_completion_tokens":1024}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("x-ratelimit-remaining-requests", "99");
            exchange.getResponseHeaders().add("x-ratelimit-remaining-tokens", "3500");
            exchange.getResponseHeaders().add("x-ratelimit-reset-tokens", "100ms");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/chat/completions", exchange -> {
            requestBodies.add(mapper.readTree(exchange.getRequestBody()));
            int attempt = completions.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");

            if (attempt == 1) {
                byte[] body = "{\"error\":{\"message\":\"slow down\"}}"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("retry-after", "0.01");
                exchange.sendResponseHeaders(429, body.length);
                exchange.getResponseBody().write(body);
            } else {
                var response = mapper.createObjectNode();
                var message = response.putArray("choices")
                    .addObject()
                    .putObject("message");
                message.put("content", "{\"translations\":[{\"order\":1,\"pt\":\"Olá\"}]}");
                byte[] body = mapper.writeValueAsBytes(response);
                exchange.getResponseHeaders().add("x-ratelimit-remaining-requests", "98");
                exchange.getResponseHeaders().add("x-ratelimit-remaining-tokens", "3000");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });

        server.start();
        try {
            List<Duration> waits = new ArrayList<>();
            List<String> events = new ArrayList<>();
            GroqRateController rateController = new GroqRateController(waits::add);
            GroqConfig config = new GroqConfig(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
                "secret-test-key",
                "test-model",
                2
            );
            HttpGroqClient client = new HttpGroqClient(
                config,
                HttpClient.newHttpClient(),
                mapper,
                rateController,
                new TokenEstimator(),
                events::add
            );

            GroqModelInfo model = client.modelInfo();
            assertEquals(4096, model.contextWindow());
            assertEquals(1024, model.maxCompletionTokens());

            TranslationCue cue = new TranslationCue(
                1, 0, 1000, 0, 1000, "", "Hello", "Hello", "",
                List.of(new br.com.immersionhub.generator.desktop.preparation.TimedText("Hello", 0, 1000))
            );
            GroqCompletion completion = client.translate(List.of(cue));

            assertEquals(2, completions.get());
            assertEquals(1, waits.size());
            assertEquals(Duration.ofMillis(160), waits.getFirst());
            assertTrue(completion.content().contains("\"pt\":\"Olá\""));
            assertTrue(events.stream().anyMatch(value -> value.contains("groq.retry status=429")));
            assertTrue(events.stream().noneMatch(value -> value.contains("secret-test-key")));

            JsonNode request = requestBodies.getLast();
            assertEquals("test-model", request.path("model").asText());
            assertTrue(request.path("response_format").path("json_schema").path("strict").asBoolean());
            assertEquals("json_schema", request.path("response_format").path("type").asText());
        } finally {
            server.stop(0);
        }
    }
}
