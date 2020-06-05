package io.github.opencubicchunks.cubicchunks.meta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class EarlyConfig {

    private static final String FILE_NAME = "config/earlyconfig.properties";

    private static final int[] VALID_CUBE_DIAMETERS = new int[] { 1, 2, 4, 8 };

    private static final String PROPERTY_NAME_CUBE_DIAMETER = "CUBE_DIAMETER";
    private static final int DEFAULT_DIAMETER = 2; //Default

    public static int getCubeDiameter() {
        int diameter = EarlyConfig.DEFAULT_DIAMETER;
        try {
            Properties prop = new Properties();
            if (!Files.exists(Paths.get(FILE_NAME))) {
                EarlyConfig.createDefaultEarlyConfigFile(FILE_NAME, prop);
            } else {
                try (InputStream inputStream = Files.newInputStream(Paths.get(FILE_NAME))) {
                    //If there are any exceptions while loading, return default.
                    prop.load(inputStream);

                    diameter = Integer.parseInt(EarlyConfig.getPropertyOrSetDefault(prop, PROPERTY_NAME_CUBE_DIAMETER, String.valueOf(diameter)));

                    boolean valid = false;
                    for (int d : VALID_CUBE_DIAMETERS) {
                        if (diameter == d) {
                            valid = true;
                            break;
                        }

                    }
                    if(!valid) {
                        throw new UnsupportedOperationException("CUBE_DIAMETER " + String.valueOf(diameter) + " is not supported. Please use one of"
                                + " [1, 2, 4, 8].");
                    }
                }
            }
        }catch(IOException e)
        {
            throw new UncheckedIOException(e);
        }

        return diameter;
    }

    private static void createDefaultEarlyConfigFile(String fileName, Properties prop) throws IOException {
        File file = new File(fileName);

        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
        prop.setProperty(PROPERTY_NAME_CUBE_DIAMETER, String.valueOf(EarlyConfig.DEFAULT_DIAMETER));

        prop.store(new FileOutputStream(file, false), null);
    }

    private static String getPropertyOrSetDefault(Properties prop, String propName, String defaultValue) {
        String property = prop.getProperty(propName);

        if(property != null)
            return property;
        else
        {
            prop.setProperty(propName, defaultValue);
            return defaultValue;
        }
    }

}
