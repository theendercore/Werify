package com.theendercore.werify.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theendercore.werify.Werify;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;

public class ModConfig {

    public static final String MONGO_URI = "";
    public static final String WS_URI = "";

    private static ModConfig SINGLE_INSTANCE = null;
    private final File configFile;

    private String mongoURI;
    private String wsURI;

    public ModConfig() {
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve("werify.json").toFile();
        this.mongoURI = MONGO_URI;
        this.wsURI = WS_URI;
    }

    public static ModConfig getConfig() {
        if (SINGLE_INSTANCE == null) {
            SINGLE_INSTANCE = new ModConfig();
        }

        return SINGLE_INSTANCE;
    }

    public void load() {
        try {
            String jsonStr = new String(Files.readAllBytes(this.configFile.toPath()));
            if (!jsonStr.equals("")) {
                JsonObject jsonObject = (JsonObject) JsonParser.parseString(jsonStr);
                this.mongoURI = jsonObject.has("mongoURI") ? jsonObject.getAsJsonPrimitive("mongoURI").getAsString() : MONGO_URI;
                this.wsURI = jsonObject.has("wsURI") ? jsonObject.getAsJsonPrimitive("wsURI").getAsString() : WS_URI;
            }
        } catch (IOException e) {
            Werify.LOGGER.info("No file - Creating One");
            File newConfig = new File(String.valueOf(FabricLoader.getInstance().getConfigDir().resolve("werify.json")));
            byte[] b = ("{\n\t\"mongoURI\":\"\",\n\t\"wsURI\":\"\"\n}").getBytes();
            try {
                FileOutputStream fos = new FileOutputStream(newConfig, true);
                fos.write(b);
                fos.close();
            } catch (IOException ex) {
                Werify.LOGGER.info("Failed to create file");
            }
        }
    }

    public void save() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("channel", this.mongoURI);
        jsonObject.addProperty("username", this.wsURI);

        try (PrintWriter out = new PrintWriter(configFile)) {
            out.println(jsonObject);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getMongoURI() {
        return mongoURI;
    }

    public void setMongoURI(String mongoURI) {
        this.mongoURI = mongoURI;
    }

    public String getWsURI() {
        return wsURI;
    }

    public void setWsURI(String wsURI) {
        this.wsURI = wsURI;
    }
}