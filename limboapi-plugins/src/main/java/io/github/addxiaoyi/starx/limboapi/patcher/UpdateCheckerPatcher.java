package io.github.addxiaoyi.starx.limboapi.patcher;

import java.io.*;
import java.util.jar.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class UpdateCheckerPatcher {

  private static final String TARGET_CLASS = "net/elytrium/commons/utils/updates/UpdatesChecker";

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: <input-jar> <output-jar>");
      System.exit(1);
    }

    File inputJar = new File(args[0]);
    File outputJar = new File(args[1]);

    int patchedCount = 0;
    try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {

      JarEntry entry;
      while ((entry = jis.getNextJarEntry()) != null) {
        byte[] data = readAllBytes(jis);

        if (entry.getName().equals(TARGET_CLASS + ".class")) {
          System.out.println("Patching: " + entry.getName());
          data = patchCheckVersionByURL(data);
          patchedCount++;
        }

        jos.putNextEntry(new JarEntry(entry.getName()));
        jos.write(data);
        jos.closeEntry();
      }
    }

    if (patchedCount == 0) {
      System.out.println("WARNING: " + TARGET_CLASS + ".class not found in input jar!");
    } else {
      System.out.println("Successfully patched " + patchedCount + " class(es) -> " + outputJar);
    }
  }

  private static byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] tmp = new byte[65536];
    int n;
    while ((n = is.read(tmp)) != -1) {
      buffer.write(tmp, 0, n);
    }
    return buffer.toByteArray();
  }

  static byte[] patchCheckVersionByURL(byte[] classBytes) {
    ClassReader cr = new ClassReader(classBytes);
    ClassNode cn = new ClassNode();
    cr.accept(cn, 0);

    int patchedMethods = 0;
    for (MethodNode mn : cn.methods) {
      if (mn.name.equals("checkVersionByURL")
          && mn.desc.equals("(Ljava/lang/String;Ljava/lang/String;)Z")) {
        System.out.println("  Found method: " + mn.name + mn.desc);

        // Clear all instructions and exception handlers
        mn.instructions.clear();
        mn.tryCatchBlocks.clear();

        // Replace with: return true
        mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
        mn.instructions.add(new InsnNode(Opcodes.IRETURN));

        // Update max stack
        mn.maxStack = 1;
        mn.maxLocals = 3;

        patchedMethods++;
        System.out.println("  Patched: checkVersionByURL now returns true immediately");
      }
    }

    if (patchedMethods == 0) {
      System.out.println("  WARNING: checkVersionByURL method not found");
      return classBytes;
    }

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }
}
