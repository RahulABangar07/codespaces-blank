import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.MediaType;

import okhttp3.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SwaggerTestRunner {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        String swaggerUrl = "https://your-api-domain.com/swagger/v3/api-docs";  // Replace with your Swagger endpoint

        OpenAPI openAPI = new OpenAPIV3Parser().read(swaggerUrl);
        if (openAPI == null) {
            System.err.println("Failed to parse Swagger spec.");
            return;
        }

        String baseUrl = openAPI.getServers().get(0).getUrl();

        openAPI.getPaths().forEach((path, pathItem) -> {
            for (PathItem.HttpMethod method : pathItem.readOperationsMap().keySet()) {
                Operation operation = pathItem.readOperationsMap().get(method);
                System.out.println("Testing endpoint: [" + method + "] " + path);

                // Build and execute request
                try {
                    Request request = buildRequest(baseUrl, path, method.name(), operation);
                    Response response = client.newCall(request).execute();
                    System.out.println("Response code: " + response.code());
                    System.out.println("Body: " + response.body().string());
                } catch (Exception e) {
                    System.err.println("Error hitting endpoint: " + e.getMessage());
                }

                System.out.println("--------------------------------------------------");
            }
        });
    }

    private static Request buildRequest(String baseUrl, String path, String method, Operation operation) {
        String fullUrl = baseUrl + path;

        // Replace path variables with dummy values
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                if ("path".equals(param.getIn())) {
                    fullUrl = fullUrl.replace("{" + param.getName() + "}", "1"); // Replace with dummy
                } else if ("query".equals(param.getIn())) {
                    fullUrl += (fullUrl.contains("?") ? "&" : "?") + param.getName() + "=test";
                }
            }
        }

        Request.Builder builder = new Request.Builder().url(fullUrl);

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            String jsonBody = buildDummyRequestBody(operation);
            builder.method(method, RequestBody.create(jsonBody, MediaType.parse("application/json")));
        } else {
            builder.method(method, null);
        }

        return builder.build();
    }

    private static String buildDummyRequestBody(Operation operation) {
        try {
            if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                MediaType mediaType = operation.getRequestBody().getContent().get("application/json");
                if (mediaType != null && mediaType.getSchema() != null) {
                    Schema<?> schema = mediaType.getSchema();
                    return generateDummyJsonFromSchema(schema);
                }
            }
        } catch (Exception e) {
            System.err.println("Error building dummy request body: " + e.getMessage());
        }
        return "{}";
    }

    @SuppressWarnings("unchecked")
    private static String generateDummyJsonFromSchema(Schema<?> schema) {
        Map<String, Object> dummyData = new LinkedHashMap<>();
        if (schema.getProperties() != null) {
            Map<String, Schema> props = schema.getProperties();
            for (Map.Entry<String, Schema> entry : props.entrySet()) {
                String key = entry.getKey();
                Schema propSchema = entry.getValue();

                String type = propSchema.getType();
                if ("string".equals(type)) {
                    dummyData.put(key, "test");
                } else if ("integer".equals(type) || "number".equals(type)) {
                    dummyData.put(key, 1);
                } else if ("boolean".equals(type)) {
                    dummyData.put(key, true);
                } else if ("array".equals(type)) {
                    dummyData.put(key, Collections.emptyList());
                } else if ("object".equals(type)) {
                    dummyData.put(key, new HashMap<>());
                } else {
                    dummyData.put(key, null);
                }
            }
        }

        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dummyData);
        } catch (Exception e) {
            return "{}";
        }
    }
}
