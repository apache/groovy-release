import java.net.http.HttpResponse
import groovy.json.JsonSlurper

class WrappedResponse {
    @Delegate HttpResponse response

    WrappedResponse(HttpResponse response) {
        this.response = response
    }

    def getJson() {
        new JsonSlurper().parseText(response.body())
    }
}
