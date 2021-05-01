package io.github.opencubicchunks.gradle;

import static java.util.Collections.singletonMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.ParameterDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.tasks.TaskDependency;

public class MergedMappingsDependency implements SelfResolvingDependency {

    private final Project project;
    private final Configuration configuration;
    private final Dependency main;
    private final Dependency fallback;
    private String reason;

    public MergedMappingsDependency(Project project, Configuration configuration, Dependency main, Dependency fallback) {
        this.project = project;
        this.configuration = configuration;
        this.main = main;
        this.fallback = fallback;
    }

    @Override public Set<File> resolve() {
        try {
            Path mappingsDir = this.project.getExtensions().findByType(LoomGradleExtension.class).getMappingsProvider().getMappingsDir();
            Path mappingsFile = mappingsDir.resolve(String.format("%s.%s-%s.tiny", getGroup(), getName(), getVersion()));

            if (Files.exists(mappingsFile) && !project.getGradle().getStartParameter().isRefreshDependencies()) {
                return Collections.singleton(mappingsFile.toFile());
            }

            Set<File> fallbackResolved = resolve(fallback);
            Set<File> mainResolved = resolve(main);

            byte[] fallbackData = readMappings(fallbackResolved.iterator().next());
            byte[] mainData = readMappings(mainResolved.iterator().next());

            TinyTree fallbackTree = TinyMappingFactory.loadWithDetection(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fallbackData))));
            TinyTree mainTinyTree = TinyMappingFactory.loadWithDetection(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mainData))));

            Mappings mappingsFallback = new Mappings();
            Mappings mappingsMojang = new Mappings();

            loadMappings(fallbackTree, mappingsFallback, "intermediary", "named");
            loadMappings(mainTinyTree, mappingsMojang, "intermediary", "named");

            Mappings mappings = mergeMappings(mappingsMojang, mappingsFallback);

            try (FileSystem fs = createNewJarFileSystem(mappingsFile)) {
                Files.createDirectories(fs.getPath("mappings"));
                Path output = fs.getPath("mappings/mappings.tiny");
                try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    new TinyWriter(writer, "intermediary", "named").write(mappings);
                }
            }
            return Collections.singleton(mappingsFile.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Mappings mergeMappings(Mappings mappingsMojang, Mappings mappingsFallback) {
        Mappings mappings = new Mappings();
        for (Map.Entry<String, String> classEntry : mappingsMojang.classMappings.entrySet()) {
            String obfClassName = classEntry.getKey();
            mappings.classMappings.put(obfClassName, classEntry.getValue());
            if (mappingsFallback.classComments.containsKey(obfClassName)) {
                mappings.classComments.put(obfClassName, mappingsFallback.classComments.get(obfClassName));
            }

            Map<MemberEntry, String> methods = mappings.methodMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> methodComments = mappings.methodComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> fields = mappings.fieldMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> fieldComments = mappings.fieldComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParams = mappings.paramNames.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParamComments = mappings.paramNames.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());

            Map<MemberEntry, String> methodCommentsIn = mappingsFallback.methodComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> fieldCommentsIn = mappingsFallback.fieldComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParamsIn = mappingsFallback.paramNames.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParamCommentsIn = mappingsFallback.paramComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());

            for (Map.Entry<MemberEntry, String> methodEntry : mappingsMojang.methodMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>()).entrySet()) {
                MemberEntry obfMethod = methodEntry.getKey();
                String mojangDeobf = methodEntry.getValue();
                methods.put(obfMethod, mojangDeobf);

                String methodComment = methodCommentsIn.get(obfMethod);
                if (methodComment != null) {
                    methodComments.put(obfMethod, methodComment);
                }

                Map<Integer, String> methodParams = classParams.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());
                Map<Integer, String> methodParamComments = classParamComments.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());

                Map<Integer, String> methodParamsIn = classParamsIn.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());
                Map<Integer, String> methodParamCommentsIn = classParamCommentsIn.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());

                for (Map.Entry<Integer, String> paramEntry : methodParamsIn.entrySet()) {
                    int idx = paramEntry.getKey();
                    String name = paramEntry.getValue();
                    String comment = methodParamCommentsIn.get(idx);
                    methodParams.put(idx, name);
                    if (comment != null) {
                        methodParamComments.put(idx, comment);
                    }
                }
            }

            for (Map.Entry<MemberEntry, String> fieldEntry : mappingsMojang.fieldMappings.computeIfAbsent(classEntry.getKey(), x -> new LinkedHashMap<>()).entrySet()) {
                MemberEntry obfField = fieldEntry.getKey();
                String mojangDeobf = fieldEntry.getValue();
                fields.put(obfField, mojangDeobf);

                String fieldComment = fieldCommentsIn.get(obfField);
                if (fieldComment != null) {
                    fieldComments.put(obfField, fieldComment);
                }
            }
        }
        for (String obfClassName : mappingsFallback.classMappings.keySet()) {
            if (mappingsMojang.classMappings.containsKey(obfClassName)) {
                continue;
            }
            String fallbackMappedClass = mappingsFallback.classMappings.get(obfClassName);
            if (!obfClassName.equals(fallbackMappedClass)) {
                project.getLogger().warn("Ignoring class remap: " + obfClassName + " -> " + fallbackMappedClass);
            }
            mappings.classComments.put(obfClassName, obfClassName);
            mappings.classComments.put(obfClassName, mappingsFallback.classComments.get(obfClassName));

            Map<MemberEntry, String> methods = mappings.methodMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> methodComments = mappings.methodComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> fields = mappings.fieldMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> fieldComments = mappings.fieldComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParams = mappings.paramNames.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParamComments = mappings.paramNames.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());

            Map<MemberEntry, String> methodCommentsIn = mappingsFallback.methodComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, String> fieldCommentsIn = mappingsFallback.fieldComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParamsIn = mappingsFallback.paramNames.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());
            Map<MemberEntry, Map<Integer, String>> classParamCommentsIn = mappingsFallback.paramComments.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>());

            for (Map.Entry<MemberEntry, String> methodEntry : mappingsFallback.methodMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>()).entrySet()) {
                MemberEntry obfMethod = methodEntry.getKey();
                String fallbackDeobf = methodEntry.getValue();
                if (!obfMethod.name.equals(fallbackDeobf)) {
                    project.getLogger().warn("Ignoring method remap: " + obfClassName + "." + obfMethod.name + obfMethod.signature + " -> " + fallbackDeobf);
                }
                methods.put(obfMethod, obfMethod.name);

                String methodComment = methodCommentsIn.get(obfMethod);
                if (methodComment != null) {
                    methodComments.put(obfMethod, methodComment);
                }

                Map<Integer, String> methodParams = classParams.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());
                Map<Integer, String> methodParamComments = classParamComments.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());

                Map<Integer, String> methodParamsIn = classParamsIn.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());
                Map<Integer, String> methodParamCommentsIn = classParamCommentsIn.computeIfAbsent(obfMethod, x -> new LinkedHashMap<>());

                for (Map.Entry<Integer, String> paramEntry : methodParamsIn.entrySet()) {
                    int idx = paramEntry.getKey();
                    String name = paramEntry.getValue();
                    String comment = methodParamCommentsIn.get(idx);
                    methodParams.put(idx, name);
                    if (comment != null) {
                        methodParamComments.put(idx, comment);
                    }
                }
            }

            for (Map.Entry<MemberEntry, String> fieldEntry : mappingsFallback.fieldMappings.computeIfAbsent(obfClassName, x -> new LinkedHashMap<>()).entrySet()) {
                MemberEntry obfField = fieldEntry.getKey();
                String fallbackDeobf = fieldEntry.getValue();
                if (!obfField.name.equals(fallbackDeobf)) {
                    project.getLogger().warn("Ignoring field remap: " + obfClassName + "." + obfField.name + ":" + obfField.signature + " -> " + fallbackDeobf);
                }
                fields.put(obfField, obfField.name);

                String fieldComment = fieldCommentsIn.get(obfField);
                if (fieldComment != null) {
                    fieldComments.put(obfField, fieldComment);
                }
            }

        }
        return mappings;
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
        return FileSystems.newFileSystem(jarUri, singletonMap("create", "true"));
    }

    private void loadMappings(TinyTree input, Mappings output, String from, String to) {
        for (final ClassDef klass : input.getClasses()) {
            String className = klass.getName(from);
            output.classMappings.put(className, klass.getName(to));
            String classComment = klass.getComment();
            if (classComment != null && !classComment.isEmpty()) {
                classComment = escape(classComment);
                output.classComments.put(className, classComment);
            }
            for (final FieldDef field : klass.getFields()) {
                MemberEntry fieldEntry = new MemberEntry(field.getDescriptor(from), field.getName(from));
                Map<MemberEntry, String> fieldMap = output.fieldMappings.computeIfAbsent(className, x -> new LinkedHashMap<>());
                // fields may change signatures between versions and loom assumes field names are unique in a class
                fieldMap.keySet().removeIf(x -> x.name.equals(fieldEntry.name));

                fieldMap.put(fieldEntry, field.getName(to));

                String comment = field.getComment();
                if (comment != null && !comment.isEmpty()) {
                    comment = escape(comment);
                    output.fieldComments.computeIfAbsent(className, x -> new LinkedHashMap<>()).put(fieldEntry, comment);
                }
            }

            for (final MethodDef method : klass.getMethods()) {
                MemberEntry methodEntry = new MemberEntry(method.getDescriptor(from), method.getName(from));
                output.methodMappings.computeIfAbsent(className, x -> new LinkedHashMap<>()).put(methodEntry, method.getName(to));
                String comment = method.getComment();
                if (comment != null && !comment.isEmpty()) {
                    comment = escape(comment);
                    output.methodComments.computeIfAbsent(className, x -> new LinkedHashMap<>()).put(methodEntry, comment);
                }
                for (ParameterDef parameter : method.getParameters()) {
                    int idx = parameter.getLocalVariableIndex();
                    output.paramNames.computeIfAbsent(className, x -> new LinkedHashMap<>()).computeIfAbsent(methodEntry, x -> new LinkedHashMap<>()).put(idx, parameter.getName(to));
                    String paramComment = parameter.getComment();
                    if (paramComment != null && !paramComment.isEmpty()) {
                        paramComment = escape(paramComment);
                        output.paramComments.computeIfAbsent(className, x -> new LinkedHashMap<>()).computeIfAbsent(methodEntry, x -> new LinkedHashMap<>()).put(idx, paramComment);
                    }
                }
            }
        }
    }

    private String escape(String paramComment) {
        return paramComment.replaceAll("\\\\", "\\\\").replaceAll("\n", "\\n").replaceAll("\t", "\\t");
    }

    private byte[] readMappings(File mappingsFile) throws IOException {
        try (FileSystem zipFs = FileSystems.newFileSystem(mappingsFile.toPath(), (ClassLoader) null)) { // Fix compile on Java 11
            Path mappingsPath = zipFs.getPath("mappings/mappings.tiny");
            return Files.readAllBytes(mappingsPath);
        }
    }

    private Set<File> resolve(Dependency dep) {
        if (dep instanceof SelfResolvingDependency) {
            return ((SelfResolvingDependency) dep).resolve();
        }
        return configuration.files(dep);
    }

    @Override public Set<File> resolve(boolean b) {
        return resolve();
    }

    @Override public TaskDependency getBuildDependencies() {
        return task -> Collections.emptySet();
    }

    @Override public String getGroup() {
        return "mergedmaps." + main.getGroup() + "." + fallback.getGroup();
    }

    @Override public String getName() {
        return "merged_" + main.getName() + "_" + fallback.getName();
    }

    @Override public String getVersion() {
        return main.getVersion() + "+(" + fallback.getVersion() + ")";
    }

    @Override public boolean contentEquals(Dependency dependency) {
        return getGroup().equals(dependency.getGroup()) && getName().equals(dependency.getName()) && getVersion().equals(dependency.getVersion());
    }

    @Override public Dependency copy() {
        return new MergedMappingsDependency(project, configuration, main.copy(), fallback.copy());
    }

    @Nullable @Override public String getReason() {
        return this.reason;
    }

    @Override public void because(@Nullable String s) {
        this.reason = s;
    }

    static class Mappings {
        Map<String, String> classMappings = new LinkedHashMap<>();
        Map<String, Map<MemberEntry, String>> methodMappings = new HashMap<>();
        Map<String, Map<MemberEntry, Map<Integer, String>>> paramNames = new HashMap<>();
        Map<String, Map<MemberEntry, String>> fieldMappings = new HashMap<>();

        Map<String, String> classComments = new LinkedHashMap<>();
        Map<String, Map<MemberEntry, String>> methodComments = new HashMap<>();
        Map<String, Map<MemberEntry, Map<Integer, String>>> paramComments = new HashMap<>();
        Map<String, Map<MemberEntry, String>> fieldComments = new HashMap<>();
    }

    static class MemberEntry {
        final String signature;
        final String name;

        MemberEntry(String signature, String name) {
            this.signature = signature;
            this.name = name;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MemberEntry that = (MemberEntry) o;
            return signature.equals(that.signature) && name.equals(that.name);
        }

        @Override public int hashCode() {
            return Objects.hash(signature, name);
        }
    }


    private static class TinyWriter {

        private final PrintWriter writer;
        private final String namespaceFrom;
        private final String namespaceTo;

        protected TinyWriter(Writer writer, String namespaceFrom, String namespaceTo) {
            this.writer = new PrintWriter(writer);
            this.namespaceFrom = namespaceFrom;
            this.namespaceTo = namespaceTo;
        }

        public void write(Mappings mappings) {
            this.writer.println("tiny\t2\t0\t" + this.namespaceFrom + "\t" + this.namespaceTo);

            mappings.classMappings.forEach((classObf, classDeobf) -> {
                this.writer.println("c\t" + classObf + "\t" + classDeobf);
                String classComment = mappings.classComments.get(classObf);
                if (classComment != null) {
                    this.writer.println("\tc\t" + classComment);
                }

                mappings.fieldMappings.getOrDefault(classObf, new HashMap<>()).forEach((fieldEntry, fieldDeobf) -> {
                    this.writer.println("\tf\t" + fieldEntry.signature + "\t" + fieldEntry.name + "\t" + fieldDeobf);
                    String comment = mappings.fieldComments.getOrDefault(classObf, new HashMap<>()).get(fieldEntry);
                    if (comment != null) {
                        this.writer.println("\t\tc\t" + comment);
                    }
                });

                mappings.methodMappings.getOrDefault(classObf, new HashMap<>()).forEach((methodEntry, methodDeobf) -> {
                    this.writer.println("\tm\t" + methodEntry.signature + "\t" + methodEntry.name + "\t" + methodDeobf);
                    String comment = mappings.methodComments.getOrDefault(classObf, new HashMap<>()).get(methodEntry);
                    if (comment != null) {
                        this.writer.println("\t\tc\t" + comment);
                    }
                    mappings.paramNames.getOrDefault(classObf, new HashMap<>()).getOrDefault(methodEntry, new HashMap<>()).forEach((idx, name) -> {
                        this.writer.println("\t\tp\t" + idx + "\t\t" + name);
                        String commentParam = mappings.paramComments.getOrDefault(classObf, new HashMap<>()).getOrDefault(methodEntry, new HashMap<>()).get(idx);
                        if (commentParam != null) {
                            this.writer.println("\t\t\tc\t" + commentParam);
                        }
                    });
                });
            });
        }
    }
}


