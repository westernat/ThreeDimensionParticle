function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');

    return {
        'add_usage_enum': {
            'target': {
                'type': 'CLASS',
                'name': 'com.mojang.blaze3d.vertex.VertexFormatElement$Usage'
            },
            'transformer': function(classNode) {
                for (var i = 0; i < classNode.fields.size(); i++) {
                    if (classNode.fields.get(i).name === 'THR_DIM_PARTICLE_LIGHT') {
                        return classNode;
                    }
                }

                var clinit = null;
                for (var i = 0; i < classNode.methods.size(); i++) {
                    if (classNode.methods.get(i).name === '<clinit>') {
                        clinit = classNode.methods.get(i);
                        break;
                    }
                }
                if (clinit === null) return classNode;

                var instructions = clinit.instructions;
                var nodes = instructions.toArray();
                if (nodes === null || nodes.length === 0) return classNode;

                var retIdx = -1;
                for (var j = nodes.length - 1; j >= 0; j--) {
                    if (nodes[j].getOpcode() === Opcodes.RETURN) {
                        retIdx = j;
                        break;
                    }
                }
                if (retIdx === -1) return classNode;

                var baseLocals = clinit.maxLocals;
                var oldArr = baseLocals;
                var newArr = baseLocals + 1;
                var newInst = baseLocals + 2;
                clinit.maxLocals = baseLocals + 3;

                var usage = 'com/mojang/blaze3d/vertex/VertexFormatElement$Usage';
                var prox = 'org/mesdag/thr_dim_particle/client/EnumProxy';
                var list = new InsnList();

                // oldArr = $VALUES
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, usage, '$VALUES', '[L' + usage + ';'));
                list.add(new VarInsnNode(Opcodes.ASTORE, oldArr));

                // newArr = new Usage[oldArr.length + 1]
                list.add(new VarInsnNode(Opcodes.ALOAD, oldArr));
                list.add(new InsnNode(Opcodes.ARRAYLENGTH));
                list.add(new InsnNode(Opcodes.ICONST_1));
                list.add(new InsnNode(Opcodes.IADD));
                list.add(new TypeInsnNode(Opcodes.ANEWARRAY, usage));
                list.add(new VarInsnNode(Opcodes.ASTORE, newArr));

                // System.arraycopy(oldArr, 0, newArr, 0, oldArr.length)
                list.add(new VarInsnNode(Opcodes.ALOAD, oldArr));
                list.add(new InsnNode(Opcodes.ICONST_0));
                list.add(new VarInsnNode(Opcodes.ALOAD, newArr));
                list.add(new InsnNode(Opcodes.ICONST_0));
                list.add(new VarInsnNode(Opcodes.ALOAD, oldArr));
                list.add(new InsnNode(Opcodes.ARRAYLENGTH));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'java/lang/System', 'arraycopy', '(Ljava/lang/Object;ILjava/lang/Object;II)V'));

                // newInst = new Usage("THR_DIM_PARTICLE_LIGHT", oldArr.length, LIGHT_NAME, LIGHT_SETUP, LIGHT_CLEAR)
                list.add(new TypeInsnNode(Opcodes.NEW, usage));
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new LdcInsnNode('THR_DIM_PARTICLE_LIGHT'));
                list.add(new VarInsnNode(Opcodes.ALOAD, oldArr));
                list.add(new InsnNode(Opcodes.ARRAYLENGTH));
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, prox, 'LIGHT_NAME', 'Ljava/lang/String;'));
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, prox, 'LIGHT_SETUP', 'Lcom/mojang/blaze3d/vertex/VertexFormatElement$Usage$SetupState;'));
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, prox, 'LIGHT_CLEAR', 'Lcom/mojang/blaze3d/vertex/VertexFormatElement$Usage$ClearState;'));
                list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, usage, '<init>', '(Ljava/lang/String;ILjava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormatElement$Usage$SetupState;Lcom/mojang/blaze3d/vertex/VertexFormatElement$Usage$ClearState;)V'));
                list.add(new VarInsnNode(Opcodes.ASTORE, newInst));

                // EnumProxy.LIGHT.setValue(newInst)
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, prox, 'LIGHT', 'Lorg/mesdag/thr_dim_particle/client/EnumProxy;'));
                list.add(new VarInsnNode(Opcodes.ALOAD, newInst));
                list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, prox, 'setValue', '(Ljava/lang/Enum;)V'));

                // newArr[oldArr.length] = newInst
                list.add(new VarInsnNode(Opcodes.ALOAD, newArr));
                list.add(new VarInsnNode(Opcodes.ALOAD, oldArr));
                list.add(new InsnNode(Opcodes.ARRAYLENGTH));
                list.add(new VarInsnNode(Opcodes.ALOAD, newInst));
                list.add(new InsnNode(Opcodes.AASTORE));

                // $VALUES = newArr
                list.add(new VarInsnNode(Opcodes.ALOAD, newArr));
                list.add(new FieldInsnNode(Opcodes.PUTSTATIC, usage, '$VALUES', '[L' + usage + ';'));

                instructions.insertBefore(nodes[retIdx], list);

                return classNode;
            }
        }
    };
}
