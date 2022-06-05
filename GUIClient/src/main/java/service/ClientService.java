package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import pojo.UserAccessRequest;
import utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClientService {
//    private static final ObjectMapper mapper = new ObjectMapper();

    public static String generateUserRegisterBody(String username, String password, String attrs) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);
        map.put("attributes", attrs);
        String res = Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        return res;
    }

    public static String generateUserLoginBody(String username, String password) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);
        return Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }

    public static String generateDevRegisterBody(String devId, String devType, String attrs) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("device_id", devId);
        map.put("device_type", devType);
        map.put("attrs", attrs);
        return Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }

    public static String generateDevLoginBody(String devId, String token) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("device_id", devId);
        map.put("token", token);
        return Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }

    public static String generateActionQueryBody(String devId, String token) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("device_id", devId);
        map.put("token", token);
        return Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
    }

    public static String generateAccessRequestBodyFromUser(String username, String password,
                                                           String targetDev, String action) throws JsonProcessingException {
        UserAccessRequest userAccessRequest = new UserAccessRequest(username, password, targetDev, action);
        return Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userAccessRequest);
    }

    public static CloseableHttpResponse sendHttpPostRequest(CloseableHttpClient client, String url, String body) throws IOException {
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(body);
        post.setEntity(entity);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");
        return client.execute(post);
    }

    public static CloseableHttpResponse sendHttpGetRequest(CloseableHttpClient client, String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-type", "application/json");
        return client.execute(get);
    }
}
