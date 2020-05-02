package Models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Client extends Person {

    @Expose
    private String name;

    public Client() {
        super("DataBase/ClientsDataBase/");
    }

    @Override
    public boolean login(String username, String password) throws IOException {

        if (this.findAccount(username, password)) {

            this.username = username;
            this.password = password;

            JsonParser parser = new JsonParser();
            Object obj = parser.parse(new FileReader(this.directory + info.getName()));
            JsonObject jsonObject = (JsonObject) obj;

            this.name = jsonObject.get("name").getAsString();

            return true;
        }

        return false;
    }

    public synchronized boolean registration(String username, String password, String name) throws IOException {
        if (this.isUsernameFree(username)) {
            this.username = username;
            this.password = password;
            this.name = name;

            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithoutExposeAnnotation();
            Gson gson = builder.create();

            FileWriter file = new FileWriter(this.directory + this.username + ".json");
            gson.toJson(this, file);
            file.flush();
            file.close();

            return true;
        }

        return false;
    }

    public String getName() {
        return this.name;
    }

}
