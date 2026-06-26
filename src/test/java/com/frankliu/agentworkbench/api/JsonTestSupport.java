package com.frankliu.agentworkbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

final class JsonTestSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestSupport() {
    }

    static long extractLong(String json, String fieldName) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(json).get(fieldName);
        if (node == null || !node.canConvertToLong()) {
            throw new IllegalArgumentException("Field is missing or is not a long: " + fieldName);
        }
        return node.asLong();
    }
}
