package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import gui.TestIotClient;
import pojo.QueryInfoResponse;
import utils.Constants;
import utils.Utils;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class LocalHttpServer {
    private int port;
    private HttpServer server;
    private TestIotClient iotClient;

    public LocalHttpServer(int port, TestIotClient iotClient) throws IOException {
        this.port = port;
        this.iotClient = iotClient;
        this.server = HttpServer.create(new InetSocketAddress(port), 100);
        server.createContext("/iot-client/query-info", httpExchange -> {
            ClientIotContext context = iotClient.getContext();
            QueryInfoResponse queryInfoResponse;
            if(!context.isLoggedIn()){
                queryInfoResponse = new QueryInfoResponse(Constants.IOT_CLIENT_NOT_LOGIN, "", "");
            }else{
                queryInfoResponse = new QueryInfoResponse(Constants.OK, context.getDevId(),
                        Utils.hasText(context.getActionsStr())? context.getActionsStr():"");
            }
//            Utils.appendToPane(iotClient.getLoggerTextPane(), "receive query info request\n", Color.BLACK);
            String responseStr = Utils.mapper.writeValueAsString(queryInfoResponse);
            byte[] response = responseStr.getBytes(StandardCharsets.UTF_8);
            httpExchange.getResponseHeaders().add("Content-type", "application/json");
            httpExchange.sendResponseHeaders(200, response.length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(response);
            out.close();
        });
    }

    public void start(){
        server.start();
    }

    public void close(){
        server.stop(0);
    }
}
