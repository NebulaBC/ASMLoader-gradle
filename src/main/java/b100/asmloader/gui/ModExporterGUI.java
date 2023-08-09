package b100.asmloader.gui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import b100.asmloader.exporter.ASMModExporter;
import b100.asmloader.gui.utils.FileDropHandler;
import b100.asmloader.gui.utils.FileDropListener;
import b100.asmloader.gui.utils.FileTextField;
import b100.asmloader.gui.utils.GridPanel;
import b100.asmloader.gui.utils.ModList;
import b100.asmloader.gui.utils.ModList.ModInfo;
import b100.asmloader.gui.utils.ModList.ModInfo.NoModException;

@SuppressWarnings("serial")
public class ModExporterGUI extends GridPanel implements FileDropListener, ActionListener {
	
	public ASMLoaderGUI asmLoaderGUI;
	
	public JTextField textFieldInput;
	public ModList modList;
	public JTextField textFieldOutput;
	public JButton buttonStart;
	public JCheckBox checkBoxIncludeOverrides;
	public JCheckBox checkBoxIncludeModFiles;
	
	public ModExporterGUI(ASMLoaderGUI asmLoaderGUI) {
		this.asmLoaderGUI = asmLoaderGUI;
		
		// Setup Components
		defaultWeightX = 1.0;
		
		GridBagConstraints c = getGridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		
		textFieldInput = new FileTextField();
		
		modList = new ModList();
		FileDropHandler.addFileDropListener(modList.list, this);
		
		textFieldOutput = new FileTextField();
		
		buttonStart = new JButton("Export");
		buttonStart.addActionListener(this);
		
		checkBoxIncludeOverrides = new JCheckBox("Overrides");
		checkBoxIncludeOverrides.setSelected(true);
		
		checkBoxIncludeModFiles = new JCheckBox("Mod Files");
		checkBoxIncludeModFiles.setSelected(true);
		
		// Setup layout elements
		GridPanel panelFiles = new GridPanel(4, 0.0, 0.0);
		panelFiles.setBorder(new TitledBorder("Files"));
		panelFiles.add(new JLabel("Minecraft Jar"), 0, 0, 0, 0);
		panelFiles.add(new JLabel("Output Jar"), 0, 1, 0, 0);
		panelFiles.add(textFieldInput, 1, 0, 1, 0);
		panelFiles.add(textFieldOutput, 1, 1, 1, 0);
		
		GridPanel panelMods = new GridPanel(4, 1.0, 1.0);
		panelMods.setBorder(new TitledBorder("Mods"));
		panelMods.add(modList.list, 0, 0);
		
		GridPanel panelInclude = new GridPanel(0, 1.0, 1.0);
		panelInclude.setBorder(new TitledBorder("Include"));
		panelInclude.add(checkBoxIncludeOverrides, 0, 0);
		panelInclude.add(checkBoxIncludeModFiles, 1, 0);
		
		// Add Components
		add(panelFiles, 0, 0);
		add(panelMods, 0, 1, 1, 1);
		add(panelInclude, 0, 2);
		add(buttonStart, 0, 3);
	}

	@Override
	public void onFileDrop(List<File> files) {
		for(File file : files) {
			file = file.getAbsoluteFile();
			
			// Check if file has already been added
			for(int i=0; i < modList.getSize(); i++) {
				File f = modList.get(i).file;
				
				if(f.getAbsolutePath().equals(file.getAbsolutePath())) {
					continue;
				}
			}
			
			try{
				modList.add(new ModInfo(file));
			}catch (NoModException e) {
				JOptionPane.showMessageDialog(this, "Not a valid mod: '"+file.getAbsolutePath()+"'");
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == buttonStart) {
			buttonStart.setEnabled(false);
			
			asmLoaderGUI.showLog();
			
			ASMModExporter asmModExporter = new ASMModExporter();

			if(textFieldInput.getText().trim().length() > 0) {
				asmModExporter.minecraftJar = new File(textFieldInput.getText());
			}
			if(textFieldOutput.getText().trim().length() > 0) {
				String outputPath = textFieldOutput.getText();
				if(outputPath.indexOf('.') == -1) {
					outputPath = outputPath + ".jar";
				}
				asmModExporter.outputFile = new File(outputPath);	
			}
			
			for(ModInfo modInfo : modList) {
				asmModExporter.modFiles.add(modInfo.file);
			}
			
			asmModExporter.includeModFiles = checkBoxIncludeModFiles.isSelected();
			asmModExporter.includeOverrides = checkBoxIncludeOverrides.isSelected();
			
			Runnable runnable = () -> {
				boolean success;
				try{
					success = asmModExporter.run();
				}catch (Exception ex) {
					ex.printStackTrace();
					success = false;
					return;
				}
				
				if(success) {
					JOptionPane.showMessageDialog(this, "Success! Jar file has been saved to '"+asmModExporter.outputFile.getAbsolutePath()+"'!");
				}else {
					JOptionPane.showMessageDialog(this, "Failure! Check the log for more information!");
				}
				
				buttonStart.setEnabled(true);
			};
			new Thread(runnable).start();
		}
	}
	
	class ModListModel implements ListModel<String> {
		
		public List<String> entries = new ArrayList<>();
		private List<ListDataListener> listDataListeners = new ArrayList<>();
		
		public void onChanged(Object source) {
			ListDataEvent e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, 0, 0);
			for(ListDataListener listDataListener : listDataListeners) {
				listDataListener.contentsChanged(e);
			}
		}
		
		@Override
		public int getSize() {
			return entries.size();
		}

		@Override
		public String getElementAt(int i) {
			return entries.get(i);
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
	
}
