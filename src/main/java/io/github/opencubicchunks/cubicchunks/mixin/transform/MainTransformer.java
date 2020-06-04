package io.github.opencubicchunks.cubicchunks.mixin.transform;

import com.google.common.collect.Sets;
import net.minecraftforge.coremod.api.ASMAPI;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainTransformer {

    public static void transformChunkManager(ClassNode targetClass) {
        final String targetMethod =
                ASMAPI.mapMethod("func_219213_a") + "(JILnet/minecraft/world/server/ChunkHolder;I)Lnet/minecraft/world/server/ChunkHolder;";
        final String newTargetName = "setCubeLevel";

        final String chunkHolderSetChunkLevel = ASMAPI.mapMethod("func_219292_a");

        Map<String, String> methodRedirects = new HashMap<>();
        methodRedirects.put("net/minecraft/world/server/ChunkHolder." + chunkHolderSetChunkLevel + "(I)V", chunkHolderSetChunkLevel);
        methodRedirects.put("it/unimi/dsi/fastutil/longs/LongSet.add(J)Z", "add");
        methodRedirects.put("it/unimi/dsi/fastutil/longs/LongSet.remove(J)Z", "remove");
        methodRedirects.put("it/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap.put(JLjava/lang/Object;)Ljava/lang/Object;", "put");
        methodRedirects.put("it/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap.remove(J)Ljava/lang/Object;", "remove");

        final String MAX_LOADED_LEVEL = ASMAPI.mapField("field_219249_a");
        final String unloadableChunks = ASMAPI.mapField("field_219261_o");
        final String chunksToUnload = ASMAPI.mapField("field_219253_g");
        final String chunkTaskPriorityQueueSorter = ASMAPI.mapField("field_219263_q");
        final String immutableLoadedChunksDirty = ASMAPI.mapField("field_219262_p");
        final String lightManager = ASMAPI.mapField("field_219256_j");
        final String loadedChunks = ASMAPI.mapField("field_219251_e");

        Map<String, String> fieldRedirects = new HashMap<>();
        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + MAX_LOADED_LEVEL, MAX_LOADED_LEVEL);
        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + unloadableChunks, "unloadableCubes");
        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + chunksToUnload, "cubesToUnload");
        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + chunkTaskPriorityQueueSorter, "cubeTaskPriorityQueueSorter");
        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + loadedChunks, "loadedCubes");

        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + immutableLoadedChunksDirty, immutableLoadedChunksDirty);
        fieldRedirects.put("net/minecraft/world/server/ChunkManager." + lightManager, lightManager);

        Map<String, String> typeRedirects = new HashMap<>();
        // TODO: create target constructor in ChunkHolder with CubePos
        // typeRedirects.put("net/minecraft/util/math/ChunkPos", "io/github/opencubicchunks/cubicchunks/chunk/util/CubePos");
        typeRedirects.put("net/minecraft/util/math/ChunkPos", "net/minecraft/util/math/ChunkPos");
        typeRedirects.put("net/minecraft/world/server/ChunkManager", "net/minecraft/world/server/ChunkManager");
        typeRedirects.put("net/minecraft/world/server/ChunkHolder", "net/minecraft/world/server/ChunkHolder");
        typeRedirects.put("it/unimi/dsi/fastutil/longs/LongCollection", "it/unimi/dsi/fastutil/longs/LongCollection");
        typeRedirects.put("it/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap", "it/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap");
        typeRedirects.put("it/unimi/dsi/fastutil/longs/LongSet", "it/unimi/dsi/fastutil/longs/LongSet");
        typeRedirects.put("net/minecraft/world/server/ServerWorldLightManager", "net/minecraft/world/server/ServerWorldLightManager");
        typeRedirects.put("net/minecraft/world/lighting/WorldLightManager", "net/minecraft/world/lighting/WorldLightManager");
        // light manager implements this
        typeRedirects.put("net/minecraft/world/server/ChunkHolder$IListener", "net/minecraft/world/server/ChunkHolder$IListener");
        // superclass
        typeRedirects.put("net/minecraft/world/server/ChunkHolder$IPlayerProvider", "net/minecraft/world/server/ChunkHolder$IPlayerProvider");
        // TODO: generate that class at runtime? transform and duplicate?
        typeRedirects.put("net/minecraft/world/chunk/ChunkTaskPriorityQueueSorter",
                "io/github/opencubicchunks/cubicchunks/chunk/ticket/CubeTaskPriorityQueueSorter");

        MethodNode newMethod = cloneAndApplyRedirects(targetClass, targetMethod, newTargetName, methodRedirects, fieldRedirects, typeRedirects);
        makeStaticSyntheticAccessor(targetClass, newMethod);
    }

    private static void makeStaticSyntheticAccessor(ClassNode node, MethodNode newMethod) {
        Type[] params = Type.getArgumentTypes(newMethod.desc);
        Type[] newParams = new Type[params.length + 1];
        System.arraycopy(params, 0, newParams, 1, params.length);
        newParams[0] = Type.getObjectType(node.name);

        Type returnType = Type.getReturnType(newMethod.desc);
        MethodNode newNode = new MethodNode(newMethod.access | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, newMethod.name,
                Type.getMethodDescriptor(returnType, newParams), null, null);

        int j = 0;
        for (Type param : newParams) {
            newNode.instructions.add(new VarInsnNode(param.getOpcode(Opcodes.ILOAD), j));
            j += param.getSize();
        }
        newNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, newMethod.name, newMethod.desc, false));
        newNode.instructions.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        node.methods.add(newNode);
    }

    public static void transformProxyTicketManager(ClassNode targetClass) {
        final String setChunkLevel =
                ASMAPI.mapMethod("func_219372_a") + "(JILnet/minecraft/world/server/ChunkHolder;I)Lnet/minecraft/world/server/ChunkHolder;";
        final String setCubeLevel = "setCubeLevel";

        Map<String, String> methodRedirects = new HashMap<>();
        methodRedirects.put(// synthetic accessor for func_219213_a (setChunkLevel)
                "net/minecraft/world/server/ChunkManager.access$700(Lnet/minecraft/world/server/ChunkManager;"
                        + "JILnet/minecraft/world/server/ChunkHolder;I)Lnet/minecraft/world/server/ChunkHolder;",
                "setCubeLevel");

        Map<String, String> fieldRedirects = new HashMap<>();
        fieldRedirects.put("net/minecraft/world/server/ChunkManager$ProxyTicketManager.this$0", "this$0");

        Map<String, String> typeRedirects = new HashMap<>();
        typeRedirects.put("net/minecraft/world/server/ChunkManager", "net/minecraft/world/server/ChunkManager");
        typeRedirects.put("net/minecraft/world/server/ChunkHolder", "net/minecraft/world/server/ChunkHolder");

        cloneAndApplyRedirects(targetClass, setChunkLevel, setCubeLevel, methodRedirects, fieldRedirects, typeRedirects);
    }

    private static MethodNode cloneAndApplyRedirects(ClassNode node, String existingDesc, String newName,
            Map<String, String> methodRedirects, Map<String, String> fieldRedirects, Map<String, String> typeRedirects) {
        MethodNode m = node.methods.stream().filter(x -> (x.name + x.desc).equals(existingDesc)).findAny().orElseThrow(() ->
                new IllegalStateException("Target method " + existingDesc + " not found"));

        Set<String> defaultKnownClasses = Sets.newHashSet(
                "javax/annotation/Nullable",
                "java/lang/Object",
                "java/lang/String",
                node.name
        );

        Remapper remapper = new Remapper() {
            @Override
            public String mapMethodName(final String owner, final String name, final String descriptor) {
                if (name.equals("<init>")) {
                    return name;
                }
                String key = owner + '.' + name + descriptor;
                String mappedName = methodRedirects.get(key);
                if (mappedName == null) {
                    throw new IllegalStateException("Unknown method " + key);
                }
                return mappedName;
            }

            @Override
            public String mapInvokeDynamicMethodName(final String name, final String descriptor) {
                throw new UnsupportedOperationException("Not implemented yet");
            }

            @Override
            public String mapFieldName(final String owner, final String name, final String descriptor) {
                String key = owner + '.' + name;
                String mapped = fieldRedirects.get(key);
                if (mapped == null) {
                    throw new IllegalStateException("Unknown field " + key);
                }
                return mapped;
            }

            @Override
            public String map(final String key) {
                String mapped = typeRedirects.get(key);
                if (mapped == null && defaultKnownClasses.contains(key)) {
                    mapped = key;
                }
                if (mapped == null) {
                    throw new IllegalStateException("Unknown class " + key);
                }
                return mapped;
            }
        };
        MethodNode output = new MethodNode(m.access, newName, m.desc, m.signature, m.exceptions.toArray(new String[0]));
        MethodRemapper methodRemapper = new MethodRemapper(output, remapper);

        m.accept(methodRemapper);
        output.name = newName;
        // remove protected and private, add public
        output.access &= ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
        output.access |= Opcodes.ACC_PUBLIC;
        node.methods.add(output);

        return output;
    }
}
