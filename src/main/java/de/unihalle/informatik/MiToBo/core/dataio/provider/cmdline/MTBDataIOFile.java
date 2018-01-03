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

package de.unihalle.informatik.MiToBo.core.dataio.provider.cmdline;

import ij.io.OpenDialog;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.helpers.ALDEnvironmentConfig;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;

/**
 * DataIO provider (for commandline-OpRunner) for classes that can 
 * only be read from and written to file.
 * <p> 
 * This class is meant to be extended for any such data class.<br>
 * Provides DataIO for the following classes:<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet}<br>
 * <p>
 * Note that some of the classes allow for interaction with the ROI manager 
 * of ImageJ, i.e. the class 
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet}.
 * It can be initialized with selections from the ROI manager, and resulting
 * polygons can also be added to the ROI manager.
 * 
 * @author Oliver Gress
 * @author moeller
 */
@ALDDataIOProvider
public class MTBDataIOFile implements ALDDataIOCmdline {

	@Override
	public Collection<Class<?>> providedClasses() {
		
		// add your class to the list !!
		
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(MTBRegion2DSetBag.class);
		l.add(MTBRegion2DSet.class);
		l.add(MTBRegion3DSet.class);
		l.add(MTBPolygon2DSet.class);
		l.add(MTBContour2DSet.class);
		
		return l;
	}

