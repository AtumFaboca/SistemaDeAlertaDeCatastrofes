package Models;

import com.google.gson.annotations.Expose;

public class Port {
    @Expose
    private String localization;
    @Expose
    private String port;
    @Expose
    private String username;
    
    public Port(String username, String port, String localization){
        this.username = username;
        this.port = port;
        this.localization = localization;
    }

    public String getLocalization() {
        return localization;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String usename) {
        this.username = usename;
    }
    
    @Override
    public String toString(){
        return "[Worker: " + this.username + " | Localization: " + this.localization + " | Port: " + this.port + "]";
    }
}
