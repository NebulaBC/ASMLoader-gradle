package b100.asmloader.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.tree.ClassNode;

import b100.asmloader.ASMHelper;
import b100.asmloader.ClassTransformer;
import b100.asmloader.exporter.Mod.ModLoadException;
import b100.utils.FileUtils;
import b100.utils.StreamUtils;

public class ASMModExporter {
	
	public final List<File> modFiles = new ArrayList<>();
	public File minecraftJar;
	public File outputFile;

	public boolean includeOverrides = true;
	public boolean includeModFiles = true;
	
	private List<Mod> mods = new ArrayList<>();
	private List<ClassTransformer> classTransformers = new ArrayList<>();
	
	private Map<String, ClassNode> allClasses = new HashMap<>();
	private Set<String> modifiedClasses = new HashSet<>();
	
	public boolean run() {
		if(minecraftJar == null) {
			log("No minecraft jar provided, cannot continue!");
			return false;
		}
		if(outputFile == null) {
			log("No output file provided, using default!");
			outputFile = new File("asmloader-export.jar").getAbsoluteFile();
		}
		if(modFiles.size() == 0) {
			log("No mod files provided, defaulting to mods folder");
			modFiles.add(new File("mods").getAbsoluteFile());
		}
		
		log("Minecraft Jar: "+minecraftJar.getAbsolutePath());
		log("Output Jar: "+outputFile.getAbsolutePath());
		
		loadMods();
		
		if(mods.size() == 0) {
			log("No mods loaded!");
			return false;
		}
		log("Loaded "+mods.size()+" mods!");
		
		loadClassTransformers();
		
		if(classTransformers.size() == 0) {
			log("No class transformers loaded!");
			return false;
		}
		log("Loaded "+classTransformers.size()+" class transformers!");
		
		loadMinecraftJarClasses();
		
		log("Loaded "+allClasses.size()+" classes from the minecraft jar!");
		
		transformMinecraftClasses();
		
		log("Modified "+modifiedClasses.size()+" classes!");
		
		export();
		
		log("Done!");
		return true;
	}
	
	private void loadMods() {
		for(int i=0; i < modFiles.size(); i++) {
			File modFile = modFiles.get(i);
			
			if(modFile.isDirectory()) {
				File modJsonFile = new File(modFile, "asmloader.mod.json");
				if(modJsonFile.exists()) {
					tryToLoadMod(modFile);
					continue;
				}
				
				File[] files = modFile.listFiles();
				if(files == null || files.length == 0) {
					continue;
				}
				for(File file : files) {
					tryToLoadMod(file);
				}
				continue;
			}
			
			tryToLoadMod(modFile);
		}
	}
	
	private void tryToLoadMod(File modFile) {
		if(!modFile.exists()) {
			log("File does not exist: '"+modFile.getAbsolutePath()+"'!");
			return;
		}
		Mod mod;
		try {
			mod = new Mod(modFile);
		}catch (ModLoadException e) {
			e.printStackTrace();
			return;
		}
		
		mods.add(mod);
	}
	
	private void loadClassTransformers() {
		for(Mod mod : mods) {
			ClassLoader classLoader = mod.classLoader;
			
			for(String transformerEntry : mod.transformerEntries) {
				Class<?> transformerEntryClass;
				Object classTransformerEntryInstance = null;
				
				try {
					transformerEntryClass = classLoader.loadClass(transformerEntry);
				}catch (Throwable e) {
					throw new RuntimeException(e);
				}
				
				classTransformerEntryInstance = createInstance(transformerEntryClass.getDeclaredConstructors()[0]);
				
				if(ClassTransformer.class.isAssignableFrom(transformerEntryClass)) {
					ClassTransformer classTransformer = ClassTransformer.class.cast(classTransformerEntryInstance);
					classTransformers.add(classTransformer);
				}

				Class<?>[] subclasses = transformerEntryClass.getDeclaredClasses();
				for (int subclassIndex = 0; subclassIndex < subclasses.length; subclassIndex++) {
					Class<?> subClass = subclasses[subclassIndex];
					
					if(ClassTransformer.class.isAssignableFrom(subClass)) {
						Object subClassInstance = null;
						
						Constructor<?> constructor = subClass.getDeclaredConstructors()[0];
						
						if(constructor.getParameterCount() == 0) subClassInstance = createInstance(constructor);
						if(constructor.getParameterCount() == 1) subClassInstance = createInstance(constructor, classTransformerEntryInstance);
						
						try {
							classTransformers.add(ClassTransformer.class.cast(subClassInstance));
						} catch (Exception e) {
							throw new RuntimeException("Class '" + subClass.getName() + "' can not be cast to '" + ClassTransformer.class.getName() + "'!", e);
						}
					}
				}
			}
		}
	}
	
