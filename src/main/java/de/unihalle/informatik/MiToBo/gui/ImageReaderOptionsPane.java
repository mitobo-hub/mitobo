/*
OME Bio-Formats package for reading and converting biological file formats.
Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
LOCI Common package: utilities for I/O, reflection and miscellaneous tasks.
Copyright (C) 2005-@year@ Melissa Linkert, Curtis Rueden and Chris Allan.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

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

/*
 * This class uses the Bio-Formats and LOCI-commons packages/libraries (see the two licenses at the top)
 */

package de.unihalle.informatik.MiToBo.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLService;

/**
 * A panel for image reader options that is used as accessory in a JFileChooser
 * @author Oliver Gress
 *
 */
public class ImageReaderOptionsPane extends JPanel implements ActionListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8158746416069757520L;

	// the filechooser dialog object
	protected JFileChooser jfc;
	
	protected volatile ConcurrentHashMap<String, CheckBoxPanel> seriespanels;
	protected volatile ConcurrentHashMap<String, String[]> seriesnames;
	protected HashMap<String, int[]> seriesIndices;
	
	protected volatile Box spbox;
	
	protected volatile JLabel statuslabel1;
	protected volatile JLabel statuslabel2;
	
	/** Property change when writer options are approved (i.e. when filechooser selection is approved) */
	public static final String READER_OPTIONS_APPROVED_PROPERTY = "ReaderOptionsApproved";
	
	/**
	 * Constructor that will install this reader options panel to the specified <code>JFileChooser</code>
	 * @param jfc file chooser to install this panel to
	 */
	public ImageReaderOptionsPane(JFileChooser jfc) {

		this.jfc = jfc;
		this.jfc.addPropertyChangeListener(this);
		this.jfc.addActionListener(this);
		
		this.seriespanels = new ConcurrentHashMap<String, CheckBoxPanel>();
		this.seriesnames = new ConcurrentHashMap<String, String[]>();
		this.seriesIndices = null;
		
		// ----- create basic structure
		// status label
		this.statuslabel1 = new JLabel(" ");
		this.statuslabel1.setMinimumSize(new Dimension(150, 15));
		this.statuslabel1.setPreferredSize(new Dimension(150, 15));
		this.statuslabel1.setFont(this.statuslabel1.getFont().deriveFont(this.statuslabel1.getFont().getSize2D() - 2));
		this.statuslabel1.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		this.statuslabel2 = new JLabel(" ");
		this.statuslabel2.setMinimumSize(new Dimension(150, 15));
		this.statuslabel2.setPreferredSize(new Dimension(150, 15));
		this.statuslabel2.setFont(this.statuslabel2.getFont().deriveFont(this.statuslabel2.getFont().getSize2D() - 2));
		this.statuslabel2.setAlignmentX(JLabel.CENTER_ALIGNMENT);

		
		this.spbox = Box.createVerticalBox();
		
		// set layout and add components to this panel
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(this.spbox);
		
		this.setVisible(true);
	
		
		// ----- install image reader options panel as accessory of the file chooser
		
		// get current accessory of the file chooser (contains e.g. Bio-Formats PreviewPane)
		JComponent jc = this.jfc.getAccessory();

		// Components to hold former accessory and the ImageWriterOptionsPane
		
		JScrollPane jsp = new JScrollPane(this);
		jsp.setBorder(BorderFactory.createEmptyBorder());

		// Panel for PreviewPane and ImageWriterOptionsPane
		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		if (jc != null) {
			jp.add(jc);
			jp.add(Box.createRigidArea(new Dimension(0,5)));
			JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
			sep.setMaximumSize(new Dimension(120,5));
			jp.add(sep);
		}
		jp.add(Box.createRigidArea(new Dimension(0,5)));

		jp.add(jsp);
		jp.add(Box.createRigidArea(new Dimension(0,5)));			
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setMaximumSize(new Dimension(120,5));
		jp.add(sep);
		jp.add(Box.createRigidArea(new Dimension(0,5)));
		jp.add(this.statuslabel1);
		jp.add(this.statuslabel2);

		this.setToolTipText("Open selected images from multi-image files");
		
		// set new accessory
		this.jfc.setAccessory(jp);
	}
	
	/**
	 * Returns a hash map with the absolute filename as key and the indices of 
	 * images selected from the corresponding file.
	 *  
	 * @return List of file names with corresponding selected images.	
	 */
	public HashMap<String, int[]> getSelections() {
		return this.seriesIndices;
	}
	
	
	/**
	 * Changes the available options of the reader option panel according to the selected 
	 * files
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();

		if (prop.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
			
			File[] files = jfc.getSelectedFiles();

			new Thread(new FileOptionsRefresher(this, files)).start();
		}
	}

	/**
	 * Assigns the reader options to the option variables when a file selection is approved by the
	 * file chooser.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		// set writer options if filechooser is closed with "OK"
		if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
			
			this.seriesIndices = new HashMap<String, int[]>(this.seriespanels.size());
			
			String[] keys = new String[this.seriespanels.keySet().size()];
			keys = this.seriespanels.keySet().toArray(keys);
			
			for (String key : keys) {
				CheckBoxPanel cbp = this.seriespanels.get(key);
				
				if (cbp != null && cbp.isVisible()) {
					
					boolean[] checks = cbp.getSelections();
					int tcount = 0;
					for (int i = 0; i < checks.length; i++) {
						if (checks[i] == true) {
							tcount++;
						}
					}
					
					int[] ids = new int[tcount];
					tcount = 0;
					for (int i = 0; i < checks.length; i++) {
						if (checks[i] == true) {
							ids[tcount++] = i;
						}
					}
					
					this.seriesIndices.put(key, ids);
				}
			}
			
			this.firePropertyChange(ImageReaderOptionsPane.READER_OPTIONS_APPROVED_PROPERTY, false, true);
		}
		
	}
	
	private class FileOptionsRefresher implements Runnable {

		private ImageReaderOptionsPane opane;
		private File[] files;
		
		public FileOptionsRefresher(ImageReaderOptionsPane opane,
									File[] files) {
			
			this.opane = opane;
			this.files = files;
		}						
		
		@Override
		public void run() {
			
			statuslabel1.setText("Retrieving");
			statuslabel2.setText("file info...");
			invokeLaterRepaint(this.opane);
			
			HashSet<String> filepaths = new HashSet<String>(this.files.length);
			for (File f : this.files) {
				filepaths.add(f.getAbsolutePath());
			}
			
			// clean up panels of deselected files
			Set<String> keys = seriespanels.keySet();
			keys.removeAll(filepaths);
			
			for (String key : keys) {
				seriespanels.remove(key);
				seriesnames.remove(key);
			}

			Component[] comps = spbox.getComponents();
			for (Component comp : comps) {
				if (!seriespanels.contains(comp)) {
					spbox.remove(comp);
				}
			}
			
			for (int j = 0; j < this.files.length; j++) {
				
				if (!this.files[j].isDirectory() && !seriespanels.containsKey(this.files[j].getAbsolutePath())) {

					IFormatReader r = null;
					
					try {
						r = new ImageReader().getReader(this.files[j].getAbsolutePath());
					} catch (FormatException e) {
						statuslabel1.setText("Format not");
						statuslabel2.setText("supported!");
						invokeLaterRepaint(this.opane);
					} catch (IOException e) {
						statuslabel1.setText("Failed to");
						statuslabel2.setText("read file!");
						invokeLaterRepaint(this.opane);
					}
					
					if (r != null) {
						
						try {
							ServiceFactory factory = new ServiceFactory();
							OMEXMLService service = factory.getInstance(OMEXMLService.class);
							OMEXMLMetadata omemeta = service.createOMEXMLMetadata();
		
							r.setMetadataStore(omemeta);
							
							r.setId(this.files[j].getAbsolutePath());
		
						
							int nImages = omemeta.getImageCount();
							
							if (nImages > 1) {
								String[] inames = new String[nImages];
								
								for (int i = 0; i < nImages; i++) {
									if (omemeta.getImageName(i) != null) {
										inames[i] = omemeta.getImageName(i);
									}
									else if (omemeta.getImageID(i) != null) {
										inames[i] = omemeta.getImageID(i);
									}
									else {
										inames[i] = "IMAGE "+i;
									}
								}
								
								CheckBoxPanel cbp = new CheckBoxPanel(this.files[j].getName());
								cbp.fillPanel(inames, null);
								
								spbox.add(cbp);
								seriespanels.put(this.files[j].getAbsolutePath(), cbp);
								seriesnames.put(this.files[j].getAbsolutePath(), inames);
								cbp.setVisible(true);
							}
		
						} catch (DependencyException e) {
							// meta data cannot be read, no information is fetched, do nothing
						} catch (ServiceException e) {
							// meta data cannot be read, no information is fetched, do nothing
						} catch (FormatException e) {
							// meta data cannot be read, no information is fetched, do nothing
						} catch (IOException e) {
							// meta data cannot be read, no information is fetched, do nothing
						}	
					}
				}
			}

			statuslabel1.setText(" ");
			statuslabel2.setText(" ");
			invokeLaterRepaint(this.opane);

		}
		
	}
	
	private void invokeLaterRepaint(final ImageReaderOptionsPane opane) {

		SwingUtilities.invokeLater(
			new Runnable() {
			
				@Override
				public void run() {
					opane.repaint();
				}
			}
		);
	}

	
}
