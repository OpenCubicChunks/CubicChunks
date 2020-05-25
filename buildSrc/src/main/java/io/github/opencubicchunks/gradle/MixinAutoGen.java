package io.github.opencubicchunks.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;

public class MixinAutoGen implements Plugin<Project> {

    @Override public void apply(@Nonnull Project target) {
        MixinGenExtension extension = new MixinGenExtension();
        target.getExtensions().add("mixinGen", extension);
        Task generateMixinConfigs = target.getTasks().create("generateMixinConfigs");
        generateMixinConfigs.setGroup("mixin");
        generateMixinConfigs.doLast(task -> {
            JavaPluginConvention convention = Utils.getJavaPluginConvention(target);
            try {
                extension.generateFiles(convention);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}