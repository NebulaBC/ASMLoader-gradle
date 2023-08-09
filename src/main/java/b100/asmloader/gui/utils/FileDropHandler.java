package b100.asmloader.gui.utils;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileDropHandler implements DropTargetListener{
	
	private static List<FileDropHandler> fileDropHandlers = new ArrayList<FileDropHandler>();
	
	public static void addFileDropListener(Component component, FileDropListener fileDropListener) {
		fileDropHandlers.add(new FileDropHandler(component, fileDropListener));
	}
	
	public final DropTarget dropTarget;
	public final FileDropListener fileDropListener;
	
	private FileDropHandler(Component comp, FileDropListener fileDropListener) {
		this.dropTarget = new DropTarget(comp, this);
		this.fileDropListener = fileDropListener;
	}
	
	@SuppressWarnings("unchecked")
	public void drop(DropTargetDropEvent e) {
		try{
			e.acceptDrop(DnDConstants.ACTION_COPY);
			
			List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
			if(fileDropListener != null && files != null) {
				fileDropListener.onFileDrop(files);
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void dragEnter(DropTargetDragEvent dtde) {}

	public void dragOver(DropTargetDragEvent dtde) {}

	public void dropActionChanged(DropTargetDragEvent dtde) {}

	public void dragExit(DropTargetEvent dte) {}
	
}
