package b100.asmloader;

import org.objectweb.asm.tree.ClassNode;

public abstract class ClassTransformer {
	
	public abstract boolean accepts(String className);
	
	public abstract void transform(String className, ClassNode classNode);

}