	@Override
	public Object readData(Field field, Class<?> cl, String iname) 
			throws ALDDataIOProviderException {
		if (field != null)
			cl = field.getType();
		
		// add your class reader
		
		if (cl.equals(MTBRegion2DSetBag.class)) {
			try {
				return new MTBRegion2DSetBag(iname);
			} catch (XmlException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion2DSetBag from xml-file " + iname + "\n" +
								e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion2DSetBag from xml-file " + iname + "\n" +
								e.getMessage());
			}
		}
		else if (cl.equals(MTBRegion2DSet.class)) {
			try {
				if (iname.endsWith(".xml")) {
					return new MTBRegion2DSet(iname);
				}
				else if (iname.endsWith(".zip") || iname.endsWith(".roi")) { 
					return RoiManagerAdapter.getInstance().getRegionSetFromRoiFile(iname);
				}
				else 
					throw new ALDDataIOProviderException( 
						ALDDataIOProviderExceptionType.FILE_IO_ERROR,
							"MTBDataIOFile::readData cannot read MTBRegion2DSet from file " 
								+ iname + ",\n" + "unknown format");
			} catch (XmlException e) {
				throw new ALDDataIOProviderException( 
					ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion2DSet from xml-file " 
							+ iname + "\n" + e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( 
					ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion2DSet from xml-file " 
							+ iname + "\n" + e.getMessage());
			} catch (ALDException e) {
				throw new ALDDataIOProviderException( 
					ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion2DSet from xml-file " 
							+ iname + "\n" + e.getMessage());
			}
		}
		else if (cl.equals(MTBRegion3DSet.class)) {
			try {
				return new MTBRegion3DSet(iname);
			} catch (XmlException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion3DSet from xml-file " + iname + "\n" +
								e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBDataIOFile::readData cannot read MTBRegion3DSet from xml-file " + iname + "\n" +
								e.getMessage());
			}
		}
		else if (cl.equals(MTBPolygon2DSet.class)) {
			try {
				if (iname.endsWith(".xml")) {
					MTBPolygon2DSet pset = new MTBPolygon2DSet(0,0,0,0);
					pset.read(iname);
					return pset;
				}
			} catch (ClassNotFoundException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.OBJECT_INSTANTIATION_ERROR,
						"MTBDataIOFile::readData cannot read MTBPolygon2DSet from xml-file " + iname + "\n" +
								e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBDataIOFile::readData cannot read MTBPolygon2DSet from xml-file " + iname + "\n" +
								e.getMessage());
			} catch (XmlException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBDataIOFile::readData cannot read MTBPolygon2DSet from xml-file " + iname + "\n" +
								e.getMessage());
			}
			try	{
				// try to read from ROI file
				if (iname.endsWith(".zip")) {
					return 
							RoiManagerAdapter.getInstance().getPolygonSetFromRoiFile(
									iname, false);
				}
				throw new ALDDataIOProviderException( 
					ALDDataIOProviderExceptionType.OBJECT_INSTANTIATION_ERROR,
						"MTBDataIOFile::readData cannot read MTBPolygon2DSet from file " 
								+ iname + ", \n format unknown!");
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( 
					ALDDataIOProviderExceptionType.OBJECT_INSTANTIATION_ERROR,
						"MTBDataIOFile::readData cannot read MTBPolygon2DSet from zip file " 
								+ iname + "\n" + e.getMessage());
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( 
					ALDDataIOProviderExceptionType.OBJECT_INSTANTIATION_ERROR,
						"MTBDataIOFile::readData cannot read MTBPolygon2DSet from zip file " 
								+ iname + "\n" + e.getMessage());
			}
		}
		else if (cl.equals(MTBContour2DSet.class)) {
			try {
				MTBContour2DSet cset = new MTBContour2DSet(0,0,0,0);
				cset.read(iname);
				return cset;
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBDataIOFile::readData cannot read MTBContour2DSet from xml-file " + iname + "\n" +
								e.getMessage());
			} catch (XmlException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBDataIOFile::readData cannot read MTBContour2DSet from xml-file " + iname + "\n" +
								e.getMessage());
			}
		}
		else {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
					"MTBDataIOFile::readData wrong class <" +
							cl.getCanonicalName() + ">");
		}
	}

	@Override
	public String writeData(Object obj, String oname) throws ALDDataIOProviderException {
		
		// add your class writer
		
		if (obj instanceof MTBRegion2DSetBag) {
			try {
				((MTBRegion2DSetBag)obj).write(oname);
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion2DSetBag\n" +
								e.getMessage());
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion2DSetBag\n" +
								e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion2DSetBag\n" +
								e.getMessage());
			}
		}
		else if (obj instanceof MTBRegion2DSet) {
			try {
				((MTBRegion2DSet)obj).write(oname);
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion2DSet\n" +
								e.getMessage());
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion2DSet\n" +
								e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion2DSet\n" +
								e.getMessage());
			}
		}
		else if (obj instanceof MTBRegion3DSet) {
			try {
				((MTBRegion3DSet)obj).write(oname);
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion3DSet\n" +
								e.getMessage());
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion3DSet\n" +
								e.getMessage());
			} catch (IOException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBRegion3DSet\n" +
								e.getMessage());
			}
		}
		else if (obj instanceof MTBPolygon2DSet) {
			try {
				((MTBPolygon2DSet)obj).write(oname);
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBPolygon2DSet\n" +
								e.getMessage());
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBPolygon2DSet\n" +
								e.getMessage());
			} catch (ALDException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBPolygon2DSet\n" +
								e.getMessage());
			}
		}
		else if (obj instanceof MTBContour2DSet) {
			try {
				((MTBContour2DSet)obj).write(oname);
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBContour2DSet\n" +
								e.getMessage());
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBContour2DSet\n" +
								e.getMessage());
			} catch (ALDException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBDataIOFile::writeData cannot write MTBContour2DSet\n" +
								e.getMessage());
			}
		}
		
		return null;
	}

	public class MTBDataIOFileButton extends JPanel	implements ActionListener {

		private JButton button;
		private JCheckBox roiManagerChecker;
		private JButton roiManagerAdd;
		private JTextField pathfield;
		private Object outdata;
		private Class<?> cl;
		
		private boolean roisAdded = false;
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -5833553725994371638L;

		public MTBDataIOFileButton(Class<?> cl, Object data) {
			this.outdata = data;
			this.cl = cl;
			
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			
			if (data == null) {

				this.pathfield = new JTextField(15);
				this.add(this.pathfield);
				
				this.button = new JButton("Select file");
				this.add(this.button);
				
				if (   cl.equals(MTBPolygon2DSet.class)
						|| cl.equals(MTBRegion2DSet.class )) {
					this.roiManagerChecker = new JCheckBox("Use Roi Manager Selections");
					this.roiManagerChecker.setActionCommand("roiManagerCheckerToggled");
					this.roiManagerChecker.addActionListener(this);
					this.add(this.roiManagerChecker);
				}
			}
			else {
				this.button = new JButton("Save to file");
				this.button.setForeground(new Color(255,0,0));
				this.add(this.button);
				if (   data.getClass().equals(MTBPolygon2DSet.class)
						|| data.getClass().equals(MTBRegion2DSet.class)) {
					this.roiManagerAdd = new JButton("Add to Roi-Manager");
					this.roiManagerAdd.setForeground(new Color(255,0,0));
					this.roiManagerAdd.setActionCommand("roiManagerAddRegions");
					this.roiManagerAdd.addActionListener(this);
					this.add(this.roiManagerAdd);
				}
			}
			
			this.add(Box.createHorizontalGlue());

			this.button.addActionListener(this);
			
		}

		public Object getData(Class<?> cla) throws ALDDataIOProviderException {
			// check if ROI manager regions should be used, if so ask manager 
			// for set of regions...
			if (this.roiManagerChecker != null && this.roiManagerChecker.isVisible() 
						&& this.roiManagerChecker.isSelected()) {
				
				Object indata = null;
				if (cla.equals(MTBPolygon2DSet.class)) {
					MTBPolygon2DSet pset= 
						RoiManagerAdapter.getInstance().getPolygonSetFromRoiManager(); 
					if (pset.size() == 0)
						System.out.println("No set returned!");	
					else
						indata = pset;
				}
				else if (cla.equals(MTBRegion2DSet.class)) {
					MTBRegion2DSet rset= 
						RoiManagerAdapter.getInstance().getRegionSetFromRoiManager();
					if (rset.size() == 0)
						System.out.println("No set returned!");
					else
						indata = rset; 
				}

				return indata;
			}
			else {
				String filepath = this.pathfield.getText();
				filepath = filepath.trim();
				
				if (filepath == null || filepath.equals("")) {
					return null;
				}
				else {
					File infile = new File(filepath);
					
					if (!infile.exists()) {
						JOptionPane.showMessageDialog(this, "File '" + infile.getName() + "' does not exist.", 
								"Read Error", JOptionPane.ERROR_MESSAGE);
						return null;
					}
					else if (!infile.canRead()) {
						JOptionPane.showMessageDialog(this, "File '" + infile.getName() + "' cannot be read.", 
								"Read Error", JOptionPane.ERROR_MESSAGE);
						return null;
					}
					else {
						return readData(null, cla, infile.getAbsolutePath());
					}
				}
			}
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			
			// save file
			if (evt.getActionCommand().equals("Save to file")) {	

				String idir = ALDEnvironmentConfig.getConfigValue("mitobo", null, "savedir");
				if (idir == null)
					idir = ALDEnvironmentConfig.getConfigValue("mitobo", null, "imagedir");
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

						// TODO: why is "Saving..." not displayed??
						this.button.setText("Saving...");
						this.button.repaint();
						this.repaint();
						this.updateUI();
						
						boolean success = true;
						try {
							writeData(this.outdata, f.getAbsolutePath());
						} catch (Exception e1) {
							success = false;
							JOptionPane.showMessageDialog(this, "Failed to write '" + f.getName() + "': " + e1.getMessage(), 
									"Write Error", JOptionPane.ERROR_MESSAGE);
						} 
						this.button.setText("Save to file");

						if (success) {
							this.button.setToolTipText("Data written to file '"+f.getName()+"'");
							this.button.setForeground(new Color(0,0,0));
							if (this.roiManagerAdd != null)
								this.roiManagerAdd.setForeground(new Color(0,0,0));
						}

						this.repaint();
					}
				}
			}
			else if (evt.getActionCommand().equals("Select file")) {
				String idir = ALDEnvironmentConfig.getConfigValue("mitobo", null, "opendir");
				if (idir == null)
					idir = ALDEnvironmentConfig.getConfigValue("mitobo", null, "imagedir");
				if (idir == null)
					idir = OpenDialog.getLastDirectory();
				if (idir == null)
					idir = OpenDialog.getDefaultDirectory();

				JFileChooser chooser = new JFileChooser();
				if (idir != null)
					chooser.setCurrentDirectory(new File(idir));
				
				if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();

					String pdir = f.getParent();
					// set initial directory of the file chooser for future call
					if (pdir != null) {
						OpenDialog.setLastDirectory(pdir);
						OpenDialog.setDefaultDirectory(pdir);
					}
					
					this.pathfield.setText(f.getAbsolutePath());
				}
			}
			else if(evt.getActionCommand().equals("roiManagerCheckerToggled")) {
				if (this.roiManagerChecker.isSelected()) {
					this.button.setEnabled(false);
				}
				else {
					this.button.setEnabled(true);
				}
			}
			else if (evt.getActionCommand().equals("roiManagerAddRegions")) {
				if (this.roisAdded) {
					Object[] options = { "OK"};
					JOptionPane.showOptionDialog(null, 
						"Regions were already added to ROI manager!",
						"Warning", JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null, options, options);
					return;
				}
				MTBPolygon2DSet polygons = null;
				if (this.outdata instanceof MTBPolygon2DSet) {
					polygons = (MTBPolygon2DSet)this.outdata;
				}
				else if (this.outdata instanceof MTBRegion2DSet) {
					MTBRegion2DSet regions = (MTBRegion2DSet)this.outdata;
					polygons = new MTBPolygon2DSet(0, 0, 0, 0);
					for (int i=0; i<regions.size(); ++i) {
						MTBRegion2D reg = regions.elementAt(i);
						try {
							MTBContour2D cont = reg.getContour();
							MTBPolygon2D poly = new MTBPolygon2D(cont.getPoints(),true);
							polygons.add(poly);
						} catch (ALDOperatorException e) {
							System.err.println("MTBDataIOFile - skipping polygon!");
						} catch (ALDProcessingDAGException e) {
							System.err.println("MTBDataIOFile - skipping polygon!");
						}
					}
				}
				if (polygons == null)
					return;
				if (polygons.size() > 2000) {
					Object[] options = { "OK", "CANCEL" };
					int ret = JOptionPane.showOptionDialog(null, 
							"Detected more than 2000 objects...\n" +
							"Really add all objects to ROI manager?",
							"Warning", JOptionPane.DEFAULT_OPTION,
							JOptionPane.WARNING_MESSAGE,
							null, options, options);
					if (ret == JOptionPane.CANCEL_OPTION)
						return;
				}
				RoiManagerAdapter.getInstance().addPolygonsToRoiManager(polygons);
				this.roisAdded = true;
				this.roiManagerAdd.setForeground(new Color(0,0,0));
				this.button.setForeground(new Color(0,0,0));
			}
		}
	}
}
