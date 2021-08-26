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

    static HttpRequest headRequest(url, user, password) {
        HttpRequest.newBuilder()
                .uri(new URI(url))
                .header('Authorization', 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64())
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
    }

    static HttpRequest putRequest(url, String json, user, password) {
        def body = HttpRequest.BodyPublishers.ofString(json)
        HttpRequest.newBuilder()
                .uri(new URI(url))
                .header('Authorization', 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64())
                .header('Content-Type', 'application/json')
                .PUT(body)
                .build()
    }

    static HttpRequest postRequest(url, String json, user, password) {
        def body = HttpRequest.BodyPublishers.ofString(json)
        HttpRequest.newBuilder()
                .uri(new URI(url))
                .header('Authorization', 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64())
                .header('Content-Type', 'application/json')
                .POST(body)
                .build()
    }

    static HttpRequest putRequest(prefix, File file, user, password) {
        def body = HttpRequest.BodyPublishers.ofFile(file.toPath())
        HttpRequest.newBuilder()
                .uri(new URI("$prefix/$file.name"))
                .header('Authorization', 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64())
                .header('Content-Type', 'octet-stream')
                .PUT(body)
                .build()
    }

    static boolean awaitPublication(url, user, password, int delay, int numTries) {
        def found = false
        def request = headRequest(url, user, password)
        def client = newClient()
        def response
        while (!found && numTries-- > 0) {
            response = client.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() == 200) found = true
            else sleep delay
        }
        found
    }

    static WrappedResponse send(HttpClient client, HttpRequest request) {
        new WrappedResponse(client.send(request, HttpResponse.BodyHandlers.ofString()))
    }
}