package b100.asmloader;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import b100.asmloader.fabric.FabricIntegration;
import b100.json.element.JsonArray;
import b100.json.element.JsonObject;
import b100.utils.StringReader;
import b100.utils.StringUtils;

public class ASMLoader implements Log {
	
	private static ASMLoader instance;
	
	private static boolean enableFabricIntegration = false;
	private static boolean loadModsFolder = false;
	private static boolean loadClasspath = false;
	
	private Instrumentation instrumentation;
	
	private File runDirectory;
	private File modsFolder;
	
	private List<File> modFolderModFiles = new ArrayList<>();
	private List<File> classpathModFiles = new ArrayList<>();
	
	private Map<String, Mod> mods = new HashMap<>();
	
	private FabricIntegration fabricIntegration;
	
	private ASMLoader() {}
	
	private void onInit(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
		
		log("ASMLoader-Init");
		
		loadSystemProperties();

		log("Enable Fabric Integration: " + enableFabricIntegration);
		log("Load Mods on classpath: " + loadClasspath);
		log("Load Mods in mods folder: " + loadModsFolder);
		
		runDirectory = new File("").getAbsoluteFile();
		modsFolder = new File(runDirectory, "mods");
		
		log("Run Directory: " + runDirectory.getAbsolutePath());
		
		findModFilesInModsFolder();
		findModFilesOnClassPath();
		
		if(enableFabricIntegration) {
			fabricIntegration = new FabricIntegration(this, instrumentation);
			return;
		}

		loadMods(ClassLoader.getSystemClassLoader());
	}
	
	private void loadSystemProperties() {
		enableFabricIntegration = "true".equalsIgnoreCase(System.getProperty("asmloader.fabric"));
		loadModsFolder = System.getProperty("asmloader.loadModsFolder") == null || System.getProperty("asmloader.loadModsFolder").equalsIgnoreCase("true");
		loadClasspath = System.getProperty("asmloader.loadClasspath") == null || System.getProperty("asmloader.loadClasspath").equalsIgnoreCase("true");
	}
	
