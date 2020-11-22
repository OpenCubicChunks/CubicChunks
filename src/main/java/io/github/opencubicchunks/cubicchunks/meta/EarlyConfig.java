package io.github.opencubicchunks.cubicchunks.meta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

public class EarlyConfig {

    private static final String FILE_NAME = "config/cubicchunks/earlyconfig.properties";

    private static final int[] VALID_CUBE_DIAMETERS = new int[] { 1, 2, 4, 8 };

    private static final String PROPERTY_NAME_DIAMETER_IN_SECTIONS = "CUBE_DIAMETER_IN_SECTIONS";
    private static final int DEFAULT_DIAMETER_IN_SECTIONS = 2; //Default

    public static int getDiameterInSections() {
        int diameter = EarlyConfig.DEFAULT_DIAMETER_IN_SECTIONS;
        try {
            Properties prop = new Properties();
            if (Files.exists(Paths.get(FILE_NAME))) {
                try (InputStream inputStream = Files.newInputStream(Paths.get(FILE_NAME))) {
                    //If there are any exceptions while loading, return default.
                    prop.load(inputStream);

                    diameter = Integer.parseInt(EarlyConfig.getPropertyOrSetDefault(prop, PROPERTY_NAME_DIAMETER_IN_SECTIONS, String.valueOf(diameter)));

                    boolean valid = false;
                    for (int d : VALID_CUBE_DIAMETERS) {
                        if (diameter == d) {
                            valid = true;
                            break;
                        }

                    }
                    if (!valid) {
                        throw new UnsupportedOperationException(PROPERTY_NAME_DIAMETER_IN_SECTIONS + " " + diameter + " is not supported. Please use one of " +
                            Arrays.stream(VALID_CUBE_DIAMETERS).mapToObj(String::valueOf).collect(Collectors.joining(", ", "[", "]")) + ".");
                    }
                }
                prop.setProperty(PROPERTY_NAME_DIAMETER_IN_SECTIONS, String.valueOf(diameter));
                try (OutputStream out = Files.newOutputStream(Paths.get(FILE_NAME))) {
                    prop.store(out, "");
                }
            }
            //If the file is not there we just use the default (and dont create the file). This should prevent most users
            // from using this setting without actually knowing what it is
            // EarlyConfig.createDefaultEarlyConfigFile(FILE_NAME, prop);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return diameter;
    }

    private static void createDefaultEarlyConfigFile(String fileName, Properties prop) throws IOException {
        File file = new File(fileName);

        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        prop.setProperty(PROPERTY_NAME_DIAMETER_IN_SECTIONS, String.valueOf(EarlyConfig.DEFAULT_DIAMETER_IN_SECTIONS));

        prop.store(new FileOutputStream(file, false), null);
    }

    private static String getPropertyOrSetDefault(Properties prop, String propName, String defaultValue) {
        String property = prop.getProperty(propName);

        if (property != null) {
            return property;
        } else {
            prop.setProperty(propName, defaultValue);
            return defaultValue;
        }
    }

}