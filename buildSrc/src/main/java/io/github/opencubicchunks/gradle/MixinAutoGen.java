package io.github.opencubicchunks.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;

public class MixinAutoGen implements Plugin<Project> {

    @Override public void apply(@Nonnull Project target) {
        MixinGenExtension extension = new MixinGenExtension();
        target.getExtensions().add("mixinGen", extension);
        target.getTasks().create("generateMixinConfigs").doLast(task -> {
            JavaPluginConvention convention = target.getConvention().findByType(JavaPluginConvention.class);
            if (convention == null) {
                convention = target.getConvention().findPlugin(JavaPluginConvention.class);
                if (convention == null) {
                    convention = target.getConvention().getByType(JavaPluginConvention.class);
                }
            }
            try {
                extension.generateFiles(convention);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
