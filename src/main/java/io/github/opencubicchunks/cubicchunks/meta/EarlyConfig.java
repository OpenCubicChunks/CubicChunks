package io.github.opencubicchunks.cubicchunks.meta;

import io.github.opencubicchunks.cubicchunks.CubicChunks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class EarlyConfig {

    private static String fileName = "config/earlyconfig.properties";

    public static int getCubeDiameter() {
        int diameter = 2; //Default
        try {
            Properties prop = new Properties();
            if (!Files.exists(Paths.get(fileName))) {
                EarlyConfig.createDefaultEarlyConfigFile(fileName, prop);
            } else {
                try (InputStream inputStream = Files.newInputStream(Paths.get(fileName))) {
                    //If there are any exceptions while loading, return default.
                    prop.load(inputStream);

                    diameter = Integer.parseInt(EarlyConfig.getPropertyOrSetDefault(prop, "CUBEDIAMETER", String.valueOf(diameter)));
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
        prop.setProperty("CUBEDIAMETER", "2");

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
