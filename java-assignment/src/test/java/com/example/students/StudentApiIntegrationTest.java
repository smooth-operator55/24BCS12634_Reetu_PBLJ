package com.example.students;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StudentApiIntegrationTest {
 @LocalServerPort int port;
 private final HttpClient client = HttpClient.newHttpClient();
 private HttpResponse<String> call(String method, String path, String body) throws Exception {
  var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
   .header("Content-Type", "application/json")
   .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
  return client.send(request, HttpResponse.BodyHandlers.ofString());
 }
 @Test void studentLifecycleAndFailures() throws Exception {
  String valid = "{\"name\":\"Asha Rao\",\"email\":\"asha@example.com\",\"course\":\"Java\",\"age\":21}";
  var created = call("POST", "/api/students", valid);
  assertEquals(201, created.statusCode());
  String location = created.headers().firstValue("Location").orElseThrow();
  assertTrue(created.body().contains("Asha Rao"));
  assertEquals(200, call("GET", location, null).statusCode());
  assertTrue(call("GET", "/api/students", null).body().contains("asha@example.com"));
  assertTrue(call("GET", "/api/students/course/java", null).body().contains("Asha Rao"));
  assertEquals("[]", call("GET", "/api/students/course/Unknown", null).body());
  var updated = call("PUT", location, valid.replace("Java", "Python"));
  assertEquals(200, updated.statusCode());
  assertTrue(updated.body().contains("Python"));
  assertEquals("[]", call("GET", "/api/students/course/Java", null).body());
  assertEquals(400, call("POST", "/api/students", valid.replace("asha@example.com", "invalid")).statusCode());
  assertEquals(400, call("POST", "/api/students", "{}").statusCode());
  assertEquals(400, call("POST", "/api/students", "{").statusCode());
  assertEquals(400, call("GET", "/api/students/not-a-number", null).statusCode());
  assertEquals(204, call("DELETE", location, null).statusCode());
  assertEquals(404, call("GET", location, null).statusCode());
  assertEquals(404, call("PUT", location, valid).statusCode());
  assertEquals(404, call("DELETE", location, null).statusCode());
 }
}
