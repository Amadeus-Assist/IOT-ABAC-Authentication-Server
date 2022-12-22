package com.columbia.iotabacserver.controller;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.columbia.iotabacserver.pojo.request.AuthRequest;
import com.columbia.iotabacserver.pojo.request.DevLoginRequest;
import com.columbia.iotabacserver.pojo.request.DevRegRequest;
import com.columbia.iotabacserver.pojo.request.QueryActionsRequest;
import com.columbia.iotabacserver.pojo.request.UserLoginRequest;
import com.columbia.iotabacserver.pojo.request.UserRegRequest;
import com.columbia.iotabacserver.pojo.response.AuthResponse;
import com.columbia.iotabacserver.pojo.response.DevRegResponse;
import com.columbia.iotabacserver.pojo.response.QueryActionsResponse;
import com.columbia.iotabacserver.service.AuthService;
import com.columbia.iotabacserver.service.AuthenticationService;
import com.columbia.iotabacserver.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.columbia.iotabacserver.pojo.request.AuthRequestSecure;

import com.columbia.iotabacserver.controller.AbacController;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DBAccessPermPojo;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import com.columbia.iotabacserver.dao.dbutils.DatabaseOperation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.time.LocalDate;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
@RestController
public class AbacController {
    private static final Logger logger = LoggerFactory.getLogger(AbacController.class);

    @Resource
    private AuthService authService;

    @Resource
    private AuthenticationService authenticationService;

    // url to receive evaluate request from client
    @PostMapping(value = "/authz/eval", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AuthResponse postEval(@RequestBody AuthRequest request) {
        String[] required_tables = {"user_attrs"};
        boolean needSecureDB = needSecureDB(request, required_tables);
        // check necessary info not empty and authentication info correct
        if (!StringUtils.hasText(request.getSubUsername()) || !StringUtils.hasText(request.getSubUserPwd())
                || !authenticationService.userAuthenticateCheck(request.getSubUsername(), request.getSubUserPwd())) {
            logger.info("invalid user authentication info");    
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_INFO);
        }

        if (!StringUtils.hasText(request.getObjDevId()) || !StringUtils.hasText(request.getObjToken())
                || !authenticationService.deviceAuthenticateCheck(request.getObjDevId(), request.getObjToken())) {
            logger.info("invalid device authentication info");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_INFO);
        }

