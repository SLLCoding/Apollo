package net.apolloclient.module.impl.util;

import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.enums.ProductType;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import net.apolloclient.Apollo;
import net.apolloclient.event.bus.SubscribeEvent;
import net.apolloclient.event.impl.client.input.KeyPressedEvent;
import net.apolloclient.event.impl.hud.GuiSwitchEvent;
import net.apolloclient.module.bus.Module;
import net.apolloclient.module.bus.event.InitializationEvent;
import net.apolloclient.utils.ResizableDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.apache.hc.core5.http.ParseException;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
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
    private static final SpotifyApi api =
            SpotifyApi.builder().setClientId(CLIENT_ID).setRedirectUri(REDIRECT_URI).build();
    private static final AuthorizationCodeUriRequest authorizationCodeUriRequest =
            api.authorizationCodePKCEUri(CHALLENGE)
                    .scope(
                            "user-read-playback-state user-read-currently-playing user-modify-playback-state streaming user-read-private")
                    .build();
    protected static String previousSong;
    protected static Integer percentage;
    protected static Track currentlyPlaying;
    protected static ResourceLocation coverImage;
    protected static BufferedImage coverImageBuffer;
    protected static Boolean playing;
    private static boolean ready = false;
    private static boolean hasPremium = false;

    @Module.EventHandler
    public void setup(InitializationEvent event) {
        if (event.module.getName().equals(Spotify.NAME)) {
            Apollo.log("[Spotify] Starting up.");
            Apollo.EVENT_BUS.register(this);
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(42069), 0);
                server.createContext(
                        "/",
                        httpExchange -> {
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
                // TODO: Change so they have to link with a button and save the refresh token to use when
                // the client is re-launched.
                Desktop.getDesktop().browse(authorizationCodeUriRequest.execute());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupApi(String code) {
        try {
            AuthorizationCodeCredentials credentials =
                    api.authorizationCodePKCE(code, CODE_VERIFIER).build().execute();
            api.setAccessToken(credentials.getAccessToken());
            api.setRefreshToken(credentials.getRefreshToken());

            int time = credentials.getExpiresIn() - 30;
            new Thread("Spotify Token Renewer") {
                @Override
                public void run() {
                    while (true) {
                        try {
                            TimeUnit.SECONDS.sleep(time);
                            AuthorizationCodeCredentials credentials1 =
                                    api.authorizationCodePKCERefresh().build().execute();
                            api.setAccessToken(credentials1.getAccessToken());
                            api.setRefreshToken(credentials1.getRefreshToken());
                        } catch (InterruptedException
                                | ParseException
                                | SpotifyWebApiException
                                | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            ready = true;

            Apollo.log(
                    "[Spotify] Logged in as: "
                            + api.getCurrentUsersProfile().build().execute().getDisplayName());

            hasPremium =
                    api.getCurrentUsersProfile().build().execute().getProduct().equals(ProductType.PREMIUM);
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
        if (ready) {
            update();
        }
    }

    private static void update() {
        new Thread("Spotify Updater Thread") {
            @Override
            public void run() {
                try {
                    CurrentlyPlayingContext context =
                            api.getInformationAboutUsersCurrentPlayback().build().execute();
                    if (context != null) currentlyPlaying = (Track) context.getItem();

                    if (context != null && currentlyPlaying != null) {
                        int pt = context.getProgress_ms();
                        int length = currentlyPlaying.getDurationMs();
                        double divided = (double) pt / length;
                        double p = divided * 100;
                        percentage = (int) p;
                        if (previousSong == null) previousSong = "";
                        if (!previousSong.equals(currentlyPlaying.getId())) {
                            coverImageBuffer =
                                    ImageIO.read(new URL(currentlyPlaying.getAlbum().getImages()[0].getUrl()));
                            Minecraft.getMinecraft()
                                    .addScheduledTask(
                                            () -> {
                                                DynamicTexture dynamicTexture = new DynamicTexture(coverImageBuffer);
                                                Apollo.log("EE: " + coverImageBuffer.getWidth());
                                                coverImage =
                                                        Minecraft.getMinecraft()
                                                                .getTextureManager()
                                                                .getDynamicTextureLocation("cover.jpg", dynamicTexture);
                                            });
                            previousSong = currentlyPlaying.getId();
                        }

                        playing = context.getIs_playing();
                    } else {
                        percentage = null;
                        coverImageBuffer = null;
                        coverImage = null;
                        previousSong = "";
                        playing = null;
                    }
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @SubscribeEvent
    public void onKeyPress(KeyPressedEvent event) {
        if (ready && currentlyPlaying != null) {
            if (event.keyCode == Keyboard.KEY_H && Minecraft.getMinecraft().currentScreen == null) {
                Minecraft.getMinecraft().displayGuiScreen(new SpotifyGui());
            }
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    public static class SpotifyGui extends GuiScreen {

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            super.drawScreen(mouseX, mouseY, partialTicks);
            if (coverImage != null && coverImageBuffer != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(coverImage);
                drawModalRectWithCustomSizedTexture(10, 10, 0, 0, 64, 64, 64, 64);
            }
            if (percentage != null) {
                int max = 300;
                int wayThroughPixels = (int) (300 * ((double) percentage / 100));
                this.drawGradientRect(
                        85, 58, 85 + max, 58 + 10, Color.WHITE.getRGB(), Color.WHITE.getRGB());
                this.drawGradientRect(
                        85, 58, 85 + wayThroughPixels, 58 + 10, Color.BLACK.getRGB(), Color.BLACK.getRGB());
            }
            if (currentlyPlaying != null) {
                this.drawString(
                        Minecraft.getMinecraft().fontRendererObj,
                        currentlyPlaying.getName(),
                        85,
                        35,
                        Color.WHITE.getRGB());
                String authorList;
                if (currentlyPlaying.getArtists().length == 1) {
                    authorList = currentlyPlaying.getArtists()[0].getName();
                } else if (currentlyPlaying.getArtists().length == 2) {
                    authorList =
                            currentlyPlaying.getArtists()[0].getName()
                                    + " and "
                                    + currentlyPlaying.getArtists()[1].getName();
                } else {
                    StringBuilder authors = new StringBuilder();
                    int index = 0;
                    for (ArtistSimplified author : currentlyPlaying.getArtists()) {
                        if (index == currentlyPlaying.getArtists().length - 1) {
                            authors.append(" and ").append(author.getName());
                        } else if (index == currentlyPlaying.getArtists().length - 2) {
                            authors.append(author.getName());
                        } else {
                            authors.append(author.getName()).append(", ");
                        }
                        index++;
                    }
                    authorList = authors.toString();
                }
                this.drawString(
                        Minecraft.getMinecraft().fontRendererObj,
                        "By " + authorList,
                        90,
                        45,
                        Color.WHITE.getRGB());
                if (mouseX < 385 && mouseX > 85 && mouseY < 68 && mouseY > 58) {
                    int where = mouseX - 85;
                    int percentage = (int) (((double) where / 300) * 100);
                    int durationMs = currentlyPlaying.getDurationMs();
                    int wayThrough = (int) (((double) percentage / 100) * durationMs);
                    List<String> text = new ArrayList<>();
                    if (durationMs >= 3600000) {
                        text.add(
                                String.format(
                                        "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(wayThrough),
                                        TimeUnit.MILLISECONDS.toMinutes(wayThrough)
                                                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(wayThrough)),
                                        TimeUnit.MILLISECONDS.toSeconds(wayThrough)
                                                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(wayThrough))));
                    } else {
                        text.add(
                                String.format(
                                        "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes(wayThrough)
                                                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(wayThrough)),
                                        TimeUnit.MILLISECONDS.toSeconds(wayThrough)
                                                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(wayThrough))));
                    }
                    this.drawHoveringText(text, mouseX, mouseY);
                }
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            boolean hasAlreadyUpdated = false;
            if (playing != null) {
                if (mouseX < 385 && mouseX > 85 && mouseY < 68 && mouseY > 58) {
                    if (hasPremium) {
                        /*try {
                            if (playing) {
                                api.startResumeUsersPlayback().build().execute();
                            } else {
                                api.pauseUsersPlayback().build().execute();
                            }
                        } catch (SpotifyWebApiException | ParseException e) {
                            e.printStackTrace();
                        }*/
                        new Thread("Seek Position on Track") {
                            @Override
                            public void run() {
                                int where = mouseX - 85;
                                percentage = (int) (((double) where / 300) * 100);
                                int durationMs = currentlyPlaying.getDurationMs();
                                int wayThrough = (int) (((double) percentage / 100) * durationMs);
                                Apollo.log(wayThrough + "");
                                try {
                                    api.seekToPositionInCurrentlyPlayingTrack(wayThrough).build().execute();
                                } catch (IOException | SpotifyWebApiException | ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                        hasAlreadyUpdated = true;
                    }
                }
            }
            if (!hasAlreadyUpdated)
                update();
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void handleComponentHover(
                IChatComponent p_175272_1_, int p_175272_2_, int p_175272_3_) {
            super.handleComponentHover(p_175272_1_, p_175272_2_, p_175272_3_);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
