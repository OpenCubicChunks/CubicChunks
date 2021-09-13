package io.github.opencubicchunks.cubicchunks.debug;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
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
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL20.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBufferData;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.client.CubicWorldLoadScreen;
import io.github.opencubicchunks.cubicchunks.levelgen.placement.UserFunction;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import it.unimi.dsi.fastutil.longs.Long2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@SuppressWarnings("FieldMayBeFinal")
public class DebugVisualization {

    private static final String VERT_SHADER =
        """
            #version 330 core
            layout(location = 0) in vec3 posIn;
            layout(location = 1) in vec4 colorIn;
            smooth out vec4 fragColor;
            uniform mat4 mvpMatrix;
            void main() {
              gl_Position = mvpMatrix * vec4(posIn, 1);
              fragColor = colorIn;
            }""";
    private static final String FRAG_SHADER =
        """
            #version 330 core
            smooth in vec4 fragColor;
            out vec4 outColor;
            void main() {
              outColor = fragColor;
            }""";
    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<ResourceKey<?>, Level> serverWorlds = new ConcurrentHashMap<>();
    private static AtomicBoolean initialized = new AtomicBoolean();
    private static long window;
    private static int shaderProgram;
    private static int matrixLocation;
    private static int posAttrib;
    private static int colAttrib;
    private static BufferBuilder bufferBuilder;
    private static BufferBuilder perfGraphBuilder;

    private static Matrix4f mvpMatrix = new Matrix4f();
    private static Matrix4f inverseMatrix = new Matrix4f();
    private static PerfTimer[] perfTimer = new PerfTimer[128];
    private static int perfTimerIdx = 0;
    private static float screenWidth = 854.0f;
    private static float screenHeight = 480f;
    private static GLCapabilities debugGlCapabilities;
    private static boolean enabled;

    private static VisualizationMode mode = VisualizationMode.AVAILABLE_MODES[0];

    private static PerfTimer timer() {
        if (perfTimer[perfTimerIdx] == null) {
            perfTimer[perfTimerIdx] = new PerfTimer();
        }
        return perfTimer[perfTimerIdx];
    }


    public static void enable() {
        enabled = true;
    }

