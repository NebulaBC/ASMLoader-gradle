package b100.asmloader.cli;

import java.io.File;

import b100.asmloader.exporter.ASMModExporter;

public class ExporterCLI {
	
	public static void main(String[] args) {
		ASMModExporter asmModExporter = new ASMModExporter();
		
		for(int i=0; i < args.length; i++) {
			String key = args[i];
			if(key.equals("inputJar")) {
				asmModExporter.minecraftJar = new File(args[++i]).getAbsoluteFile();
			}else if(key.equals("outputJar")) {
				asmModExporter.outputFile = new File(args[++i]).getAbsoluteFile();
			}else if(key.equals("mods")) {
				String[] values = args[++i].split(";");
				asmModExporter.modFiles.clear();
				for(String value : values) {
					asmModExporter.modFiles.add(new File(value).getAbsoluteFile());
				}
			}else {
				ASMModExporter.log("Unknown argument: '" + key + "'!");
			}
		}
		
		asmModExporter.run();
	}

}
