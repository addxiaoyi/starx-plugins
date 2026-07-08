package io.github.addxiaoyi.starx.limboapi.patcher;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class BlockEntityVersionPatcher {

  private static final String BLOCK_ENTITY_VERSION =
      "net/elytrium/limboapi/api/chunk/BlockEntityVersion";
  private static final String PROTOCOL_VERSION = "com/velocitypowered/api/network/ProtocolVersion";

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("Usage: <velocity-jar-or-fields> <input-jar> <output-jar>");
      System.err.println(
          "  velocity-jar-or-fields: path to Velocity jar (to read ProtocolVersion.class)");
      System.err.println(
          "                         OR comma-separated list of 'FieldName:exists' entries");
      System.err.println(
          "  input-jar:  path to input LimboAPI jar (e.g. limboapi-1.1.27-SNAPSHOT.jar)");
      System.err.println("  output-jar: path to output patched jar");
      System.exit(1);
    }

    String velocityRef = args[0];
    File inputJar = new File(args[1]);
    File outputJar = new File(args[2]);

    // Step 1: Determine which ProtocolVersion fields are available
    Set<String> availableFields;
    if (velocityRef.equals("none")) {
      // No reference jar provided — skip patching entirely (keep all enum entries)
      System.out.println("No reference Velocity jar provided, copying input jar as-is");
      copyFile(inputJar, outputJar);
      System.out.println("Done: " + outputJar);
      return;
    } else if (velocityRef.endsWith(".jar")) {
      availableFields = getProtocolVersionFields(new File(velocityRef));
    } else {
      availableFields = new HashSet<>(Arrays.asList(velocityRef.split(",")));
    }
    System.out.println("Available ProtocolVersion fields: " + availableFields.size());
    System.out.println(
        "  (e.g. MINECRAFT_1_21_11 exists: " + availableFields.contains("MINECRAFT_1_21_11") + ")");

    // Step 2: Read input jar, patch BlockEntityVersion, write output jar
    int patchedCount = 0;
    try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar));
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {

      JarEntry entry;
      while ((entry = jis.getNextJarEntry()) != null) {
        byte[] data = readAllBytes(jis);

        if (entry.getName().equals(BLOCK_ENTITY_VERSION + ".class")) {
          System.out.println("Patching: " + entry.getName());
          data = patchClass(data, availableFields);
          patchedCount++;
        }

        jos.putNextEntry(new JarEntry(entry.getName()));
        jos.write(data);
        jos.closeEntry();
      }
    }

    if (patchedCount == 0) {
      System.out.println("WARNING: BlockEntityVersion.class not found in input jar!");
    } else {
      System.out.println("Successfully patched " + patchedCount + " class(es) → " + outputJar);
    }
  }

  private static void copyFile(File src, File dst) throws IOException {
    dst.getParentFile().mkdirs();
    try (InputStream is = new FileInputStream(src);
        OutputStream os = new FileOutputStream(dst)) {
      byte[] buf = new byte[65536];
      int n;
      while ((n = is.read(buf)) != -1) {
        os.write(buf, 0, n);
      }
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

  static Set<String> getProtocolVersionFields(File velocityJar) throws Exception {
    Set<String> fields = new HashSet<>();
    try (JarFile jf = new JarFile(velocityJar)) {
      JarEntry pvEntry = jf.getJarEntry("com/velocitypowered/api/network/ProtocolVersion.class");
      if (pvEntry == null) {
        System.err.println("WARNING: ProtocolVersion.class not found in " + velocityJar);
        return fields;
      }
      try (InputStream is = jf.getInputStream(pvEntry)) {
        ClassReader cr = new ClassReader(readAllBytes(is));
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);

        for (FieldNode fn : cn.fields) {
          if ((fn.access & Opcodes.ACC_ENUM) != 0) {
            fields.add(fn.name);
            if (fn.name.startsWith("MINECRAFT_")) {
              System.out.println("  Found ProtocolVersion field: " + fn.name);
            }
          }
        }
      }
    }
    return fields;
  }

  static byte[] patchClass(byte[] classBytes, Set<String> validProtocolVersions) {
    ClassReader cr = new ClassReader(classBytes);
    ClassNode cn = new ClassNode();
    cr.accept(cn, 0);

    // Step 1: Identify which enum entries reference missing ProtocolVersion fields
    // We scan <clinit> for NEW BlockEntityVersion → GETSTATIC ProtocolVersion.MISSING_FIELD
    // patterns
    MethodNode clinit = null;
    MethodNode valuesMethod = null;
    for (MethodNode mn : cn.methods) {
      if (mn.name.equals("<clinit>")) clinit = mn;
      if (mn.name.equals("$values")) valuesMethod = mn;
    }

    if (clinit == null || valuesMethod == null) {
      System.out.println("  WARNING: Could not find <clinit> or $values() methods");
      return classBytes;
    }

    // Fields to remove: the BlockEntityVersion enum constant fields whose creation
    // references a ProtocolVersion field that doesn't exist
    Set<String> fieldsToRemove = new HashSet<>();

    // Scan <clinit> for NEW BlockEntityVersion...PUTSTATIC patterns
    for (AbstractInsnNode insn : clinit.instructions.toArray()) {
      if (insn.getOpcode() == Opcodes.NEW) {
        TypeInsnNode typeInsn = (TypeInsnNode) insn;
        if (!typeInsn.desc.equals(BLOCK_ENTITY_VERSION)) continue;

        // Walk forward to find PUTSTATIC (which stores the created enum constant to a field)
        String fieldName = null;
        FieldInsnNode putstaticNode = null;
        boolean hasMissingRef = false;

        for (AbstractInsnNode n = insn.getNext(); n != null; n = n.getNext()) {
          if (n.getOpcode() == Opcodes.PUTSTATIC) {
            FieldInsnNode fin = (FieldInsnNode) n;
            if (fin.owner.equals(BLOCK_ENTITY_VERSION)) {
              fieldName = fin.name;
              putstaticNode = fin;
              break;
            }
          }
          // Check for GETSTATIC on ProtocolVersion with a field not in our valid set
          if (n.getOpcode() == Opcodes.GETSTATIC) {
            FieldInsnNode fin = (FieldInsnNode) n;
            if (fin.owner.equals(PROTOCOL_VERSION) && !validProtocolVersions.contains(fin.name)) {
              System.out.println("  Found missing reference: " + fin.owner + "." + fin.name);
              hasMissingRef = true;
            }
          }
        }

        if (hasMissingRef && fieldName != null) {
          System.out.println("  → Removing enum entry: " + fieldName);
          fieldsToRemove.add(fieldName);
        }
      }
    }

    if (fieldsToRemove.isEmpty()) {
      System.out.println("  No incompatible entries found, no patching needed.");
      return classBytes;
    }

    // Step 2: Remove field declarations
    cn.fields.removeIf(f -> fieldsToRemove.contains(f.name));
    System.out.println("  Removed " + fieldsToRemove.size() + " field(s)");

    // Step 3: Patch <clinit> - remove the NEW...PUTSTATIC sequences for removed entries
    patchClinit(clinit, fieldsToRemove);

    // Step 4: Patch $values() - remove entries referencing removed fields
    patchValuesMethod(valuesMethod, fieldsToRemove);

    // Step 5: Write back
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }

  private static void patchClinit(MethodNode clinit, Set<String> fieldsToRemove) {
    Set<AbstractInsnNode> toRemove = new HashSet<>();

    // Find each NEW BlockEntityVersion...PUTSTATIC sequence for removed fields
    for (AbstractInsnNode insn : clinit.instructions.toArray()) {
      if (insn.getOpcode() == Opcodes.NEW) {
        TypeInsnNode typeInsn = (TypeInsnNode) insn;
        if (!typeInsn.desc.equals(BLOCK_ENTITY_VERSION)) continue;

        // Walk forward to find PUTSTATIC; collect all instructions in this range.
        // If the PUTSTATIC field is in fieldsToRemove, mark all for removal.
        List<AbstractInsnNode> range = new ArrayList<>();
        String fieldName = null;
        boolean foundPutstatic = false;

        range.add(insn);
        for (AbstractInsnNode n = insn.getNext(); n != null; n = n.getNext()) {
          range.add(n);
          if (n.getOpcode() == Opcodes.PUTSTATIC) {
            FieldInsnNode fin = (FieldInsnNode) n;
            if (fin.owner.equals(BLOCK_ENTITY_VERSION)) {
              fieldName = fin.name;
              foundPutstatic = true;
              break;
            }
          }
        }

        if (foundPutstatic && fieldsToRemove.contains(fieldName)) {
          toRemove.addAll(range);
        }
      }
    }

    // Remove marked instructions
    toRemove.forEach(clinit.instructions::remove);
    System.out.println("  Patched <clinit>: removed " + toRemove.size() + " instruction(s)");
  }

  private static void patchValuesMethod(MethodNode valuesMethod, Set<String> fieldsToRemove) {
    Set<AbstractInsnNode> toRemove = new HashSet<>();

    // In $values(), each entry is: DUP, [ICONST_n | BIPUSH n], GETSTATIC <field>, AASTORE
    // We need to remove the entries whose GETSTATIC field is in fieldsToRemove
    // Also need to fix the array size constant (first instruction: SIPUSH/BIPUSH n)

    for (AbstractInsnNode insn : valuesMethod.instructions.toArray()) {
      if (insn.getOpcode() == Opcodes.GETSTATIC) {
        FieldInsnNode fin = (FieldInsnNode) insn;
        if (fin.owner.equals(BLOCK_ENTITY_VERSION) && fieldsToRemove.contains(fin.name)) {
          // Walk backward to find DUP (preceding the index push)
          AbstractInsnNode dupNode = null;
          for (AbstractInsnNode n = insn.getPrevious(); n != null; n = n.getPrevious()) {
            if (n.getOpcode() == Opcodes.DUP) {
              dupNode = n;
              break;
            }
          }
          // The AASTORE is right after GETSTATIC
          AbstractInsnNode aastore = insn.getNext();

          if (dupNode != null && aastore != null && aastore.getOpcode() == Opcodes.AASTORE) {
            toRemove.add(dupNode); // DUP
            AbstractInsnNode current = dupNode.getNext();
            while (current != aastore) {
              toRemove.add(current); // [ICONST_n|BIPUSH n] and GETSTATIC
              current = current.getNext();
            }
            toRemove.add(aastore); // AASTORE
          }
        }
      }
    }

    // Remove marked instructions
    toRemove.forEach(valuesMethod.instructions::remove);

    // Fix the array size: find the first instruction (BIPUSH or SIPUSH) and decrement
    // The array size is the total number of entries minus the number removed
    for (AbstractInsnNode insn : valuesMethod.instructions.toArray()) {
      if ((insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH)) {
        IntInsnNode iin = (IntInsnNode) insn;
        int originalSize = iin.operand;
        int newSize = originalSize - fieldsToRemove.size();
        System.out.println("  $values array size: " + originalSize + " → " + newSize);
        iin.operand = newSize;
        break;
      }
    }

    System.out.println("  Patched $values(): removed " + toRemove.size() + " instruction(s)");
  }
}
