package com.github.thedragonconquerors;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.assets.AssetService;
import com.shared.shared.model.Packet;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 *  The first class made after the application is launched. Acts as the entry point.
 * */
public class Main extends Game
{
    public static final float WORLD_WIDTH = 30f;
    public static final float WORLD_HEIGHT = 17f;
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

    private GLProfiler glProfiler;
    private FPSLogger fpsLogger;

    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();

    /**
     *  LibGDX automatically calls this function when Main() is called in the Lwjgl3Launcher.
     *  This function calls FirstScreen which acts as the game screen. In the future, this will be the
     *  lobby or character creation screen of the game instead of being taken directly to the game
     */
    @Override
    public void create()
    {

        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        this.assetService = new AssetService(new InternalFileHandleResolver());
        this.glProfiler = new GLProfiler(Gdx.graphics);
        this.glProfiler.enable();
        this.fpsLogger = new FPSLogger();

        setupNetworkClient();

        addScreen(new GameOneScreen(this));
        setScreen(GameOneScreen.class);
    }

    /**
     * Sets up the means to communicate with the server via the networkClient object
     */
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


    /**
     * VERY IMPORTANT: This function is automatically called by the application whenever a packet
     * arrives from the server. The packet from the server is arrived in the form of the function parameter.
     * Handle the packet from the server based on the 'action' field of the packet.
     */
    private void handlePacket(Packet packet)
    {
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
    public void render()
    {
        glProfiler.reset();
        super.render();
        Gdx.graphics.setTitle("TDC - Draw Calls: " + glProfiler.getDrawCalls());
        fpsLogger.log();
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
