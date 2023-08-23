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
 * This class uses the Bio-Formats package/library (see license at the top)
 */

package de.unihalle.informatik.MiToBo.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import de.unihalle.informatik.MiToBo.io.tools.ImageIOUtils;

import loci.formats.FormatException;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.gui.ComboFileFilter;
import loci.formats.gui.ExtensionFileFilter;
import loci.formats.out.AVIWriter;
import loci.formats.out.QTWriter;

/**
 * A panel for image writer options that is used as accessory in a JFileChooser
 * @author Oliver Gress
 *
 */
public class ImageWriterOptionsPane extends JPanel implements PropertyChangeListener, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7502452479625180633L;
	
	// option panels for compression, codec and quality
	protected RadioButtonPanel comppanel, codpanel, qualpanel;
	
	// panel for video options (contains only framerate)
	protected TwoColumnPanel vidpanel;
	
	// textfield for framerate
	protected JTextField fpsfield;
	
	// the filechooser dialog object
	protected JFileChooser jfc;
	
	// option variables
	protected String compression;
	protected int fps, defaultfps;
	protected int quality;
	protected int codec;
	
	/** Property change when writer options are approved (i.e. when filechooser selection is approved) */
	public static final String WRITER_OPTIONS_APPROVED_PROPERTY = "WriterOptionsApproved";
	
	/**
	 * Constructor that will install this writer options panel to the specified <code>JFileChooser</code>
	 * @param jfc file chooser to install this panel to
	 */
	public ImageWriterOptionsPane(JFileChooser jfc) {
		
		this.compression = null;
		this.fps = -1;
		this.defaultfps = -1;
		this.quality = -1;
		this.codec = -1;
		
		this.jfc = jfc;
		this.jfc.setAcceptAllFileFilterUsed(false);
		this.jfc.addPropertyChangeListener(this);
		this.jfc.addActionListener(this);
		
	
		// ----- create basic structure
		// writer options headline
		Box header = Box.createHorizontalBox();
		
		JLabel headlabel = new JLabel("Image File Options:");
		headlabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
	
		header.add(headlabel);
		header.add(Box.createHorizontalGlue());
		
		// panel for compression options
		this.comppanel = new RadioButtonPanel("Compression");
		this.comppanel.addPropertyChangeListener(this);
		
		// panel for video options
		this.vidpanel = new TwoColumnPanel("Video");

		// panel for quality options
		this.qualpanel = new RadioButtonPanel("Quality");
		this.qualpanel.addPropertyChangeListener(this);
		
		// panel for codec options
		this.codpanel = new RadioButtonPanel("Codec");
		this.codpanel.addPropertyChangeListener(this);
		
		// set layout and add components to this panel
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(header);
		this.add(this.comppanel);
		this.add(this.vidpanel);
		this.add(this.qualpanel);
		this.add(this.codpanel);
		this.add(Box.createVerticalGlue());

		this.setVisible(false);
		
		
		// install image writer options panel as accessory of the file chooser
		
		// get current accessory of the file chooser (contains e.g. Bio-Formats PreviewPane)
		JComponent jc = this.jfc.getAccessory();

		// Components to hold former accessory and the ImageWriterOptionsPane
	//	JPanel jpIWO = new JPanel();
	//	jpIWO.setLayout(new BoxLayout(jpIWO, BoxLayout.X_AXIS));
		
		JScrollPane jsp = new JScrollPane(this);
		jsp.setBorder(BorderFactory.createEmptyBorder());
//		jpIWO.add(jsp);
//		jpIWO.add(Box.createHorizontalGlue());
		
		// Panel for PreviewPane and ImageWriterOptionsPane
		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		if (jc != null)
			jp.add(jc);
		jp.add(jsp);
		
		// set new accessory
		this.jfc.setAccessory(jp);
	}
	
	/**
	 * Set a default fps to show in the option panel, e.g. if the fps can be determined from some image metadata
	 */
	public void setDefaultFps(int fps) {
		
	}
	
	/**
	 * Get compression option
	 * @return compression string or null
	 */
	public String getCompression() {
		return this.compression;
	}
	
	/**
	 * Get fps option
	 * @return fps or -1 if not assigned
	 */
	public int getFramesPerSecond() {
		return this.fps;
	}
	
	/**
	 * Get quality option
	 * @return quality ID or -1 if not assigned
	 */
	public int getQuality() {
		return this.quality;
	}

	/**
	 * Get codec option
	 * @return codec ID or -1 if not assigned
	 */
	public int getCodec() {
		return this.codec;
	}
	
	/**
	 * Helper function to notify pane of manual changes in JFileChooser text field.
	 * 
	 * @param evt	Event providing the new file name.
	 */
	public void myPropertyChange(PropertyChangeEvent evt) {
		this.propertyChange(evt);
	}
	
	/**
	 * Changes the available options of the writer option panel according to the selected 
	 * file filter of the file chooser
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		
		if (   prop.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)
				|| prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			
			FileFilter ff = this.jfc.getFileFilter();
	
			// do not change image writer options if the file filter has not changed and ...
			if (prop.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
				
				// ... the file filter is an extension file filter (image writer options do not change because
				// the writer doesn't change
				if (ff instanceof ExtensionFileFilter)
					return;
				
				// ... if the file filter permits multiple formats, but the format has not changed from the
				// the last to the current selected file
				if (ff instanceof ComboFileFilter) {
					
					File oldfile = null;
					File newfile = null;
					try {
						//if (evt.getOldValue() != null)
						oldfile = (File)evt.getOldValue();
						//if (evt.getNewValue() != null)
						newfile = (File)evt.getNewValue();
					} catch (ClassCastException e) {
						// do nothing
					}
					
					if (oldfile != null && newfile != null) {
					
						// test if old and new selected file are of same format (by extension)
						ComboFileFilter cff = (ComboFileFilter)ff;	
						FileFilter[] ffs = cff.getFilters();
						for (int i = 0; i < ffs.length; i++) {
							if (ffs[i].accept(oldfile) && ffs[i].accept(newfile))
								return;
						}
					}
				}
				
				// otherwise go on
			}
			
			// filename for writer instantiation (no file is written, only to determine the required writer)
			String fname = "";

			if (ff instanceof ExtensionFileFilter) {
				// by specific format
				
				ExtensionFileFilter eff = (ExtensionFileFilter)ff;
				fname = "tmp." + eff.getExtension();
			}
			else {
				// by extension
				if (evt.getOldValue() == "_dummy_") {
					fname = (String)evt.getNewValue();
				}
				else {
					File file = this.jfc.getSelectedFile();
					if (file != null)
						fname = this.jfc.getSelectedFile().getName();
				}
			}

			this.setVisible(false);
			this.comppanel.setVisible(false);
			this.vidpanel.setVisible(false);
			this.qualpanel.setVisible(false);
			this.codpanel.setVisible(false);
			
			// try to create a writer based on the filename (by extension)
			IFormatWriter w = null;
			try {
				w = new ImageWriter().getWriter(fname);
			} catch (FormatException e) {
				// no writer could be instantiated for the given filename (by extension)
				// -> no options
				return;
			}				


			if (!(w instanceof QTWriter
					|| w instanceof AVIWriter)) {
				// --- image options
				// (video compression is determined by quality/codec if available)
			
				// compression options
				String[] comps = w.getCompressionTypes();
				
				if (comps != null) {
					this.comppanel.clearPanel();
					this.comppanel.fillPanel(comps, "LZW");
					
					this.comppanel.setVisible(true);
					this.setVisible(true);
				}
			}
			else {
				// --- video options

				// video options
				HashMap<String, Component> vidopts = new HashMap<String, Component>();
	
				this.fpsfield = new JTextField();
				this.fpsfield.setAlignmentX(JTextField.RIGHT_ALIGNMENT);
				if (this.defaultfps != -1)
					this.fpsfield.setText(""+this.defaultfps);
				else
					this.fpsfield.setText(""+w.getFramesPerSecond());
				Dimension fpsdim = new Dimension(40, 20);
				this.fpsfield.setSize(fpsdim);
				this.fpsfield.setMaximumSize(fpsdim);
				this.fpsfield.setMinimumSize(fpsdim);	
				
				vidopts.put("Frames per second", this.fpsfield);
				
				this.vidpanel.clearPanel();
				this.vidpanel.fillPanel(vidopts);
				
				this.vidpanel.setVisible(true);
				this.setVisible(true);
				
				// quality and codec options of quicktime writers
				if (w instanceof QTWriter) {
					
					this.comppanel.setVisible(false);
					
					HashMap<Integer, String> qualities = ImageIOUtils.availableQualities(QTWriter.class);
					
					String[] quals = new String[qualities.size()];
					quals = qualities.values().toArray(quals);
					
					this.qualpanel.clearPanel();
					this.qualpanel.fillPanel(quals, "HIGH");
					
					this.qualpanel.setVisible(true);
					
					
					HashMap<Integer, String> codecs = ImageIOUtils.availableCodecs(QTWriter.class);
					
					String[] cods = new String[codecs.size()];
					cods = codecs.values().toArray(cods);
					
					this.codpanel.clearPanel();
					this.codpanel.fillPanel(cods, "MPEG_4");
					
					this.codpanel.setVisible(true);
					
//					Dimension d = new Dimension(this.qualpanel.getPreferredSize().width, this.vidpanel.getPreferredSize().height);
//					this.vidpanel.setMinimumSize(d);
//					this.vidpanel.setMaximumSize(d);
//						System.out.println(d.width + " " + d.height + " " + this.qualpanel.getPreferredSize().width);
//						this.vidpanel.repaint();
				}
			}
			
			this.repaint();
			this.setSize(this.getPreferredSize());
		}
	}

	/**
	 * Assigns the writer options to the option variables when a file selection is approved by the
	 * file chooser.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		// set writer options if filechooser is closed with "Save"
		if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
			
			if (this.comppanel.isVisible())
				this.compression = this.comppanel.getSelection();
			else
				this.compression = null;
			
			if (this.vidpanel.isVisible())
				this.fps = Integer.parseInt(this.fpsfield.getText().trim());
			else
				this.fps = -1;
			
			if (this.qualpanel.isVisible()) {
				Integer qual = ImageIOUtils.getKey(ImageIOUtils.availableQualities(QTWriter.class), 
														this.qualpanel.getSelection());
				this.quality = (qual == null) ? -1 : qual.intValue();
			}
			else
				this.quality = -1;
			
			
			if (this.codpanel.isVisible()) {
				Integer cod = ImageIOUtils.getKey(ImageIOUtils.availableCodecs(QTWriter.class), 
						this.codpanel.getSelection());
				this.codec = (cod == null) ? -1 : cod.intValue();
			}
			else
				this.codec = -1;
			
			this.firePropertyChange(ImageWriterOptionsPane.WRITER_OPTIONS_APPROVED_PROPERTY, false, true);
		}
		
	}
	
}
