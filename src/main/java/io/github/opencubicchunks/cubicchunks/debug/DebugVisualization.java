package io.github.opencubicchunks.cubicchunks.debug;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL20.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBufferData;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGenBuffers;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import com.mojang.datafixers.util.Pair;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.Long2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.WorldLoadProgressScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.Vector4f;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugVisualization {

    private static final String VERT_SHADER =
            "#version 330 core\n" +
                    "layout(location = 0) in vec3 posIn;\n" +
                    "layout(location = 1) in vec4 colorIn;\n" +
                    "smooth out vec4 fragColor;\n" +
                    "uniform mat4 mvpMatrix;\n" +
                    "void main() {\n" +
                    "  gl_Position = mvpMatrix * vec4(posIn, 1);\n" +
                    "  fragColor = colorIn;\n" +
                    "}";
    private static final String FRAG_SHADER =
            "#version 330 core\n" +
                    "smooth in vec4 fragColor;\n" +
                    "out vec4 outColor;\n" +
                    "void main() {\n" +
                    "  outColor = fragColor;\n" +
                    "}";
    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile World clientWorld;
    private static volatile Map<DimensionType, World> serverWorlds = new ConcurrentHashMap<>();
    private static AtomicBoolean initialized = new AtomicBoolean();
    private static long window;
    private static int shaderProgram;
    private static int matrixLocation;
    private static int vao;
    private static int posAttrib;
    private static int colAttrib;
    private static int glBuffer;
    private static BufferBuilder bufferBuilder;
    private static BufferBuilder perfGraphBuilder;

    private static Matrix4f projectionMatrix = new Matrix4f();
    private static Matrix4f mvpMatrix = new Matrix4f();
    private static Matrix4f inverseMatrix = new Matrix4f();
    private static PerfTimer[] perfTimer = new PerfTimer[128];
    private static int perfTimerIdx = 0;
    private static float screenWidth = 854.0f;
    private static float screenHeight = 480f;
    private static final boolean IS_LINUX = Util.getOSType() == Util.OS.LINUX && false;
    private static GLCapabilities debugGlCapabilities;

    private static PerfTimer timer() {
        if (perfTimer[perfTimerIdx] == null) {
            return perfTimer[perfTimerIdx] = new PerfTimer();
        }
        return perfTimer[perfTimerIdx];
    }


    public static void onRender(RenderWorldLastEvent evt) {
        if (IS_LINUX) {
            return;
        }
        long ctx = glfwGetCurrentContext();
        GLCapabilities capabilities = GL.getCapabilities();
        GL.setCapabilities(debugGlCapabilities);
        try {
            glfwMakeContextCurrent(window);
            render();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                return;
            }
            try {
                bufferBuilder.finishDrawing();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } finally {
            glfwMakeContextCurrent(ctx);
            GL.setCapabilities(capabilities);
        }
    }

    public static void onWorldLoad(WorldEvent.Load t) {
        if (!initialized.getAndSet(true)) {
            initializeWindow();
        }
        IWorld w = t.getWorld();
        if (w instanceof ClientWorld) {
            clientWorld = (World) w;
        } else if (w instanceof ServerWorld) {
            serverWorlds.put(w.getDimension().getType(), (World) w);
        }
    }

    public static void onWorldUnload(WorldEvent.Unload t) {
        IWorld w = t.getWorld();
        if (w instanceof ServerWorld) {
            serverWorlds.remove(w.getDimension());
        }
    }

    public static void initializeWindow() {
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(854, 480, "CubicChunks debug", 0L, 0L);
        if (window == 0L) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer monPosLeft = stack.mallocInt(1);
            IntBuffer monPosTop = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwGetMonitorPos(glfwGetPrimaryMonitor(), monPosLeft, monPosTop);
            glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2 + monPosLeft.get(0),
                    (vidmode.height() - pHeight.get(0)) / 2 + monPosTop.get(0));
        }

        if (!IS_LINUX) {
            long oldCtx = glfwGetCurrentContext();
            initWindow();
            glfwMakeContextCurrent(oldCtx);
            return;
        }

        Thread thread = new Thread(() -> {
            initWindow();
            while (true) {
                try {
                    render();
                    glfwPollEvents();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        return;
                    }
                    try {
                        bufferBuilder.finishDrawing();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void initWindow() {
        glfwShowWindow(window);
        glfwMakeContextCurrent(window);
        glfwPollEvents(); // Note: this WILL break on a mac
        glfwSetErrorCallback(GLFWErrorCallback.createPrint());
        debugGlCapabilities = GL.createCapabilities();
        initialize();
        glfwSwapBuffers(window);
    }

    private static void initialize() {
        int vert = glCreateShader(GL_VERTEX_SHADER);
        compileShader(vert, VERT_SHADER);
        int frag = glCreateShader(GL_FRAGMENT_SHADER);
        compileShader(frag, FRAG_SHADER);
        int program = glCreateProgram();
        linkShader(program, vert, frag);
        shaderProgram = program;
        posAttrib = glGetAttribLocation(shaderProgram, "posIn");
        colAttrib = glGetAttribLocation(shaderProgram, "colorIn");
        matrixLocation = glGetUniformLocation(shaderProgram, "mvpMatrix");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        glBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, glBuffer);

        bufferBuilder = new BufferBuilder(4096);
        perfGraphBuilder = new BufferBuilder(4096);

        projectionMatrix = new Matrix4f(new float[]{
                2f / 854f, 0, 0, 0,
                0, -2f / 480f, 0, 0,
                0, 0, -2f / 2000f, 0,
                0, 0, 0, 1
        });
    }

    private static void compileShader(int id, String shader) {
        glShaderSource(id, shader);
        glCompileShader(id);
        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        int length = glGetShaderi(id, GL_INFO_LOG_LENGTH);
        if (length > 0) {
            String log = glGetShaderInfoLog(id);
            LOGGER.error(log);
            if (status != GL_TRUE) {
                throw new RuntimeException("Shader failed to compile, see log");
            }
        }
    }

    private static void linkShader(int program, int vert, int frag) {
        glAttachShader(program, vert);
        glAttachShader(program, frag);
        glLinkProgram(program);
        int status = glGetProgrami(program, GL_COMPILE_STATUS);
        int length = glGetProgrami(program, GL_INFO_LOG_LENGTH);
        if (length > 0) {
            String log = glGetProgramInfoLog(program);
            LOGGER.error(log);
            if (status != GL_TRUE) {
                throw new RuntimeException("Shader failed to compile, see log");
            }
        }
        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    public static void render() {
        perfTimerIdx++;
        perfTimerIdx %= perfTimer.length;
        timer().clear();
        timer().beginFrame = System.nanoTime();
        glStateSetup();
        timer().glStateSetup = System.nanoTime();
        matrixSetup();
        timer().matrixSetup = System.nanoTime();
        resetBuffer();
        timer().bufferReset = System.nanoTime();

        drawSelectedWorld(bufferBuilder);

        sortQuads();
        timer().sortQuads = System.nanoTime();
        Pair<Integer, FloatBuffer> renderBuffer = quadsToTriangles();
        timer().toTriangles = System.nanoTime();
        setBufferData(renderBuffer);
        timer().setBufferData = System.nanoTime();
        preDrawSetup();
        timer().preDrawSetup = System.nanoTime();
        drawBuffer(renderBuffer);
        timer().draw = System.nanoTime();
        postDraw();
        timer().postDraw = System.nanoTime();
        freeBuffer(renderBuffer);
        timer().freeMem = System.nanoTime();
        glFinish();
        timer().glFinish = System.nanoTime();

        drawPerfStats();
        glfwSwapBuffers(window);
    }

    private static void glStateSetup() {
        glClearColor(0.1f, 0.1f, 0.9f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        glUseProgram(shaderProgram);
    }

    private static void matrixSetup() {
        ByteBuffer byteBuffer = MemoryUtil.memAlignedAlloc(64, 64);
        FloatBuffer fb = byteBuffer.asFloatBuffer();

        mvpMatrix.setIdentity();
        // mvp = projection*view*model
        // projection
        mvpMatrix.mul(Matrix4f.perspective(60, screenWidth / screenHeight, 0.01f, 1000));
        Matrix4f modelView = inverseMatrix;
        modelView.setIdentity();
        // view
        modelView.mul(Matrix4f.makeTranslate(0, 0, -500));
        // model
        modelView.mul(Vector3f.XP.rotationDegrees(30));
        modelView.mul(Vector3f.YP.rotationDegrees((float) ((System.currentTimeMillis() * 0.04) % 360)));//

        mvpMatrix.mul(modelView);
        mvpMatrix.write(fb);
        glUniformMatrix4fv(matrixLocation, false, fb);

        inverseMatrix.invert();

        MemoryUtil.memFree(byteBuffer);
    }

    private static void resetBuffer() {
        if (bufferBuilder.isDrawing()) {
            bufferBuilder.finishDrawing();
        }
        bufferBuilder.reset();
        bufferBuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    }

    private static void drawSelectedWorld(BufferBuilder bufferBuilder) {
        World w = serverWorlds.get(DimensionType.OVERWORLD);
        if (w == null) {
            return;
        }

        drawWorld(bufferBuilder, w);
    }

    private static void drawWorld(BufferBuilder bufferBuilder, World world) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        int playerX = player == null ? 0 : Coords.getCubeXForEntity(player);
        int playerY = player == null ? 0 : Coords.getCubeYForEntity(player);
        int playerZ = player == null ? 0 : Coords.getCubeZForEntity(player);

        AbstractChunkProvider chunkProvider = world.getChunkProvider();
        if (chunkProvider instanceof ServerChunkProvider) {
            Long2ByteLinkedOpenHashMap cubeMap = buildStatusMap((ServerChunkProvider) chunkProvider);
            timer().buildStatusMap = System.nanoTime();
            buildQuads(bufferBuilder, playerX, playerY, playerZ, cubeMap);
            timer().buildQuads = System.nanoTime();
        }

    }

    private static Long2ByteLinkedOpenHashMap buildStatusMap(ServerChunkProvider chunkProvider) {
        ChunkManager chunkManager = chunkProvider.chunkManager;
        Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedCubes =
                ObfuscationReflectionHelper.getPrivateValue(ChunkManager.class, chunkManager, "immutableLoadedCubes");
        Object[] data = ObfuscationReflectionHelper.getPrivateValue(Long2ObjectLinkedOpenHashMap.class, loadedCubes, "value");
        long[] keys = ObfuscationReflectionHelper.getPrivateValue(Long2ObjectLinkedOpenHashMap.class, loadedCubes, "key");
        Long2ByteLinkedOpenHashMap cubeMap = new Long2ByteLinkedOpenHashMap(100000);
        for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
            long pos = keys[i];
            if (pos == 0) {
                continue;
            }
            ChunkHolder holder = (ChunkHolder) data[i];
            ChunkStatus status = holder == null ? null : ICubeHolder.getCubeStatusFromLevel(holder.getChunkLevel());
            IChunk chunk = holder == null ? null : holder.func_219302_f().getNow(null);
            ChunkStatus realStatus = chunk == null ? null : chunk.getStatus();
            cubeMap.put(pos, realStatus == null ? (byte) 255 : (byte) Registry.CHUNK_STATUS.getId(realStatus));
        }
        return cubeMap;
    }

    private static void buildQuads(BufferBuilder bufferBuilder, int playerX, int playerY, int playerZ, Long2ByteLinkedOpenHashMap cubeMap) {
        Object2IntMap<ChunkStatus> colors = ObfuscationReflectionHelper.getPrivateValue(
                WorldLoadProgressScreen.class, null, "field_213042_c"
        );
        int[] colorsArray = new int[256];
        ChunkStatus[] statusLookup = new ChunkStatus[256];
        for (ChunkStatus chunkStatus : colors.keySet()) {
            int id = Registry.CHUNK_STATUS.getId(chunkStatus);
            colorsArray[id] = colors.get(chunkStatus);
            statusLookup[id] = chunkStatus;
        }
        colorsArray[255] = 0x00FF00FF;
        EnumSet<Direction> renderFaces = EnumSet.noneOf(Direction.class);
        Direction[] directions = Direction.values();
        float ratioFactor =  1/(float) ChunkStatus.FULL.ordinal();
        for (Long2ByteMap.Entry e : cubeMap.long2ByteEntrySet()) {
            long posLong = e.getLongKey();
            int posX = CubePos.extractX(posLong);
            int posY = CubePos.extractY(posLong);
            int posZ = CubePos.extractZ(posLong);
            int status = e.getByteValue() & 0xFF;
            renderFaces.clear();
            ChunkStatus statusObj = statusLookup[status];
            float ratio = statusObj == null ? 1 : statusObj.ordinal() * ratioFactor;
            int alpha = status == 255 ? 0x22 : (int) (0x20 + ratio * (0xFF - 0x20));
            int c = colorsArray[status] | (alpha << 24);
            for (Direction value : directions) {
                long l = CubePos.asLong(posX + value.getXOffset(), posY + value.getYOffset(), posZ + value.getZOffset());
                int cubeStatus = cubeMap.get(l) & 0xFF;
                // this.ordinal() >= status.ordinal();
                if (status == 255 || cubeStatus == 255 || cubeStatus < status) {
                    renderFaces.add(value);
                }
            }
            drawCube(bufferBuilder, posX - playerX, posY - playerY, posZ - playerZ, 7, c, renderFaces);
        }
    }

    private static void sortQuads() {
        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(inverseMatrix);
        bufferBuilder.sortVertexData(vec.getX(), vec.getY(), vec.getZ());

        bufferBuilder.finishDrawing();
    }

    private static Pair<Integer, FloatBuffer> quadsToTriangles() {
        Pair<BufferBuilder.DrawState, ByteBuffer> stateBuffer = bufferBuilder.getNextBuffer();
        return toTriangles(stateBuffer);
    }

    private static Pair<Integer, FloatBuffer> toTriangles(Pair<BufferBuilder.DrawState, ByteBuffer> stateBuffer) {
        FloatBuffer in = stateBuffer.getSecond().asFloatBuffer();
        in.clear();
        int quadCount = stateBuffer.getFirst().getVertexCount() / 4;
        int triangleCount = quadCount * 2;
        int floatsPerVertex = stateBuffer.getFirst().getFormat().getIntegerSize();
        FloatBuffer out = MemoryUtil.memAllocFloat(triangleCount * 3 * floatsPerVertex);
        for (int i = 0; i < quadCount; i++) {
            int startPos = i * 4 * floatsPerVertex;
            int endPos = startPos + floatsPerVertex * 3;
            in.limit(endPos);
            in.position(startPos);
            out.put(in);

            in.limit(startPos + floatsPerVertex);
            in.position(startPos);
            out.put(in);

            in.limit(endPos);
            in.position(endPos - floatsPerVertex);
            out.put(in);

            in.limit(endPos + floatsPerVertex);
            in.position(endPos);
            out.put(in);
        }
        out.clear();
        return new Pair<>(triangleCount * 3, out);
    }

    private static void setBufferData(Pair<Integer, FloatBuffer> renderBuffer) {
        glBufferData(GL_ARRAY_BUFFER, renderBuffer.getSecond(), GL_STREAM_DRAW);
    }

    private static void preDrawSetup() {
        glEnableVertexAttribArray(posAttrib);
        glEnableVertexAttribArray(colAttrib);

        // 12 bytes per float pos + 4 bytes per color = 16 bytes
        glVertexAttribPointer(posAttrib, 3, GL_FLOAT, false, 16, 0);
        glVertexAttribPointer(colAttrib, 4, GL_UNSIGNED_BYTE, true, 16, 12);
    }

    private static void drawBuffer(Pair<Integer, FloatBuffer> renderBuffer) {
        glDrawArrays(GL_TRIANGLES, 0, renderBuffer.getFirst());
    }

    private static void postDraw() {
        glDisableVertexAttribArray(posAttrib);
        glDisableVertexAttribArray(colAttrib);
    }

    private static void freeBuffer(Pair<Integer, FloatBuffer> renderBuffer) {
        MemoryUtil.memFree(renderBuffer.getSecond());
    }


    private static void drawPerfStats() {
        if (perfGraphBuilder.isDrawing()) {
            perfGraphBuilder.finishDrawing();
        }
        perfGraphBuilder.reset();

        int[] colors = {
                0x000000, // glStateSetup
                0xFFFFFF, // matrixSetup
                0xFF0000, // bufferReset
                0x00FF00, // buildStatusMap
                0xFFFF00, // buildQuads
                0xFF00FF, // sortQuads
                0xC0C0C0, // toTriangles
                0x808080, // setBufferData
                0x800000, // preDrawSetup
                0x808000, // draw
                0x008000, // postDraw
                0x800080, // freeMem
                0x8B4513, // glFinish
                0x708090, //
                0x8FBC8F, //
                0x808000, //
                0xB8860B
        };
        perfGraphBuilder.reset();
        perfGraphBuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < perfTimer.length; i++) {
            int x = perfTimer.length - 1 - i;
            PerfTimer timer = perfTimer[(i + perfTimerIdx) % perfTimer.length];
            if(timer == null) {
                continue;
            }
            AtomicInteger ci = new AtomicInteger();
            timer.drawTimer((yStart, yEnd) -> {
                int cx = ci.getAndIncrement();
                int col = (cx >= colors.length ? 0 : colors[cx]) | 0xFF000000;
                quad2d(perfGraphBuilder, x * 3, yStart, x * 3 + 3, yEnd, col);
            });
        }

        perfGraphBuilder.finishDrawing();

        glUseProgram(shaderProgram);

        Matrix4f ortho = new Matrix4f(new float[]{
                2f / 854f, 0, 0, -1f,
                0, 2f / 480f, 0, -1f,
                0, 0, -2f / 2000f, 0,
                0, 0, 0, 1
        });;
        FloatBuffer buffer = MemoryUtil.memAllocFloat(16);
        ortho.write(buffer);
        buffer.clear();
        glUniformMatrix4fv(matrixLocation, false, buffer);
        MemoryUtil.memFree(buffer);


        Pair<BufferBuilder.DrawState, ByteBuffer> nextBuffer = perfGraphBuilder.getNextBuffer();
        nextBuffer.getSecond().clear();
        Pair<Integer, FloatBuffer> buf = toTriangles(nextBuffer);
        buf.getSecond().clear();
        glBufferData(GL_ARRAY_BUFFER, buf.getSecond(), GL_STREAM_DRAW);
        preDrawSetup();
        glDrawArrays(GL_TRIANGLES, 0, buf.getFirst());
        postDraw();
    }

    private static void quad2d(BufferBuilder buf, float x1, float y1, float x2, float y2, int color) {
        vertex(buf, x1, y1, 1,  0, 0, 0, color);
        vertex(buf, x1, y2, 1,  0, 0, 0, color);
        vertex(buf, x2, y2, 1,  0, 0, 0, color);
        vertex(buf, x2, y1, 1,  0, 0, 0, color);
    }

    private static void drawCube(BufferBuilder buffer, int x, int y, int z, float scale, int color, EnumSet<Direction> renderFaces) {
        float x0 = x * scale;
        float x1 = x0 + scale;
        float y0 = y * scale;
        float y1 = y0 + scale;
        float z0 = z * scale;
        float z1 = z0 + scale;
        if (renderFaces.contains(Direction.UP)) {
            // up face
            vertex(buffer, x0, y1, z0, 0, 1, 0, color);
            vertex(buffer, x0, y1, z1, 0, 1, 0, color);
            vertex(buffer, x1, y1, z1, 0, 1, 0, color);
            vertex(buffer, x1, y1, z0, 0, 1, 0, color);
        }
        if (renderFaces.contains(Direction.DOWN)) {
            int c = darken(color, 40);
            // down face
            vertex(buffer, x0, y0, z0, 0, 1, 0, color);
            vertex(buffer, x0, y0, z1, 0, 1, 0, color);
            vertex(buffer, x1, y0, z1, 0, 1, 0, color);
            vertex(buffer, x1, y0, z0, 0, 1, 0, color);
        }
        if (renderFaces.contains(Direction.EAST)) {
            int c = darken(color, 30);
            // right face
            vertex(buffer, x1, y1, z0, 1, 0, 0, c);
            vertex(buffer, x1, y1, z1, 1, 0, 0, c);
            vertex(buffer, x1, y0, z1, 1, 0, 0, c);
            vertex(buffer, x1, y0, z0, 1, 0, 0, c);
        }
        if (renderFaces.contains(Direction.WEST)) {
            int c = darken(color, 30);
            // left face
            vertex(buffer, x0, y1, z0, 1, 0, 0, c);
            vertex(buffer, x0, y1, z1, 1, 0, 0, c);
            vertex(buffer, x0, y0, z1, 1, 0, 0, c);
            vertex(buffer, x0, y0, z0, 1, 0, 0, c);
        }
        if (renderFaces.contains(Direction.NORTH)) {
            int c = darken(color, 20);
            // front face (facing camera)
            vertex(buffer, x0, y1, z0, 0, 0, -1, c);
            vertex(buffer, x1, y1, z0, 0, 0, -1, c);
            vertex(buffer, x1, y0, z0, 0, 0, -1, c);
            vertex(buffer, x0, y0, z0, 0, 0, -1, c);
        }
        if (renderFaces.contains(Direction.SOUTH)) {
            int c = darken(color, 20);
            // back face
            vertex(buffer, x0, y1, z1, 0, 0, -1, c);
            vertex(buffer, x1, y1, z1, 0, 0, -1, c);
            vertex(buffer, x1, y0, z1, 0, 0, -1, c);
            vertex(buffer, x0, y0, z1, 0, 0, -1, c);
        }
    }

    private static int darken(int color, int amount) {
        int r = color >>> 16 & 0xFF;
        r -= (r * amount) / 100;
        int g = color >>> 8 & 0xFF;
        g -= (g * amount) / 100;
        int b = color & 0xFF;
        b -= (b * amount) / 100;
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z, int nx, int ny, int nz, int color) {
        // color = (color & 0xFF000000) | ((~color) & 0x00FFFFFF);
        int r = color >>> 16 & 0xFF;
        int g = color >>> 8 & 0xFF;
        int b = color & 0xFF;
        int a = color >>> 24;

        buffer.pos(x, y, z);
        buffer.color(r, g, b, a);
        buffer.endVertex();
    }

    private static class PerfTimer {

        private long beginFrame;
        private long glStateSetup;
        private long matrixSetup;
        private long bufferReset;
        private long buildStatusMap;
        private long buildQuads;
        private long sortQuads;
        private long toTriangles;
        private long setBufferData;
        private long preDrawSetup;
        private long draw;
        private long postDraw;
        private long freeMem;
        private long glFinish;

        public void clear() {
            beginFrame = 0;
            glStateSetup = 0;
            matrixSetup = 0;
            bufferReset = 0;
            buildStatusMap = 0;
            buildQuads = 0;
            sortQuads = 0;
            toTriangles = 0;
            setBufferData = 0;
            preDrawSetup = 0;
            draw = 0;
            postDraw = 0;
            freeMem = 0;
            glFinish = 0;
        }

        public void drawTimer(FloatBiConsumer line) {
            double scale = 0.3f / TimeUnit.MILLISECONDS.toNanos(1);


            double glFinish = this.glFinish;
            double freeMem = this.freeMem;
            double postDraw = this.postDraw;
            double draw = this.draw;
            double preDrawSetup = this.preDrawSetup;
            double setBufferData = this.setBufferData;
            double toTriangles = this.toTriangles;
            double sortQuads = this.sortQuads;
            double buildQuads = this.buildQuads;
            double buildStatusMap = this.buildStatusMap;
            double bufferReset = this.bufferReset;
            double matrixSetup = this.matrixSetup;
            double glStateSetup = this.glStateSetup;

            glFinish -= freeMem;
            freeMem -= postDraw;
            postDraw -= draw;
            draw -= preDrawSetup;
            preDrawSetup -= setBufferData;
            setBufferData -= toTriangles;
            toTriangles -= sortQuads;
            sortQuads -= buildQuads;
            buildQuads -= buildStatusMap;
            buildStatusMap -= bufferReset;
            bufferReset -= matrixSetup;
            matrixSetup -= glStateSetup;
            glStateSetup -= beginFrame;

            float y = 0;
            line.accept(y, y += (glStateSetup * scale));
            line.accept(y, y += (matrixSetup * scale));
            line.accept(y, y += (bufferReset * scale));
            line.accept(y, y += (buildStatusMap * scale));
            line.accept(y, y += (buildQuads * scale));
            line.accept(y, y += (sortQuads * scale));
            line.accept(y, y += (toTriangles * scale));
            line.accept(y, y += (setBufferData * scale));
            line.accept(y, y += (preDrawSetup * scale));
            line.accept(y, y += (draw * scale));
            line.accept(y, y += (postDraw * scale));
            line.accept(y, y += (freeMem * scale));
            line.accept(y, y += (glFinish * scale));
        }
    }

    @FunctionalInterface
    private interface FloatBiConsumer {

        void accept(float a, float b);
    }
}
