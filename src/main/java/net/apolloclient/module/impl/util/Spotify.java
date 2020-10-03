package net.apolloclient.module.impl.util;

import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import net.apolloclient.Apollo;
import net.apolloclient.event.bus.SubscribeEvent;
import net.apolloclient.event.impl.hud.GuiSwitchEvent;
import net.apolloclient.module.bus.Module;
import net.apolloclient.module.bus.event.InitializationEvent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.apache.hc.core5.http.ParseException;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Module(name = Spotify.NAME, description = Spotify.DESCRIPTION, author = Spotify.AUTHOR)
public class Spotify {

    public static final String NAME = "Spotify";
    public static final String DESCRIPTION = "See what you are listening to in-game.";
    public static final String AUTHOR = "SLLCoding";

    private static final String CLIENT_ID = "e7ae558508d046d3bd9d3fdee91229cd";
    private static final URI REDIRECT_URI = SpotifyHttpManager.makeUri("http://localhost:42069");
    private static final String CHALLENGE = "w6iZIj99vHGtEx_NVl9u3sthTN646vvkiP8OMCGfPmo";
    private static final String CODE_VERIFIER = "NlJx4kD4opk4HY7zBM6WfUHxX7HoF8A2TUhOIPGA74w";

    private static boolean ready = false;

    private static final SpotifyApi api = SpotifyApi.builder()
            .setClientId(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .build();
    private static final AuthorizationCodeUriRequest authorizationCodeUriRequest = api.authorizationCodePKCEUri(CHALLENGE)
            .scope("user-read-playback-state user-read-currently-playing user-modify-playback-state streaming user-read-private")
            .build();

    @Module.EventHandler
    public void setup(InitializationEvent event) {
        if (event.module.getName().equals(Spotify.NAME)) {
            Apollo.log("[Spotify] Starting up.");
            Apollo.EVENT_BUS.register(this);
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(42069), 0);
                server.createContext("/", httpExchange -> {
                    httpExchange.getResponseHeaders().set("Location", "https://apolloclient.net");
                    httpExchange.sendResponseHeaders(302, 0);
                    String code = queryToMap(httpExchange.getRequestURI().getQuery()).get("code");
                    if (code != null) {
                        server.stop(0);
                        setupApi(code);
                    }
                });
                server.start();
                Apollo.log("[Spotify] Opening authentication in your browser.");
                Desktop.getDesktop().browse(authorizationCodeUriRequest.execute());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupApi(String code) {
        try {
            AuthorizationCodeCredentials credentials = api.authorizationCodePKCE(code, CODE_VERIFIER).build().execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());

            int time = credentials.getExpiresIn() - 30;
            new Thread("Spotify Token Renewer") {
                @Override
                public void run() {
                    while (true) {
                        try {
                            TimeUnit.SECONDS.sleep(time);
                            AuthorizationCodeCredentials credentials1 = api.authorizationCodePKCERefresh().build().execute();
                            api.setAccessToken(credentials1.getAccessToken());
                            api.setRefreshToken(credentials1.getRefreshToken());
                        } catch (InterruptedException | ParseException | SpotifyWebApiException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            ready = true;

            Apollo.log("[Spotify] Logged in as: " + api.getCurrentUsersProfile().build().execute().getDisplayName());
            CurrentlyPlayingContext currentlyPlaying = api.getInformationAboutUsersCurrentPlayback().build().execute();
            Apollo.log("[Spotify] Currently Playing: " + ((Track) currentlyPlaying.getItem()).getName());
            Track song = (Track) currentlyPlaying.getItem();
            int pt = currentlyPlaying.getProgress_ms();
            int length = song.getDurationMs();
            double divided = (double) pt / length;
            double p = divided * 100;
            int percentage = (int) p;
            Apollo.log("[Spotify] Current Playthrough: " + currentlyPlaying.getProgress_ms() + "ms");
            Apollo.log(percentage + "%");
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onGuiSwitch(GuiSwitchEvent event) {
        if (api.getAccessToken() == null && event.guiScreen instanceof GuiMainMenu) {
            try {
                // TODO: Fix or get someone to Mixin it.
                Field buttonListField = GuiScreen.class.getDeclaredField("buttonList");
                buttonListField.setAccessible(true);
                List<GuiButton> buttonList = (List<GuiButton>) buttonListField.get(event.guiScreen);
                buttonList.add(new GuiButton(69, 10, 10, 50, 20, "Setup Spotify"));
                buttonListField.set(event.guiScreen, buttonList);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

}