	public void loadMods(ClassLoader classLoader) {
		List<Object> classTransformers = new ArrayList<>();
		
		addModFilesToClassPath(instrumentation);
		loadModsAndRegisterTransformers(classLoader, classTransformers);
		
		ClassFileTransformer classFileTransformer;
		try {
			classFileTransformer = (ClassFileTransformer) classLoader.loadClass("b100.asmloader.ASMLoaderClassFileTransformer").getConstructor(List.class).newInstance(classTransformers);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		instrumentation.addTransformer(classFileTransformer);
		
		log("Loaded "+mods.size()+" mods!");
		
		int transformerCount = classTransformers.size();
		if(transformerCount == 0) {
			log("No class transformers loaded!");
		}else if(transformerCount == 1) {
			log("Loaded 1 class transformer!");
		}else {
			log("Loaded "+transformerCount+" class transformers!");	
		}
	}
	
	private void findModFilesOnClassPath() {
		if(!loadClasspath) {
			return;
		}
		
		String classPath = System.getProperty("java.class.path");
		String[] classPathEntries = classPath.split(";");
		
		for(String classpathEntry : classPathEntries) {
			File file = new File(classpathEntry);
			if(peepModJson(file)) {
				log("Found mod json in classpath entry '" + file.getAbsolutePath() + "'");
				classpathModFiles.add(file);
			}
		}
		log("Found "+classpathModFiles.size()+" mods on classpath!");
	}
	
	private void findModFilesInModsFolder() {
		if(!loadModsFolder) {
			return;
		}
		
		if(!modsFolder.exists()) {
			return;
		}
		
		File[] files = modsFolder.listFiles();
		if(files == null) {
			return;
		}
		
		for(int i=0; i < files.length; i++) {
			File file = files[i];
			String name = file.getName();
			if(file.isFile() && (name.endsWith(".zip") || name.endsWith(".jar")) && peepModJson(file)) {
				log("Found mod json in mod folder '" + file.getAbsolutePath() + "'");
				modFolderModFiles.add(file);
			}
		}
		log("Found "+modFolderModFiles.size()+" mods in mods folder!");
	}
	
	private void addModFilesToClassPath(Instrumentation instrumentation) {
		for(File modFile : modFolderModFiles) {
			JarFile jarFile = null;
			try {
				if(fabricIntegration != null) {
					fabricIntegration.addFileToClassLoader(modFile);
				}else {
					try {
						jarFile = new JarFile(modFile);
					}catch (Exception e) {
						throw new RuntimeException("Could not read jar file.", e);
					}
					instrumentation.appendToSystemClassLoaderSearch(jarFile);
				}
			}catch (Exception e) {
				throw new RuntimeException("Could not add mod to classpath: '" + modFile.getAbsolutePath() + "'", e);
			}finally {
				try {
					jarFile.close();
				}catch (Exception e) {}
			}
		}
	}
	
	private void loadModsAndRegisterTransformers(ClassLoader classLoader, List<Object> classTransformers) {
		for(File file : classpathModFiles) {
			loadModAndRegisterTransformers(file, classLoader, classTransformers);
		}
		for(File file : modFolderModFiles) {
			loadModAndRegisterTransformers(file, classLoader, classTransformers);
		}
	}
	
	/**
	 * Load a mod. This mod can be either a jar file, or a directory.
	 * At this point in time we have already checked that the asmloader.mod.json exists.
	 * If an error happens, stop everything.
	 */
	private void loadModAndRegisterTransformers(File file, ClassLoader classLoader, List<Object> classTransformers) {
		Class<?> classTransformerClass;
		try{
			// ClassTransformer class has to be loaded on the same class loader, or else everything will go up in flames
			classTransformerClass = classLoader.loadClass("b100.asmloader.ClassTransformer");
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		ZipFile zipFile = null;
		JsonObject modJson;
		
		if(file.isDirectory()) {
			modJson = new JsonObject(new StringReader(StringUtils.getFileContentAsString(new File(file, "asmloader.mod.json"))));
		}else {
			try {
				zipFile = new ZipFile(file);
				InputStream in = zipFile.getInputStream(zipFile.getEntry("asmloader.mod.json"));
				modJson = new JsonObject(new StringReader(StringUtils.readInputString(in)));
			}catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		String modid = modJson.getString("modid");
		Mod mod = new Mod(modid, file);

		log("Loading mod '" + modid + "' from '"+file.getAbsolutePath()+"'");
		
		if(mods.containsKey(modid)) {
			throw new RuntimeException("Duplicate mod id '"+modid+"' in files '"+file.getAbsolutePath()+"' and '"+mods.get(modid).file.getAbsolutePath()+"'!");
		}
		
		mods.put(modid, mod);
		
		JsonArray transformerArray = modJson.getArray("transformers");
		for (int transformerEntryIndex = 0; transformerEntryIndex < transformerArray.length(); transformerEntryIndex++) {
			String transformerEntry = transformerArray.get(transformerEntryIndex).getAsString().value;
			log("Load transformer class: '"+transformerEntry+"'");
			
			Class<?> transformerEntryClass;
			try {
				transformerEntryClass = classLoader.loadClass(transformerEntry);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Transformer entry class does not exist: '"+transformerEntry+"'!", e);
			}
			
			boolean isTransformerClass = false;
			boolean hasTransformerSubClasses = false;

			Object classTransformerEntryInstance = createInstance(transformerEntryClass.getDeclaredConstructors()[0]);
			
			if(classTransformerClass.isAssignableFrom(transformerEntryClass)) {
				isTransformerClass = true;
				
				try {
					classTransformerClass.cast(classTransformerEntryInstance);
				} catch (Exception e) {
					throw new RuntimeException("Class '" + transformerEntryClass.getName() + "' can not be cast to '" + classTransformerClass.getName() + "'!", e);
				}
				
				classTransformers.add(classTransformerEntryInstance);
			}
			
			Class<?>[] subclasses = transformerEntryClass.getDeclaredClasses();
			for (int subclassIndex = 0; subclassIndex < subclasses.length; subclassIndex++) {
				Class<?> subClass = subclasses[subclassIndex];
				
				if(classTransformerClass.isAssignableFrom(subClass)) {
					log("Found transformer subclass: " + subClass.getName());
					hasTransformerSubClasses = true;
					
					Object subClassInstance = null;
					
					Constructor<?> constructor = subClass.getDeclaredConstructors()[0];
					
					if(constructor.getParameterCount() == 0) subClassInstance = createInstance(constructor);
					if(constructor.getParameterCount() == 1) subClassInstance = createInstance(constructor, classTransformerEntryInstance);
					
					try {
						classTransformerClass.cast(subClassInstance);
					} catch (Exception e) {
						throw new RuntimeException("Class '" + subClass.getName() + "' can not be cast to '" + classTransformerClass.getName() + "'!", e);
					}
					
					classTransformers.add(subClassInstance);
				}
			}
			
			if(!isTransformerClass && !hasTransformerSubClasses) {
				throw new RuntimeException("Invalid transformer entry '" + transformerEntry + "'! Class does not extend b100.asmloader.ClassTransformer, and does not have any subclasses that extend b100.asmloader.ClassTransformer!");
			}
		}
	}
	
	/**
	 * Check if a mod json exists in the directory or zip file. If any error happens, return false.
	 */
	private boolean peepModJson(File file) {
		if(file.isDirectory()) {
			File modJson = new File(file, "asmloader.mod.json");
			if(modJson.exists()) {
				return true;
			}
		}else if(file.isFile()) {
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(file);
				
				return zipFile.getEntry("asmloader.mod.json") != null;
			}catch (Exception e) {
				return false;
			}finally {
				try {
					zipFile.close();
				}catch (Exception e) {}
			}
		}
		return false;
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
	
	@Override
	public void print(String string) {
		System.out.print("[ASMLoader] " + string + "\n");
	}
	
	public static void log(String string) {
		instance.print(string);
	}
	
	public static void init(Instrumentation instrumentation) {
		if(instance != null) {
			throw new IllegalStateException("Already initialized!");
		}
		instance = new ASMLoader();
		instance.onInit(instrumentation);
	}
	
}
