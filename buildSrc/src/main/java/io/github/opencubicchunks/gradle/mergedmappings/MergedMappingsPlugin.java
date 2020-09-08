package io.github.opencubicchunks.gradle.mergedmappings;

import net.minecraftforge.gradle.common.util.MinecraftExtension;
import net.minecraftforge.gradle.mcp.task.DownloadMCPMappingsTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class MergedMappingsPlugin implements Plugin<Project> {

    @Override public void apply(Project project) {
        Path mappingsInfoPath = project.file("build/mergedMappings.txt").toPath();
        project.getExtensions().configure(MinecraftExtension.class, mc -> {
            if (project.file("build/mergedMappings.txt").exists()) {
                try {
                    List<String> lines = Files.readAllLines(project.file("build/mergedMappings.txt").toPath());
                    mc.mappings(lines.get(0), lines.get(1));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                mc.mappings("official", "1.16.2");
            }
        });

        MergedMappingsExtension mergedMappings = project.getExtensions().create("mergedMappings", MergedMappingsExtension.class);

        project.afterEvaluate(p-> {
            try {
                if (project.file("build/mergedMappings.txt").exists()) {
                    List<String> lines = Files.readAllLines(project.file("build/mergedMappings.txt").toPath());
                    if (!lines.get(0).equals("snapshot") || !lines.get(1)
                            .equals("merged-" + mergedMappings.getOfficialMappings() + "-" + mergedMappings.getParamMappingsVersion())) {
                        Files.delete(mappingsInfoPath);
                        throw new RuntimeException("Merged mappings outdated, "
                                + "re-run generateMergedMappings task or try again to use official mappings without mcp");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        TaskProvider<DownloadMCPMappingsTask> dlOfficialMappings = project.getTasks()
                .register("downloadOfficialMappingsForMerge", DownloadMCPMappingsTask.class);
        File officialFile = project.file("build/mappings_official.zip");
        dlOfficialMappings.configure((task) -> {
            task.setMappings("official_" + mergedMappings.getOfficialMappings());
            task.setOutput(project.file("build/mappings_official.zip"));
        });

        TaskProvider<DownloadMCPMappingsTask> dlParamMappings = project.getTasks()
                .register("downloadParamMappingsForMerge", DownloadMCPMappingsTask.class);
        File paramsFile = project.file("build/mappings_params.zip");
        dlParamMappings.configure((task) -> {
            task.setMappings(mergedMappings.getParamMappingsChannel() + "_" + mergedMappings.getParamMappingsVersion());
            task.setOutput(project.file("build/mappings_params.zip"));
        });

        TaskProvider<Task> createMergedMappings = project.getTasks().register("createMergedMappings");
        File output = project.file("build/mappings_merged.zip");
        createMergedMappings.configure(task -> {
            task.dependsOn(dlOfficialMappings, dlParamMappings);
            task.getOutputs().file(output);
            task.doLast(self -> {
                if (output.exists()) {
                    output.delete();
                }
                try (FileSystem officialMappingsFS = FileSystems.newFileSystem(officialFile.toPath(), null);
                        FileSystem paramsFS = FileSystems.newFileSystem(paramsFile.toPath(), null);
                        FileSystem outputFS = createNewJarFileSystem(output.toPath())) {
                    Files.copy(officialMappingsFS.getPath("fields.csv"), outputFS.getPath("fields.csv"));
                    Files.copy(officialMappingsFS.getPath("methods.csv"), outputFS.getPath("methods.csv"));
                    Files.copy(paramsFS.getPath("params.csv"), outputFS.getPath("params.csv"));
                    Files.write(project.file("build/mergedMappings.txt").toPath(), Arrays.asList(
                            "snapshot",
                            "merged-" + mergedMappings.getOfficialMappings() + "-" + mergedMappings.getParamMappingsVersion()
                    ));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        });

        project.getExtensions().configure(PublishingExtension.class, publishing -> {
            publishing.getPublications().create("mavenLocalMappings", MavenPublication.class, publication -> {
                publication.setGroupId("net.minecraft");
                publication.setArtifactId("mappings_snapshot");
                project.afterEvaluate((x) -> {
                    publication.setVersion("merged-" + mergedMappings.getOfficialMappings() + "-" + mergedMappings.getParamMappingsVersion());
                });
                publication.artifact(output);
            });
            publishing.getPublications().create("mavenLocalMappingsMcp", MavenPublication.class, publication -> {
                publication.setGroupId("de.oceanlabs.mcp");
                publication.setArtifactId("mcp_snapshot");
                project.afterEvaluate((x) -> {
                    publication.setVersion("merged-" + mergedMappings.getOfficialMappings() + "-" + mergedMappings.getParamMappingsVersion());
                });
                publication.artifact(output);
            });
        });

        TaskProvider<PublishToMavenLocal> task1 = project.getTasks().register("uploadMergedMappings1", PublishToMavenLocal.class, task -> {
            task.setPublication((MavenPublication) project.getExtensions().getByType(PublishingExtension.class).getPublications()
                    .getByName("mavenLocalMappings"));
            task.dependsOn(createMergedMappings);
        });
        TaskProvider<PublishToMavenLocal> task2 = project.getTasks().register("uploadMergedMappings2", PublishToMavenLocal.class, task -> {
            task.setPublication((MavenPublication) project.getExtensions().getByType(PublishingExtension.class).getPublications()
                    .getByName("mavenLocalMappingsMcp"));
            task.dependsOn(createMergedMappings);
        });
        project.getTasks().register("generateMergedMappings", task -> {
            task.dependsOn(task1, task2);
        });

    }

    public static FileSystem createNewJarFileSystem(Path jarPath) throws IOException {
        Files.createDirectories(jarPath.getParent());
        Files.deleteIfExists(jarPath);
        URI uri = jarPath.toUri();
        URI jarUri;
        try {
            jarUri = new URI("jar:" + uri.getScheme(), uri.getPath(), null);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
        return FileSystems.newFileSystem(jarUri, Collections.singletonMap("create", "true"));
    }

    public static class MergedMappingsExtension {

        private String officialMappings;
        private String paramMappingsChannel;
        private String paramMappingsVersion;

        public String getOfficialMappings() {
            return officialMappings;
        }

        public void setOfficialMappings(String officialMappings) {
            this.officialMappings = officialMappings;
        }

        public String getParamMappingsChannel() {
            return paramMappingsChannel;
        }

        public void setParamMappingsChannel(String paramMappingsChannel) {
            this.paramMappingsChannel = paramMappingsChannel;
        }

        public String getParamMappingsVersion() {
            return paramMappingsVersion;
        }

        public void setParamMappingsVersion(String paramMappingsVersion) {
            this.paramMappingsVersion = paramMappingsVersion;
        }
    }
}
