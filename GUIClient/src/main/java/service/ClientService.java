package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClientService {
    public static String generateUserRegisterBody(String username, String password, String attrs) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);
        map.put("attributes", attrs);
        String res =  new ObjectMapper().writeValueAsString(map);
        return res;
    }

    public static String generateUserLoginBody(String username, String password) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);
        return new ObjectMapper().writeValueAsString(map);
    }

    public static String generateDevRegisterBody(String devId, String devType) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("device_id", devId);
        map.put("device_type", devType);
        return new ObjectMapper().writeValueAsString(map);
    }

    public static String generateDevLoginBody(String devId, String token) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("device_id", devId);
        map.put("token", token);
        return new ObjectMapper().writeValueAsString(map);
    }

    public static String generateActionQueryBody(String devId, String token) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("device_id", devId);
        map.put("token", token);
        return new ObjectMapper().writeValueAsString(map);
    }

    public static CloseableHttpResponse sendHttpPostRequest(CloseableHttpClient client, String url, String body) throws IOException {
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(body);
        post.setEntity(entity);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = client.execute(post);
        return response;
    }
}
