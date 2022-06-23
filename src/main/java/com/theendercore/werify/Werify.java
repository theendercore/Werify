package com.theendercore.werify;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mongodb.client.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import com.theendercore.werify.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;

public class Werify implements ModInitializer {

    public static final String MODID = "werify";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        ModConfig.getConfig().load();


        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> dispatcher.register(CommandManager.literal("werify").then(CommandManager.argument("password", StringArgumentType.word()).executes(context -> {
            ModConfig config = ModConfig.getConfig();
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                Werify.LOGGER.info("Werification can only be done by a player!");
                return 0;
            }
            if (Objects.equals(config.getMongoURI(), "") || Objects.equals(config.getWsURI(), "")) {
                LOGGER.warn("Werify not setup Properly!");
                player.sendMessage(Text.literal("WafVerify not setup Properly! Contact admin!"));
                return 0;
            }
            String pwd = StringArgumentType.getString(context, "password");
            String playerUUID = player.getUuidAsString();

            try (MongoClient mongoClient = MongoClients.create(config.getMongoURI())) {
                MongoDatabase database = mongoClient.getDatabase("myFirstDatabase");
                MongoCollection<Document> collection = database.getCollection("temppasswordmodels");
                MongoCollection<Document> submitCluster = database.getCollection("verifymodels");
                if (collection.find(eq("password", pwd)).first() == null) {
                    player.sendMessage(Text.literal("Please provide the password you where sent in Discord! \n Or run !reverify in the discord server to generate a new password"));
                    return 0;
                }

                if (submitCluster.find(eq("minecraftUUID", playerUUID)).first() != null) {
                    player.sendMessage(Text.literal("This Minecraft account has been linked to a discord account already!"));
                    return 0;
                }
                Document playerInfo = (collection.find(eq("password", pwd)).first());

                assert playerInfo != null;
                String id = (String) playerInfo.get("userID");
                String serverID = (String) playerInfo.get("serverID");
                List<Document> VerifiedSerevrs = (List<Document>) ((submitCluster.find(eq("_id", id)).first())).get("verifiedSerevrs");

                int value = 0;
                for (int i = 0; i < VerifiedSerevrs.size(); i++) {
                    String yes = (String) VerifiedSerevrs.get(i).get("serverID");
                    if (Objects.equals(yes, serverID)) {
                        value = i;
                        break;
                    }
                }

                Bson updates = Updates.combine(Updates.set("minecraftUUID", playerUUID), Updates.set("verifiedSerevrs." + value + ".verified", true));

                submitCluster.updateOne(new Document().append("_id", id), updates);
                collection.findOneAndDelete(new Document().append("_id", playerInfo.get("_id")));

                WebSocketClient webSocketClient = new WebSocketClient(new URI(Objects.requireNonNull(config.getWsURI()))) {
                    @Override
                    public void onOpen(ServerHandshake serverHandshake) {
                        LOGGER.info("Connected to server!");
                    }

                    @Override
                    public void onMessage(String s) {
                        LOGGER.info("Message: " + s);
                    }

                    @Override
                    public void onClose(int i, String s, boolean b) {
                        LOGGER.info("Disconnected from server!");
                    }

                    @Override
                    public void onError(Exception e) {
                        LOGGER.warn("ERROR: \n" + e);
                    }
                };
                webSocketClient.connectBlocking();
                webSocketClient.send("{\"server\":\"" + serverID + "\",\"user\": \"" + id + "\"}");
                player.networkHandler.disconnect(Text.literal("You have been verified! Welcome to the server! :)"));

                webSocketClient.close();
            } catch (URISyntaxException | InterruptedException e) {
                LOGGER.warn("Cannot Connect to DB or WS!");
                throw new RuntimeException(e);
            }
            return 1;
        })).executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                Werify.LOGGER.info("Only player can preform this action!");
                return 0;
            }
            player.sendMessage(Text.literal("Please provide the password you where sent in Discord!"));
            return 0;
        })));
    }
}