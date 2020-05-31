package io.github.opencubicchunks.cubicchunks.test;

import cpw.mods.modlauncher.Launcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MinecraftTestRunner extends BlockJUnit4ClassRunner {
    // referencing this from the "wrong" classloader in TestRunnerMod is fine
    // because this is a compile time constant expression and java compiler inlines it in the bytecode
    public static final String TEST_WRAPPER_MESSAGE = "Test wrapper message. You shouldn't be see this exception.";

    private static final Logger LOGGER;

    static {
        final String markerselection = "SCAN,REGISTRIES,REGISTRYDUMP";
        Arrays.stream(markerselection.split(",")).forEach(marker -> System.setProperty("forge.logging.marker." + marker.toLowerCase(Locale.ROOT), "ACCEPT"));
        String level = "debug";
        System.setProperty("forge.logging.console.level", level);
        String earlyLoadingWindow = "false";
        System.setProperty("fml.earlyprogresswindow", earlyLoadingWindow);
        LOGGER = LogManager.getLogger();
        LOGGER.info("Initialized FML properties: log level=" + level + ", markers=" + markerselection
                + ", earlyprogresswindow=" + earlyLoadingWindow);
    }

    public static volatile ClassLoader gameClassLoader;
    public static volatile Consumer<Runnable> runnableConsumer;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public MinecraftTestRunner(Class<?> klass) throws InitializationError {
        super(loadTestClass(klass));
    }

    private static Class<?> loadTestClass(Class<?> klass) {
        if (gameClassLoader == null) {
            Thread thread = new Thread(() -> {
                try {
                    launchMinecraftDataRun();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new UncheckedIOException(e);
                }
            });
            thread.setDaemon(true);
            thread.start();
            while (gameClassLoader == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        try {
            return Class.forName(klass.getName(), true, gameClassLoader);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }

    }

    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable[] t = {null};
                runnableConsumer.accept(() -> {
                    try {
                        method.invokeExplosively(test);
                    } catch (Throwable throwable) {
                        t[0] = throwable;
                    }
                });
                if (t[0] != null) {
                    while (t[0].getCause() != null && t[0].getMessage().equals(TEST_WRAPPER_MESSAGE)) {
                        t[0] = t[0].getCause();
                    }
                    throw t[0];
                }
            }
        };
    }


    private static void launchMinecraftDataRun() throws IOException {
        Path runDir = Files.createTempDirectory("MinecraftTestRunner");

        Properties env = new Properties();
        LOGGER.info("Reading env from " + MinecraftTestRunner.class.getResource("/env.txt"));
        InputStream resourceAsStream = MinecraftTestRunner.class.getResourceAsStream("/env.txt");
        env.load(new BufferedReader(new InputStreamReader(resourceAsStream)));

        String modClasses;
        try {
            String pathStr = MinecraftTestRunner.class.getResource("/").toURI().getPath();
            Path path = Paths.get(pathStr);
            Path grandparent = path.getParent().getParent();

            Map<String, String> mainPaths = new HashMap<>();
            List<String> extraPaths = new ArrayList<>();
            try (Stream<Path> stream = Files.list(grandparent)) {
                for (Path parent : stream.collect(Collectors.toList())) {
                    try (Stream<Path> paths = Files.list(parent)) {
                        List<Path> pathList = paths.collect(Collectors.toList());
                        for (Path p : pathList) {
                            String modid = findModId(p.toAbsolutePath().toString());
                            if (modid == null) {
                                extraPaths.add(p.toAbsolutePath().toString());
                            } else {
                                mainPaths.put(modid, p.toAbsolutePath().toString());
                            }
                        }
                    }
                }
            }

            if (!mainPaths.containsKey("testrunner")) {
                throw new IllegalStateException("Mod testrunner mods.toml not found! Search root: " + grandparent.toAbsolutePath().toString());
            }

            StringBuilder classes = new StringBuilder(2048);
            String mainTestRunnerPath = mainPaths.remove("testrunner");
            classes.append("testrunner%%").append(mainTestRunnerPath).append(":testrunner%%").append(mainTestRunnerPath);
            for (String extraPath : extraPaths) {
                classes.append(":testrunner%%").append(extraPath);
            }
            mainPaths.forEach((id, p) ->
                    classes.append(":").append(id).append("%%").append(p)
                            .append(":").append(id).append("%%").append(p));

            modClasses = classes.toString();

            // pattern: (?)
            // mod1:mainResources, mod1:mainClasses, mod1:otherClassesResources, mod1:otherClassesResources...
            // mod2:mainResources, mod2:mainClasses, mod2:otherClassesResources, mod2:otherClassesResources...
            // modClasses = "testrunner%%" + pathStr + ":" + "testrunner%%" + pathStr;
            // modClasses += ":cubicchunks%%/home/bartosz/Desktop/dev/java/Minecraft/CubicChunks/CubicChunks/1.15/out/production/1.15.main";
            // modClasses += ":cubicchunks%%/home/bartosz/Desktop/dev/java/Minecraft/CubicChunks/CubicChunks/1.15/out/production/1.15.main";
            // modClasses += ":cubicchunks%%/home/bartosz/Desktop/dev/java/Minecraft/CubicChunks/CubicChunks/1.15/out/production/1.15.api";

            LOGGER.info("ModClasses=" + modClasses);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
        env.setProperty("MOD_CLASSES", modClasses);

        for (Map.Entry<Object, Object> objectObjectEntry : env.entrySet()) {
            try {
                injectEnvironmentVariable("" + objectObjectEntry.getKey(), "" + objectObjectEntry.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Launcher.main(
                "--gameDir", runDir.toString(),
                "--launchTarget", env.getProperty("target"),
                "--fml.mcpVersio", env.getProperty("MCP_VERSION"),
                "--fml.mcVersion", env.getProperty("MC_VERSION"),
                "--fml.forgeGroup", env.getProperty("FORGE_GROUP"),
                "--fml.forgeVersion", env.getProperty("FORGE_VERSION"),
                "--mod",
                "testrunner",
                "--all",
                "--output",
                runDir.toString()
        );
    }

    @Nullable
    private static String findModId(String a) {
        Path modsToml = Paths.get(a).resolve("META-INF").resolve("mods.toml");
        if (!Files.exists(modsToml)) {
            return null;
        }
        try {
            return Files.readAllLines(modsToml).stream()
                    .filter(l -> l.trim().startsWith("modId="))
                    .map(s -> s.substring("modId=".length()))
                    .map(s -> s.substring(1, s.length() - 1))//remove quotes
                    .findFirst().orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void injectEnvironmentVariable(String key, String value) throws Exception {
        LOGGER.info("Injecting environment variable " + key + " = " + value);
        Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");

        Field unmodifiableMapField = getAccessibleField(processEnvironment, "theUnmodifiableEnvironment");
        Object unmodifiableMap = unmodifiableMapField.get(null);
        injectIntoUnmodifiableMap(key, value, unmodifiableMap);

        Class<?> variableClass = Class.forName("java.lang.ProcessEnvironment$Variable");
        Method valueOfQueryOnly = variableClass.getDeclaredMethod("valueOfQueryOnly", String.class);
        valueOfQueryOnly.setAccessible(true);
        Object variable = valueOfQueryOnly.invoke(null, key);

        Class<?> valueClass = Class.forName("java.lang.ProcessEnvironment$Value");
        Method valueOfQueryOnlyVal = valueClass.getDeclaredMethod("valueOfQueryOnly", String.class);
        valueOfQueryOnlyVal.setAccessible(true);
        Object valueObj = valueOfQueryOnlyVal.invoke(null, value);

        Field mapField = getAccessibleField(processEnvironment, "theEnvironment");
        Map<Object, Object> map = (Map<Object, Object>) mapField.get(null);
        map.put(variable, valueObj);
    }

    private static Field getAccessibleField(Class<?> clazz, String fieldName)
            throws NoSuchFieldException {

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private static void injectIntoUnmodifiableMap(String key, String value, Object map)
            throws ReflectiveOperationException {

        Class<?> unmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
        Field field = getAccessibleField(unmodifiableMap, "m");
        Object obj = field.get(map);
        //((Map<String, String>) obj).put(key, value);
    }

}
