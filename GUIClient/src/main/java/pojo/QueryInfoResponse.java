package pojo;

public class QueryInfoResponse {
    private String message;
    private String dev_id;
    private String actions;

    public QueryInfoResponse(){
        super();
    };

    public QueryInfoResponse(String message, String dev_id, String actions) {
        this.message = message;
        this.dev_id = dev_id;
        this.actions = actions;
    }

    public String getDev_id() {
        return dev_id;
    }

    public void setDev_id(String dev_id) {
        this.dev_id = dev_id;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "QueryInfoResponse{" +
                "message='" + message + '\'' +
                ", dev_id='" + dev_id + '\'' +
                ", actions='" + actions + '\'' +
                '}';
    }
}