        if (!StringUtils.hasText(request.getAction()) || !StringUtils.hasText(request.getEnvInfo())) {
            logger.info("empty access request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }

        if(needSecureDB) {  // Need to check DB permission
            AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
            for(String table: required_tables){
                // DBAccessPermPojo pojo = mapper.findDenyDate(request.getSubUsername(), table);

                DBAccessPermPojo pojo;
                try{
                    pojo = DatabaseOperation.findRemoteAccessDate(request.getSubUsername(), table);
                } catch (IOException e) {
                    logger.info("cannot assemble access request: {}", e.toString());
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
                }
                if(pojo.getAllowDate() == null && pojo.getDenyDate() == null) {
                    mapper.insertPermInfo(request.getSubUsername(), table, Constants.DEFAULT_TIME);
                }
                if(LocalDate.now().compareTo(LocalDate.parse(pojo.getDenyDate())) <= 0) {
                    logger.info("cannot gain access to DB");
                    return new AuthResponse(Constants.FALSE);
                }
            }
            return new AuthResponse(Constants.DK); // further confirmation is required, return status "don't know"
        }
        
        boolean pass = false;//don't need private db access
        try {
            // assemble the real access request and forward to OpaServer
            pass = authService.opaEval(authService.assembleAccessRequest(request.getSubUsername(),
                    request.getObjDevId(), request.getAction(), request.getEnvInfo()));
        } catch (JsonProcessingException e) {
            logger.info("cannot assemble access request: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        if (pass) {
            return new AuthResponse(Constants.TRUE);
        }
        return new AuthResponse(Constants.FALSE);
    }

    @PostMapping(value = "/authz/evalsecure", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    
    @ResponseBody
    public AuthResponse postEvalSecure(@RequestBody AuthRequestSecure request) {
        // check necessary info not empty and authentication info correct
        if (!StringUtils.hasText(request.getSubUsername()) || !StringUtils.hasText(request.getSubUserPwd())
                || !authenticationService.userAuthenticateCheck(request.getSubUsername(), request.getSubUserPwd())) {
            logger.info("invalid user authentication info");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_INFO);
        }

        if (!StringUtils.hasText(request.getObjDevId()) || !StringUtils.hasText(request.getObjToken())
                || !authenticationService.deviceAuthenticateCheck(request.getObjDevId(), request.getObjToken())) {
            logger.info("invalid device authentication info");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_INFO);
        }

        if (!StringUtils.hasText(request.getAction()) || !StringUtils.hasText(request.getEnvInfo())) {
            logger.info("empty access request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        
        String[] requiredDB = new String[]{"user_attrs"}; //load the required DBs
        return postEvalDBAuth(request, requiredDB);        
    }
    //handle secure evaluation
    public AuthResponse postEvalDBAuth(AuthRequestSecure request, String[] requiredDB) {

        if(!authenticationService.dbAuthorizeCheck(request.getDbauth(), requiredDB, request.getSubUsername())) { 
            logger.info("cannot gain access to DB");
            return new AuthResponse(Constants.FALSE);
        }

        boolean pass = false;
        try {
            // assemble the real access request and forward to OpaServer
            pass = authService.opaEval(authService.assembleAccessRequest(request.getSubUsername(),
                    request.getObjDevId(), request.getAction(), request.getEnvInfo()));
        } catch (JsonProcessingException e) {
            logger.info("cannot assemble access request: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        if (pass) {
            return new AuthResponse(Constants.TRUE);
        }
        return new AuthResponse(Constants.FALSE);

    }
    // handle device registration
    @PostMapping(value = "/authz/register/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DevRegResponse postDevRegister(@RequestBody DevRegRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getDevType())
                || !StringUtils.hasText(request.getAttrs())) {
            logger.info("invalid device register request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_REG_INFO);
        }

        if (authenticationService.deviceExists(request.getDevId())) {
            logger.info("device already registered");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.DEV_ALREADY_REG);
        }

        String token = authenticationService.registerDevice(request.getDevId(), request.getDevType(),
                request.getAttrs());
        return new DevRegResponse(token);
    }


    // handle device login
    @PostMapping(value = "authz/login/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postDevLogin(@RequestBody DevLoginRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getToken())
                || !authenticationService.deviceAuthenticateCheck(request.getDevId(), request.getToken())) {
            logger.info("invalid device login request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_AUTHENTICATION);
        }
    }


    // handle user registration
    @PostMapping(value = "authz/register/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postUserRegister(@RequestBody UserRegRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getAttrs())) {
            logger.info("invalid user register request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_REG_INFO);
        }

        if (authenticationService.userExists(request.getUsername())) {
            logger.info("user already registered");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.USER_ALREADY_REG);
        }

        authenticationService.registerUser(request.getUsername(), request.getPassword(), request.getAttrs());
    }


    // handle user login
    @PostMapping(value = "authz/login/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postUserLogin(@RequestBody UserLoginRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !authenticationService.userAuthenticateCheck(request.getUsername(), request.getPassword())) {
            logger.info("invalid user login request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_AUTHENTICATION);
        }
    }


    // handle action query request
    @PostMapping(value = "authz/query-actions/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public QueryActionsResponse getDeviceActions(@RequestBody QueryActionsRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getToken())
                || !authenticationService.deviceAuthenticateCheck(request.getDevId(), request.getToken())) {
            logger.info("invalid device actions query request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_AUTHENTICATION);
        }
        String actions = authenticationService.queryDevActions(request.getDevId());
        return new QueryActionsResponse(actions);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return new ResponseEntity<>(e.getMessage(), e.getStatus());
    }

    public boolean needSecureDB(AuthRequest request, String[] requiredDB) {
        boolean flag = false;
        for(String table:requiredDB){
            System.out.println(request.getSubUsername()+ ": " + table);
            // DBAccessPermPojo pojo = mapper.findAccessDate(request.getSubUsername(), table);
            DBAccessPermPojo pojo;
            try{
                pojo = DatabaseOperation.findRemoteAccessDate(request.getSubUsername(), table);
                
            } catch (IOException e) {
                logger.info("cannot assemble access request: {}", e.toString());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
            }
            System.out.println("found record: " + pojo.getAllowDate());
            if(LocalDate.parse(pojo.getAllowDate()).compareTo(LocalDate.now()) >= 0) {
                System.out.println("condition satisfied");
            }
            else {
                flag = true;
            }
        }
        return flag;
    }

    public boolean needSecureDB2(AuthRequestSecure request, String[] requiredDB) {
        boolean flag = false;
        // AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        for(String table:requiredDB){
            System.out.println(request.getSubUsername()+ ": " + table);
            // DBAccessPermPojo pojo = mapper.findAccessDate(request.getSubUsername(), table);
            DBAccessPermPojo pojo;
            try{
                pojo = DatabaseOperation.findRemoteAccessDate(request.getSubUsername(), table);
            } catch (IOException e) {
                logger.info("cannot assemble access request: {}", e.toString());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
            }

            System.out.println("found record: " + pojo.getAllowDate());
            if(LocalDate.parse(pojo.getAllowDate()).compareTo(LocalDate.now()) >= 0){
                System.out.println("satisfied");
            }
            else {
                flag = true;
            }
        }
        return flag;
    }
    
}

/*

public String findRemoteAccessDate(String user_id, String tbl_name ) throws IOException {
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
            return response.toString();
		} else {
			System.out.println("GET request did not work.");
            return "";
		}
    }


private static final String USER_AGENT = "Mozilla/5.0";

	private static final String GET_URL = "https://localhost:9090/SpringMVCExample";

	private static final String POST_URL = "https://localhost:9090/SpringMVCExample/home";

	private static final String POST_PARAMS = "userName=Pankaj";

    public static void main(String[] args) throws IOException {
		sendGET();
		System.out.println("GET DONE");
		sendPOST();
		System.out.println("POST DONE");
	}
	private static void sendPOST() throws IOException {
		URL obj = new URL(POST_URL);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);

		// For POST only - START
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		// For POST only - END

		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("POST request did not work.");
		}
	}

}
*/