package com.timing.plugin.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

public final class TimingMethodAdapter extends LocalVariablesSorter implements Opcodes {

    private int startVarIndex;

    private String methodName;

    public TimingMethodAdapter(String name, int access, String desc, MethodVisitor mv) {
        super(Opcodes.ASM5, access, desc, mv);
        this.methodName = name.replace("/", ".");
    }

    @Override
    public void visitCode() {
        super.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        startVarIndex = newLocal(Type.LONG_TYPE);
        mv.visitVarInsn(Opcodes.LSTORE, startVarIndex);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitVarInsn(LLOAD, startVarIndex);
            mv.visitInsn(LSUB);
            int index = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, index);
            mv.visitLdcInsn(methodName);
            mv.visitVarInsn(LLOAD, index);
            mv.visitMethodInsn(INVOKESTATIC, "com/time/timeplugin/block/BlockManager", "timingMethod", "(Ljava/lang/String;J)V", false);
        }
        super.visitInsn(opcode);

//        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
//        mv.visitVarInsn(LLOAD, 1);
//        mv.visitInsn(LSUB);
//        int index = newLocal(Type.LONG_TYPE);
//        mv.visitVarInsn(LSTORE, index);
//
//        mv.visitLdcInsn("com.quinn.hunter.timing.MainActivity\\onCreate");
//        mv.visitVarInsn(LLOAD, index);
//        mv.visitMethodInsn(INVOKESTATIC, "com/hunter/library/timing/BlockManager", "timingMethod", "(Ljava/lang/String;J)V", false);
    }
//    mv = cw.visitMethod(ACC_PUBLIC, "a", "()V", null, null);
//            mv.visitCode();
//    Label l0 = new Label();
//            mv.visitLabel(l0);
//            mv.visitLineNumber(36, l0);
//            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
//            mv.visitVarInsn(LSTORE, 1);
//    Label l1 = new Label();
//            mv.visitLabel(l1);
//            mv.visitLineNumber(37, l1);
//            mv.visitIntInsn(BIPUSH, 13);
//            mv.visitVarInsn(ISTORE, 3);
//    Label l2 = new Label();
//            mv.visitLabel(l2);
//            mv.visitLineNumber(38, l2);
//            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
//            mv.visitVarInsn(LLOAD, 1);
//            mv.visitInsn(LSUB);
//            mv.visitVarInsn(LSTORE, 4);
//    Label l3 = new Label();
//            mv.visitLabel(l3);
//            mv.visitLineNumber(39, l3);
//            mv.visitLdcInsn("com.quinn.hunter.timing.MainActivity\\onCreate");
//            mv.visitVarInsn(LLOAD, 4);
//            mv.visitMethodInsn(INVOKESTATIC, "com/hunter/library/timing/BlockManager", "timingMethod", "(Ljava/lang/String;J)V", false);
}
