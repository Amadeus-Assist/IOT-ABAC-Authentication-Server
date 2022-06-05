package service;

import org.apache.http.impl.client.CloseableHttpClient;

public class ClientIotContext {
    private CloseableHttpClient client;
    private String devId;
    private String token;
    private String actionsStr;
    private boolean isLoggedIn;
    private LocalHttpServer localHttpServer;
    private SearchService searchService;

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

    public String getActionsStr() {
        return actionsStr;
    }

    public void setActionsStr(String actionsStr) {
        this.actionsStr = actionsStr;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public LocalHttpServer getLocalHttpServer() {
        return localHttpServer;
    }

    public void setLocalHttpServer(LocalHttpServer localHttpServer) {
        this.localHttpServer = localHttpServer;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
}
