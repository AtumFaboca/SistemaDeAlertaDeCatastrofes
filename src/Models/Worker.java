package Models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Worker extends Person {

    @Expose
    protected String localization;

    public Worker() {
        super("DataBase/WorkersDataBase/");
    }

    @Override
    public boolean login(String nomeUtilizador, String password) throws FileNotFoundException, IOException {

        if (this.findAccount(nomeUtilizador, password)) {

            this.username = nomeUtilizador;
            this.password = password;

            JsonParser parser = new JsonParser();
            Object obj = parser.parse(new FileReader(this.directory + info.getName()));
            JsonObject jsonObject = (JsonObject) obj;

            this.localization = jsonObject.get("localization").getAsString();

            return true;
        }

        return false;
    }

    public boolean registration(String username, String password, String localization) throws IOException {
        if (this.isUsernameFree(username)) {
            this.username = username;
            this.password = password;
            this.localization = localization;

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

    public String getLocalization() {
        return this.localization;
    }

}
