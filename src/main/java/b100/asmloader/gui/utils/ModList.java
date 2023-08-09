package b100.asmloader.gui.utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import b100.asmloader.gui.utils.ModList.ModInfo;
import b100.json.element.JsonObject;
import b100.utils.StringReader;
import b100.utils.StringUtils;

public class ModList implements Iterable<ModInfo> {
	
	private List<ModInfo> entries = new ArrayList<>();
	private List<ListDataListener> listDataListeners = new ArrayList<>();
	
	public JList<ModInfo> list;
	
	public ModList() {
		this.list = new JList<>(new ListModelImpl());
	}
	
	public int getSize() {
		return entries.size();
	}
	
	public ModInfo get(int i) {
		return entries.get(i);
	}
	
	public void add(ModInfo modInfo) {
		entries.add(modInfo);
		onUpdate();
	}
	
	public void clear() {
		entries.clear();
		onUpdate();
	}
	
	private void onUpdate() {
		ListDataEvent listDataEvent = new ListDataEvent(list, ListDataEvent.CONTENTS_CHANGED, 0, 0);
		for(ListDataListener listDataListener : listDataListeners) {
			listDataListener.contentsChanged(listDataEvent);
		}
	}

	@Override
	public Iterator<ModInfo> iterator() {
		return new IteratorImpl();
	}
	
	class ListModelImpl implements ListModel<ModInfo> {
		@Override
		public int getSize() {
			return entries.size();
		}

		@Override
		public ModInfo getElementAt(int index) {
			return entries.get(index);
		}

		@Override
		public void addListDataListener(ListDataListener l) {
			listDataListeners.add(l);
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			listDataListeners.remove(l);
		}
	}
	
	class IteratorImpl implements Iterator<ModInfo> {

		private int pos;
		
		@Override
		public boolean hasNext() {
			return pos < entries.size();
		}

		@Override
		public ModInfo next() {
			return entries.get(pos++);
		}
		
	}
	
	public static class ModInfo {
		
		public final File file;
		public final String modid;
		public final String name;
		public final String description;
		public final String version;
		
		public ModInfo(File file) {
			if(!file.exists()) {
				throw new NoModException();
			}
			this.file = file;
			
			if(file.isFile()) {
				ZipFile zipFile = null;
				InputStream inputStream = null;
				try {
					zipFile = new ZipFile(file);
					
					ZipEntry modJsonEntry = zipFile.getEntry("asmloader.mod.json");
					if(modJsonEntry == null) {
						throw new NoModException();
					}
					
					inputStream = zipFile.getInputStream(modJsonEntry);
					JsonObject jsonObject = new JsonObject(new StringReader(StringUtils.readInputString(inputStream)));
					
					this.modid = jsonObject.getString("modid");
				}catch (NoModException e) {
					throw e;
				}catch (Exception e) {
					throw new RuntimeException("Cannot read zip file: '"+file.getAbsolutePath()+"'", e);
				}finally {
					try {
						zipFile.close();
					}catch (Exception e) {}
					try {
						inputStream.close();
					}catch (Exception e) {}
				}
			}else {
				File modJsonFile = new File(file, "asmloader.mod.json");
				if(!modJsonFile.exists()) {
					throw new NoModException();
				}
				
				JsonObject jsonObject = new JsonObject(new StringReader(StringUtils.getFileContentAsString(modJsonFile)));
				
				this.modid = jsonObject.getString("modid");
			}
			
			//TODO
			this.name = null;
			this.description = null;
			this.version = null;
		}
		
		@SuppressWarnings("serial")
		public static class NoModException extends RuntimeException {
			
		}
		
		@Override
		public String toString() {
			return modid;
		}
		
	}
}
