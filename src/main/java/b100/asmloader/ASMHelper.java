package b100.asmloader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class ASMHelper {
	
	public static ClassNode getClassNode(byte[] bytes) {
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		return classNode;
	}
	
	public static byte[] getBytes(ClassNode classNode) {
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

}
