package dbg;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JPanel;

@Mod.EventBusSubscriber
public class ChunkProviderDebug {

    public static ChunkProviderDebug instance = new ChunkProviderDebug();

    private static Thread tickThread;
    private static Thread serverInitThread;
    private static Thread clientInitThread;
    private JFrame frame;
    private JPanel canvas;
    private static final MethodHandle get_id2ChunkMap_server = getFieldGetterHandle(ChunkProviderServer.class, "field_73244_f");
    private static final MethodHandle get_chunkMapping_client = getFieldGetterHandle(ChunkProviderClient.class, "field_73236_b");
    private static final MethodHandle get_internalMapArray = getFieldGetterHandle(Long2ObjectOpenHashMap.class, "value");

    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        // TODO: is needed?
        init();
        if (!event.getWorld().isRemote)
            instance.onLoad(event.getChunk().x, event.getChunk().z);
    }

    public static MethodHandle getFieldGetterHandle(Class<?> owner, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);
        Field field = getFieldFromSrg(owner, name);
        try {
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException e) {
            //if it happens - eighter something has gone horribly wrong or the JVM is blocking access
            throw new Error(e);
        }
    }

    public static MethodHandle getFieldSetterHandle(Class<?> owner, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);
        Field field = getFieldFromSrg(owner, name);
        try {
            return MethodHandles.lookup().unreflectSetter(field);
        } catch (IllegalAccessException e) {
            //if it happens - eighter something has gone horribly wrong or the JVM is blocking access
            throw new Error(e);
        }
    }


    private static Field getFieldFromSrg(Class<?> owner, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);

        Field foundField = findFieldByName(owner, name);
        foundField.setAccessible(true);
        return foundField;
    }

    private static Field findFieldByName(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return findFieldByName(owner.getSuperclass(), name);
        }
    }

    private static final int SIZE = 2;
    private static final int width = 1024, height = 768;
    private volatile World theWorldServer;
    private volatile World theWorldClient;
    private Map<ChunkPos, Exception> lastColumnLoadCause = new HashMap<>();

    //optionally call this when column is loaded to get debug information from where it was first loaded when clicking on that column
    public void onLoad(int x, int z) {
        lastColumnLoadCause.put(new ChunkPos(x, z), new Exception());
    }

    //call it from using evaluate expression once in main menu or ingame (not earlier to avoid classloading issues)
    public static void init() {
        if (serverInitThread == null) {
            startServerAutoInit();
        }
        if (clientInitThread == null) {
            startClientAutoInit();
        }
        if (tickThread == null) {
            startTickThread();
        }
    }

    private static void startTickThread() {
        tickThread = new Thread("CubeProviderDebug tick thread") {
            @Override public void run() {
                while (true) {
                    instance.theWorldClient = Minecraft.getMinecraft().world;
                    if (instance.theWorldServer != null &&
                            !instance.theWorldServer.getMinecraftServer().isServerRunning()) {
                        instance.theWorldServer = null;
                    }
                    instance.tick();
                    delay(100);
                }
            }
        };
        tickThread.start();
    }

    private static void startServerAutoInit() {
        serverInitThread = new Thread("CubeProviderDebug server autoinit thread") {
            @Override
            public void run() {
                while (true) {
                    MinecraftServer server = getMinecraftServer();
                    WorldServer world = getWorldFromServer(server);
                    instance.theWorldServer = world;

                    while (instance.theWorldServer != null) {
                        delay(1000);
                    }
                }
            }

            private WorldServer getWorldFromServer(MinecraftServer server) {
                while ((!server.isServerRunning() || server.worlds.length < 1) && !server.isServerStopped()) {
                    delay(1000);
                }
                if (server.isServerStopped()) {
                    return null;
                }
                WorldServer vanillaWorld;

                while ((vanillaWorld = (WorldServer) server.getEntityWorld()) == null && !server.isServerStopped()) {
                    delay(100);
                }
                return vanillaWorld;
            }

            @Nonnull private MinecraftServer getMinecraftServer() {
                MinecraftServer server;
                while ((server = FMLCommonHandler.instance().getMinecraftServerInstance()) == null) {
                    delay(100);
                }
                return server;
            }
        };
        serverInitThread.start();
    }

    private static void startClientAutoInit() {
        clientInitThread = new Thread("CubeProviderDebug client autoinit thread") {
            @Override
            public void run() {
                while (true) {
                    Minecraft server = getMinecraft();
                    WorldClient world = getClientWorld(server);

                    instance.theWorldClient = world;

                    while (instance.theWorldClient != null) {
                        delay(100);
                    }
                }
            }

            @Nonnull private WorldClient getClientWorld(Minecraft client) {
                while (!client.isIntegratedServerRunning() || client.world == null) {
                    delay(100);
                }
                WorldClient vanillaWorld;

                while ((vanillaWorld = client.world) == null) {
                    delay(100);
                }
                return vanillaWorld;
            }

            @Nonnull private Minecraft getMinecraft() {
                Minecraft mc;
                while ((mc = FMLClientHandler.instance().getClient()) == null) {
                    delay(100);
                }
                return mc;
            }
        };
        clientInitThread.start();
    }

    private static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ChunkProviderDebug() {
        EventQueue.invokeLater(() -> {
            frame = new JFrame("CubeProviderServer - loaded chunks");
            frame.setSize(width, height);
            frame.setLayout(new CardLayout());
            frame.setVisible(true);
            frame.setResizable(false);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent windowEvent) {
                    frame.dispose();
                    if (tickThread != null) {
                        tickThread.stop();
                    }
                }
            });

            canvas = new MCanvas();
            canvas.setSize(width, height);

            canvas.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    //canvasPos = wh/2 - SIZE/2 + (column-center)*SIZE
                    //(canvasPos + SIZE/2 - wh/2)/SIZE + center= column
                    int canvasX = mouseEvent.getX();
                    int canvasZ = mouseEvent.getY();
                    int columnX = -(canvasX + SIZE/2 - width/2)/SIZE + MCanvas.serverCenterX;
                    int columnZ = -(canvasZ + SIZE/2 - height/2)/SIZE + MCanvas.serverCenterZ;
                    ChunkPos pos = new ChunkPos(columnX, columnZ);
                    Exception e = lastColumnLoadCause.get(pos);
                    if (e != null) {
                        System.err.println("Column at " + columnX + ", " + columnZ);
                        e.printStackTrace();
                    }
                }
            });

            frame.add(canvas);
        });
    }

    public void tick() {
        if (this.canvas != null) {
            this.canvas.repaint();
        }
    }

    private static class MCanvas extends JPanel {

        static int serverCenterX, clientCenterX;
        static int serverCenterZ, clientCenterZ;

        @Override
        public void paintComponent(Graphics g) {
            EntityPlayer serverPlayer = getServerPlayer();

            int chunkX_MP = serverPlayer == null ? Integer.MAX_VALUE : blockToCube(serverPlayer.posX);
            int chunkZ_MP = serverPlayer == null ? Integer.MAX_VALUE : blockToCube(serverPlayer.posZ);

            this.setServerCenter(chunkX_MP, chunkZ_MP);

            EntityPlayer clientPlayer = getClientPlayer();

            int chunkX_SP = clientPlayer == null ? Integer.MAX_VALUE : blockToCube(clientPlayer.posX);
            int chunkZ_SP = clientPlayer == null ? Integer.MAX_VALUE : blockToCube(clientPlayer.posZ);

            this.setClientCenter(chunkX_SP, chunkZ_SP);

            g.clearRect(0, 0, width, height);

            Collection<ChunkInfo> chunks = findChunks();

            this.drawChunks(g, chunks);
        }

        private void drawChunks(Graphics g, Collection<ChunkInfo> chunks) {
            for (ChunkInfo info : chunks) {
                drawColumn(g, info);
            }
        }

        private Collection<ChunkInfo> findChunks() {
            Map<ChunkPos, ChunkInfo> chunks = new HashMap<>();
            if (instance.theWorldServer != null) {
                addServerChunks(chunks);
            }
            if (instance.theWorldClient != null) {
                addClientChunks(chunks);
            }
            return chunks.values();
        }

        private void addClientChunks(Map<ChunkPos, ChunkInfo> chunks) {
            try {
                Long2ObjectMap<Chunk> id2ChunkMap =
                        (Long2ObjectMap<Chunk>) get_chunkMapping_client.invoke(instance.theWorldClient.getChunkProvider());
                Object[] arr = (Object[]) get_internalMapArray.invoke(id2ChunkMap);
                for (Object obj : arr) {
                    Chunk chunk = (Chunk) obj;
                    if (chunk == null) {
                        continue;
                    }
                    ChunkPos pos = new ChunkPos(chunk.x, chunk.z);
                    if (!chunks.containsKey(pos)) {
                        chunks.put(pos, new ChunkInfo());
                    }
                    ChunkInfo info = chunks.get(pos);
                    info.chunkX = pos.x;
                    info.chunkZ = pos.z;
                    info.client = true;
                }
                ChunkPos playerPos = new ChunkPos(clientCenterX, clientCenterZ);
                if (!chunks.containsKey(playerPos)) {
                    chunks.put(playerPos, new ChunkInfo());
                }
                ChunkInfo info = chunks.get(playerPos);
                info.isClientSpecial = true;
                info.chunkX = playerPos.x;
                info.chunkZ = playerPos.z;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        private void addServerChunks(Map<ChunkPos, ChunkInfo> chunks) {
            try {
                Long2ObjectMap<Chunk> id2ChunkMap = (Long2ObjectMap<Chunk>) get_id2ChunkMap_server.invoke(instance.theWorldServer.getChunkProvider());
                Object[] arr = (Object[]) get_internalMapArray.invoke(id2ChunkMap);
                for (Object obj : arr) {
                    Chunk chunk = (Chunk) obj;
                    if (chunk == null) {
                        continue;
                    }
                    ChunkPos pos = new ChunkPos(chunk.x, chunk.z);
                    if (!chunks.containsKey(pos)) {
                        chunks.put(pos, new ChunkInfo());
                    }
                    ChunkInfo info = chunks.get(pos);
                    info.chunkX = pos.x;
                    info.chunkZ = pos.z;
                    info.server = true;
                }
                ChunkPos playerPos = new ChunkPos(serverCenterX, serverCenterZ);
                if (!chunks.containsKey(playerPos)) {
                    chunks.put(playerPos, new ChunkInfo());
                }
                ChunkInfo info = chunks.get(playerPos);
                info.isServerSpecial = true;
                info.chunkX = playerPos.x;
                info.chunkZ = playerPos.z;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        private void setClientCenter(int chunkX_sp, int chunkZ_sp) {
            this.clientCenterX = chunkX_sp;
            this.clientCenterZ = chunkZ_sp;
        }

        private EntityPlayer getClientPlayer() {
            List<EntityPlayer> players = instance.theWorldClient == null ? null :
                    ((WorldClient) instance.theWorldClient).playerEntities;

            return players == null || players.isEmpty() ? null : players.get(0);
        }

        @Nullable private EntityPlayer getServerPlayer() {
            List<EntityPlayer> serverPlayers =
                    instance.theWorldServer == null ? null : instance.theWorldServer.playerEntities;

            return serverPlayers == null || serverPlayers.isEmpty() ? null : serverPlayers.get(0);
        }

        private void setServerCenter(int chunkX, int chunkZ) {
            this.serverCenterX = chunkX;
            this.serverCenterZ = chunkZ;
        }

        private void drawColumn(Graphics g, ChunkInfo c) {
            int centerX = this.serverCenterX == Integer.MAX_VALUE ? this.clientCenterX == Integer.MAX_VALUE ? 0 : clientCenterX : serverCenterX;
            int centerZ = this.serverCenterZ == Integer.MAX_VALUE ? this.clientCenterZ == Integer.MAX_VALUE ? 0 : clientCenterZ : serverCenterZ;
            int x = c.chunkX - centerX;
            int y = c.chunkZ - centerZ;
            Color color = c.getColor();
            g.setColor(color);
            g.fillRect(width / 2 - SIZE / 2 - x * SIZE, height / 2 - SIZE / 2 - y * SIZE, SIZE, SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(width / 2 - SIZE / 2 - x * SIZE, height / 2 - SIZE / 2 - y * SIZE, SIZE, SIZE);
            g.setColor(Color.white);
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            int tx = width / 2 - SIZE / 2 + x * SIZE + 2;
            int ty = height / 2 - y * SIZE - 5;
            int ty2 = ty + 12;
        }

        private static class ChunkInfo {

            private int chunkX, chunkZ;
            private boolean server, client, isClientSpecial, isServerSpecial;

            public Color getColor() {
                if (isClientSpecial && isServerSpecial) {
                    return new Color(255, 32, 32);
                }
                if (isServerSpecial && !isClientSpecial) {
                    return new Color(255, 150, 150);
                }
                if (isClientSpecial && !isServerSpecial) {
                    return new Color(128, 0, 0);
                }
                if (server && client) {
                    return new Color(0, 0, 255);
                }
                if (server && !client) {
                    return new Color(150, 150, 255);
                }
                if (client && !server) {
                    return new Color(0, 255, 100);
                }
                return new Color(0, 0, 0);
            }
        }
    }


    public static class Mappings {

        private static boolean IS_DEV;
        //since srg field and method names are guarranted not to collide -  we can store them in one map
        private static final Map<String, String> srgToMcp = new HashMap<>();

        static {
            String location = System.getProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp");
            IS_DEV = location != null;
            if (IS_DEV) {
                initMappings(location);
            }
        }

        public static String getNameFromSrg(String srgName) {
            if (IS_DEV) {
                String result = srgToMcp.get(srgName);
                return result == null ? srgName : result;
            }
            return srgName;
        }

        private static void initMappings(String property) {
            try (Scanner scanner = new Scanner(new File(property))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    parseLine(line);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private static void parseLine(String line) {
            if (line.startsWith("FD: ")) {
                parseField(line.substring("FD: ".length()));
            }
            if (line.startsWith("MD: ")) {
                parseMethod(line.substring("MD: ".length()));
            }
        }

        private static void parseMethod(String substring) {
            String[] s = substring.split(" ");

            final int SRG_NAME = 0/*, SRG_DESC = 1*/, MCP_NAME = 2/*, MCP_DESC = 3*/;

            int lastIndex = s[SRG_NAME].lastIndexOf('/') + 1;
            if (lastIndex < 0) {
                lastIndex = 0;
            }

            s[SRG_NAME] = s[SRG_NAME].substring(lastIndex);

            lastIndex = s[MCP_NAME].lastIndexOf("/") + 1;
            if (lastIndex < 0) {
                lastIndex = 0;
            }

            s[MCP_NAME] = s[MCP_NAME].substring(lastIndex);

            srgToMcp.put(s[SRG_NAME], s[MCP_NAME]);
        }

        private static void parseField(String str) {
            if (!str.contains(" ")) {
                return;
            }
            String[] s = str.split(" ");
            assert s.length == 2;

            int lastIndex = s[0].lastIndexOf('/') + 1;
            if (lastIndex < 0) {
                lastIndex = 0;
            }

            s[0] = s[0].substring(lastIndex);

            lastIndex = s[1].lastIndexOf("/") + 1;
            if (lastIndex < 0) {
                lastIndex = 0;
            }

            s[1] = s[1].substring(lastIndex);

            srgToMcp.put(s[0], s[1]);
        }
    }

    public static int blockToCube(int val) {
        return val >> 4;
    }

    public static int blockToCube(double blockPos) {
        return blockToCube(MathHelper.floor(blockPos));
    }

}
