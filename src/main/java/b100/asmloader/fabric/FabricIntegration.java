package b100.asmloader.fabric;

import static b100.asmloader.ASMHelper.*;
import static b100.asmloader.ASMLoader.*;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.security.ProtectionDomain;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import b100.asmloader.ASMLoader;
import b100.asmloader.ClassTransformer;

public class FabricIntegration {
	
	private static FabricIntegration instance;
	
	private ClassLoader fabricClassLoader;
	
	private ASMLoader asmLoader;
	
	public FabricIntegration(ASMLoader asmLoader, Instrumentation instrumentation) {
		if(instance != null) {
			throw new IllegalStateException();
		}
		instance = this;
		this.asmLoader = asmLoader;
		
		System.setProperty("fabric.debug.disableClassPathIsolation", "true");
		
		ASMLoader.log("Fabric Integration Init");
		instrumentation.addTransformer(new Transformer());
	}
	
	public void addFileToClassLoader(File file) {
		try {
			String string = "jar:file:/" + file.getAbsolutePath() + "!/";
			string = string.replace('\\', '/');
			string = string.replace(" ", "%20");
			URL url = new URI(string).toURL();
			
			Method method_addUrlFwd = fabricClassLoader.getClass().getDeclaredMethod("addUrlFwd", URL.class);
			method_addUrlFwd.setAccessible(true);
			method_addUrlFwd.invoke(fabricClassLoader, url);	
		}catch (Exception e) {
			throw new RuntimeException("Could not add file '"+file.getAbsolutePath()+"' to fabric class loader!", e);
		}
	}
	
	private void setFabricClassLoader(ClassLoader classLoader) {
		this.fabricClassLoader = classLoader;
	}
	
	public ClassLoader getClassLoader() {
		return fabricClassLoader;
	}
	
	public static void onFabricClassLoaderCreated(Object classDelegate) {
		try {
			Class<?> class_KnotClassDeletage = classDelegate.getClass();
			log("Fabric Class Loader Created: " + classDelegate + " : " + class_KnotClassDeletage);
			
			ClassLoader classLoader;
			try {
				Field field_classLoader = class_KnotClassDeletage.getDeclaredField("classLoader");
				field_classLoader.setAccessible(true);
				classLoader = (ClassLoader) field_classLoader.get(classDelegate);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			instance.setFabricClassLoader(classLoader);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void preLaunch() {
		instance.asmLoader.loadMods(instance.fabricClassLoader);
	}
	
	class Transformer implements ClassFileTransformer {

		@Override
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
			if(className == null) {
				return classfileBuffer;
			}
			if(className.equals("net/fabricmc/loader/impl/launch/knot/KnotClassDelegate")) {
				ClassNode classNode = getClassNode(classfileBuffer);
				new TransformKnotClassDelegate().transform(className, classNode);
				return getBytes(classNode);
			}
			if(className.equals("net/fabricmc/loader/impl/launch/knot/Knot")) {
				ClassNode classNode = getClassNode(classfileBuffer);
				new TransformKnot().transform(className, classNode);
				return getBytes(classNode);
			}
			return classfileBuffer;
		}
		
	}
	
	/**
	 * you cannot stop me
	 */
	class TransformKnotClassDelegate extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/fabricmc/loader/impl/launch/knot/KnotClassDelegate");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			MethodNode methodInit = null;
			for(MethodNode methodNode : classNode.methods) {
				if(methodNode.name.equals("<init>")) {
					methodInit = methodNode;
					break;
				}
			}
			
			InsnList insert = new InsnList();
			insert.add(new VarInsnNode(Opcodes.ALOAD, 0));
			insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "b100/asmloader/fabric/FabricIntegration", "onFabricClassLoaderCreated", "(Ljava/lang/Object;)V"));
			
			InsnList instructions = methodInit.instructions;
			for(int i = instructions.size() - 1; i >= 0; i--) {
				AbstractInsnNode ins = instructions.get(i);
				if(ins.getOpcode() == Opcodes.RETURN) {
					instructions.insertBefore(ins, insert);
					break;
				}
			}
		}
	}
	
	class TransformKnot extends ClassTransformer {

		@Override
		public boolean accepts(String className) {
			return className.equals("net/fabricmc/loader/impl/launch/knot/Knot");
		}

		@Override
		public void transform(String className, ClassNode classNode) {
			MethodNode methodInit = null;
			for(MethodNode methodNode : classNode.methods) {
				if(methodNode.name.equals("init")) {
					methodInit = methodNode;
					break;
				}
			}
			
			InsnList insert = new InsnList();
			insert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "b100/asmloader/fabric/FabricIntegration", "preLaunch", "()V"));

			InsnList instructions = methodInit.instructions;
			for(int i = instructions.size() - 1; i >= 0; i--) {
				AbstractInsnNode ins = instructions.get(i);
				if(ins.getOpcode() == Opcodes.ARETURN) {
					instructions.insertBefore(ins, insert);
					return;
				}
			}
			throw new RuntimeException();
		}
		
	}
}
