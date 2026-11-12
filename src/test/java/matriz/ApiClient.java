package matriz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends real HTTP requests to the running application, as a browser or a direct API call would, and
 * handles the session cookie and the CSRF header by hand, so the tests can also send them wrong or not at all.
 */
final class ApiClient {

    static final String COOKIE = "MATRIZ_SESSION";

    record Response(int status, String body, HttpResponse<String> raw) {

        /** The session cookie's value set by this response, if any. */
        Optional<String> sessionCookie() {
            return setCookie().map(header -> header.substring(COOKIE.length() + 1, header.indexOf(';')));
        }

        Optional<String> setCookie() {
            return raw.headers().allValues("Set-Cookie").stream()
                    .filter(header -> header.startsWith(COOKIE + "="))
                    .findFirst();
        }

        /** The CSRF token in a {@code Session} body. */
        String csrfToken() {
            return field("csrfToken");
        }

        /** A text field of a JSON body: enough for these tests, without a JSON library. */
        String field(String name) {
            String key = "\"" + name + "\":\"";
            int start = body.indexOf(key) + key.length();
            return body.substring(start, body.indexOf('"', start));
        }

        /** The whole JSON body. */
        JsonNode json() {
            return JSON.readTree(body);
        }

        /** The {@code Location} header of a created resource. */
        String location() {
            return raw.headers().firstValue("Location").orElseThrow();
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** A JSON body from a map, with strings and numbers escaped properly. */
    static String json(Object value) {
        return JSON.writeValueAsString(value);
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    ApiClient(int port) {
        this.base = "http://localhost:" + port;
    }

    Response send(String method, String path, String jsonBody, String sessionCookie, String csrfToken) {
        return send(method, path, jsonBody, jsonBody == null ? null : "application/json", sessionCookie, csrfToken);
    }

    /** As {@link #send(String, String, String, String, String)}, with any {@code Content-Type}. */
    Response send(String method, String path, String body, String contentType, String sessionCookie, String csrfToken) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (sessionCookie != null) {
            request.header("Cookie", COOKIE + "=" + sessionCookie);
        }
        if (csrfToken != null) {
            request.header("X-CSRF-Token", csrfToken);
        }
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), response);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    Response logIn(String email, String password) {
        return send("POST", "/api/session",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null, null);
    }

    /** A logged-in user: their session cookie and CSRF token. */
    record LoggedIn(String cookie, String csrfToken) {
    }

    LoggedIn logInAs(String email, String password) {
        Response response = logIn(email, password);
        if (response.status() != 200) {
            throw new IllegalStateException("Log-in failed with " + response.status());
        }
        return new LoggedIn(response.sessionCookie().orElseThrow(), response.csrfToken());
    }
}
