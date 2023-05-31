package org.jembi.ciol.dhis2Exporter;

import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class OKHTTP {
    private static final Logger LOGGER = LogManager.getLogger(OKHTTP.class);
    OkHttpClient httpClient = null;
    String credentials = null;

    private OKHTTP() { }

    public static OKHTTP getInstance() {
        return OKHTTPHolder.INSTANCE;
    }

    public void createAuthenticatedClient(final String username,
                                          final String password) {
        httpClient = new OkHttpClient();
        credentials = Credentials.basic(username, password);
    }

    public Response checkClientAvailability(String anyURL) throws IOException {
        Request request = new Request.Builder()
                                     .url(anyURL)
                                     .addHeader("Authorization", credentials)
                                     .build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        LOGGER.info("Yay I am in!!!, {}",response.code());
        return response;
    }

    public Response clientPost(String anyURL, String json) throws IOException {
        final var body = RequestBody.create(json, MediaType.parse("application/json"));
        final var request = new Request.Builder()
                                                .url(anyURL)
                                                .post(body)
                                                .addHeader("Authorization", credentials)
                                                .addHeader("Accept", "application/json")
                                                .build();
        final var call = httpClient.newCall(request);
        return call.execute();
    }

    private static class OKHTTPHolder {
        public static final OKHTTP INSTANCE = new OKHTTP();
    }
}
