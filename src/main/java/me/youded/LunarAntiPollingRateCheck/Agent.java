package me.youded.LunarAntiPollingRateCheck;

import java.lang.instrument.Instrumentation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class Agent {
    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classfileBuffer == null || classfileBuffer.length == 0) {
                    return new byte[0];
                }
                if (!className.startsWith("com/moonsworth/lunar/client")) {
                    return classfileBuffer;
                }

                ClassReader cr = new ClassReader(classfileBuffer);
                if (cr.getSuperName().equals("java/lang/Object") && cr.getInterfaces().length == 1
                        && cr.getInterfaces()[0].startsWith("com/moonsworth/lunar/client")) {
                    ClassNode cn = new ClassNode();

                    cr.accept(cn, 0);

                    for (MethodNode method : cn.methods) {
                        if (method.name.equals("start") && method.desc.equals("()V")) {
                            for (AbstractInsnNode insn : method.instructions) {
                                if (insn.getOpcode() == Opcodes.LDC && ((LdcInsnNode) insn).cst
                                        .equals("Unable to start polling detection thread in headless client!")) {
                                    for (MethodNode methoda : cn.methods) {
                                        methoda.instructions.clear();
                                        methoda.localVariables.clear();
                                        methoda.exceptions.clear();
                                        methoda.tryCatchBlocks.clear();
                                        InsnList insnList = new InsnList();
                                        if (methoda.desc.endsWith(")V")) {
                                            insnList.add(new InsnNode(Opcodes.RETURN));
                                        } else {
                                            insnList.add(new InsnNode(Opcodes.ICONST_1));
                                            insnList.add(new InsnNode(Opcodes.IRETURN));
                                        }
                                        methoda.instructions.insert(insnList);
                                    }
                                    ClassWriter cw = new ClassWriter(cr, 0);
                                    cn.accept(cw);
                                    return cw.toByteArray();
                                }
                            }
                        }
                    }
                }
                return classfileBuffer;
            }
        });
    }
}