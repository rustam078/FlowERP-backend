package com.skr.erp.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns raw request/response bytes into safe JSON for the audit log: secrets masked, size capped,
 * files summarised, and successful responses reduced to a short summary.
 */
@Component
@RequiredArgsConstructor
class AuditPayloadSanitizer {

    private static final int MAX_FULL_BYTES = 64 * 1024;
    private static final int PREVIEW_CHARS = 2000;
    private static final int MAX_SUMMARY_CHARS = 2000;
    private static final List<String> SUMMARY_FIELDS = List.of(
            "success", "message", "id", "userId", "email", "invoiceNo", "invoiceNumber", "batchNumber", "amountSettled", "remainingDue");

    private final ObjectMapper objectMapper;

    // Request payload: full JSON (secrets masked), or a note for files / very large bodies.
    JsonNode request(byte[] body, String contentType) {
        if (body == null || body.length == 0) return null;
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("json")) {
            return note("non-JSON body", contentType, body.length);
        }
        return fullJson(body);
    }

    // Errors keep the full response; successes keep only a short summary (status message, ids).
    JsonNode response(byte[] body, int status) {
        if (body == null || body.length == 0) return null;
        JsonNode parsed = parse(body);
        if (status >= 400 || parsed == null) return status >= 400 ? fullJson(body) : note("non-JSON response", null, body.length);
        return summary(parsed);
    }

    // The message the user saw on a failed request.
    String errorMessage(byte[] body, int status) {
        if (status < 400 || body == null || body.length == 0) return null;
        JsonNode parsed = parse(body);
        JsonNode message = parsed != null ? parsed.get("message") : null;
        return message != null && !message.isNull() ? message.asText() : preview(body);
    }

    JsonNode params(Map<String, String[]> parameters) {
        ObjectNode node = objectMapper.createObjectNode();
        parameters.forEach((key, values) -> node.put(key, values.length == 1 ? values[0] : String.join(",", values)));
        return mask(node);
    }

    // Field value of a JSON request body (e.g. the email of a login attempt).
    String field(JsonNode body, String name) {
        JsonNode value = body != null ? body.get(name) : null;
        return value != null && value.isValueNode() ? value.asText() : null;
    }

    private JsonNode fullJson(byte[] body) {
        if (body.length > MAX_FULL_BYTES) {
            ObjectNode node = note("truncated (too large)", null, body.length);
            node.put("preview", preview(body));
            return node;
        }
        JsonNode parsed = parse(body);
        if (parsed == null) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("raw", preview(body));
            return node;
        }
        return mask(parsed);
    }

    private JsonNode summary(JsonNode parsed) {
        ObjectNode summary = objectMapper.createObjectNode();
        if (parsed.isArray()) {
            summary.put("items", parsed.size());
            return summary;
        }
        JsonNode data = parsed.get("data");
        for (String field : SUMMARY_FIELDS) {
            JsonNode value = parsed.has(field) ? parsed.get(field) : (data != null && data.isObject() ? data.get(field) : null);
            if (value != null && value.isValueNode()) summary.set(field, value);
        }
        JsonNode masked = mask(summary);
        return masked.toString().length() > MAX_SUMMARY_CHARS ? note("summary too large", null, masked.toString().length()) : masked;
    }

    // Recursively hides passwords, tokens and similar secrets.
    private JsonNode mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSecret(field.getKey())) field.setValue(object.textNode("***"));
                else mask(field.getValue());
            }
        } else if (node != null && node.isArray()) {
            node.forEach(this::mask);
        }
        return node;
    }

    private static boolean isSecret(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return k.contains("password") || k.contains("token") || k.equals("authorization") || k.equals("secret");
    }

    private JsonNode parse(byte[] body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    private ObjectNode note(String text, String contentType, int bytes) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("note", text);
        if (contentType != null) node.put("contentType", contentType);
        node.put("bytes", bytes);
        return node;
    }

    private static String preview(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        return text.length() > PREVIEW_CHARS ? text.substring(0, PREVIEW_CHARS) + "…" : text;
    }
}
