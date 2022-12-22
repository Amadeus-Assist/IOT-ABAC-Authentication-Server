package com.columbia.iotabacserver.dao.dbutils;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.time.LocalDate;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.columbia.iotabacserver.dao.model.DBAccessPermPojo;
import com.columbia.iotabacserver.dao.model.UserAttrsPojo;

public class DatabaseOperation {
    public static DBAccessPermPojo findRemoteAccessDate(String user_id, String tbl_name ) throws IOException {
		// query the permission info for user_id and tbl_name (allowance expiration date and prohibition expiration date)
        String jsonString;
        String GET_URL = "http://localhost:3333/find_db_access/" + user_id + "/" + tbl_name;
		URL obj = new URL(GET_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println("response: "+response.toString());
            jsonString = response.toString();
		} else {
			System.out.println("GET request did not work.");
            jsonString =  "";
		}
        DBAccessPermPojo pojo;
        ObjectMapper objmapper = new ObjectMapper();
        pojo = objmapper.readValue(jsonString, DBAccessPermPojo.class);
        return pojo;
    }

	public static UserAttrsPojo findUserAttrs(String user_id) throws IOException { //Query User's attributes from Secure DB
		String jsonString;
        String GET_URL = "http://localhost:3333/find_user_attrs/" + user_id ;
		URL obj = new URL(GET_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println("response: "+response.toString());
            jsonString = response.toString();
		} else {
			System.out.println("GET request did not work.");
            jsonString =  "";
		}
        UserAttrsPojo pojo;
        ObjectMapper objmapper = new ObjectMapper();
        pojo = objmapper.readValue(jsonString, UserAttrsPojo.class);
        return pojo;
	}
}
