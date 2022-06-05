package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import pojo.DevInfo;
import pojo.QueryInfoResponse;
import utils.Constants;
import utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class SearchService {
    private CloseableHttpClient client;
    private Map<String, DevInfo> nearByDevices;
    private int selfPort;
    private JTextPane loggerPane;
    private volatile boolean stopped;
    ThreadPoolExecutor poolExecutor;

    public SearchService(int selfPort, JTextPane loggerPane) {
        this.client = HttpClients.createDefault();
        this.nearByDevices = new HashMap<>();
        this.selfPort = selfPort;
        this.loggerPane = loggerPane;
        this.poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        stopped = false;
    }

    public void search() {
        ConcurrentHashMap<String, DevInfo> newMap = new ConcurrentHashMap<>();
        final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        for (int i = 6200; i < 6210 && !stopped; i++) {
            if (i == selfPort) {
                continue;
            }
            final int destPort = i;
            this.poolExecutor.execute(() -> {
                String url = "http://localhost:" + destPort + "/iot-client/query-info";
                CloseableHttpResponse response;
                try {
                    response = ClientService.sendHttpGetRequest(client, url);
                } catch (IOException ioException) {
                    queue.offer(destPort);
                    return;
                }
                assert response != null;
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBodyStr;
                    try {
                        responseBodyStr = EntityUtils.toString(response.getEntity(), "UTF-8");
                    } catch (IOException ioException) {
                        Utils.appendToPane(loggerPane, "err: read response body exception\n", Color.red);
                        queue.offer(destPort);
                        return;
                    }
                    QueryInfoResponse queryInfoResponse;
                    try {
                        queryInfoResponse = Utils.mapper.readValue(responseBodyStr,
                                QueryInfoResponse.class);
                    } catch (JsonProcessingException e) {
                        Utils.appendToPane(loggerPane, "err: parse body exception\n", Color.red);
                        queue.offer(destPort);
                        return;
                    }
                    assert queryInfoResponse != null;
                    if (Utils.hasText(queryInfoResponse.getMessage())
                            && Constants.OK.equals(queryInfoResponse.getMessage())
                            && Utils.hasText(queryInfoResponse.getDev_id())
                            && Utils.hasText(queryInfoResponse.getActions())) {
                        newMap.put(queryInfoResponse.getDev_id(), new DevInfo(destPort, queryInfoResponse.getDev_id()
                                , queryInfoResponse.getActions()));
                    }
                }
                queue.offer(destPort);
            });
        }
        while (queue.size() < 9) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.nearByDevices = new HashMap<>(newMap);
    }

    public Map<String, DevInfo> getNearByDevices() {
        return this.nearByDevices;
    }

    public void stop() {
        this.stopped = true;
    }

    public void restart() {
        this.stopped = false;
        this.nearByDevices = new HashMap<>();
    }

    public void close() {
        this.stopped = true;
        try {
            client.close();
        } catch (IOException ioException) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.poolExecutor.shutdown();
    }
}
