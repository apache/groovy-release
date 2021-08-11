import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpUtil {
    private HttpUtil() {}

    static HttpClient newClient() {
        HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build()

    }

    static HttpRequest getRequest(url, user, password) {
        HttpRequest.newBuilder()
                .uri(new URI(url))
                .header('Authorization', 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64())
                .GET()
                .build()
    }

    static HttpRequest putRequest(url, json, user, password) {
        def body = HttpRequest.BodyPublishers.ofString(json)
        HttpRequest.newBuilder()
                .uri(new URI(url))
                .header('Authorization', 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64())
                .header('Content-Type', 'application/json')
                .PUT(body)
                .build()
    }

    static WrappedResponse send(HttpClient client, HttpRequest request) {
        new WrappedResponse(client.send(request, HttpResponse.BodyHandlers.ofString()))
    }
}