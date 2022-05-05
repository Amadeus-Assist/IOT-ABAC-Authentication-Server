package pojo;

public class BadRequestResponse {
    private String message;

    public BadRequestResponse(){
        super();
    }

    public BadRequestResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