    public static void onRender() {
        if (!enabled) {
            return;
        }

        long ctx = glfwGetCurrentContext();

        GLCapabilities capabilities = GL.getCapabilities();
        if (!initialized.getAndSet(true)) {
            initializeWindow();
        }
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
                bufferBuilder.end();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } finally {
            glfwMakeContextCurrent(ctx);
            GL.setCapabilities(capabilities);
        }
    }

    public static void onWorldLoad(Level w) {
        if (!enabled) {
            return;
        }
        if (w instanceof ServerLevel) {
            serverWorlds.put(w.dimension(), w);
        }

    }

    public static void onWorldUnload(Level w) {
        if (!enabled) {
            return;
        }
        if (w instanceof ServerLevel) {
            serverWorlds.remove(w.dimension());
        }
    }

    public static void initializeWindow() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }


        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
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
            assert vidmode != null;
            glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2 + monPosLeft.get(0),
                (vidmode.height() - pHeight.get(0)) / 2 + monPosTop.get(0));
        }

        GLFW.glfwSetWindowSizeCallback(window, (resizedWindow, width, height) -> {
            screenWidth = width;
            screenHeight = height;
        });
        initWindow();
    }

    private static void initWindow() {
        glfwShowWindow(window);
        glfwMakeContextCurrent(window);

        debugGlCapabilities = GL.createCapabilities();
        initialize();
        glfwSwapBuffers(window);
    }

    private static void initialize() {
        int vert = glCreateShader(GL_VERTEX_SHADER);
        System.out.println("E0=" + glGetError());
        compileShader(vert, VERT_SHADER);
        System.out.println("E1=" + glGetError());
        int frag = glCreateShader(GL_FRAGMENT_SHADER);
        System.out.println("E2=" + glGetError());
        compileShader(frag, FRAG_SHADER);
        System.out.println("E3=" + glGetError());
        int program = glCreateProgram();
        System.out.println("E4=" + glGetError());
        linkShader(program, vert, frag);
        System.out.println("E5=" + glGetError());
        shaderProgram = program;
        posAttrib = glGetAttribLocation(shaderProgram, "posIn");
        colAttrib = glGetAttribLocation(shaderProgram, "colorIn");
        System.out.println("E6=" + glGetError());
        matrixLocation = glGetUniformLocation(shaderProgram, "mvpMatrix");
        System.out.println("E7=" + glGetError());
        int vao = glGenVertexArrays();
        System.out.println("E8=" + glGetError());
        glBindVertexArray(vao);
        System.out.println("E9=" + glGetError());
        int glBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, glBuffer);
        System.out.println("E10=" + glGetError());
        bufferBuilder = new BufferBuilder(4096);
        perfGraphBuilder = new BufferBuilder(4096);
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
        System.out.println("E5a=" + glGetError());
        glAttachShader(program, vert);
        System.out.println("E5b=" + glGetError());
        glAttachShader(program, frag);
        System.out.println("E5c=" + glGetError());
        glLinkProgram(program);
        System.out.println("p=" + program + ", v=" + vert + ", f=" + frag);
        System.out.println("E5d=" + glGetError());
        int status = glGetProgrami(program, GL_LINK_STATUS);
        System.out.println("E5e=" + glGetError() + " status=" + status);
        int length = glGetProgrami(program, GL_INFO_LOG_LENGTH);
        System.out.println("E5f=" + glGetError());
        if (length > 0) {
            String log = glGetProgramInfoLog(program);
            LOGGER.error(log);
            if (status != GL_TRUE) {
                throw new RuntimeException("Shader failed to compile, see log");
            }
        }
        glDeleteShader(vert);
        glDeleteShader(frag);
        System.out.println("E5g=" + glGetError());
    }

    public static void render() {
        for (int i = GLFW.GLFW_KEY_1; i < GLFW.GLFW_KEY_9; i++) {
            if (GLFW.glfwGetKey(window, i) == GLFW.GLFW_PRESS) {
                int idx = i - GLFW.GLFW_KEY_1;
                if (idx < VisualizationMode.AVAILABLE_MODES.length) {
                    mode = VisualizationMode.AVAILABLE_MODES[idx];
                }
            }
        }
        glEnable(GL_CULL_FACE);
        GLFW.glfwSetWindowTitle(window, "CubicChunks debug: " + mode.toString());
        perfTimerIdx++;
        perfTimerIdx %= perfTimer.length;
        timer().clear();
        timer().beginFrame = System.nanoTime();
        glStateSetup();
        matrixSetup();
        resetBuffer();

        drawSelectedWorld(bufferBuilder);

        sortQuads();
        Pair<Integer, FloatBuffer> renderBuffer = quadsToTriangles();
        setBufferData(renderBuffer);
        preDrawSetup();
        shaderUniforms();
        drawBuffer(renderBuffer);
        freeBuffer(renderBuffer);
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

        timer().glStateSetup = System.nanoTime();
    }

    private static void matrixSetup() {
        GL11.glViewport(0, 0, (int) screenWidth, (int) screenHeight);

        mvpMatrix.setIdentity();
        // mvp = projection*view*model
        // projection
        mvpMatrix.multiply(Matrix4f.perspective(60, screenWidth / screenHeight, 0.01f, 1000));
        Matrix4f modelView = inverseMatrix;
        modelView.setIdentity();
        // view
        modelView.multiply(Matrix4f.createTranslateMatrix(0, 0, -500));
        // model
        modelView.multiply(Vector3f.XP.rotationDegrees(30));
        modelView.multiply(Vector3f.YP.rotationDegrees((float) ((System.currentTimeMillis() * 0.04) % 360)));

        mvpMatrix.multiply(modelView);
        inverseMatrix.invert();
        timer().matrixSetup = System.nanoTime();
    }

    private static void resetBuffer() {
        if (bufferBuilder.building()) {
            bufferBuilder.end();
        }
        bufferBuilder.discard();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        timer().bufferReset = System.nanoTime();
    }

    private static void drawSelectedWorld(BufferBuilder builder) {
        Level w = serverWorlds.get(LevelStem.OVERWORLD);
        if (w == null) {
            return;
        }

        drawWorld(builder, w);
    }

    private static void drawWorld(BufferBuilder builder, Level world) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        int playerX = player == null ? 0 : Coords.getCubeXForEntity(player);
        int playerY = player == null ? 0 : Coords.getCubeYForEntity(player);
        int playerZ = player == null ? 0 : Coords.getCubeZForEntity(player);

        Long2ByteMap cubeMap = mode.buildStateMap(world);
        timer().buildStatusMap = System.nanoTime();
        buildQuads(builder, playerX, playerY, playerZ, cubeMap);
        timer().buildQuads = System.nanoTime();
    }

    @SuppressWarnings("unchecked") private static <T> T getField(Class<?> cl, Object obj, String name) {
        try {
            Field f = cl.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void buildQuads(BufferBuilder builder, int playerX, int playerY, int playerZ, Long2ByteMap cubeMap) {
        int[] colorsArray = mode.getColorMap();

        Direction[] directions = Direction.values();

        Map<Integer, List<Vertex>> verts = new HashMap<>();
        for (Long2ByteMap.Entry e : cubeMap.long2ByteEntrySet()) {
            long posLong = e.getLongKey();
            int posX = CubePos.extractX(posLong);
            int posY = CubePos.extractY(posLong);
            int posZ = CubePos.extractZ(posLong);
            int status = e.getByteValue() & 0xFF;
            int c = colorsArray[status];

            EnumSet<Direction> renderFaces = findRenderFaces(cubeMap, directions, posX, posY, posZ, status);

            List<Vertex> buffer = verts.computeIfAbsent(status, x -> new ArrayList<>());
            drawCube(buffer, posX - playerX, posY - playerY, posZ - playerZ, 7, c, renderFaces);
        }
        buildVertices(builder, verts);
    }

    private static EnumSet<Direction> findRenderFaces(Long2ByteMap cubeMap, Direction[] directions, int posX, int posY, int posZ, int status) {
        EnumSet<Direction> renderFaces = EnumSet.noneOf(Direction.class);
        for (Direction value : directions) {
            long l = CubePos.asLong(posX + value.getStepX(), posY + value.getStepY(), posZ + value.getStepZ());
            int cubeStatus = cubeMap.getOrDefault(l, (byte) 255) & 0xFF;
            if (status != 255 && (cubeStatus == 255 || cubeStatus < status)) {
                renderFaces.add(value);
            }
        }
        return renderFaces;
    }

    private static void buildVertices(BufferBuilder builder, Map<Integer, List<Vertex>> verts) {
        for (int i = 255; i >= 0; i--) {
            List<Vertex> vertices = verts.get(i);
            if (vertices == null) {
                continue;
            }
            for (Vertex v : vertices) {
                vertex(builder, v.x, v.y, v.z, v.rgba);
            }
        }
    }

    private static void sortQuads() {
        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(inverseMatrix);
        //bufferBuilder.setQuadSortOrigin(vec.x(), vec.y(), vec.z());

        bufferBuilder.end();
        timer().sortQuads = System.nanoTime();
    }

    private static Pair<Integer, FloatBuffer> quadsToTriangles() {
        Pair<BufferBuilder.DrawState, ByteBuffer> stateBuffer = bufferBuilder.popNextBuffer();
        stateBuffer.getSecond().order(ByteOrder.nativeOrder());
        Pair<Integer, FloatBuffer> integerFloatBufferPair = toTriangles(stateBuffer);
        timer().toTriangles = System.nanoTime();
        return integerFloatBufferPair;
    }

    private static Pair<Integer, FloatBuffer> toTriangles(Pair<BufferBuilder.DrawState, ByteBuffer> stateBuffer) {
        FloatBuffer in = stateBuffer.getSecond().asFloatBuffer();
        in.clear();
        int quadCount = stateBuffer.getFirst().vertexCount() / 4;
        int triangleCount = quadCount * 2;
        int floatsPerVertex = stateBuffer.getFirst().format().getIntegerSize();
        ByteBuffer outBytes = MemoryUtil.memAlloc(Float.BYTES * triangleCount * 3 * floatsPerVertex);
        outBytes.order(ByteOrder.nativeOrder());
        FloatBuffer out = outBytes.asFloatBuffer();
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
        timer().setBufferData = System.nanoTime();
    }

    private static void preDrawSetup() {
        glEnableVertexAttribArray(posAttrib);
        glEnableVertexAttribArray(colAttrib);

        // 12 bytes per float pos + 4 bytes per color = 16 bytes
        glVertexAttribPointer(posAttrib, 3, GL_FLOAT, false, 16, 0);
        glVertexAttribPointer(colAttrib, 4, GL_UNSIGNED_BYTE, true, 16, 12);

        timer().preDrawSetup = System.nanoTime();
    }

    private static void shaderUniforms() {
        ByteBuffer byteBuffer = MemoryUtil.memAlignedAlloc(64, 64);
        FloatBuffer fb = byteBuffer.asFloatBuffer();
        mvpMatrix.store(fb);
        glUniformMatrix4fv(matrixLocation, false, fb);
        MemoryUtil.memFree(byteBuffer);
    }

    private static void drawBuffer(Pair<Integer, FloatBuffer> renderBuffer) {
        glDrawArrays(GL_TRIANGLES, 0, renderBuffer.getFirst());
        timer().draw = System.nanoTime();
    }

    private static void freeBuffer(Pair<Integer, FloatBuffer> renderBuffer) {
        MemoryUtil.memFree(renderBuffer.getSecond());
        timer().freeMem = System.nanoTime();
    }


    private static void drawPerfStats() {
        if (perfGraphBuilder.building()) {
            perfGraphBuilder.end();
        }
        perfGraphBuilder.discard();

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
            0x008000, // freeMem
            0x800080, // glFinish
            0x8B4513, //
            0x708090, //
            0x8FBC8F, //
            0x808000, //
            0xB8860B
        };
        perfGraphBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < perfTimer.length; i++) {
            int x = perfTimer.length - 1 - i;
            PerfTimer timer = perfTimer[(i + perfTimerIdx) % perfTimer.length];
            if (timer == null) {
                continue;
            }
            AtomicInteger ci = new AtomicInteger();
            timer.drawTimer((yStart, yEnd) -> {
                int cx = ci.getAndIncrement();
                int col = (cx >= colors.length ? 0 : colors[cx]) | 0xFF000000;
                quad2d(perfGraphBuilder, x * 3, yStart, x * 3 + 3, yEnd, col);
            });
        }

        perfGraphBuilder.end();

        glUseProgram(shaderProgram);

        Matrix4f ortho = MathUtil.createMatrix(new float[] {
            2f / screenWidth, 0, 0, -1f,
            0, 2f / screenHeight, 0, -1f,
            0, 0, -2f / 2000f, 0,
            0, 0, 0, 1
        });

        FloatBuffer buffer = MemoryUtil.memAllocFloat(16);
        ortho.store(buffer);
        buffer.clear();
        glUniformMatrix4fv(matrixLocation, false, buffer);
        MemoryUtil.memFree(buffer);


        Pair<BufferBuilder.DrawState, ByteBuffer> nextBuffer = perfGraphBuilder.popNextBuffer();
        nextBuffer.getSecond().clear().order(ByteOrder.nativeOrder());
        Pair<Integer, FloatBuffer> buf = toTriangles(nextBuffer);
        buf.getSecond().clear();
        glBufferData(GL_ARRAY_BUFFER, buf.getSecond(), GL_STREAM_DRAW);
        preDrawSetup();
        glDrawArrays(GL_TRIANGLES, 0, buf.getFirst());
        MemoryUtil.memFree(buf.getSecond());
    }

    private static void quad2d(BufferBuilder buf, float x1, float y1, float x2, float y2, int color) {
        vertex(buf, x1, y1, 1, color);
        vertex(buf, x1, y2, 1, color);
        vertex(buf, x2, y2, 1, color);
        vertex(buf, x2, y1, 1, color);
    }

    @SuppressWarnings({ "DuplicatedCode", "SameParameterValue" })
    private static void drawCube(List<Vertex> buffer, int x, int y, int z, float scale, int color, EnumSet<Direction> renderFaces) {
        float x0 = x * scale;
        float x1 = x0 + scale;
        float y0 = y * scale;
        float y1 = y0 + scale;
        float z0 = z * scale;
        float z1 = z0 + scale;
        if (renderFaces.contains(Direction.UP)) {
            // up face
            buffer.add(new Vertex(x0, y1, z0, 0, 1, 0, color));
            buffer.add(new Vertex(x0, y1, z1, 0, 1, 0, color));
            buffer.add(new Vertex(x1, y1, z1, 0, 1, 0, color));
            buffer.add(new Vertex(x1, y1, z0, 0, 1, 0, color));
        }
        if (renderFaces.contains(Direction.DOWN)) {
            int c = darken(color, 40);
            // down face
            buffer.add(new Vertex(x1, y0, z0, 0, 1, 0, c));
            buffer.add(new Vertex(x1, y0, z1, 0, 1, 0, c));
            buffer.add(new Vertex(x0, y0, z1, 0, 1, 0, c));
            buffer.add(new Vertex(x0, y0, z0, 0, 1, 0, c));
        }
        if (renderFaces.contains(Direction.EAST)) {
            int c = darken(color, 30);
            // right face
            buffer.add(new Vertex(x1, y1, z0, 1, 0, 0, c));
            buffer.add(new Vertex(x1, y1, z1, 1, 0, 0, c));
            buffer.add(new Vertex(x1, y0, z1, 1, 0, 0, c));
            buffer.add(new Vertex(x1, y0, z0, 1, 0, 0, c));

        }
        if (renderFaces.contains(Direction.WEST)) {
            int c = darken(color, 30);
            // left face
            buffer.add(new Vertex(x0, y0, z0, 1, 0, 0, c));
            buffer.add(new Vertex(x0, y0, z1, 1, 0, 0, c));
            buffer.add(new Vertex(x0, y1, z1, 1, 0, 0, c));
            buffer.add(new Vertex(x0, y1, z0, 1, 0, 0, c));
        }
        if (renderFaces.contains(Direction.NORTH)) {
            int c = darken(color, 20);
            // front face (facing camera)
            buffer.add(new Vertex(x0, y1, z0, 0, 0, -1, c));
            buffer.add(new Vertex(x1, y1, z0, 0, 0, -1, c));
            buffer.add(new Vertex(x1, y0, z0, 0, 0, -1, c));
            buffer.add(new Vertex(x0, y0, z0, 0, 0, -1, c));
        }
        if (renderFaces.contains(Direction.SOUTH)) {
            int c = darken(color, 20);
            // back face
            buffer.add(new Vertex(x0, y0, z1, 0, 0, -1, c));
            buffer.add(new Vertex(x1, y0, z1, 0, 0, -1, c));
            buffer.add(new Vertex(x1, y1, z1, 0, 0, -1, c));
            buffer.add(new Vertex(x0, y1, z1, 0, 0, -1, c));
        }
    }

    @SuppressWarnings("DuplicatedCode") private static int darken(int color, int amount) {
        int r = color >>> 16 & 0xFF;
        r -= (r * amount) / 100;
        int g = color >>> 8 & 0xFF;
        g -= (g * amount) / 100;
        int b = color & 0xFF;
        b -= (b * amount) / 100;
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z, int color) {
        // color = (color & 0xFF000000) | ((~color) & 0x00FFFFFF);
        int r = color >>> 16 & 0xFF;
        int g = color >>> 8 & 0xFF;
        int b = color & 0xFF;
        int a = color >>> 24;

        buffer.vertex(x, y, z);
        buffer.color(r, g, b, a);
        buffer.endVertex();
    }

    static class Vertex {
        float x, y, z;
        int nx, ny, nz;
        int rgba;

        Vertex(float x, float y, float z, int nx, int ny, int nz, int rgba) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.rgba = rgba;
        }
    }

    public static class PerfTimer {

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
            freeMem = 0;
            glFinish = 0;
        }

        @SuppressWarnings({ "DuplicatedCode", "UnusedAssignment" }) public void drawTimer(FloatBiConsumer line) {
            double scale = 0.5f / TimeUnit.MILLISECONDS.toNanos(1);


            double glFinishTime = this.glFinish;
            double freeMemTime = this.freeMem;
            double drawTime = this.draw;
            double preDrawSetupTime = this.preDrawSetup;
            double setBufferDataTime = this.setBufferData;
            double toTrianglesTime = this.toTriangles;
            double sortQuadsTime = this.sortQuads;
            double buildQuadsTime = this.buildQuads;
            double buildStatusMapTime = this.buildStatusMap;
            double bufferResetTime = this.bufferReset;
            double matrixSetupTime = this.matrixSetup;
            double glStateSetupTime = this.glStateSetup;

            glFinishTime -= freeMem;
            freeMemTime -= draw;
            drawTime -= preDrawSetup;
            preDrawSetupTime -= setBufferData;
            setBufferDataTime -= toTriangles;
            toTrianglesTime -= sortQuads;
            sortQuadsTime -= buildQuads;
            buildQuadsTime -= buildStatusMap;
            buildStatusMapTime -= bufferReset;
            bufferResetTime -= matrixSetup;
            matrixSetupTime -= glStateSetup;
            glStateSetupTime -= beginFrame;

            float y = 0;
            line.accept(y, y += (glStateSetupTime * scale));
            line.accept(y, y += (matrixSetupTime * scale));
            line.accept(y, y += (bufferResetTime * scale));
            line.accept(y, y += (buildStatusMapTime * scale));
            line.accept(y, y += (buildQuadsTime * scale));
            line.accept(y, y += (sortQuadsTime * scale));
            line.accept(y, y += (toTrianglesTime * scale));
            line.accept(y, y += (setBufferDataTime * scale));
            line.accept(y, y += (preDrawSetupTime * scale));
            line.accept(y, y += (drawTime * scale));
            line.accept(y, y += (freeMemTime * scale));
            line.accept(y, y += (glFinishTime * scale));
        }
    }

    @FunctionalInterface
    public interface FloatBiConsumer {

        void accept(float a, float b);
    }

    private interface VisualizationMode {
        VisualizationMode[] AVAILABLE_MODES = new VisualizationMode[] {
            new ChunkStatusVisualizationMode(ChunkStatusVisualizationMode.Type.TICKET_LEVEL),
            new ChunkStatusVisualizationMode(ChunkStatusVisualizationMode.Type.TICKET_LEVEL_FULL_CHUNK_STATUS),
            new ChunkStatusVisualizationMode(ChunkStatusVisualizationMode.Type.TO_SAVE_LEVEL),
            new ChunkStatusVisualizationMode(ChunkStatusVisualizationMode.Type.HIGHEST_LOADED),
            new ChunkStatusVisualizationMode(ChunkStatusVisualizationMode.Type.ACTUAL_FULL_CHUNK_STATUS),
            new ChunkTicketVisualizationMode()
        };

        Long2ByteMap buildStateMap(Level level);

        int[] getColorMap();
    }

    private static final class ChunkStatusVisualizationMode implements VisualizationMode {

        private static final int[] COLORS = new int[256];

        static {
            UserFunction alphaFunc =
                UserFunction.builder().point(0, 0.1f)
                    .point(ChunkStatus.STRUCTURE_STARTS.getIndex(), 0.15f)
                    .point(ChunkStatus.STRUCTURE_REFERENCES.getIndex(), 0.28f)
                    .point(ChunkStatus.CARVERS.getIndex(), 0.7f)
                    .point(ChunkStatus.LIQUID_CARVERS.getIndex(), 0.2f)
                    .point(ChunkStatus.FULL.getIndex(), 1).build();

            for (Object2IntMap.Entry<ChunkStatus> entry : CubicWorldLoadScreen.getStatusColorMap().object2IntEntrySet()) {
                int idx = entry.getKey().getIndex();
                int alpha = (int) (255 * alphaFunc.getValue(idx));
                COLORS[idx] = entry.getIntValue() | alpha << 24;
            }
            // INACCESSIBLE
            // BORDER
            // TICKING
            // ENTITY_TICKING
            COLORS[128] = 0x20FF0000;
            COLORS[129] = 0x40FFFF00;
            COLORS[130] = 0x8000FF00;
            COLORS[131] = 0x400000FF;
            COLORS[255] = 0xFFFF00FF;
        }

        private final Long2ByteMap map = new Long2ByteLinkedOpenHashMap();

        private final Type type;

        ChunkStatusVisualizationMode(Type type) {
            this.type = type;
        }

        @Override public String toString() {
            return "Status [" + type + "]";
        }

        @Override public Long2ByteMap buildStateMap(Level level) {
            map.clear();

            if (!(level instanceof ServerLevel)) {
                return map;
            }
            ChunkMap chunkManager = ((ServerChunkCache) level.getChunkSource()).chunkMap;
            Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedCubes = getField(ChunkMap.class, chunkManager, "visibleCubeMap");

            Object[] data = getField(Long2ObjectLinkedOpenHashMap.class, loadedCubes, "value");
            long[] keys = getField(Long2ObjectLinkedOpenHashMap.class, loadedCubes, "key");
            Long2ByteLinkedOpenHashMap cubeMap = new Long2ByteLinkedOpenHashMap(100000);
            for (int i = 0, keysLength = keys.length; i < keysLength; i++) {
                long pos = keys[i];
                if (pos == 0) {
                    continue;
                }
                ChunkHolder holder = (ChunkHolder) data[i];
                cubeMap.put(pos, (byte) type.getIndex(holder));
            }
            return cubeMap;
        }

        @Override public int[] getColorMap() {
            return COLORS;
        }

        enum Type {
            TICKET_LEVEL(chunkHolder -> ICubeHolder.getCubeStatusFromLevel(chunkHolder.getTicketLevel()).getIndex()),
            TICKET_LEVEL_FULL_CHUNK_STATUS(holder -> ChunkHolder.getFullChunkStatus(holder.getTicketLevel()).ordinal() + 128),
            TO_SAVE_LEVEL(holder -> {
                ChunkAccess cube = holder.getChunkToSave().getNow(null);
                return cube == null ? 255 : cube.getStatus().getIndex();
            }),
            HIGHEST_LOADED(holder -> {
                ChunkStatus lastAvailableStatus = holder.getLastAvailableStatus();
                return lastAvailableStatus == null ? 255 : lastAvailableStatus.getIndex();
            }),
            ACTUAL_FULL_CHUNK_STATUS(holder -> {
                int base = 128;
                Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> entityTicking = holder.getEntityTickingChunkFuture().getNow(null);
                if (entityTicking != null && entityTicking.left().isPresent()) {
                    return base + ChunkHolder.FullChunkStatus.ENTITY_TICKING.ordinal();
                }
                Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> ticking = holder.getTickingChunkFuture().getNow(null);
                if (ticking != null && ticking.left().isPresent()) {
                    return base + ChunkHolder.FullChunkStatus.TICKING.ordinal();
                }
                Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> border = holder.getFullChunkFuture().getNow(null);
                if (border != null && border.left().isPresent()) {
                    return base + ChunkHolder.FullChunkStatus.BORDER.ordinal();
                }
                return base + ChunkHolder.FullChunkStatus.INACCESSIBLE.ordinal();
            });

            private final ToIntFunction<ChunkHolder> function;

            Type(ToIntFunction<ChunkHolder> function) {
                this.function = function;
            }

            int getIndex(ChunkHolder holder) {
                return function.applyAsInt(holder);
            }
        }
    }

    private static final class ChunkTicketVisualizationMode implements VisualizationMode {
        private static final int[] COLORS = new int[256];

        static {
            COLORS[0] = 0x90FF0000;
            COLORS[1] = 0x9000FF00;
            COLORS[2] = 0x900000FF;
            COLORS[3] = 0x90FFFF00;
            COLORS[4] = 0x90FF00FF;
            COLORS[5] = 0x9000FFFF;
            COLORS[255] = 0x90FF00FF;
        }

        private final Long2ByteLinkedOpenHashMap cubeMap = new Long2ByteLinkedOpenHashMap();

        @Override public Long2ByteMap buildStateMap(Level level) {
            try {
                this.cubeMap.clear();
                if (!(level instanceof ServerLevel)) {
                    return cubeMap;
                }
                ChunkMap chunkMap = ((ServerChunkCache) level.getChunkSource()).chunkMap;
                Field distManF = ChunkMap.class.getDeclaredField("distanceManager");
                distManF.setAccessible(true);
                ChunkMap.DistanceManager distMan = (ChunkMap.DistanceManager) distManF.get(chunkMap);
                @SuppressWarnings("JavaReflectionMemberAccess")
                Field ticketsF = DistanceManager.class.getDeclaredField("cubeTickets");
                ticketsF.setAccessible(true);
                @SuppressWarnings("unchecked")
                Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = (Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>>) ticketsF.get(distMan);

                tickets.long2ObjectEntrySet().fastForEach(entry -> {
                    long posLong = entry.getLongKey();
                    for (Ticket<?> ticket : entry.getValue()) {
                        TicketType<?> type = ticket.getType();
                        if (type == TicketType.PLAYER || type == CCTicketType.CCPLAYER) {
                            cubeMap.put(posLong, (byte) 0);
                        } else if (type == TicketType.START) {
                            cubeMap.put(posLong, (byte) 1);
                        } else if (type == TicketType.UNKNOWN || type == CCTicketType.CCUNKNOWN) {
                            cubeMap.put(posLong, (byte) 2);
                        } else if (type == TicketType.LIGHT || type == CCTicketType.CCLIGHT) {
                            cubeMap.put(posLong, (byte) 3);
                        } else if (type == TicketType.FORCED || type == CCTicketType.CCFORCED) {
                            cubeMap.put(posLong, (byte) 4);
                        } else {
                            cubeMap.put(posLong, (byte) 5);
                        }
                    }
                });

            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
            return cubeMap;
        }

        @Override public int[] getColorMap() {
            return COLORS;
        }
    }
}