	private void loadMinecraftJarClasses() {
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(minecraftJar);
		} catch (IOException e) {
			throw new RuntimeException("Could not open minecraft jar! ", e);
		}
		
		Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
		while(enumeration.hasMoreElements()) {
			ZipEntry entry = enumeration.nextElement();	
			
			String name = entry.getName();
			if(!name.endsWith(".class")) {
				continue;
			}
			
			String className = name;
			className = className.substring(0, className.length() - 6);
			
			InputStream in;
			byte[] bytes;
			try {
				in = zipFile.getInputStream(entry);
				bytes = readAll(in);
			}catch (Exception e) {
				throw new RuntimeException("Could not read class: '"+className+"'!", e);
			}
			
			ClassNode classNode = ASMHelper.getClassNode(bytes);
			
			allClasses.put(className, classNode);
		}
		
		try {
			zipFile.close();
		}catch (Exception e) {}
	}
	
	private void transformMinecraftClasses() {
		for(String className : allClasses.keySet()) {
			ClassNode classNode = allClasses.get(className);
			
			boolean modified = false;
			
			for(ClassTransformer classTransformer : classTransformers) {
				if(classTransformer.accepts(className)) {
					log("Transforming " + className);
					classTransformer.transform(className, classNode);
					
					modified = true;
				}
			}
			
			if(modified) {
				modifiedClasses.add(className);
			}
		}
	}
	
	private void export() {
		FileOutputStream fileOutputStream;
		ZipOutputStream zipOutputStream;
		FileUtils.createFolderForFile(outputFile);
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			zipOutputStream = new ZipOutputStream(fileOutputStream);
		}catch (Exception e) {
			throw new RuntimeException("Could not open output file: "+outputFile.getAbsolutePath(), e);
		}
		
		Set<String> writtenEntries = new HashSet<>();
		
		if(includeOverrides) {
			// Write modified classes into jar
			for(String className : modifiedClasses) {
				String classPath = className + ".class";
				
				ClassNode classNode = allClasses.get(className);
				byte[] bytes = ASMHelper.getBytes(classNode);
				
				writeToZip(zipOutputStream, classPath, bytes);
				writtenEntries.add(classPath);
			}
		}
		if(includeModFiles) {
			// Write other resources into jar
			for(Mod mod : mods) {
				List<String> allResources = mod.getAllResources();
				for(String entry : allResources) {
					log("Copy Resource: " + entry);
					if(entry.equals("asmloader.mod.json")) {
						continue;
					}
					
					if(writtenEntries.contains(entry)) {
						log("Duplicate Resource: '"+entry+"'!");
						continue;
					}
					
					InputStream in = mod.getResource(entry);
					
					try {
						zipOutputStream.putNextEntry(new ZipEntry(entry));
						StreamUtils.transferData(in, zipOutputStream);
						writtenEntries.add(entry);
					} catch (Exception e) {
						throw new RuntimeException("Error writing to zip file!", e);
					}
					
					try {
						in.close();
					}catch (Exception e) {}
				}
			}
		}
		
		try {
			zipOutputStream.close();
		}catch (Exception e) {}
		try {
			fileOutputStream.close();
		}catch (Exception e) {}
	}
	
	private static void writeToZip(ZipOutputStream zipOutputStream, String entry, byte[] bytes) {
		try {
			zipOutputStream.putNextEntry(new ZipEntry(entry));
			zipOutputStream.write(bytes);
		}catch (Exception e) {
			throw new RuntimeException("Error to zip file: "+entry, e);
		}
	}
	
	private static byte[] readAll(InputStream inputStream) throws IOException {
		final int cacheSize = 4096;
		
		ByteCache byteCache = new ByteCache();
		while(true) {
			byte[] cache = new byte[cacheSize];
			int read = inputStream.read(cache, 0, cache.length);
			if(read == -1) {
				break;
			}
			byteCache.put(cache, 0, read);
		}
		
		try {
			inputStream.close();
		}catch (Exception e) {}
		
		return byteCache.getAll();
	}
	
	private static <E> E createInstance(Constructor<E> constructor, Object...parameters) {
		try {
			if(!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance(parameters);
		}catch (Exception e) {
			throw new RuntimeException("Could not create instance of class '" + constructor.getDeclaringClass().getName() + "'!", e);
		}
	}
	
	public static void log(String string) {
		System.out.print("[ASMModExporter] " + string + "\n");
	}
	
}
