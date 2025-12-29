package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.*;
import io.sandboxdev.gitmcp.protocol.McpJsonRpcDispatcher;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for JSON-RPC protocol compliance.
 * 
 * These tests verify that the MCP server correctly validates JSON-RPC messages
 * and returns properly formatted responses with correct error codes.
 */
@Tag("git-mcp-server")
@Tag("property-1")
@Tag("json-rpc-compliance")
class JsonRpcProtocolComplianceProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final McpJsonRpcDispatcher dispatcher = new McpJsonRpcDispatcher();

    /**
     * Property 1: JSON-RPC Protocol Compliance (Req 1.1, 1.2)
     * Valid requests are processed correctly
     */
    @Property
    @Tag("property-1")
    @Tag("json-rpc-compliance")
    void validJsonRpcRequestsAreProcessedCorrectly(@ForAll("validMcpRequests") McpRequest request) {
        // This test should fail initially as McpJsonRpcDispatcher doesn't exist yet
        assertThat(request.isValid()).isTrue();

        // The dispatcher should validate the request according to MCP specification
        // and return a properly formatted response
        McpResponse response = dispatcher.dispatch(request, null);

        assertThat(response).isNotNull();
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo(request.id());
        assertThat(response.isValid()).isTrue();
    }

    /**
     * Property 1: JSON-RPC Protocol Compliance (Req 1.3)
     * Invalid version returns error
     */
    @Property
    @Tag("property-1")
    @Tag("json-rpc-compliance")
    void invalidJsonRpcVersionReturnsError(@ForAll("invalidVersionRequests") McpRequest request) {
        // This test should fail initially as McpJsonRpcDispatcher doesn't exist yet
        assertThat(request.jsonrpc()).isNotEqualTo("2.0");

        McpResponse response = dispatcher.dispatch(request, null);

        assertThat(response).isNotNull();
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo(request.id());
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.INVALID_REQUEST);
    }

    /**
     * Property 1: JSON-RPC Protocol Compliance (Req 1.3)
     * Empty method name returns error
     */
    @Property
    @Tag("property-1")
    @Tag("json-rpc-compliance")
    void emptyMethodNameReturnsError(@ForAll("emptyMethodRequests") McpRequest request) {
        // This test should fail initially as McpJsonRpcDispatcher doesn't exist yet
        assertThat(request.method()).isBlank();

        McpResponse response = dispatcher.dispatch(request, null);

        assertThat(response).isNotNull();
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.INVALID_REQUEST);
    }

    /**
     * Property 1: JSON-RPC Protocol Compliance (Req 1.4)
     * Unknown method returns METHOD_NOT_FOUND
     */
    @Property
    @Tag("property-1")
    @Tag("json-rpc-compliance")
    void unknownMethodReturnsMethodNotFoundError(@ForAll("unknownMethodRequests") McpRequest request) {
        // This test should fail initially as McpJsonRpcDispatcher doesn't exist yet
        // Assume the method name is not a known MCP method

        McpResponse response = dispatcher.dispatch(request, null);

        assertThat(response).isNotNull();
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.METHOD_NOT_FOUND);
        assertThat(response.error().message()).contains(request.method());
    }

    /**
     * Property 1: JSON-RPC Protocol Compliance (Req 1.3)
     * Malformed JSON returns PARSE_ERROR
     */
    @Property
    @Tag("property-1")
    @Tag("json-rpc-compliance")
    void malformedJsonReturnsParseError(@ForAll("malformedJsonStrings") String malformedJson) {
        // This test should fail initially as McpJsonRpcDispatcher doesn't exist yet
        try {
            objectMapper.readValue(malformedJson, McpRequest.class);
            // If parsing succeeds, this is not a malformed JSON test case
            Assume.that(false);
        } catch (JsonProcessingException e) {
            // Expected - this is malformed JSON
            McpResponse response = dispatcher.handleMalformedJson(malformedJson);

            assertThat(response).isNotNull();
            assertThat(response.jsonrpc()).isEqualTo("2.0");
            assertThat(response.isError()).isTrue();
            assertThat(response.error().code()).isEqualTo(McpError.PARSE_ERROR);
        }
    }

    /**
     * Property 1: JSON-RPC Protocol Compliance (Req 1.2)
     * Responses maintain request ID correlation
     */
    @Property
    @Tag("property-1")
    @Tag("json-rpc-compliance")
    void responsesMaintainRequestIdCorrelation(@ForAll("validMcpRequests") McpRequest request) {
        // This test should fail initially as McpJsonRpcDispatcher doesn't exist yet
        McpResponse response = dispatcher.dispatch(request, null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(request.id());
    }

    // Generators for test data
    @Provide
    Arbitrary<McpRequest> validMcpRequests() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1),
                jsonNodes(),
                Arbitraries.strings().alpha().ofMinLength(1))
                .as((method, params, id) -> new McpRequest("2.0", method, params, id));
    }

    @Provide
    Arbitrary<McpRequest> invalidVersionRequests() {
        return Combinators.combine(
                Arbitraries.strings().alpha().filter(v -> !"2.0".equals(v)),
                Arbitraries.strings().alpha().ofMinLength(1),
                jsonNodes(),
                Arbitraries.strings().alpha().ofMinLength(1)).as(McpRequest::new);
    }

    @Provide
    Arbitrary<McpRequest> emptyMethodRequests() {
        return Combinators.combine(
                Arbitraries.of("", "   ", "\t", "\n"),
                jsonNodes(),
                Arbitraries.strings().alpha().ofMinLength(1))
                .as((method, params, id) -> new McpRequest("2.0", method, params, id));
    }

    @Provide
    Arbitrary<McpRequest> unknownMethodRequests() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1)
                        .filter(method -> !isKnownMcpMethod(method)),
                jsonNodes(),
                Arbitraries.strings().alpha().ofMinLength(1))
                .as((method, params, id) -> new McpRequest("2.0", method, params, id));
    }

    @Provide
    Arbitrary<String> malformedJsonStrings() {
        return Arbitraries.of(
                "{invalid json",
                "{'single': 'quotes'}",
                "{\"unclosed\": \"string",
                "{\"trailing\": \"comma\",}",
                "not json at all",
                "",
                "null",
                "{\"duplicate\": 1, \"duplicate\": 2}");
    }

    @Provide
    Arbitrary<JsonNode> jsonNodes() {
        return Arbitraries.strings().alpha().map(s -> {
            try {
                return objectMapper.readTree("\"" + s + "\"");
            } catch (JsonProcessingException e) {
                return objectMapper.createObjectNode().put("value", s);
            }
        });
    }

    private boolean isKnownMcpMethod(String method) {
        // Define known MCP methods - this will be expanded as we implement tools
        return method.equals("initialize") ||
                method.equals("tools/list") ||
                method.equals("tools/call") ||
                method.equals("resources/list") ||
                method.equals("resources/read");
    }
}