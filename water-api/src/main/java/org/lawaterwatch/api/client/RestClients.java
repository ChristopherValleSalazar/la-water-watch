package org.lawaterwatch.api.client;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

//Shared RestClient setup for the outbound clients, so a hung API can't block startup forever
final class RestClients {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    //ArcGIS pages with geometry can take a while (data loader only, no user is waiting)
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

    private RestClients() {}

    static RestClient create(String baseUrl) {
        return create(baseUrl, READ_TIMEOUT);
    }

    //For clients a user waits on, so a slow API can't hold a request (and a Tomcat thread) for long
    static RestClient create(String baseUrl, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
