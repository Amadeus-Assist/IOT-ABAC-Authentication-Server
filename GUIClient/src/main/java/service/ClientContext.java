package service;

import org.apache.http.impl.client.CloseableHttpClient;

public class ClientContext {
    private CloseableHttpClient client;
    private String username;
    private String password;

    public CloseableHttpClient getClient() {
        return client;
    }

    public void setClient(CloseableHttpClient client) {
        this.client = client;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
