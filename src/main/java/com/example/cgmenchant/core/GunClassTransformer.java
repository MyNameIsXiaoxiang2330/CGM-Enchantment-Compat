package com.example.cgmenchant.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class GunClassTransformer implements IClassTransformer {
    private static final String ITEMGUN = "com.mrcrayfish.guns.item.ItemGun";
    private static final String RELOADER = "com.mrcrayfish.guns.event.CommonEvents$ReloadTracker";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (ITEMGUN.equals(transformedName)) return addEnchantability(basicClass);
        if (RELOADER.equals(transformedName)) return patchIsWeaponFull(basicClass);

        return basicClass;
    }

    private byte[] addEnchantability(byte[] bc) {
        ClassReader cr = new ClassReader(bc);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getItemEnchantability", "()I", null, null);
                mv.visitCode();
                mv.visitIntInsn(Opcodes.BIPUSH, 30);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            }
        }, 0);
        return cw.toByteArray();
    }

    /**
     * isWeaponFull() 原始字节码中已有 NBTTagCompound.func_74762_e 调用。
     * 替换 getfield gun → getfield general → getfield maxAmmo
     * 为 aload_1 → ldc "MaxAmmo" → func_74762_e
     * （方法名用 SRG func_74762_e，与原始代码一致）
     */
    private byte[] patchIsWeaponFull(byte[] bc) {
        ClassReader cr = new ClassReader(bc);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
                if (!"isWeaponFull".equals(name) || !"()Z".equals(desc)) return mv;

                return new MethodVisitor(Opcodes.ASM5, mv) {
                    boolean seenAload0 = false;
                    boolean gunField = false;

                    @Override public void visitVarInsn(int op, int v) {
                        if (op == Opcodes.ALOAD && v == 0) { seenAload0 = true; return; }
                        seenAload0 = false;
                        super.visitVarInsn(op, v);
                    }

                    @Override
                    public void visitFieldInsn(int op, String owner, String fName, String fDesc) {
                        if (op == Opcodes.GETFIELD && seenAload0) {
                            if ("gun".equals(fName)) { gunField = true; seenAload0 = false; return; }
                            seenAload0 = false;
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitFieldInsn(op, owner, fName, fDesc);
                            return;
                        }
                        if (op == Opcodes.GETFIELD && gunField) {
                            if ("general".equals(fName)) return;
                            if ("maxAmmo".equals(fName)) {
                                gunField = false;
                                // aload_1 (tag) → ldc "MaxAmmo" → func_74762_e (getInteger, SRG名)
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                super.visitLdcInsn("MaxAmmo");
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                    "net/minecraft/nbt/NBTTagCompound",
                                    "func_74762_e", "(Ljava/lang/String;)I", false);
                                return;
                            }
                        }
                        seenAload0 = false; gunField = false;
                        super.visitFieldInsn(op, owner, fName, fDesc);
                    }

                    @Override public void visitInsn(int op) { seenAload0 = false; super.visitInsn(op); }
                    @Override public void visitIntInsn(int op, int v) { seenAload0 = false; super.visitIntInsn(op, v); }
                    @Override public void visitLdcInsn(Object c) { seenAload0 = false; super.visitLdcInsn(c); }
                    @Override public void visitMethodInsn(int op, String o, String n, String d, boolean i) { seenAload0 = false; super.visitMethodInsn(op, o, n, d, i); }
                    @Override public void visitJumpInsn(int op, Label l) { seenAload0 = false; super.visitJumpInsn(op, l); }
                    @Override public void visitLabel(Label l) { seenAload0 = false; super.visitLabel(l); }
                };
            }
        }, 0);
        return cw.toByteArray();
    }
}
