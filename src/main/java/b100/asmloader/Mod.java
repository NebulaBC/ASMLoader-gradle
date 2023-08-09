package b100.asmloader;

import java.io.File;

public class Mod {
	
	public final String modid;
	public final File file;
	
	public Mod(String modid, File modfile) {
		this.modid = modid;
		this.file = modfile;
	}
	
	@Override
	public int hashCode() {
		return modid.hashCode();
	}

}
