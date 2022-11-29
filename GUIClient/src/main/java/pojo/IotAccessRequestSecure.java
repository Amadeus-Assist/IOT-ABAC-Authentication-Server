package pojo;


public class IotAccessRequestSecure {
    private String sub_username;
    private String sub_user_password;
    private String obj_dev_id;
    private String obj_token;
    private String action;
    private String env_info;
    private String secured;
    private String dbauth;

    public IotAccessRequestSecure(){
        super();
    }

    public IotAccessRequestSecure(String sub_username, String sub_user_password, String obj_dev_id, String obj_token,
                            String action, String env_info, String secured, String dbauth) {
        this.sub_username = sub_username;
        this.sub_user_password = sub_user_password;
        this.obj_dev_id = obj_dev_id;
        this.obj_token = obj_token;
        this.action = action;
        this.env_info = env_info;
        this.secured = secured;
        this.dbauth = dbauth;
    }

    public String getSub_username() {
        return sub_username;
    }

    public void setSub_username(String sub_username) {
        this.sub_username = sub_username;
    }

    public String getSub_user_password() {
        return sub_user_password;
    }

    public void setSub_user_password(String sub_user_password) {
        this.sub_user_password = sub_user_password;
    }

    public String getObj_dev_id() {
        return obj_dev_id;
    }

    public void setObj_dev_id(String obj_dev_id) {
        this.obj_dev_id = obj_dev_id;
    }

    public String getObj_token() {
        return obj_token;
    }

    public void setObj_token(String obj_token) {
        this.obj_token = obj_token;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEnv_info() {
        return env_info;
    }

    public void setEnv_info(String env_info) {
        this.env_info = env_info;
    }

    public String getSecured() {
        return secured;
    }

    public String getDbAuth() {
        return dbauth;
    }
}
