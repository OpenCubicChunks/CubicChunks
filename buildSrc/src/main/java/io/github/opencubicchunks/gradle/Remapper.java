package io.github.opencubicchunks.gradle;

import static net.minecraftforge.remapper.MappingDownloader.getCsvs;
import static net.minecraftforge.remapper.MappingDownloader.getMaven;
import static net.minecraftforge.remapper.MappingDownloader.getMcp;
import static net.minecraftforge.remapper.MappingDownloader.mappings;
import static net.minecraftforge.remapper.MappingDownloader.needsDownload;

import com.google.common.io.ByteStreams;
import io.github.opencubicchunks.gradle.fgfix.ForgePluginFixed;
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension;
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin;
import net.minecraftforge.remapper.RemapperTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Remapper implements Plugin<Project> {

    @Override public void apply(Project target) {
        target.getTasks().create("updateMappings").doLast(task -> {
            if (!target.getPlugins().hasPlugin(ForgePluginFixed.class) && !target.getPlugins().hasPlugin(ForgePlugin.class)) {
                throw new IllegalStateException("Forge plugin has not been applied! This is ot supported!");
            }
            String targetMappings = (String) target.getProperties().get("newMappings");
            if (targetMappings == null) {
                throw new IllegalArgumentException("You need to specify target mappings version using -PnewMappings=version");
            }
            File cache = new File(target.getGradle().getGradleUserHomeDir(), "/caches/minecraft/").getAbsoluteFile();
            JavaPluginConvention java = Utils.getJavaPluginConvention(target);
            SourceSet main = java.getSourceSets().getByName("main");
            List<File> deps = new ArrayList<>(main.getCompileClasspath().getFiles());
            List<File> srcs = new ArrayList<>(main.getAllJava().getSrcDirs());
            ForgeExtension forge = target.getExtensions().getByType(ForgeExtension.class);
            String currentMappings = forge.getMappings();
            String mcVersion = forge.getVersion().split("-")[0];
            if (mcVersion.equals("1.12.2")) {
                mcVersion = "1.12";
            }

            downloadMappings(target, mcVersion, currentMappings, targetMappings, cache);
            RemapperTask.runRemapMod_Thread(
                    deps, srcs,
                    mcVersion, currentMappings, targetMappings,
                    cache, line -> target.getLogger().info(line)
            );
        });
    }

    private void downloadMappings(Project target, String mcVersion, String currentMappings, String targetMappings, File cache) {
        if (needsDownload(mcVersion, currentMappings, cache)) {
            download(mcVersion, currentMappings, cache, () -> target.getLogger().info("Downloaded mappings " + currentMappings));
        }
        if (needsDownload(mcVersion, targetMappings, cache)) {
            download(mcVersion, targetMappings, cache, () -> target.getLogger().info("Downloaded mappings " + targetMappings));
        }
    }

    // a copy of MappingDownloader.download because the original downloads no longer existing _nodoc mappings from forge maven
    private void download(final String mcVersion, final String mapping, final File cacheDir, Runnable callback) {
        if (!getMcp(mcVersion, cacheDir, "joined.srg").exists() ||
                !getMcp(mcVersion, cacheDir, "joined.exc").exists() ||
                !getMcp(mcVersion, cacheDir, "static_methods.txt").exists())
        {
            URL maven = getMaven("de.oceanlabs.mcp", "mcp", mcVersion, "srg", "zip");
            File base = getMcp(mcVersion, cacheDir, "");
            downloadZip(base, maven);
        }

        String tmp = mapping.replace("_nodoc", "");
        String channel = tmp.substring(0, tmp.lastIndexOf('_'));
        String version = tmp.substring(tmp.lastIndexOf('_') + 1);

        for (File f : getCsvs(mapping, cacheDir)) {
            if (!f.exists()) {

                String mavenVer = mcVersion;
                for (String key : mappings.keySet()) {
                    if (mappings.get(key).contains(channel + "_" + version)) {
                        mavenVer = key;
                        break;
                    }
                }

                URL maven = getMaven("de.oceanlabs.mcp", "mcp_" + channel, version + "-" + mavenVer, null, "zip");
                downloadZip(f.getParentFile(), maven);
                break;
            }
        }
        callback.run();
    }

    private void downloadZip(File cacheDir, URL url) {
        System.out.println("Downloading: " + url);
        System.out.println("To:          " + cacheDir.getAbsolutePath());

        try(ZipInputStream zis = new ZipInputStream(url.openStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                File cache = new File(cacheDir, entry.getName());
                if (!cache.getParentFile().exists()) {
                    cache.getParentFile().mkdirs();
                }
                try (FileOutputStream out = new FileOutputStream(cache)) {
                    ByteStreams.copy(zis, out);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
