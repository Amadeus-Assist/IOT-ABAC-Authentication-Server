package pojo;

import java.util.Objects;

public class DevInfo {
    int port;
    String devId;
    String actions;

    public DevInfo(int port, String devId, String actions) {
        this.port = port;
        this.devId = devId;
        this.actions = actions;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DevInfo devInfo = (DevInfo) o;
        return port == devInfo.port && devId.equals(devInfo.devId) && actions.equals(devInfo.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, devId, actions);
    }

    @Override
    public String toString() {
        return "DevInfo{" +
                "port=" + port +
                ", devId='" + devId + '\'' +
                ", actions='" + actions + '\'' +
                '}';
    }
}
