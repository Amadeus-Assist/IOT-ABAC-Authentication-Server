package pojo;

public class UserAccessRequest {
    private String username;
    private String password;
    private String targetDev;
    private String action;

    public UserAccessRequest(){
        super();
    }

    public UserAccessRequest(String username, String password, String targetDev, String action) {
        this.username = username;
        this.password = password;
        this.targetDev = targetDev;
        this.action = action;
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
}
