/*
 * Post-Retrolambda ASM pass that injects writeReplace() into every
 * Retrolambda-generated lambda class implementing a Fluxtion
 * LambdaReflection$Serializable* interface.
 *
 * Why: Retrolambda (issue #195) does not synthesize writeReplace() for
 * serializable lambdas, which breaks Fluxtion's LambdaReflection.serialized()
 * default method (it calls getDeclaredMethod("writeReplace") unconditionally).
 *
 * What this does, per Lambda$N class:
 *   1. Detect — name pattern `*$$Lambda$<digits>`, AND implements a
 *      LambdaReflection$Serializable* interface.
 *   2. Read the functional method body (apply/accept/etc.) — always a
 *      single INVOKESTATIC followed by return. Extract the impl method
 *      handle (owner, name, descriptor).
 *   3. Read constructor parameters → captured-arg field names (arg$1, ...).
 *   4. Inject a private writeReplace() method returning a SerializedLambda
 *      describing the functional interface, captured args, and impl handle.
 *
 * Bytecode shape we emit for writeReplace():
 *   private Object writeReplace() {
 *       return new SerializedLambda(
 *           Lambda$N.class,
 *           "<functionalInterface>",
 *           "<functionalMethodName>",
 *           "<functionalMethodDescriptor>",
 *           MethodHandleInfo.REF_invokeStatic,
 *           "<implClass>",
 *           "<implMethodName>",
 *           "<implMethodDescriptor>",
 *           "<instantiatedMethodType>",
 *           new Object[]{ arg$1, arg$2, ... }
 *       );
 *   }
 *
 * `instantiatedMethodType` is set equal to functionalMethodDescriptor —
 * Fluxtion's LambdaReflection.method() only reads getImplMethodName(), so
 * the exact type erasure encoded here doesn't matter for our use case.
 */

