package io.github.opencubicchunks.cubicchunks.config;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlWriter;

public class AbstractCommentedConfigHelper {

    private final CommentedConfig config;
    private final Path filePath;

    public AbstractCommentedConfigHelper(Path filePath) {
        this.filePath = filePath;
        if (!filePath.getParent().toFile().exists()) {
            try {
                Files.createDirectories(filePath.getParent());
            } catch (IOException e) {

            }
        }

        CommentedFileConfig config = CommentedFileConfig.builder(filePath).sync().autosave().writingMode(WritingMode.REPLACE).build();
        config.load();
        this.config = config;
    }


    public <T> List<T> addList(String comment, String key, List<T> defaultValue) {
        if (config.get(key) == null) {
            config.set(key, defaultValue);
        }

        if (config.getComment(key) == null) {
            config.setComment(key, comment);
        }
        return config.get(key);
    }


    public Map<?, ?> addMap(String comment, String key, Map<?, Number> defaultValue) {
        if (config.get(key) == null) {
            CommentedConfig subConfig = config.createSubConfig();
            defaultValue.forEach((a, b) -> {
                String subConfigKey = a.toString();
                if (subConfig.get(a.toString()) == null) {
                    subConfig.set(subConfigKey, b);
                }
            });
            config.set(key, subConfig);
        }

        CommentedConfig subConfig = config.get(key);
        String commentValue = config.getComment(key);
        if (commentValue == null) {
            config.setComment(key, comment);
        }

        return subConfig.valueMap();
    }


    public <T extends Number> T addNumber(String comment, String key, T defaultValue, T min, T max) {
        if (config.get(key) == null) {
            config.set(key, defaultValue);
        }

        if (config.getComment(key) == null) {
            config.setComment(key, comment + String.format("\nRange: %s-%s", min, max));
        }
        return config.get(key);
    }

    public <T> void updateValue(String key, T newValue) {
        this.config.set(key, newValue);
    }

    public void build() {
        TomlWriter writer = new TomlWriter();
        writer.write(config, filePath.toFile(), WritingMode.REPLACE);
    }
}