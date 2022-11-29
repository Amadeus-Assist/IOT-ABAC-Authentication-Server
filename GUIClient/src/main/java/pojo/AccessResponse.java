package pojo;

public class AccessResponse {
    private String decision;

    public AccessResponse(){
        super();
    }

    public AccessResponse(String decision) {
        this.decision = decision;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    @Override
    public String toString() {
        return "AccessResponse{" +
                "decision='" + decision + '\'' +
                '}';
    }
    public boolean dbAuthRequirement = false;
}
