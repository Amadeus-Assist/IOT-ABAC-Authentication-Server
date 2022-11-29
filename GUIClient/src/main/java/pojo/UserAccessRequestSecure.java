package pojo;

public class UserAccessRequestSecure {
    private String username;
    private String password;
    private String targetDev;
    private String action;
    private String secured = "false";
    private String dbAuth = "";

    public UserAccessRequestSecure(){
        super();
    }

    public UserAccessRequestSecure(String username, String password, String targetDev, String action, String secured, String dbauth) {
        this.username = username;
        this.password = password;
        this.targetDev = targetDev;
        this.action = action;
        this.secured = secured;
        this.dbAuth = dbauth;
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

    public String getTargetDev() {
        return targetDev;
    }

    public void setTargetDev(String targetDev) {
        this.targetDev = targetDev;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSecured() {
        return secured;
    }

    public String getDbAuth() {
        return dbAuth;
    }
}
