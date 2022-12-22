package service;

import com.sun.net.httpserver.HttpServer;
import gui.TestIotClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import pojo.*;
import utils.Constants;
import utils.Utils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocalHttpServer {
    private int port;
    private HttpServer server;
    private TestIotClient iotClient;

    public LocalHttpServer(int port, TestIotClient iotClient) throws IOException {
        this.port = port;
        this.iotClient = iotClient;
        this.server = HttpServer.create(new InetSocketAddress(port), 100);
        server.createContext("/iot-client/query-info", httpExchange -> {
            if (httpExchange.getRequestMethod().equals("GET")) {
                ClientIotContext context = iotClient.getContext();
                QueryInfoResponse queryInfoResponse;
                if (!context.isLoggedIn()) {
                    queryInfoResponse = new QueryInfoResponse(Constants.IOT_CLIENT_NOT_LOGIN, "", "");
                } else {
                    queryInfoResponse = new QueryInfoResponse(Constants.OK, context.getDevId(),
                            Utils.hasText(context.getActionsStr()) ? context.getActionsStr() : "");
                }
//            Utils.appendToPane(iotClient.getLoggerTextPane(), "receive query info request\n", Color.BLACK);
                String responseStr = Utils.mapper.writeValueAsString(queryInfoResponse);
                byte[] response = responseStr.getBytes(StandardCharsets.UTF_8);
                httpExchange.getResponseHeaders().add("Content-type", "application/json");
                httpExchange.sendResponseHeaders(200, response.length);
                OutputStream out = httpExchange.getResponseBody();
                out.write(response);
                out.close();
            }
        });
        server.createContext("/iot-client/access-request", httpExchange -> {
            if (httpExchange.getRequestMethod().equals("POST")) {
                ClientIotContext context = iotClient.getContext();
                InputStream is = httpExchange.getRequestBody();
                String inputstr = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//                System.out.println(inputstr);
                UserAccessRequestSecure request = Utils.mapper.readValue(inputstr, UserAccessRequestSecure.class);
                httpExchange.getResponseHeaders().add("Content-type", "application/json");
                byte[] response;
                if (!Utils.hasText(request.getUsername()) || !Utils.hasText(request.getPassword())
                        || !Utils.hasText(request.getTargetDev()) || !Utils.hasText(request.getAction())
                        || !request.getTargetDev().equals(context.getDevId())) {
                    String responseStr = Utils.mapper.writeValueAsString(new BadRequestResponse("Invalid request " +
                            "content"));
                    response = responseStr.getBytes(StandardCharsets.UTF_8);
                    httpExchange.sendResponseHeaders(400, response.length);
                } else {
                    // forward this request to server
                    System.out.println("DBauth: "+ request.getDbAuth());
                    System.out.println("Secure: " + request.getSecured());
                    Map<String, DevInfo> nearbyDevices = context.getSearchService().getNearByDevices();
                    List<Map<String, String>> nearbyList = new ArrayList<>();
                    nearbyDevices.forEach((k, v) -> {
                        Map<String, String> localMap = new HashMap<>();
                        localMap.put("dev_id", k);
                        nearbyList.add(localMap);
                    });

                    Map<String, Object> finalMap = new HashMap<>();
                    finalMap.put("near_dev", nearbyList);
                    finalMap.put("timestamp", System.currentTimeMillis());

                    String envInfo = Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalMap);

                    if(request.getSecured().equals("true")) {
                        System.out.println("Try secure request");

                        CloseableHttpResponse sendJWTResponse = ClientService.sendJwtToken(context.getClient(), "Aster", "123", request.getDbAuth());
                        System.out.println(EntityUtils.toString(sendJWTResponse.getEntity(), "UTF-8"));
                        if(sendJWTResponse.getStatusLine().getStatusCode() != 200) {
                            String jwtResponseBodyStr = EntityUtils.toString(sendJWTResponse.getEntity(), "UTF-8");
                            Utils.appendToPane(iotClient.getLoggerTextPane(),
                                    "send DB perm failed, returned body:\n" + jwtResponseBodyStr +
                                            "\n", Color.red);
                            response = jwtResponseBodyStr.getBytes(StandardCharsets.UTF_8);
                            httpExchange.sendResponseHeaders(400, response.length);
                        }
                        Utils.appendToPane(iotClient.getLoggerTextPane(), "secured request here \n", Color.blue);
                        IotAccessRequestSecure iotAccessRequestSecure = new IotAccessRequestSecure(request.getUsername(),
                                request.getPassword(), context.getDevId(), context.getToken(), request.getAction(),
                                envInfo, request.getSecured(), request.getDbAuth());

                        CloseableHttpResponse evalResponse = ClientService.sendHttpPostRequest(context.getClient(),
                                Constants.EVAL_SECURE_ACCESS_REQUEST_URL,
                                Utils.mapper.writeValueAsString(iotAccessRequestSecure));

                        String evalResponseBodyStr = EntityUtils.toString(evalResponse.getEntity(), "UTF-8");

                        if (evalResponse.getStatusLine().getStatusCode() == 200) {
                            AccessResponse decision = Utils.mapper.readValue(evalResponseBodyStr, AccessResponse.class);
                            Utils.appendToPane(iotClient.getLoggerTextPane(), "decision: " + decision.getDecision()+"\n",
                                    Color.blue);
                            if (Utils.hasText(decision.getDecision()) && decision.getDecision().equals("true")) {
                                Utils.appendToPane(iotClient.getLoggerTextPane(),
                                        "perform action: " + request.getAction() + "\n", Color.blue);
                            }
                            response = evalResponseBodyStr.getBytes(StandardCharsets.UTF_8);
                            httpExchange.sendResponseHeaders(200, response.length);
                        } else {
                            Utils.appendToPane(iotClient.getLoggerTextPane(),
                                    "evaluate failed, returned body:\n" + evalResponseBodyStr +
                                    "\n", Color.red);
                            response = evalResponseBodyStr.getBytes(StandardCharsets.UTF_8);
                            httpExchange.sendResponseHeaders(400, response.length);
                        }
                    }
                    else{
                        System.out.println("Try normal request");
                        IotAccessRequest iotAccessRequest = new IotAccessRequest(request.getUsername(),
                                request.getPassword(), context.getDevId(), context.getToken(), request.getAction(),
                                envInfo);

                        CloseableHttpResponse evalResponse = ClientService.sendHttpPostRequest(context.getClient(),
                                Constants.EVAL_ACCESS_REQUEST_URL,
                                Utils.mapper.writeValueAsString(iotAccessRequest));

                        String evalResponseBodyStr = EntityUtils.toString(evalResponse.getEntity(), "UTF-8");

                        if (evalResponse.getStatusLine().getStatusCode() == 200) {
                            AccessResponse decision = Utils.mapper.readValue(evalResponseBodyStr, AccessResponse.class);
                            Utils.appendToPane(iotClient.getLoggerTextPane(), "decision: " + decision.getDecision()+"\n",
                                    Color.blue);
                            if (Utils.hasText(decision.getDecision()) && decision.getDecision().equals("true")) {
                                Utils.appendToPane(iotClient.getLoggerTextPane(),
                                        "perform action: " + request.getAction() + "\n", Color.blue);
                            }
                            response = evalResponseBodyStr.getBytes(StandardCharsets.UTF_8);
                            httpExchange.sendResponseHeaders(200, response.length);
                        } else {
                            Utils.appendToPane(iotClient.getLoggerTextPane(),
                                    "evaluate failed, returned body:\n" + evalResponseBodyStr +
                                    "\n", Color.red);
                            response = evalResponseBodyStr.getBytes(StandardCharsets.UTF_8);
                            httpExchange.sendResponseHeaders(400, response.length);
                        }
                    }
                }

                System.out.println("End request GUI");
                OutputStream out = httpExchange.getResponseBody();
                out.write(response);
                out.close();
            }
        });
    }

    public void start() {
        server.start();
    }

    public void close() {
        server.stop(0);
    }
}
