import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

public class RestApiTestAutomation {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode testCases = mapper.readTree(new File("api_tests.json"));

        for (JsonNode testCase : testCases) {
            String method = testCase.path("method").asText("GET");
            String url = testCase.path("url").asText();
            JsonNode headersNode = testCase.path("headers");
            JsonNode payloadNode = testCase.path("payload");

            RequestSpecification request = RestAssured.given();

            // Set headers
            if (!headersNode.isMissingNode()) {
                Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    request.header(field.getKey(), field.getValue().asText());
                }
            }

            // Set payload if exists
            if (!payloadNode.isMissingNode() && !payloadNode.isEmpty()) {
                request.body(payloadNode.toString());
            }

            Response response;
            switch (method.toUpperCase()) {
                case "POST":
                    response = request.post(url);
                    break;
                case "PUT":
                    response = request.put(url);
                    break;
                case "DELETE":
                    response = request.delete(url);
                    break;
                default: // GET
                    response = request.get(url);
                    break;
            }

            System.out.println("URL: " + url);
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Response: " + response.getBody().asString());
            System.out.println("-------------------------------------------");
        }
    }
}
