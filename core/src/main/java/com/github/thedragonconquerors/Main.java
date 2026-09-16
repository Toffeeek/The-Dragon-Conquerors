// File Location: core/src/main/java/com/github/thedragonconquerors/Main.java
package com.github.thedragonconquerors;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.client.client.NetworkClient;
import com.github.thedragonconquerors.assets.AssetService;
import com.shared.shared.model.CharacterBuild;
import com.shared.shared.model.Packet;
import com.shared.shared.model.world.Environment;
import com.shared.shared.network.MatchState;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 *  The first class made after the application is launched. Acts as the entry point.
 * */
public class Main extends Game
{
    public static final String DEFAULT_SERVER_URL = "ws://localhost:8080/ws";
    public static final int SERVER_PORT = 8080;
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
    private float profileTitleTimer;
    private com.github.thedragonconquerors.rendering.ConnectionOverlay connectionOverlay;
    private volatile boolean recoveringConnection;
    private Process serverProcess;
    private Thread serverShutdownHook;
    private int localServerPort = SERVER_PORT;
    @Getter
    private String hostedJoinUrl = DEFAULT_SERVER_URL;

    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();

    /**
     *  LibGDX automatically calls this function when Main() is called in the Lwjgl3Launcher.
     *  This function calls FirstScreen which acts as the game screen. In the future, this will be the
     *  lobby or character creation screen of the game instead of being taken directly to the game
     */
    @Override
    public void create()
    {

        boolean profiling = Boolean.getBoolean("tdc.profile");
        Gdx.app.setLogLevel(profiling ? Application.LOG_DEBUG : Application.LOG_INFO);
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        this.assetService = new AssetService(new InternalFileHandleResolver());
        this.connectionOverlay = new com.github.thedragonconquerors.rendering.ConnectionOverlay(this::retryConnection, this::returnToMenu);
        if (profiling) {
            this.glProfiler = new GLProfiler(Gdx.graphics);
            this.glProfiler.enable();
        }
        Gdx.graphics.setTitle("The Dragon Conquerors");

        serverShutdownHook = new Thread(this::stopLocalServer, "tdc-server-shutdown");
        Runtime.getRuntime().addShutdownHook(serverShutdownHook);

        addScreen(new MenuScreen(this));
        setScreen(MenuScreen.class);
    }

    /**
     * Sets up the means to communicate with the server via the networkClient object
     */
    public NetworkClient connectToServer(String url) throws Exception
    {
        NetworkClient client = new NetworkClient(url);
        client.setPacketHandler(packet -> Gdx.app.postRunnable(() -> handlePacket(packet)));
        client.connect();
        return client;
    }

    public void startLobby(NetworkClient networkClient, String joinUrl)
    {
        this.networkClient = networkClient;
        this.hostedJoinUrl = joinUrl;
        addScreen(new LobbyScreen(this, joinUrl));
        setScreen(LobbyScreen.class);
    }

    public void startGame(int teamIndex, CharacterBuild chosenBuild,
                          Environment environment, int localPlayerId,
                          List<Packet> roster, MatchState initialState)
    {
        addScreen(new GameOneScreen(this, teamIndex, chosenBuild, environment,
            localPlayerId, roster, initialState));
        setScreen(GameOneScreen.class);
    }

    public void showPostMatch(int teamIndex, CharacterBuild chosenBuild,
                              Environment environment, int localPlayerId,
                              MatchState finalState)
    {
        addScreen(new PostMatchScreen(this, teamIndex, chosenBuild, environment,
            localPlayerId, finalState));
        setScreen(PostMatchScreen.class);
    }

    /** Disconnects the current client, stops a locally hosted server, and returns to the cached menu. */
    public void returnToMenu()
    {
        connectionOverlay.hide();
        if(networkClient != null)
        {
            try
            {
                networkClient.disconnect();
            }
            finally
            {
                networkClient = null;
            }
        }

        stopLocalServer();
        hostedJoinUrl = DEFAULT_SERVER_URL;
        setScreen(MenuScreen.class);
    }

    public boolean startLocalServer() throws IOException
    {
        return startLocalServer(false);
    }

