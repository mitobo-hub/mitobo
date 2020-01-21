/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - @YEAR@
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Fore more information on MiToBo, visit
 *
 *    http://www.informatik.uni-halle.de/mitobo/
 *
 */

package de.unihalle.informatik.MiToBo.apps.cellMorphology;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerCmdline;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOCmdline;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentComboBox;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentItem;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentTextField;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;

/**
 * Data I/O provider for input data of {@link PaCeQuant_FeatureColorMapper}.
 * 
 * @author moeller
 */
@ALDDataIOProvider(priority=10)
public class PaCeQuant_FeatureColorMapperInputDataIOSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(PaCeQuant_FeatureColorMapperInputData.class);
		return l;
	}

	@Override
	public ALDSwingComponent createGUIElement(
			Field field, Class<?> cl, Object obj, ALDParameterDescriptor descr) {
		return new ColorMapperDataIOFileInPanel(field, cl, obj, descr);
	}

	@Override
	public void setValue(			
		Field field, Class<?> cl, ALDSwingComponent guiElement, Object value) 
			throws ALDDataIOProviderException {
		
		if (!(guiElement instanceof ColorMapperDataIOFileInPanel)) {
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT,
					"[ColorMapperDataIOFileSwing] setValue() received wrong GUI element!");
		}
		if (!this.providedClasses().contains(cl)) {
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"[ColorMapperDataIOFileSwing] setValue() received object of invalid type!");
		}
		ColorMapperDataIOFileInPanel inPanel = (ColorMapperDataIOFileInPanel)guiElement;
		try {
			inPanel.setValue(field, cl, value);
		} catch (ALDDataIOException e) {
			throw new ALDDataIOProviderException(
				ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
					"[ColorMapperDataIOFileSwing] setValue() failed, reason is...\n" +
						e.getCommentString());
		}
	}

	@Override
	public Object readData(Field field, Class<?> cl, ALDSwingComponent guiElem) 
			throws ALDDataIOProviderException {
		Object data = null;
		if (	guiElem instanceof ColorMapperDataIOFileInPanel 
			&& 	this.providedClasses().contains(cl)) {
			try {
				data = ((ColorMapperDataIOFileInPanel)guiElem).getData();
			} catch (ALDDataIOException e) {
				throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						e.getCommentString());
			}
		}
		return data;
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) 
			throws ALDDataIOProviderException {
		throw new ALDDataIOProviderException(
			ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
				"[ColorMapperDataIOFileSwing] writeData() not yet supported...\n");
	}

	/**
	 * Panel for handling GUI I/O of input directories for {@link PaCeQuant_FeatureColorMapper}.
	 * 
	 * @author moeller
	 */
	private class ColorMapperDataIOFileInPanel extends ALDSwingComponent 
		implements ActionListener, ALDSwingValueChangeListener {

		/**
		 * Main panel containing all graphical components.
		 */
		private JPanel panelContainer;

		/**
		 * Panel containing elements for selecting input directory.
		 */
		private JPanel dirLoaderPanel;

		/**
		 * Directory chooser.
		 */
		private ColorMapperInputDirectoryIOPanel dirSelection;

		private HashMap<String,Integer> columnNameToIDMap;
		private ALDSwingComponentComboBox columnSelection;
		private Vector<ALDSwingComponentItem> columnSelectionItems;

		/**
		 * Button to load data, e.g., from file or ROI manager.
		 */
		private JButton buttonLoad;
		/**
		 * Button to delete currently loaded data.
		 */
		private JButton buttonReset;

		private JLabel iconLabel = null;
		private ImageIcon iconNoData = null;
		private ImageIcon iconData = null;

		private Class<?> myClass;
		private Field myField;
		private ALDParameterDescriptor paramDescr;

		/**
		 * Default constructor.
		 * 
		 * @param field		Parameter field.
		 * @param cla			Class of associated parameter object.
		 * @param data		Default data object.
		 * @param d				(Operator) parameter descriptor.
		 */
		public ColorMapperDataIOFileInPanel(Field field, Class<?> cla, 
				@SuppressWarnings("unused") Object _data, ALDParameterDescriptor d) {

			this.panelContainer = new JPanel();
			this.panelContainer.setLayout(new GridLayout(2,1));
			this.panelContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

			this.dirLoaderPanel = new JPanel();
			this.dirLoaderPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

			this.dirSelection = new ColorMapperInputDirectoryIOPanel(myField, myClass, _data, d);

			this.columnNameToIDMap = new HashMap<>();
			this.columnSelectionItems = new Vector<>();
			this.columnSelection = new ALDSwingComponentComboBox(d, this.columnSelectionItems);
			this.columnSelection.addValueChangeEventListener(this);

			this.myClass = cla;
			this.myField = field;
			this.paramDescr = d;

			// initialize the icons, either from file system or from jar archive
			Image img = null;
			BufferedImage bi = null;
			Graphics g = null;
			InputStream is = null;

			String iconDataName = "/share/icons/Gnome-emblem-default.png";
			String iconNoDataName = "/share/icons/Gnome-emblem-important.png";

			try {
				ImageIcon icon;
				File iconDataFile = new File("./" + iconDataName);
				if(iconDataFile.exists()) {
					icon = new ImageIcon("./" + iconDataName);
					img = icon.getImage();
				}
				// try to find it inside a jar archive....
				else {
					is = ColorMapperDataIOFileInPanel.class.getResourceAsStream(iconDataName);
					if (is == null) {
						System.err.println("Warning - cannot find icons...");
						img = new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
					}
					else
						img = ImageIO.read(is);
				}
				bi= new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
				g = bi.createGraphics();
				g.drawImage(img, 0, 0, 20, 20, null);
				this.iconData = new ImageIcon(bi);

				File iconNoDataFile = new File("./" + iconNoDataName);
				if(iconNoDataFile.exists()) {
					icon = new ImageIcon("./" + iconNoDataName);
					img = icon.getImage();
				}
				// try to find it inside a jar archive....
				else {
					is = ColorMapperDataIOFileInPanel.class.getResourceAsStream(iconNoDataName);
					if (is == null) {
						System.err.println("Warning - cannot find icons...");
						img = new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
					}
					else {
						img = ImageIO.read(is);
					}
				}
				bi= new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
				g = bi.createGraphics();
				g.drawImage(img, 0, 0, 20, 20, null);
				this.iconNoData = new ImageIcon(bi);
			} catch (IOException ex) {
				System.err.println("[ColorMapperDataIOFileSwing] could not load icons!");
			}

			// arrange elements
			this.dirLoaderPanel.setLayout(new BoxLayout(this.dirLoaderPanel,BoxLayout.X_AXIS));
			this.dirLoaderPanel.add(this.dirSelection.getJComponent());
			this.dirLoaderPanel.add(Box.createHorizontalGlue());
			this.buttonLoad= new JButton("Load");
			this.buttonLoad.setActionCommand("load");
			this.buttonLoad.addActionListener(this);
			this.dirLoaderPanel.add(this.buttonLoad);
			this.dirLoaderPanel.add(Box.createHorizontalGlue());
			this.dirLoaderPanel.add(Box.createHorizontalGlue());
			this.buttonReset= new JButton("Reset");
			this.buttonReset.setActionCommand("reset");
			this.buttonReset.addActionListener(this);
			this.dirLoaderPanel.add(this.buttonReset);
			this.dirLoaderPanel.add(Box.createRigidArea(new Dimension(20, 0)));
			// add a nice icon to indicate if data was loaded already...
			if (this.iconNoData != null) {
				this.iconLabel = new JLabel(this.iconNoData);
				this.iconLabel.setToolTipText("No data loaded until now!");
				this.iconLabel.setSize(new Dimension(15,15));
				this.dirLoaderPanel.add(this.iconLabel);
			}

			this.panelContainer.add(this.dirLoaderPanel);

			JPanel columnSelectPanel = new JPanel();
			columnSelectPanel.add(new JLabel("Column to map:    "));
			columnSelectPanel.add(this.columnSelection.getJComponent());
			this.panelContainer.add(columnSelectPanel);
		}

		@Override
		public JComponent getJComponent() {
			return this.panelContainer;
		}

		/**
		 * Returns the data read from GUI or file.
		 * @return	Data object, might be null.
		 * @throws ALDDataIOException Thrown if reading data failed.
		 */
		public Object getData() throws ALDDataIOException {
			String inDir = ((ALDDirectoryString)this.dirSelection.readData(
						myField, myClass)).getDirectoryName();
			if (this.columnSelection.getJComponent().getItemCount() == 0)
				return null;
			ALDSwingComponentItem it = 
				(ALDSwingComponentItem)(this.columnSelection.getJComponent().getSelectedItem());
			return new PaCeQuant_FeatureColorMapperInputData(inDir, 
				this.columnNameToIDMap.get(it.getItemText()));
		}

		/**
		 * Sets panel to given object value.
		 * @param field		Field of data.
		 * @param cl			Class of data.
		 * @param value		Value to set.
		 * @throws ALDDataIOException Thrown if setting data failed.
		 */
		@SuppressWarnings("unused")
		public void setValue(Field field, Class<?> cl, Object value) 
				throws ALDDataIOException {

			// set value is only called from the inside of the Alida framework,
			// so make sure that no additional value change events are triggered
			// during setting a new value in the GUI elements
			ALDDataIOManagerSwing.getInstance().setTriggerValueChangeEvents(false);

			if (value == null)
				return;
			PaCeQuant_FeatureColorMapperInputData pid = 
			 	(PaCeQuant_FeatureColorMapperInputData)value;
			this.dirSelection.setValue(field, cl, pid.getDirectoryName());

			// reactivate value change events
			ALDDataIOManagerSwing.getInstance().setTriggerValueChangeEvents(true);
		}

		@Override
		public void actionPerformed(ActionEvent evt) {

			String command = evt.getActionCommand();
			if (command.equals("load")) {
				try {
					String inDir = ((ALDDirectoryString)this.dirSelection.readData(
						myField, myClass)).getDirectoryName();

					// find first relevant file to parse column headers
					DirectoryTree imgDirTree = new DirectoryTree(inDir, true);
					Vector<String> imgFiles = imgDirTree.getFileList();
	  
					// search for relevant image files
					String tabFile = "";
					for (String img : imgFiles) {
						if (   img.endsWith("grayscale-result.tif") 
							&& img.contains(File.separator + "results" + File.separator)) {
							String dir = ALDFilePathManipulator.getPath(img);
							DirectoryTree localDir = new DirectoryTree(dir, false);
	  
							String shortImg = ALDFilePathManipulator.getFileName(img);
							tabFile = "";
					  		int maxLength = 0;
							for (String tab : localDir.getFileList()) {
								// don't consider other than table files
							  	if (!tab.endsWith("-table.txt"))
									continue;
								// table files with lobe features are to be ignored
								if (tab.endsWith("-lobe-table.txt"))
									continue;
								String shortTab = ALDFilePathManipulator.getFileName(tab);
								int minLength = Math.min(shortImg.length(), shortTab.length());
								for (int i = 0; i < minLength; i++) {
									if (shortImg.charAt(i) != shortTab.charAt(i)) {
										if (i+1 > maxLength) {
											maxLength = i+1;
											tabFile = tab;
										}
										break;
									}
								}
							}
							break;
						}
					}

					// read table model
					// get a provider to read table models
  					ALDDataIOManagerCmdline mc = ALDDataIOManagerCmdline.getInstance();
  					ALDDataIOCmdline pc = (ALDDataIOCmdline)mc.getProvider(
						MTBTableModel.class, ALDDataIOCmdline.class);

					MTBTableModel tm = (MTBTableModel)pc.readData(null,
						MTBTableModel.class, "@" + tabFile);

					// get column names
					String cName;
					this.columnNameToIDMap.clear();

					LinkedList<ALDSwingComponentItem> its = new LinkedList<>();
					for (int i=0;i<tm.getColumnCount();++i) {
						cName = tm.getColumnName(i).split("_")[0];
						its.add(new ALDSwingComponentItem(cName, cName, cName));
						this.columnNameToIDMap.put(cName, i);
					}
					Collections.sort(its);
					Vector<ALDSwingComponentItem> itsv = new Vector<>();
					for (ALDSwingComponentItem sc : its)
						itsv.add(sc);
					this.columnSelection.updateItems(itsv);

					if (this.iconData != null)
						this.iconLabel.setIcon(this.iconData);
					this.iconLabel.setToolTipText("Configuration loaded!");
					// trigger event that configuration changed
					this.handleValueChangeEvent(
						new ALDSwingValueChangeEvent(this, this.paramDescr));
				} catch (ALDDataIOException ex) {
					ex.printStackTrace();
					 JOptionPane.showMessageDialog(this.panelContainer, 
					 	"Problem loading file configuration...!", 
 							"Warning", JOptionPane.WARNING_MESSAGE);
				}
			}
			// reset provider
			else if (command.equals("reset")) {
				this.columnSelection.clearItems();
				if (this.iconNoData != null)
					this.iconLabel.setIcon(this.iconNoData);
				this.iconLabel.setToolTipText("No data loaded until now!");
				// trigger event that configuration changed
				this.handleValueChangeEvent(
					new ALDSwingValueChangeEvent(this, this.paramDescr));
			}
		}

		@Override
		public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}

		@Override
	    public void disableComponent() {
			this.buttonLoad.setEnabled(false);
			this.buttonReset.setEnabled(false);
    	}

		@Override
    	public void enableComponent() {
			this.buttonLoad.setEnabled(true);
			this.buttonReset.setEnabled(true);
    	}

		@Override
    	public void dispose() {
			// nothing to do here
    	}
	}

	protected class ColorMapperInputDirectoryIOPanel extends ALDSwingComponent 
		implements ActionListener, ALDSwingValueChangeListener {

		/**
		 * Swing component to be integrated in GUI.
		 */
		private JPanel ioPanel = null;
		
		/**
		 * Button to select via dialogue.
		 */
		private JButton selectFileDir = null;

		/**
		 * File/directory chooser dialogue.
		 */
		private JFileChooser getDirDialog;

		/**
		 * Last directory selected by user.
		 */
		private File lastDirectory = null;
		
		/**
		 * Last file selected by user.
		 */
		private File lastFile = null;

		/**
		 * Text field in the panel.
		 */
		private ALDSwingComponentTextField textField = null;
		
		/**
		 * Descriptor of the associated (operator) parameter.
		 */
		private ALDParameterDescriptor paramDescriptor;
		
		/**
		 * Default directory to be used initially.
		 */
		protected final String directoryDefault = 
			System.getProperty("user.dir");

		/**
		 * Default constructor.
		 * 
		 * @param field	Field to consider.
		 * @param cl		Class to consider.
		 * @param obj		Default object.
		 * @param descr Descriptor of associated parameter.
		 */
		protected ColorMapperInputDirectoryIOPanel(
				@SuppressWarnings("unused") Field field, Class<?> cl, Object obj,
				ALDParameterDescriptor descr) {

			this.paramDescriptor = descr;
			
			// some local variables and default settings
			String defValue = null;
			if (descr.isRequired())
				defValue = directoryDefault;
			else
				defValue = new String();
			
			// init the panel
			this.ioPanel = new JPanel();
			this.ioPanel.setLayout(new BoxLayout(this.ioPanel, BoxLayout.X_AXIS));
			this.textField = 
				new ALDSwingComponentTextField(String.class, descr, 20);
			this.textField.getJComponent().setSize(70,5);
			this.textField.addValueChangeEventListener(this);
			if (obj != null) {
				defValue = ((PaCeQuant_FeatureColorMapperInputData)obj).getDirectoryName();
			}
			if (defValue != null) {
				this.lastDirectory = new File(defValue);
				this.textField.setText(defValue);
			}
			this.ioPanel.add(this.textField.getJComponent());
			this.selectFileDir= new JButton("Choose...");
			this.selectFileDir.setActionCommand("choose");
			this.selectFileDir.addActionListener(this);
			this.ioPanel.add(this.selectFileDir);
		}
		
		@Override
		public JPanel getJComponent() {
			return this.ioPanel;
		}
		
		/**
		 * Returns the contents of the text field.
		 * 
		 * @param field	Field to consider.
		 * @param cl		Class to consider.
		 * @param value	Object value to be set, here just a directory (string) name.
		 */
		public void setValue(@SuppressWarnings("unused") Field field, 
				@SuppressWarnings("unused") Class<?> cl, Object value) {
			this.textField.setText((String)value);
			this.lastDirectory = new File((String)value);
		}

		/**
		 * Returns the contents of the text field.
		 * 
		 * @param field	Field to consider.
		 * @param cl		Class to consider.
		 * @return	Read object.
		 * @throws ALDDataIOProviderException Thrown in case of failure.
		 */
		public Object readData(@SuppressWarnings("unused") Field field, Class<?> cl) 
			throws ALDDataIOProviderException {
			if (	this.textField.getText() == null 
				|| 	this.textField.getText().isEmpty())
				return null;
			return new ALDDirectoryString(this.textField.getText());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();
			if (command.equals("choose")) {
				// open file chooser
				this.getDirDialog= new JFileChooser();
				if (    this.textField.getText() != null 
					&& !this.textField.getText().isEmpty()) {
					String selection = this.textField.getText();
					String file = 
						ALDFilePathManipulator.removeLeadingDirectories(selection);
					String dir = ALDFilePathManipulator.getPath(selection);
					if (!file.isEmpty())
						this.getDirDialog.setSelectedFile(new File(file));
					if (!dir.isEmpty())
						this.getDirDialog.setCurrentDirectory(new File(dir));
					else {
						if (selection.substring(0,1).equals(
								System.getProperty("file.separator"))) {
							this.getDirDialog.setCurrentDirectory(new File(selection));
						}
					}
				}
				else {
					this.getDirDialog.setCurrentDirectory(this.lastDirectory);
					this.getDirDialog.setSelectedFile(this.lastFile);
				}
				this.getDirDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				this.getDirDialog.setDialogTitle("Select a directory...");
				
				int returnVal = this.getDirDialog.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = this.getDirDialog.getSelectedFile();
					this.textField.setText(file.getPath());
					this.lastDirectory = new File(file.getPath());
					this.lastFile = this.getDirDialog.getSelectedFile();
					this.handleValueChangeEvent(
						new ALDSwingValueChangeEvent(this, this.paramDescriptor));
				}
			}
	  	}

		@Override
		public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			this.fireALDSwingValueChangeEvent(event);
		}

		@Override
	    public void disableComponent() {
			this.selectFileDir.setEnabled(false);
			this.textField.disableComponent();
    	}

		@Override
    	public void enableComponent() {
			this.selectFileDir.setEnabled(true);
			this.textField.enableComponent();
    	}
		
		@Override
    	public void dispose() {
			// nothing to do here
		}
	}
}
