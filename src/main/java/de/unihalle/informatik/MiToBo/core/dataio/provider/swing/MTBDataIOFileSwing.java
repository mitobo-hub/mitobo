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

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing;

import ij.io.OpenDialog;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import javax.imageio.ImageIO;
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
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwingInitialGUIValueDefaultHandler;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDParametrizedClassConfigWindow;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeListener;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.helpers.ALDEnvironmentConfig;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.core.dataio.provider.cmdline.MTBDataIOFile;
import de.unihalle.informatik.MiToBo.core.dataio.provider.swing.components.MTBTableWindow;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

/**
 * Data I/O provider for GUI-OpRunner supporting MiToBo container classes
 * <p> 
 * Provides data I/O for the following classes:<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet}<br>
 * <p>
 * Note that some of the classes allow for interaction with the ROI manager 
 * of ImageJ, i.e. the class 
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet}.
 * It can be initialized with selections from the ROI manager, and resulting
 * polygons can also be added to the ROI manager.
 * 
 * @author moeller, misiak
 */
@ALDDataIOProvider(priority=10)
public class MTBDataIOFileSwing 
	extends ALDDataIOSwingInitialGUIValueDefaultHandler {

	/**
	 * Input mode for data.
	 * @author moeller
	 */
	public static enum InputMode {
		/**
		 * Get data from ImageJ roi manager.
		 */
		ROI_MANAGER,
		/**
		 * Read data from XML beans MiToBo XML file (library: xbeans).
		 */
		MTB_XML,
		/**
		 * Read data from serialized XML file (library: xstream).
		 */
		SERIAL_XML,
		/**
		 * Manual input of the data.
		 */
		MANUAL
	}

	/**
	 * Output mode for data.
	 * @author moeller
	 */
	public static enum OutputMode {
		/**
		 * Export the data to the ImageJ roi manager.
		 */
		ROI_MANAGER,
		/**
		 * Measure data for some datatypes, like area of regions, length of contours. 
		 */
		MEASURE,
		/**
		 * Save the data to XML beans MiToBo XML file (library: xbeans).
		 */
		MTB_XML,
		/**
		 * Save the data to serialized XML file (library: xstream).
		 */
		SERIAL_XML,
		/**
		 * Show the data in graphical component.
		 */
		GUI
	}

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(MTBRegion2DSetBag.class);
		l.add(MTBRegion2DSet.class);
		l.add(MTBRegion3DSet.class);
		l.add(MTBPolygon2DSet.class);
		l.add(MTBContour2DSet.class);
		l.add(MTBBorder2DSet.class);
		return l;
	}

	@Override
	public ALDSwingComponent createGUIElement(
			Field field, Class<?> cl, Object obj, ALDParameterDescriptor descr) {
		return new MTBDataIOFileInPanel(field, cl, obj, descr);
	}

	@Override
	public void setValue(			
			Field field, Class<?> cl, ALDSwingComponent guiElement, Object value) 
					throws ALDDataIOProviderException {
		
		if (!(guiElement instanceof MTBDataIOFileInPanel)) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.INVALID_GUI_ELEMENT,
					"[MTBDataIOFileSwing] setValue() received wrong GUI element!");
		}
		if (!this.providedClasses().contains(cl)) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"[MTBDataIOFileSwing] setValue() received object of invalid type!");
		}
		MTBDataIOFileInPanel inPanel = (MTBDataIOFileInPanel)guiElement;
		try {
			inPanel.setValue(field, cl, value);
		} catch (ALDDataIOException e) {
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
					"[MTBDataIOFileSwing] setValue() failed, reason is...\n" +
							e.getCommentString());
		}
	}

	@Override
	public Object readData(Field field, Class<?> cl, ALDSwingComponent guiElem) 
			throws ALDDataIOProviderException {
		Object data = null;
		if (   guiElem instanceof MTBDataIOFileInPanel 
				&& this.providedClasses().contains(cl)) {
			try {
				data = ((MTBDataIOFileInPanel)guiElem).getData();
			} catch (ALDDataIOException e) {
				throw new ALDDataIOProviderException(
						ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						e.getCommentString());
			}
		}
		return data;
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) {
		return new MTBDataIOFileOutPanel(obj, descr);
	}

	/**
	 * Panel for handling GUI I/O of MiToBo container classes.
	 * 
	 * @author moeller
	 */
	private class MTBDataIOFileInPanel extends ALDSwingComponent 
	implements ActionListener, ALDSwingValueChangeListener {

		/**
		 * Main panel containing all graphical components.
		 */
		private JPanel mainPanel;

		private JComboBox modeSelection;

		private Vector<Object> inModes = new Vector<Object>();

		/**
		 * Currently loaded data set.
		 */
		private Object data = null;

		/**
		 * Button to load data, e.g., from file or ROI manager.
		 */
		private JButton buttonLoad;
		/**
		 * Button to edit loaded data or insert data manually.
		 */
		private JButton buttonEdit;
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

		private boolean roisAdded = false;

		/**
		 * Configuration window to show/edit loaded data. 
		 */
		private ALDParametrizedClassConfigWindow confWin;

		/**
		 * Last directory selected by user.
		 */
		private File lastDirectory;

		/**
		 * Last file selected by user.
		 */
		private File lastFile;

		/**
		 * Default constructor.
		 * 
		 * @param field		Parameter field.
		 * @param cla			Class of associated parameter object.
		 * @param data		Default data object.
		 * @param d				(Operator) parameter descriptor.
		 */
		public MTBDataIOFileInPanel(Field field, Class<?> cla, 
				@SuppressWarnings("unused") Object _data, ALDParameterDescriptor d) {
			this.mainPanel = new JPanel();
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
					is = MTBDataIOFileInPanel.class.getResourceAsStream(iconDataName);
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
					is = MTBDataIOFileInPanel.class.getResourceAsStream(iconNoDataName);
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
				System.err.println("MTBDataIOFileSwing - could not load icons!");
			}

			// fill output modes into selection
			Object[] consts = InputMode.class.getEnumConstants();
			for ( Object c : consts ) {
				this.inModes.add( c);
			}
			this.modeSelection = new JComboBox(this.inModes);

			// arrange elements
			this.mainPanel.setLayout(new BoxLayout(this.mainPanel,BoxLayout.X_AXIS));
			this.mainPanel.add(new JLabel("    Input mode =   "));
			this.mainPanel.add(this.modeSelection);
			this.mainPanel.add(Box.createHorizontalGlue());
			this.buttonLoad= new JButton("Load");
			this.buttonLoad.setActionCommand("load");
			this.buttonLoad.addActionListener(this);
			this.mainPanel.add(this.buttonLoad);
			this.mainPanel.add(Box.createHorizontalGlue());
			this.buttonEdit= new JButton("Edit");
			this.buttonEdit.setActionCommand("edit");
			this.buttonEdit.addActionListener(this);
			this.mainPanel.add(this.buttonEdit);
			this.mainPanel.add(Box.createHorizontalGlue());
			this.buttonReset= new JButton("Reset");
			this.buttonReset.setActionCommand("reset");
			this.buttonReset.addActionListener(this);
			this.mainPanel.add(this.buttonReset);
			this.mainPanel.add(Box.createRigidArea(new Dimension(20, 0)));
			// add a nice icon to indicate if data was loaded already...
			if (this.iconNoData != null) {
				this.iconLabel = new JLabel(this.iconNoData);
				this.iconLabel.setToolTipText("No data loaded until now!");
				this.iconLabel.setSize(new Dimension(15,15));
				this.mainPanel.add(this.iconLabel);
			}

			// init associated window
			this.confWin = new ALDParametrizedClassConfigWindow(this.myClass, 
					this.paramDescr);
			this.confWin.addValueChangeEventListener(this);
			try {
				this.confWin.setVisible(false);
			} catch (ALDDataIOProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.lastDirectory = new File(System.getProperty("user.home"));
			this.lastFile = null;
		}

		@Override
		public JComponent getJComponent() {
			return this.mainPanel;
		}

		/**
		 * Returns the data read from GUI or file.
		 * @return	Data object, might be null.
		 * @throws ALDDataIOException Thrown if reading data failed.
		 */
		public Object getData() throws ALDDataIOException {
			InputMode selectedMode = 
					(InputMode)this.modeSelection.getSelectedItem();
			if (selectedMode == InputMode.MANUAL) {
				this.data = this.confWin.readData(this.myField, this.myClass);
			}
			return this.data;
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
			this.data = value;
			if (this.data != null) {
				if (this.iconData != null)
					this.iconLabel.setIcon(this.iconData);
				this.iconLabel.setToolTipText("Data loaded!");
			}
			if (this.confWin.isVisible() && value != null)
				this.confWin.setValue(value);
		}

		@Override
		public void actionPerformed(ActionEvent evt) {

			String command = evt.getActionCommand();
			if (command.equals("load")) {
				InputMode selectedMode = 
						(InputMode)this.modeSelection.getSelectedItem();
				switch(selectedMode)
				{
				case ROI_MANAGER:
					if (this.myClass.equals(MTBPolygon2DSet.class)) {
						MTBPolygon2DSet pset= 
								RoiManagerAdapter.getInstance().getPolygonSetFromRoiManager();
						if (pset.size() == 0)
							JOptionPane.showMessageDialog(this.mainPanel, 
									"ROI Manager does not contain any valid selection... no data!", 
									"Warning", JOptionPane.WARNING_MESSAGE);
						else {
							this.data = pset;
						}
					}
					else if (this.myClass.equals(MTBRegion2DSet.class)) {
						MTBRegion2DSet rset= 
								RoiManagerAdapter.getInstance().getRegionSetFromRoiManager();
						if (rset.size() == 0)
							JOptionPane.showMessageDialog(this.mainPanel, 
									"ROI Manager does not contain any valid selection... no data!", 
									"Warning", JOptionPane.WARNING_MESSAGE);
						else {
							this.data = rset; 
						}
					}
					else if (this.myClass.equals(MTBContour2DSet.class)) {
						MTBContour2DSet cset= 
								RoiManagerAdapter.getInstance().getContourSetFromRoiManager();
						if (cset.size() == 0)
							JOptionPane.showMessageDialog(this.mainPanel, 
									"ROI Manager does not contain any valid selection... no data!", 
									"Warning", JOptionPane.WARNING_MESSAGE);
						else {
							this.data = cset;
						}
					}
					else if (this.myClass.equals(MTBBorder2DSet.class)) {
							MTBBorder2DSet bset= 
									RoiManagerAdapter.getInstance().getBorderSetFromRoiManager();
							if (bset.size() == 0)
								JOptionPane.showMessageDialog(this.mainPanel, 
										"ROI Manager does not contain any valid selection... no data!", 
										"Warning", JOptionPane.WARNING_MESSAGE);
							else {
								this.data = bset;
							}
						}
					else {
						JOptionPane.showMessageDialog(this.mainPanel, "Input mode not supported \n" + 
								"for data type \"" + this.myClass.getSimpleName() + "\"",
								"Warning", JOptionPane.WARNING_MESSAGE);
					}
					try {
						if (this.confWin.isVisible() && this.data != null) {
							this.confWin.setValue(this.data);
						}
						else {
							this.handleValueChangeEvent(
								new ALDSwingValueChangeEvent(this, this.paramDescr));
						}
						// remember that regions were read from ROI manager
						this.roisAdded = true;
					} catch (ALDDataIOException e) {
						JOptionPane.showMessageDialog(this.mainPanel, "Problem setting data...!", 
								"Warning", JOptionPane.WARNING_MESSAGE);
					}
					break;
				case MANUAL:
					if (   this.myClass.equals(MTBPolygon2DSet.class)
							|| this.myClass.equals(MTBRegion2DSet.class)) {
						try {
							this.confWin.setVisible(true);
						} catch (ALDDataIOProviderException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						this.roisAdded = false;
					}
					else {
						JOptionPane.showMessageDialog(this.mainPanel, "Input mode not supported \n" + 
								"for data type \"" + this.myClass.getSimpleName() + "\"",
								"Warning", JOptionPane.WARNING_MESSAGE);
					}
					break;
				case MTB_XML:
					// open file chooser
					JFileChooser getDirDialog= new JFileChooser();
					if (this.lastFile != null)
						getDirDialog.setSelectedFile(this.lastFile);
					if (this.lastDirectory != null)
						getDirDialog.setCurrentDirectory(this.lastDirectory);
					getDirDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
					getDirDialog.setDialogTitle("Select a file...");
					int returnVal = getDirDialog.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = getDirDialog.getSelectedFile();
						this.lastDirectory = getDirDialog.getCurrentDirectory();
						this.lastFile = getDirDialog.getSelectedFile();
						MTBDataIOFile helperIO = new MTBDataIOFile();
						try {
							this.data = helperIO.readData(
									this.myField, this.myClass,	file.getAbsolutePath());
						} catch (ALDDataIOProviderException e1) {
							JOptionPane.showMessageDialog(this.mainPanel, "Problem setting data...!", 
									"Warning", JOptionPane.WARNING_MESSAGE);
						}
						this.roisAdded = false;
						try {
							this.confWin.setValue(this.data);
						} catch (ALDDataIOException e) {
							JOptionPane.showMessageDialog(this.mainPanel, "Problem setting data...!", 
									"Warning", JOptionPane.WARNING_MESSAGE);
						}
					}
					break;
				case SERIAL_XML:
					JOptionPane.showMessageDialog(this.mainPanel, "Not yet supported!", 
							"Warning", JOptionPane.WARNING_MESSAGE);
					break;
				}
				if (this.data != null) {
					if (this.iconData != null)
						this.iconLabel.setIcon(this.iconData);
					this.iconLabel.setToolTipText("Data loaded!");
				}
			}
			// edit the data
			else if (command.equals("edit")) {
				if (this.data == null) {
					JOptionPane.showMessageDialog(this.mainPanel, "No data available!\n" + 
							"You did not load/enter data yet!", "Data Error", 
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					try {
						this.confWin.setValue(this.data);
						this.confWin.setVisible(true);
					} catch (ALDDataIOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			// reset provider
			else if (command.equals("reset")) {
				this.data = null;
				try {
	        this.confWin.setValue(null);
					this.confWin.setVisible(false);
					if (this.iconNoData != null)
						this.iconLabel.setIcon(this.iconNoData);
					this.iconLabel.setToolTipText("No data loaded until now!");
					// trigger event that configuration changed
					this.handleValueChangeEvent(
							new ALDSwingValueChangeEvent(this, this.paramDescr));
        } catch (ALDDataIOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
			}
		}

		@Override
		public void handleValueChangeEvent(ALDSwingValueChangeEvent event) {
			//			System.out.println("Data changed...");
			this.fireALDSwingValueChangeEvent(event);
		}

		@Override
    public void disableComponent() {
			this.buttonEdit.setEnabled(false);
			this.buttonLoad.setEnabled(false);
			this.modeSelection.setEnabled(false);
    }

		@Override
    public void enableComponent() {
			this.buttonEdit.setEnabled(true);
			this.buttonLoad.setEnabled(true);
			this.modeSelection.setEnabled(true);
    }

		@Override
    public void dispose() {
			if (this.confWin != null)
				this.confWin.dispose();
    }
	}

	/**
	 * Panel for displaying and saving MiToBo container classes.
	 * 
	 * @author moeller
	 */
	private class MTBDataIOFileOutPanel extends JPanel 
		implements ActionListener {

		private JComboBox modeSelection;

		private Vector<Object> outModes = new Vector<Object>();

		private Object myData = null;
		
		private ALDParameterDescriptor myDescr;

		private JButton buttonShow;

		private ALDParametrizedClassConfigWindow confWin = null;

		private boolean roisAdded = false;

		/**
		 * Default constructor.
		 * 
		 * @param field
		 * @param cla
		 * @param data
		 */
		public MTBDataIOFileOutPanel(Object data, ALDParameterDescriptor d) {

			this.myData = data;
			this.myDescr = d;

			// fill output modes into selection
			Object[] consts = OutputMode.class.getEnumConstants();
			for ( Object c : consts ) {
				this.outModes.add( c);
			}
			this.modeSelection = new JComboBox(this.outModes);

			// arrange elements
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			this.add(new JLabel("    Output mode =   "));
			this.add(this.modeSelection);
			this.add(Box.createHorizontalGlue());
			this.buttonShow= new JButton("Save/View Data");
			this.buttonShow.setActionCommand("save");
			this.buttonShow.addActionListener(this);
			this.add(this.buttonShow);
		}

		@Override
		public void actionPerformed(ActionEvent evt) {

			String command = evt.getActionCommand();
			if (command.equals("save")) {
				OutputMode selectedMode = 
						(OutputMode)this.modeSelection.getSelectedItem();
				switch(selectedMode)
				{
				case ROI_MANAGER:
					if (this.roisAdded) {
						Object[] options = { "OK"};
						JOptionPane.showOptionDialog(null, 
								"Regions were already added to ROI manager!",
								"Warning", JOptionPane.DEFAULT_OPTION,
								JOptionPane.WARNING_MESSAGE,
								null, options, options);
						return;
					}
					if (this.myData instanceof MTBPolygon2DSet) {
						MTBPolygon2DSet polygons = (MTBPolygon2DSet)this.myData;
						RoiManagerAdapter.getInstance().addPolygonsToRoiManager(polygons);
						this.roisAdded = true;
					}
					else if (this.myData instanceof MTBRegion2DSet) {
						MTBRegion2DSet regions = (MTBRegion2DSet)this.myData;
						RoiManagerAdapter.getInstance().addRegionsToRoiManager(regions);
						this.roisAdded = true;
					}
					else if (this.myData instanceof MTBContour2DSet) {
						MTBContour2DSet contours = (MTBContour2DSet)this.myData;
						RoiManagerAdapter.getInstance().addContoursToRoiManager(contours);
						this.roisAdded = true;


						//										MTBRegion2DSet regions = (MTBRegion2DSet)this.myData;
						//						polygons = new MTBPolygon2DSet(0, 0, 0, 0);
						//						for (int i=0; i<regions.size(); ++i) {
						//							MTBRegion2D reg = regions.elementAt(i);
						//							try {
						//								MTBContour2D cont = reg.getContour();
						//								MTBPolygon2D poly = new MTBPolygon2D(cont.getPoints(),true);
						//								polygons.add(poly);
						//							} catch (ALDOperatorException e) {
						//								System.err.println("MTBDataIOFile - skipping polygon!");
						//							} catch (ALDProcessingDAGException e) {
						//								System.err.println("MTBDataIOFile - skipping polygon!");
						//							}
						//						}
					}
					else if (this.myData instanceof MTBBorder2DSet) {
							MTBBorder2DSet borders = (MTBBorder2DSet)this.myData;
							RoiManagerAdapter.getInstance().addBordersToRoiManager(borders);
							this.roisAdded = true;
					}
					//					if (polygons == null) {
					//						Object[] options = { "OK"};
					//						JOptionPane.showOptionDialog(null, 
					//								"Adding data to ROI manager not supported for type \n" +
					//								this.myData.getClass().getSimpleName() + "...",
					//								"Warning", JOptionPane.DEFAULT_OPTION,
					//								JOptionPane.WARNING_MESSAGE,
					//								null, options, options);
					//						return;
					//					}
					//					if (polygons.size() > 2000) {
					//						Object[] options = { "OK", "CANCEL" };
					//						int ret = JOptionPane.showOptionDialog(null, 
					//								"Detected more than 2000 objects...\n" +
					//								"Really add all objects to ROI manager?",
					//								"Warning", JOptionPane.DEFAULT_OPTION,
					//								JOptionPane.WARNING_MESSAGE,
					//								null, options, options);
					//						if (ret == JOptionPane.CANCEL_OPTION)
					//							return;
					//					}
					//					RoiManagerAdapter.getInstance().addPolygonsToRoiManager(polygons);
					//					this.roisAdded = true;
										break;
								case MEASURE:
										MTBTableModel measureTable = new MTBTableModel(0, 0);
										Vector<String> header = new Vector<String>();
										String tableTitle = "";
										boolean isBorder = false;

										if (this.myData instanceof MTBPolygon2DSet) {
												tableTitle = "Measurements-MTBPolygon2DSet";
												header.add("MTBPolygon2D");
												header.add("length");
												header.add("isClosed");
												header.add("isSimple");
												header.add("isConvex");
												header.add("isClockwise");
												measureTable = new MTBTableModel(0, header.size(), header);
												MTBPolygon2DSet polygons = (MTBPolygon2DSet) this.myData;
												for (int i = 0; i < polygons.size(); i++) {
														MTBPolygon2D polygon = polygons.elementAt(i);
														measureTable.setValueAt(new Integer(i), i, 0);
														measureTable.setValueAt(new Double(polygon.getLength()), i, 1);
														measureTable.setValueAt(new Boolean(polygon.isClosed()), i, 2);
//														measureTable.setValueAt(polygon.jni_isSimple(), i, 3);
//														measureTable.setValueAt(polygon.jni_isConvex(), i, 4);
														measureTable.setValueAt(new Boolean(polygon.isSimple()), i, 3);
														measureTable.setValueAt(new Boolean(polygon.isConvex()), i, 4);
//														try {
//																measureTable.setValueAt(polygon.jni_isClockwiseOriented(), i, 5);
//														} catch (MTBPolygon2DException e) {
//																// TODO Auto-generated catch block
//																e.printStackTrace();
//																measureTable.setValueAt("NA", i, 5);
//														}
													measureTable.setValueAt(
														new Boolean(polygon.isOrderedClockwise()), i, 5);
												}
										} else if (this.myData instanceof MTBRegion2DSet) {
												tableTitle = "Measurements-MTBRegion2DSet";
												header.add("MTBRegion2D");
												header.add("area");
												header.add("orientation");
												header.add("circularity");
												header.add("majorAxisLength");
												measureTable = new MTBTableModel(0, header.size(), header);
												MTBRegion2DSet regions = (MTBRegion2DSet) this.myData;
												for (int i = 0; i < regions.size(); i++) {
														MTBRegion2D region = regions.elementAt(i);
														measureTable.setValueAt(i, i, 0);
														measureTable.setValueAt(region.getArea(), i, 1);
														measureTable.setValueAt(region.getOrientation(), i, 2);
														try {
																measureTable.setValueAt(region.getCorrCircularity(), i, 3);
														} catch (ALDOperatorException e) {
																// TODO Auto-generated catch block
																e.printStackTrace();
														} catch (ALDProcessingDAGException e) {
																// TODO Auto-generated catch block
																e.printStackTrace();
														}
														measureTable.setValueAt(region.getMajorAxisLength(), i, 4);
												}
										} else if (this.myData instanceof MTBContour2DSet) {
												tableTitle = "Measurements-MTBContour2DSet";
												header.add("MTBContour2D_ID");
												header.add("length");
												header.add("innerContours");
												header.add("innerContour_ID");
												header.add("inner_length");
												measureTable = new MTBTableModel(0, header.size(), header);
												MTBContour2DSet contours = (MTBContour2DSet) this.myData;
												int row = 0;
												for (int i = 0; i < contours.size(); i++) {
														MTBContour2D contour = contours.elementAt(i);
														Vector<MTBContour2D> innerContours = contour.getAllInner();
														if (innerContours.size() == 0) {
																measureTable.setValueAt(i, row, 0);
																measureTable.setValueAt(contour.getContourLength(), row, 1);
																measureTable.setValueAt(contour.getInnerCount(), row, 2);
																measureTable.setValueAt(0, row, 3);
																measureTable.setValueAt(0, row, 4);
																row++;
														} else {
																for (int j = 0; j < innerContours.size(); j++) {
																		measureTable.setValueAt(i, row, 0);
																		measureTable.setValueAt(contour.getContourLength(), row, 1);
																		measureTable.setValueAt(contour.getInnerCount(), row, 2);
																		MTBContour2D inner = innerContours.elementAt(j);
																		measureTable.setValueAt(j, row, 3);
																		measureTable.setValueAt(inner.getContourLength(), row, 4);
																		row++;
																}
														}
												}
										} else if (this.myData instanceof MTBBorder2DSet) {
												isBorder = true;
												JOptionPane.showMessageDialog(this,
												    "No measurements supportet for type MTBBorder2D!", "Warning",
												    JOptionPane.WARNING_MESSAGE);
										}
										
										if (!isBorder) {
												// show measurements in table window
												MTBTableWindow window = new MTBTableWindow(measureTable);
												window.setTitle(tableTitle);
												window.setVisible(true);
										}
										break;
				case MTB_XML:
					String idir = 
					ALDEnvironmentConfig.getConfigValue("mitobo", null, "savedir");
					if (idir == null)
						idir = 
						ALDEnvironmentConfig.getConfigValue("mitobo", null, "imagedir");
					if (idir == null)
						idir = OpenDialog.getLastDirectory();
					if (idir == null)
						idir = OpenDialog.getDefaultDirectory();

					JFileChooser chooser = new JFileChooser();
					if (idir != null)
						chooser.setCurrentDirectory(new File(idir));

					if(chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
						File f = chooser.getSelectedFile();
						boolean write = true;
						if (f.exists()) {
							if (JOptionPane.showConfirmDialog(this,"Overwrite file '" + f.getName() +"' ?", 
									"File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
								write = false;
						}

						if (write) {
							String pdir = f.getParent();
							// set initial directory of the file chooser for future call
							if (pdir != null) {
								OpenDialog.setLastDirectory(pdir);
								OpenDialog.setDefaultDirectory(pdir);
							}
							try {
								MTBDataIOFile helperIO = new MTBDataIOFile();
								helperIO.writeData(this.myData, f.getAbsolutePath());
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(this, "Failed to write '" + f.getName() + "': " + e1.getMessage(), 
										"Write Error", JOptionPane.ERROR_MESSAGE);
							} 
						}
					}
					break;
				case SERIAL_XML:
					JOptionPane.showMessageDialog(this, "Not yet supported!", 
							"Warning", JOptionPane.WARNING_MESSAGE);
					break;
				case GUI:
					try {
						// init associated window, data is non-editable
						if (this.confWin == null)
							this.confWin = new ALDParametrizedClassConfigWindow(
									this.myData, this.myDescr, true);
						else
							this.confWin.setValue(this.myData);
						this.confWin.setVisible(true);
					} catch (ALDDataIOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}
	}
}
