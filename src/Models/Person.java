package Models;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;

public abstract class Person implements Serializable {

    protected String directory;
    protected File dir;
    protected File info;
    protected File[] files;
    
    @Expose
    protected String username;
    @Expose
    protected String password;

    protected Person(String directory) {
        this.directory = directory;
        this.dir = new File(this.directory);
        this.files = dir.listFiles();
    }

    public abstract boolean login(String username, String password) throws FileNotFoundException, IOException;

    protected boolean findAccount(String username, String password) throws IOException {
        boolean found = false;
        
        for (int i = 0; i < this.files.length && !found; i++) {
            if (this.files[i].getName().equals(username + ".json")) {
                this.info = this.files[i];
                found = true;
                
            }
        }
        
        if(!found){
            return false;
        }
        
        JsonParser parser = new JsonParser();
        Object obj = parser.parse(new FileReader(this.directory + this.info.getName()));
        JsonObject jsonObject = (JsonObject) obj;
        
        if (password.equals((String) jsonObject.get("password").getAsString())) {
            return true;
        }
        
        return false;
    }

    public boolean isUsernameFree(String username) {
        username = username + ".json";
        File[] filesCid = new File("DataBase/ClientsDataBase").listFiles();
        File[] filesProtec = new File("DataBase/WorkersDataBase").listFiles();

        for (File file : filesCid) {
            if (username.equals(file.getName())) {
                return false;
            }
        }

        for (File file : filesProtec) {
            if (username.equals(file.getName())) {
                return false;
            }
        }

        return true;
    }
    
    public String getUsername(){
        return this.username;
    }
    
}
