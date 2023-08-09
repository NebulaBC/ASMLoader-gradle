package b100.asmloader.gui.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

@SuppressWarnings("serial")
public class FileTextField extends JTextField implements FileDropListener {
	
	private List<FileEventListener> fileEventListeners = new ArrayList<>();
	
	public FileTextField() {
		FileDropHandler.addFileDropListener(this, this);
	}
	
	public FileTextField(int size) {
		super(size);
		FileDropHandler.addFileDropListener(this, this);
	}

	@Override
	public void onFileDrop(List<File> files) {
		if(files.size() > 0) {
			File file = files.get(0);
			setFile(file);
			for(int i=0; i < fileEventListeners.size(); i++) {
				fileEventListeners.get(i).onFileEvent(this, file);
			}
		}
	}
	
	public void setFile(File file) {
		String filePath = file.getAbsolutePath();
		String runDirectory = new File("").getAbsoluteFile().getAbsolutePath();
		
		if(filePath.startsWith(runDirectory + "/")) {
			filePath = filePath.substring(runDirectory.length());
		}
		
		this.setText(filePath);
	}
	
	public void addFileEventListener(FileEventListener fileEventListener) {
		fileEventListeners.add(fileEventListener);
	}
	
	public void removeFileEventListener(FileEventListener fileEventListener) {
		fileEventListeners.remove(fileEventListener);
	}
	
}
