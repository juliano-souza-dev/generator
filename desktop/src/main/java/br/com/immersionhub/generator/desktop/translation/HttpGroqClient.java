package br.com.immersionhub.generator.desktop.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HttpGroqClient implements GroqClient {
    private final GroqConfig config;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final GroqRateController rateController;
    private final TokenEstimator estimator;

    private volatile GroqModelInfo modelInfo;

    public HttpGroqClient(GroqConfig config) {
        this(
            config,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build(),
            new ObjectMapper(),
            new GroqRateController(),
            new TokenEstimator()
        );
    }

    HttpGroqClient(
        GroqConfig config,
        HttpClient http,
        ObjectMapper mapper,
        GroqRateController rateController,
        TokenEstimator estimator
    ) {
        this.config = config;
        this.http = http;
        this.mapper = mapper;
        this.rateController = rateController;
        this.estimator = estimator;
    }

    @Override
    public GroqModelInfo modelInfo() throws Exception {
        GroqModelInfo cached = modelInfo;
        if (cached != null) return cached;

        URI uri = config.baseUri().resolve("models/" + config.model());
        HttpRequest request = request(uri).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        GroqRateSnapshot rate = rateOf(response);
        rateController.observe(rate);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw apiError(response, rate);
        }

        JsonNode root = mapper.readTree(response.body());
        long contextWindow = root.path("context_window").asLong(0L);
        long maxCompletion = root.path("max_completion_tokens").asLong(0L);
        GroqModelInfo resolved = new GroqModelInfo(
            root.path("id").asText(config.model()),
            contextWindow,
            maxCompletion
        );
        modelInfo = resolved;
        return resolved;
    }

    @Override
    public GroqCompletion translate(List<TranslationCue> cues) throws Exception {
        if (cues == null || cues.isEmpty()) {
            throw new IllegalArgumentException("Chunk de tradução vazio.");
        }

        GroqModelInfo model = modelInfo();
        long estimatedPrompt = estimator.estimateRequest(cues);
        long estimatedCompletion = estimator.estimateCompletion(cues);
        long requestedCompletion = Math.min(
            model.maxCompletionTokens(),
            Math.max(512L, estimatedCompletion + 256L)
        );

        rateController.beforeRequest(estimatedPrompt + requestedCompletion);

        String body = requestBody(cues, requestedCompletion);
        int attempt = 0;

        while (true) {
            HttpRequest request = request(config.baseUri().resolve("chat/completions"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            GroqRateSnapshot rate = rateOf(response);
            rateController.observe(rate);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = mapper.readTree(response.body());
                String content = root.path("choices").path(0).path("message").path("content").asText("");
                if (content.isBlank()) {
                    throw new GroqApiException(
                        response.statusCode(),
                        "A Groq retornou uma resposta vazia.",
                        rate
                    );
                }
                return new GroqCompletion(content, rate);
            }

            GroqApiException error = apiError(response, rate);
            if (!error.retryable() || attempt >= config.maxRetries()) {
                throw error;
            }

            rateController.waitAfterFailure(error, attempt);
            attempt++;
        }
    }

    private String requestBody(List<TranslationCue> cues, long maxCompletionTokens) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", config.model());
        root.put("temperature", 0);
        root.put("max_completion_tokens", maxCompletionTokens);

        ArrayNode messages = root.putArray("messages");
        messages.addObject()
            .put("role", "system")
            .put(
                "content",
                "Você traduz falas de cenas do inglês para pt-BR. Preserve sentido, tom e contexto. " +
                    "Não explique. Não altere order. Retorne somente o objeto solicitado."
            );

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", cuePayload(cues));

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_schema");
        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", "ihub_scene_translation");
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", translationSchema(cues.size()));

        return mapper.writeValueAsString(root);
    }

    private String cuePayload(List<TranslationCue> cues) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode array = payload.putArray("cues");
        for (TranslationCue cue : cues) {
            ObjectNode item = array.addObject();
            item.put("order", cue.order());
            item.put("en", cue.approvedEn());
        }
        return mapper.writeValueAsString(payload);
    }

    private ObjectNode translationSchema(int expectedCount) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        ObjectNode translations = properties.putObject("translations");
        translations.put("type", "array");
        translations.put("minItems", expectedCount);
        translations.put("maxItems", expectedCount);

        ObjectNode item = translations.putObject("items");
        item.put("type", "object");
        item.put("additionalProperties", false);
        ObjectNode itemProperties = item.putObject("properties");
        itemProperties.putObject("order").put("type", "integer");
        itemProperties.putObject("pt").put("type", "string").put("minLength", 1);
        ArrayNode itemRequired = item.putArray("required");
        itemRequired.add("order");
        itemRequired.add("pt");

        schema.putArray("required").add("translations");
        return schema;
    }

    public List<TranslatedCue> parseTranslations(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode translations = root.path("translations");
        if (!translations.isArray() || translations.isEmpty()) {
            throw new IllegalArgumentException("Resposta Groq sem traduções.");
        }

        java.util.ArrayList<TranslatedCue> result = new java.util.ArrayList<>();
        for (JsonNode item : translations) {
            result.add(new TranslatedCue(
                item.path("order").asInt(0),
                item.path("pt").asText("")
            ));
        }
        return List.copyOf(result);
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer " + config.apiKey())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");
    }

    private GroqApiException apiError(
        HttpResponse<String> response,
        GroqRateSnapshot rate
    ) {
        String message = "Falha na comunicação com a Groq.";
        try {
            JsonNode root = mapper.readTree(response.body());
            String remote = root.path("error").path("message").asText("");
            if (!remote.isBlank()) {
                message = "Groq HTTP " + response.statusCode() + ": " + remote;
            }
        } catch (Exception ignored) {
            message = "Groq HTTP " + response.statusCode() + ".";
        }
        return new GroqApiException(response.statusCode(), message, rate);
    }

    static GroqRateSnapshot rateOf(HttpResponse<?> response) {
        return new GroqRateSnapshot(
            longHeader(response, "x-ratelimit-remaining-requests"),
            longHeader(response, "x-ratelimit-remaining-tokens"),
            durationHeader(response, "x-ratelimit-reset-requests"),
            durationHeader(response, "x-ratelimit-reset-tokens"),
            durationHeader(response, "retry-after")
        );
    }

    private static Long longHeader(HttpResponse<?> response, String name) {
        Optional<String> value = response.headers().firstValue(name);
        if (value.isEmpty()) return null;
        try {
            return Long.parseLong(value.get().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Duration parseDuration(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (normalized.matches("\\d+(\\.\\d+)?")) {
                return Duration.ofMillis(Math.round(Double.parseDouble(normalized) * 1000d));
            }

            double seconds = 0d;
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)(ms|h|m|s)")
                .matcher(normalized);

            int consumed = 0;
            while (matcher.find()) {
                if (matcher.start() != consumed) return null;
                double amount = Double.parseDouble(matcher.group(1));
                seconds += switch (matcher.group(2)) {
                    case "h" -> amount * 3600d;
                    case "m" -> amount * 60d;
                    case "s" -> amount;
                    case "ms" -> amount / 1000d;
                    default -> 0d;
                };
                consumed = matcher.end();
            }
            if (consumed != normalized.length()) return null;
            return Duration.ofMillis(Math.max(0L, Math.round(seconds * 1000d)));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Duration durationHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name)
            .map(HttpGroqClient::parseDuration)
            .orElse(null);
    }
}
