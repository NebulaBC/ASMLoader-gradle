package b100.asmloader.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import b100.asmloader.gui.utils.GridPanel;

public class ASMLoaderGUI implements ActionListener {
	
	public static void main(String[] args) {
		new ASMLoaderGUI();
	}
	
	public JFrame frame;
	public GridPanel panel;
	public JTabbedPane tabbedPane;
	
	public ModExporterGUI modExporterGUI;
	public LogGUI logGUI;
	
	public JMenuBar menuBar;
	public JMenu menuStyle = new JMenu("Style");
	public JMenuItem menuItemStyleDefault;
	public JMenuItem menuItemStyleSystem;
	
	public ASMLoaderGUI() {
		frame = new JFrame("ASMLoader GUI");
		frame.setMinimumSize(new Dimension(400, 400));
		
		panel = new GridPanel();
		
		modExporterGUI = new ModExporterGUI(this);
		logGUI = new LogGUI();
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Export", modExporterGUI);
		tabbedPane.addTab("Log", logGUI);
		tabbedPane.setPreferredSize(new Dimension(480, 520));
		
		createMenuBar();
		
		panel.add(menuBar, 0, 0, 1, 0);
		panel.add(tabbedPane, 0, 1, 1, 1);
		
		frame.add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private void createMenuBar() {
		menuBar = new JMenuBar();
		
		menuStyle = new JMenu("Style");
		
		menuItemStyleDefault = new JRadioButtonMenuItem("Default", !isSystemStyleActive);
		menuItemStyleSystem = new JRadioButtonMenuItem("System", isSystemStyleActive);
		
		menuItemStyleDefault.addActionListener(this);
		menuItemStyleSystem.addActionListener(this);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(menuItemStyleDefault);
		buttonGroup.add(menuItemStyleSystem);
		
		menuStyle.add(menuItemStyleDefault);
		menuStyle.add(menuItemStyleSystem);
		
		menuBar.add(menuStyle);
	}
	
	public void showLog() {
		tabbedPane.setSelectedIndex(1);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == menuItemStyleDefault) {
			updateStyle(false);
			frame.setVisible(false);
			frame.dispose();
			new ASMLoaderGUI();
		}
		if(e.getSource() == menuItemStyleSystem) {
			updateStyle(true);
			frame.setVisible(false);
			frame.dispose();
			new ASMLoaderGUI();
		}
	}
	
	private static void updateStyle(boolean system) {
		try {
			String className;
			if(system) {
				className = UIManager.getSystemLookAndFeelClassName();
			}else {
				className = UIManager.getCrossPlatformLookAndFeelClassName();
			}
			UIManager.setLookAndFeel(className);
			isSystemStyleActive = system;
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static boolean isSystemStyleActive;

}
