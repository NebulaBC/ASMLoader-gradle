package b100.asmloader;

import static b100.asmloader.ASMHelper.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;

public class ASMLoaderClassFileTransformer implements ClassFileTransformer {
	
	private List<?> classTransformers = new ArrayList<>();
	
	private ClassNode classNode;
	
	public ASMLoaderClassFileTransformer(List<?> classTransformers) {
		this.classTransformers = classTransformers;
	}
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
		synchronized (this) {
			if(className == null) {
				// no idea
				return bytes;
			}
			
			try {
				int transformerCount = classTransformers.size();
				for(int i=0; i < transformerCount; i++) {
					ClassTransformer transformer = (ClassTransformer) classTransformers.get(i);
					try {
						if(transformer.accepts(className)) {
							if(classNode == null) {
								classNode = getClassNode(bytes);
							}
							
							System.out.println("Transforming "+className);
							transformer.transform(className, classNode);
						}	
					}catch (Exception e) {
						System.err.println("Error transforming class '"+className+"' using transformer: '"+transformer+"'!");
						e.printStackTrace();
					}
				}
				
				if(classNode != null) {
					bytes = getBytes(classNode);
					classNode = null;
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			return bytes;
		}
	}

}
