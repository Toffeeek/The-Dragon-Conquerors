package com.github.thedragonconquerors;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.assets.AssetService;
import com.shared.shared.model.Packet;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game
{
    public static final float WORLD_WIDTH = 20f;
    public static final float WORLD_HEIGHT = 12f;
    public static final float UNIT_SCALE = 1f/16f;

    @Getter
    private Batch batch;
    @Getter
    private OrthographicCamera camera;
    @Getter
    private Viewport viewport;
    @Getter
    private AssetService assetService;
    @Getter
    private NetworkClient networkClient;
    private Packet latestPacketFromServer;

    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();

    @Override
    public void create()
    {

        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        this.assetService = new AssetService(new InternalFileHandleResolver());

        setupNetworkClient();

        addScreen(new FirstScreen(this));
        setScreen(FirstScreen.class);
    }

    private void setupNetworkClient()
    {
        try
        {
            this.networkClient = new NetworkClient("ws://localhost:8080/ws");
            this.networkClient.setPacketHandler(packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));
            this.networkClient.connect();
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to connect to server", e);
        }
    }

    private void exampleFunction()
    {
        Packet examplePacket = new Packet();
        networkClient.send(examplePacket);

        /*
         * Receiving does not happen immediately here.
         *
         * networkClient.send(examplePacket) sends a packet to the server and returns right away.
         * Later, when the server broadcasts/replies, NetworkClient receives that packet inside
         * its STOMP subscription and calls Main.handlePacket(packet).
         *
         * So the received packet is accepted in handlePacket(...), not returned from send(...).
         */
        Packet alreadyReceivedPacket = latestPacketFromServer;
        if(alreadyReceivedPacket != null)
        {
            System.out.println("Last packet received from server: " + alreadyReceivedPacket.getAction());
        }
    }

    private void handlePacket(Packet packet)
    {
        latestPacketFromServer = packet;

        switch(packet.getAction())
        {
            case PRIVATE_JOIN_CONFIRMATION:
                System.out.println("My player ID is " + packet.getID());
                break;
            case PLAYER_COORDINATE:
            case JOIN:
            case MOVE:
            case LEAVE:
                System.out.println("Game packet received: " + packet.getAction());
                break;
            default:
                break;
        }
    }




    @Override
    public void resize(int width, int height){      //ensures that if the size of the window changes, it does no distort the overall rendering
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    public void addScreen(Screen screen){
        screenCache.put(screen.getClass(), screen);
    }

    public void setScreen(Class<? extends Screen> screenClass){
        Screen screen = screenCache.get(screenClass);
        if(screen == null)  throw new GdxRuntimeException("No screen with class " + screenClass + " found");

        super.setScreen(screen);
    }

    @Override
    public void dispose(){
        screenCache.values().forEach(Screen::dispose);
        screenCache.clear();

        this.batch.dispose();
        this.assetService.debugDiagnostic();
        this.assetService.dispose();
    }

}
