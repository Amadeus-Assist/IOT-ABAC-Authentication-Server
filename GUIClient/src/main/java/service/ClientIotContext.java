package service;

import org.apache.http.impl.client.CloseableHttpClient;

public class ClientIotContext {
    private CloseableHttpClient client;
    private String devId;
    private String token;

    public CloseableHttpClient getClient() {
        return client;
    }

    public void setClient(CloseableHttpClient client) {
        this.client = client;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
