package b100.asmloader.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import b100.json.element.JsonArray;
import b100.json.element.JsonObject;
import b100.utils.FileUtils;
import b100.utils.StringReader;
import b100.utils.StringUtils;

class Mod {
	
	public File file;
	public List<String> transformerEntries = new ArrayList<>();
	public ClassLoader classLoader;
	
	public ZipFile zipFile;
	
	public Mod(File file) {
		this.file = file;
		
		if(file.isFile()) {
			try {
				zipFile = new ZipFile(file);
			} catch (IOException e) {
				throw new ModLoadException(e);
			}
		}
		
		ASMModExporter.log("Loading mod: " + file.getAbsolutePath());
		
		InputStream in = getResource("asmloader.mod.json");
		if(in == null) {
			throw new ModLoadException("No mod json!");
		}
		
		JsonObject modJson = new JsonObject(new StringReader(StringUtils.readInputString(in)));
		JsonArray transformerArray = modJson.getArray("transformers");
		
		for(int i=0; i < transformerArray.length(); i++) {
			transformerEntries.add(transformerArray.get(i).getAsString().value);
		}
		
		if(transformerEntries.size() == 0) {
			ASMModExporter.log("Mod has no transformers!");
		}else if(transformerEntries.size() == 1) {
			ASMModExporter.log("Mod has 1 transformer!");
		}else {
			ASMModExporter.log("Mod has "+transformerEntries.size()+" transformers!");
		}
		
		createClassLoader();
	}
	
	public List<String> getAllResources() {
		List<String> allResources = new ArrayList<>();
		if(zipFile != null) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if(entry.getName().endsWith("/")) {
					continue;
				}
				allResources.add(entry.getName());
			}
		}else {
			int pathLength = file.getAbsolutePath().length();
			List<File> allFiles = FileUtils.getAllFiles(file);
			for(File file : allFiles) {
				allResources.add(file.getAbsolutePath().substring(pathLength + 1));
			}
		}
		return allResources;
	}
	
	public InputStream getResource(String path) {
		if(zipFile != null) {
			ZipEntry entry = zipFile.getEntry(path);
			if(entry == null) {
				throw new NullPointerException("Resource does not exist: '" + path + "'!");
			}
			try {
				return zipFile.getInputStream(entry);
			} catch (IOException e) {
				throw new RuntimeException("Could not load resource: '" + path + "'!", e);
			}
		}else {
			File file = new File(this.file, path);
			if(!file.exists()) {
				throw new NullPointerException("Resource does not exist: '" + path + "'!");
			}
			try {
				return new FileInputStream(file);
			}catch (Exception e) {
				throw new RuntimeException("Could not load resource: '" + path + "'!");
			}
		}
	}
	
	private void createClassLoader() {
		try {
			URL url;
			if(file.isFile()) {
				url = new URL("jar:file:" + file.getAbsolutePath() + "!/");
			}else {
				url = file.toURI().toURL();
			}
			
			this.classLoader = new URLClassLoader(new URL[] {url});	
		}catch (Exception e) {
			throw new RuntimeException("Could not create class loader for file '" + file.getAbsolutePath() + "'!", e);
		}
	}
	
	@SuppressWarnings("serial")
	class ModLoadException extends RuntimeException {
		
		public ModLoadException(Throwable throwable) {
			super(throwable);
		}
		
		public ModLoadException(String msg, Throwable throwable) {
			super(msg, throwable);
		}
		
		public ModLoadException(String msg) {
			super(msg);
		}
		
	}
	
}