package com.telamin.fluxtion.web.buildtools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.invoke.MethodHandleInfo;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public final class RetroPatch {

    static final String SERIALIZABLE_BASE =
            "com/telamin/fluxtion/runtime/partition/LambdaReflection$Serializable";

    /** functionalInterface name -> (methodName, methodDescriptor). */
    static final Map<String, String[]> FN = new LinkedHashMap<>();
    static {
        // generic erasure forms
        put("SerializableConsumer",        "accept",        "(Ljava/lang/Object;)V");
        put("SerializableFunction",        "apply",         "(Ljava/lang/Object;)Ljava/lang/Object;");
        put("SerializableBiFunction",      "apply",         "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        put("SerializableBiConsumer",      "accept",        "(Ljava/lang/Object;Ljava/lang/Object;)V");
        put("SerializableSupplier",        "get",           "()Ljava/lang/Object;");
        put("SerializableRunnable",        "run",           "()V");
        // primitive specializations
        put("SerializableIntConsumer",     "accept",        "(I)V");
        put("SerializableLongConsumer",    "accept",        "(J)V");
        put("SerializableDoubleConsumer",  "accept",        "(D)V");
        put("SerializableIntSupplier",     "getAsInt",      "()I");
        put("SerializableLongSupplier",    "getAsLong",     "()J");
        put("SerializableDoubleSupplier",  "getAsDouble",   "()D");
        put("SerializableToIntFunction",   "applyAsInt",    "(Ljava/lang/Object;)I");
        put("SerializableToLongFunction",  "applyAsLong",   "(Ljava/lang/Object;)J");
        put("SerializableToDoubleFunction","applyAsDouble", "(Ljava/lang/Object;)D");
        put("SerializableIntFunction",     "apply",         "(I)Ljava/lang/Object;");
        put("SerializableLongFunction",    "apply",         "(J)Ljava/lang/Object;");
        put("SerializableDoubleFunction",  "apply",         "(D)Ljava/lang/Object;");
        put("SerializableIntUnaryOperator","applyAsInt",    "(I)I");
        put("SerializableLongUnaryOperator","applyAsLong",  "(J)J");
        put("SerializableDoubleUnaryOperator","applyAsDouble","(D)D");
    }
    private static void put(String simple, String m, String d) {
        FN.put(SERIALIZABLE_BASE + simple.substring("Serializable".length()),
               new String[]{m, d});
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: RetroPatch <dir>");
            System.exit(2);
        }
        int[] counts = transformDir(Paths.get(args[0]));
        System.out.println("scanned=" + counts[0] + " patched=" + counts[1] + " skipped=" + counts[2]);
    }

    /** Walk a directory of .class files, patch each Retrolambda lambda
     *  that implements a Fluxtion Serializable* interface, write back.
     *  Returns int[]{ scanned, patched, skipped }. */
    public static int[] transformDir(Path root) throws IOException {
        int patched = 0, scanned = 0, skipped = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (!p.toString().endsWith(".class")) continue;
                scanned++;
                byte[] in = Files.readAllBytes(p);
                byte[] out;
                try {
                    out = transform(in);
                } catch (SkipException s) {
                    skipped++;
                    continue;
                }
                if (out != null) {
                    Files.write(p, out);
                    patched++;
                }
            }
        }
        return new int[]{scanned, patched, skipped};
    }

    static byte[] transform(byte[] in) {
        ClassReader cr = new ClassReader(in);
        String name = cr.getClassName();
        // Retrolambda lambda class name pattern: <Outer>$$Lambda$<digits>
        if (!name.matches(".+\\$\\$Lambda\\$\\d+")) return null;

        // Detect Serializable functional interface this lambda implements.
        String[] interfaces = cr.getInterfaces();
        String fnIface = null;
        for (String i : interfaces) {
            if (FN.containsKey(i)) { fnIface = i; break; }
        }
        if (fnIface == null) return null; // not a Fluxtion serializable lambda

        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // Functional method name + descriptor (interface-erased form).
        String[] fn = FN.get(fnIface);
        String fnName = fn[0], fnDesc = fn[1];

        // Find the impl method handle (first method invocation in any
        // instance method that's not a call into the LambdaReflection
        // default-method companion). Retrolambda lambda bodies can use
        // INVOKESTATIC (for explicit lambda bodies), INVOKEVIRTUAL (bound
        // method ref to instance), INVOKEINTERFACE (interface method ref),
        // or INVOKESPECIAL (constructor reference).
        Handle implHandle = findImplHandle(cn);
        if (implHandle == null) throw new SkipException("no impl invocation found");

        // Captured args: from constructor parameters (in order). The
        // constructor takes exactly the captured args, mirrored to fields
        // arg$1..arg$N.
        Type ctorTypes[] = findCtorArgTypes(cn);

        // Already has writeReplace? Don't overwrite.
        for (MethodNode m : cn.methods) {
            if (m.name.equals("writeReplace") && m.desc.equals("()Ljava/lang/Object;")) {
                return null;
            }
        }

        // Emit: write-replaced class with new writeReplace method.
        // COMPUTE_MAXS lets us pass visitMaxs(0,0) and have ASM fill in real values.
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        injectWriteReplace(cw, name, ctorTypes, fnIface, fnName, fnDesc, implHandle);
        return cw.toByteArray();
    }

    static Handle findImplHandle(ClassNode cn) {
        // Scan instance methods (excluding constructor) for the first
        // method invocation that's not into the LambdaReflection
        // MethodReferenceReflection$ default-method companion.
        for (MethodNode m : cn.methods) {
            if ((m.access & Opcodes.ACC_STATIC) != 0) continue;
            if (m.name.equals("<init>")) continue;
            if (m.instructions == null) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode mi = (MethodInsnNode) insn;
                // skip the LambdaReflection$MethodReferenceReflection$ default-companion calls
                if (mi.owner.endsWith("MethodReferenceReflection$")) continue;
                int handleTag;
                switch (insn.getOpcode()) {
                    case Opcodes.INVOKESTATIC:    handleTag = Opcodes.H_INVOKESTATIC;    break;
                    case Opcodes.INVOKEVIRTUAL:   handleTag = Opcodes.H_INVOKEVIRTUAL;   break;
                    case Opcodes.INVOKEINTERFACE: handleTag = Opcodes.H_INVOKEINTERFACE; break;
                    case Opcodes.INVOKESPECIAL:
                        handleTag = mi.name.equals("<init>")
                                ? Opcodes.H_NEWINVOKESPECIAL
                                : Opcodes.H_INVOKESPECIAL;
                        break;
                    default: continue;
                }
                return new Handle(handleTag, mi.owner, mi.name, mi.desc, mi.itf);
            }
        }
        return null;
    }

    /** MethodHandleInfo.REF_* constant for an ASM Handle.getTag() value. */
    static int methodHandleInfoKind(int handleTag) {
        switch (handleTag) {
            case Opcodes.H_INVOKESTATIC:     return MethodHandleInfo.REF_invokeStatic;
            case Opcodes.H_INVOKEVIRTUAL:    return MethodHandleInfo.REF_invokeVirtual;
            case Opcodes.H_INVOKEINTERFACE:  return MethodHandleInfo.REF_invokeInterface;
            case Opcodes.H_INVOKESPECIAL:    return MethodHandleInfo.REF_invokeSpecial;
            case Opcodes.H_NEWINVOKESPECIAL: return MethodHandleInfo.REF_newInvokeSpecial;
            default: throw new IllegalArgumentException("unsupported handle tag " + handleTag);
        }
    }

    static Type[] findCtorArgTypes(ClassNode cn) {
        for (MethodNode m : cn.methods) {
            if (m.name.equals("<init>")) {
                return Type.getArgumentTypes(m.desc);
            }
        }
        return new Type[0];
    }

    static void injectWriteReplace(
            ClassWriter cw,
            String selfInternal,
            Type[] capturedTypes,
            String fnIface,
            String fnName,
            String fnDesc,
            Handle impl) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE,
                "writeReplace",
                "()Ljava/lang/Object;",
                null,
                new String[]{"java/io/ObjectStreamException"});
        mv.visitCode();

        // new SerializedLambda(...)
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/SerializedLambda");
        mv.visitInsn(Opcodes.DUP);

        // 1. capturingClass
        mv.visitLdcInsn(Type.getObjectType(selfInternal));
        // 2. functionalInterfaceClass (slash-separated internal name)
        mv.visitLdcInsn(fnIface);
        // 3. functionalInterfaceMethodName
        mv.visitLdcInsn(fnName);
        // 4. functionalInterfaceMethodSignature
        mv.visitLdcInsn(fnDesc);
        // 5. implMethodKind (mapped from the ASM handle tag)
        mv.visitIntInsn(Opcodes.BIPUSH, methodHandleInfoKind(impl.getTag()));
        // 6. implClass
        mv.visitLdcInsn(impl.getOwner());
        // 7. implMethodName
        mv.visitLdcInsn(impl.getName());
        // 8. implMethodSignature
        mv.visitLdcInsn(impl.getDesc());
        // 9. instantiatedMethodType (use functional erasure form — Fluxtion
        //    only reads getImplMethodName)
        mv.visitLdcInsn(fnDesc);
        // 10. capturedArgs: new Object[]{ this.arg$1, this.arg$2, ... }
        pushCapturedArgArray(mv, selfInternal, capturedTypes);

        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/invoke/SerializedLambda",
                "<init>",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V",
                false);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(0, 0); // computed via ClassWriter.COMPUTE_MAXS not used; supply rough upper bound
        mv.visitEnd();
    }

    static void pushCapturedArgArray(MethodVisitor mv, String selfInternal, Type[] capturedTypes) {
        // ICONST_<n> / BIPUSH <n>
        int n = capturedTypes.length;
        if (n <= 5) mv.visitInsn(Opcodes.ICONST_0 + n);
        else mv.visitIntInsn(Opcodes.BIPUSH, n);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < n; i++) {
            mv.visitInsn(Opcodes.DUP);                // arr, arr
            if (i <= 5) mv.visitInsn(Opcodes.ICONST_0 + i);
            else mv.visitIntInsn(Opcodes.BIPUSH, i);  // arr, arr, idx
            mv.visitVarInsn(Opcodes.ALOAD, 0);        // arr, arr, idx, this
            String fieldName = "arg$" + (i + 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, selfInternal, fieldName, capturedTypes[i].getDescriptor());
            // box primitives
            box(mv, capturedTypes[i]);
            mv.visitInsn(Opcodes.AASTORE);            // arr
        }
    }

    static void box(MethodVisitor mv, Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean",   "valueOf", "(Z)Ljava/lang/Boolean;",   false); break;
            case Type.BYTE:    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte",      "valueOf", "(B)Ljava/lang/Byte;",      false); break;
            case Type.CHAR:    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false); break;
            case Type.SHORT:   mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short",     "valueOf", "(S)Ljava/lang/Short;",     false); break;
            case Type.INT:     mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer",   "valueOf", "(I)Ljava/lang/Integer;",   false); break;
            case Type.LONG:    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long",      "valueOf", "(J)Ljava/lang/Long;",      false); break;
            case Type.FLOAT:   mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float",     "valueOf", "(F)Ljava/lang/Float;",     false); break;
            case Type.DOUBLE:  mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double",    "valueOf", "(D)Ljava/lang/Double;",    false); break;
            default: /* reference — already boxed */
        }
    }

    static class SkipException extends RuntimeException {
        SkipException(String msg) { super(msg); }
    }
}