    public boolean startLocalServer(boolean practice) throws IOException
    {
        if(serverProcess != null && serverProcess.isAlive()) return false;

        // Extract the bundled companion, so hosting works outside the source checkout.
        if (Runtime.version().feature() < 21) throw new IOException("Hosting requires Java 21+. Use the bundled game runtime.");
        String javaName = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        File javaExecutable = new File(System.getProperty("java.home"), "bin/" + javaName);
        java.nio.file.Path serverJar = java.nio.file.Files.createTempFile("tdc-server-", ".jar");
        serverJar.toFile().deleteOnExit();
        try (var bundled = Main.class.getResourceAsStream("/server/tdc-server.jar")) {
            if (bundled == null) throw new IOException("Bundled server is missing. Rebuild or reinstall the complete game package.");
            java.nio.file.Files.copy(bundled, serverJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        localServerPort = findAvailablePort();

        ProcessBuilder processBuilder = new ProcessBuilder(
            javaExecutable.getAbsolutePath(),
            "-jar", serverJar.toAbsolutePath().toString(),
            "--server.port=" + localServerPort,
            "--server.address=" + (practice ? "127.0.0.1" : "0.0.0.0"),
            "--game.testing-mode=" + practice
        );
        processBuilder.redirectErrorStream(true);
        serverProcess = processBuilder.start();

        Process startedProcess = serverProcess;
        Thread logThread = new Thread(() -> consumeServerOutput(startedProcess), "tdc-server-output");
        logThread.setDaemon(true);
        logThread.start();
        return true;
    }

    public boolean waitForLocalServer(Duration timeout)
    {
        long deadline = System.nanoTime() + timeout.toNanos();
        while(System.nanoTime() < deadline)
        {
            if(serverProcess != null && !serverProcess.isAlive()) return false;

            try(Socket socket = new Socket())
            {
                socket.connect(new InetSocketAddress("localhost", localServerPort), 250);
                return true;
            }
            catch(IOException ignored)
            {
                try
                {
                    Thread.sleep(250L);
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    public void stopLocalServer()
    {
        Process process = serverProcess;
        if(process == null) return;

        ProcessHandle processHandle = process.toHandle();
        processHandle.descendants()
            .sorted(Comparator.comparing(ProcessHandle::pid).reversed())
            .forEach(ProcessHandle::destroy);
        processHandle.destroy();

        waitForExit(process, Duration.ofSeconds(3));

        processHandle.descendants()
            .sorted(Comparator.comparing(ProcessHandle::pid).reversed())
            .forEach(ProcessHandle::destroyForcibly);
        if(processHandle.isAlive()) processHandle.destroyForcibly();

        waitForExit(process, Duration.ofSeconds(2));
        serverProcess = null;
        localServerPort = SERVER_PORT;
        hostedJoinUrl = DEFAULT_SERVER_URL;
    }

    private void waitForExit(Process process, Duration timeout)
    {
        try
        {
            process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public String getLocalJoinUrl()
    {
        return getLanAddress()
            .map(address -> "ws://" + address + ":" + localServerPort + "/ws")
            .orElse(getLocalServerUrl());
    }

    public String getLocalServerUrl()
    {
        return "ws://localhost:" + localServerPort + "/ws";
    }

    private int findAvailablePort() throws IOException
    {
        try(ServerSocket socket = new ServerSocket(SERVER_PORT))
        {
            socket.setReuseAddress(true);
            return SERVER_PORT;
        }
        catch(IOException ignored)
        {
            // Fall back to a random free port when the default server port is in use.
        }

        try(ServerSocket socket = new ServerSocket(0))
        {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private Optional<String> getLanAddress()
    {
        try
        {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = interfaces.nextElement();
                if(!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) continue;

                var addresses = networkInterface.getInetAddresses();
                while(addresses.hasMoreElements())
                {
                    var address = addresses.nextElement();
                    if(address instanceof Inet4Address && !address.isLoopbackAddress())
                    {
                        return Optional.of(address.getHostAddress());
                    }
                }
            }
        }
        catch(SocketException e)
        {
            System.out.println("Could not detect LAN address: " + e.getMessage());
        }

        return Optional.empty();
    }

    private void consumeServerOutput(Process process)
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            String line;
            while((line = reader.readLine()) != null)
            {
                System.out.println("[Server] " + line);
            }
        }
        catch(IOException e)
        {
            System.out.println("[Server] Output reader stopped: " + e.getMessage());
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
        Screen previous = screenCache.put(screen.getClass(), screen);
        if(previous != null && previous != screen)
        {
            previous.dispose();
        }
    }

    public void setScreen(Class<? extends Screen> screenClass){
        Screen screen = screenCache.get(screenClass);
        if(screen == null)  throw new GdxRuntimeException("No screen with class " + screenClass + " found");

        super.setScreen(screen);
    }

    @Override
    public void render()
    {
        if (glProfiler != null) glProfiler.reset();
        super.render();
        if (networkClient != null) {
            networkClient.pollTimeout();
            var state = networkClient.getState();
            if (state == NetworkClient.State.FAILED || state == NetworkClient.State.RECOVERING) {
                connectionOverlay.render(networkClient.getFailure(), recoveringConnection || state == NetworkClient.State.RECOVERING,
                    Gdx.graphics.getDeltaTime());
            } else connectionOverlay.hide();
        } else connectionOverlay.hide();
        if (glProfiler != null) {
            profileTitleTimer += Gdx.graphics.getDeltaTime();
            if (profileTitleTimer >= 1f) {
                Gdx.graphics.setTitle("The Dragon Conquerors - FPS: " + Gdx.graphics.getFramesPerSecond()
                    + " | Draw calls: " + glProfiler.getDrawCalls());
                profileTitleTimer = 0f;
            }
        }
    }



    @Override
    public void dispose(){
        connectionOverlay.dispose();
        screenCache.values().forEach(Screen::dispose);
        screenCache.clear();

        if(networkClient != null) networkClient.disconnect();
        stopLocalServer();
        removeServerShutdownHook();
        this.batch.dispose();
        if (glProfiler != null) {
            this.assetService.debugDiagnostic();
            glProfiler.disable();
        }
        this.assetService.dispose();
    }

    private void removeServerShutdownHook()
    {
        if(serverShutdownHook == null) return;

        try
        {
            Runtime.getRuntime().removeShutdownHook(serverShutdownHook);
        }
        catch(IllegalStateException ignored)
        {
            // JVM shutdown already started; the hook will run normally.
        }
    }

    private void retryConnection() {
        NetworkClient client = networkClient;
        if (client == null || recoveringConnection) return;
        recoveringConnection = true;
        Thread worker = new Thread(() -> {
            try { client.recover(); } catch (Exception ignored) { /* NetworkClient exposes the failure to the overlay. */ }
            finally { recoveringConnection = false; }
        }, "tdc-reconnect");
        worker.setDaemon(true); worker.start();
    }

}
