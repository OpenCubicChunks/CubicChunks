function initializeCoreMod() {
    return {
        'fortune-hoe': {
            'target': {
                'type': 'CLASS',
                'class': 'net.minecraft.world.server.ChunkHolder'
            },
            'transformer': function(clazz) {
                var ASM = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
                var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
                var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
                var MethodNodeClass = Java.type('org.objectweb.asm.tree.MethodNode');

                var constructor = findMethod(clazz, ASM);
                var descriptor = constructor.desc.split("Lnet/minecraft/world/chunk/ChunkTaskPriorityQueueSorter;").join("Lcubicchunks/cc/chunk/ticket/CubeTaskPriorityQueueSorter;")
                var newMethod = new MethodNodeClass(constructor.access, constructor.name, descriptor, null, null);

                var newInstructions = new InsnList();
                for (var iterator = constructor.instructions.iterator(); iterator.hasNext();) {
                    newInstructions.add(iterator.next());
                }

                // return injectForEachInsn(clazz, Opcodes.IRETURN, function (target) {
                //     var newInstructions = new InsnList();
                //
                //     newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                //     newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                //     newInstructions.add(ASM.buildMethodCall(
                //         "vazkii/quark/base/handler/AsmHooks",
                //         "canFortuneApply",
                //         "(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)Z",
                //         ASM.MethodType.STATIC
                //     ));
                //     newInstructions.add(new InsnNode(Opcodes.IOR));
                //     method.instructions.insertBefore(target, newInstructions);
                // })
            }
        }
    }
}

function findMethod(clazz, asmapi) {
    for (var iterator = clazz.methods.iterator(); iterator.hasNext();) {
        var method = iterator.next();
        if(method.name === "<init>")
        {
            return method;
        }
    }
    return null;
}