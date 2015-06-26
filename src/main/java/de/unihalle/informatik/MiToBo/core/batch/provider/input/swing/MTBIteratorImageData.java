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
LOCI Plugins for ImageJ: a collection of ImageJ plugins including the
Bio-Formats Importer, Bio-Formats Exporter, Bio-Formats Macro Extensions,
Data Browser and Stack Slicer. Copyright (C) 2005-@year@ Melissa Linkert,
Curtis Rueden and Christopher Peterson.

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
 * as well as source code from the LOCI-plugins package (see third license from the top)
 */

package de.unihalle.informatik.MiToBo.core.batch.provider.input.swing;

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDBatchIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDBatchIOProviderException.ALDBatchIOProviderExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.Alida.annotations.ALDBatchInputProvider;
import de.unihalle.informatik.Alida.batch.provider.input.swing.ALDBatchInputIteratorSwing;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Implementation of {@link ALDBatchInputIteratorSwing} for MitoBo images.
 * 
 * @author moeller
 */
@ALDBatchInputProvider(priority=1)
public class MTBIteratorImageData
	implements ALDBatchInputIteratorSwing {
	
	/**
	 * Interface method to announce class for which IO is provided for
	 * field is ignored.
	 * 
	 * @return	Collection of classes provided
	 */
	@Override
  public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add( MTBImage.class);
		classes.add( MTBImageByte.class);
		classes.add( MTBImageDouble.class);
		classes.add( MTBImageFloat.class);
		classes.add( MTBImageInt.class);
		classes.add( MTBImageRGB.class);
		classes.add( MTBImageShort.class);
		return classes;
	}
	
	@Override
	public ALDSwingComponent createGUIElement(Field field, Class<?> cl, 
																	Object obj, ALDParameterDescriptor descr) 
																	throws ALDBatchIOProviderException {
		try {
	    return new MTBImageIteratorImageDataPanel(field, cl, obj, descr);
    } catch (ALDDataIOException e) {
    	throw new ALDBatchIOProviderException(
    			ALDBatchIOProviderExceptionType.OBJECT_INSTANTIATION_ERROR,
    			"[MTBIteratorImageData] Cannot instantiate GUI element... Error!");
    }
	}

	@Override
	public void setValue(Field field, Class<?> cl, ALDSwingComponent guiElement,
			Object value) throws ALDBatchIOProviderException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public Iterator<Object> readData(Field field, Class<?> cl, 
				ALDSwingComponent guiElement) {
		try {
			System.out.println("Image batch , type = " + guiElement.getClass());
			MTBImageIteratorImageDataPanel panel = 
					(MTBImageIteratorImageDataPanel)guiElement;
			ALDDirectoryString dirName = panel.getDirName();
			boolean recursiveProcessing = panel.recursiveProcessing();
			return new MTBBatchInputImageDataIterator(
					dirName.getDirectoryName(),	recursiveProcessing);
		} catch (ALDDataIOException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * GUI element class for image batch iterator.
	 * @author moeller
	 */
	protected class MTBImageIteratorImageDataPanel extends ALDSwingComponent {
		
		/**
		 * Provider to read input directory.
		 */
		private ALDSwingComponent dirProvider;

		/**
		 * Flag to enable/disable recursive directory processing.
		 */
		private JCheckBox recursiveBox;
		
		/**
		 * Main panel.
		 */
		private JPanel mainPanel;
		
		/**
		 * Field associated with parameter.
		 */
		private Field myField;
		
		/**
		 * Class associated with parameter.
		 */
		@SuppressWarnings("unused")
    private Class<?> myClass;
		
		/**
		 * (Default) object value associated with parameter.
		 */
		@SuppressWarnings("unused")
    private Object myObject;
		
		/**
		 * Descriptor associated with parameter.
		 */
		@SuppressWarnings("unused")
    private ALDParameterDescriptor myDescriptor;
		
		/**
		 * Default constructor.
		 * @param field		Field associated with parameter.
		 * @param cl			Class associated with parameter.
		 * @param obj			(Default) object value associated with parameter.
		 * @param descr		Descriptor associated with parameter.
		 * @throws ALDDataIOException
		 */
		public MTBImageIteratorImageDataPanel(Field field, Class<?> cl,
				Object obj, ALDParameterDescriptor descr) throws ALDDataIOException {
			this.myField = field;
			this.myClass = cl;
			this.myObject = obj;
			this.myDescriptor = descr;
			this.dirProvider = 	ALDDataIOManagerSwing.getInstance().createGUIElement(
					field, ALDDirectoryString.class, null, descr);
			this.recursiveBox = new JCheckBox(" Include sub-directories ");
			this.mainPanel = new JPanel();
			this.mainPanel.setLayout(new BorderLayout());
			this.mainPanel.add(this.dirProvider.getJComponent(), BorderLayout.NORTH);
			this.mainPanel.add(this.recursiveBox, BorderLayout.SOUTH);
		}
		
		/**
		 * Get directory for processing.
		 * @return	Name of directory to be processed.
		 * @throws ALDDataIOException
		 */
		public ALDDirectoryString getDirName() throws ALDDataIOException {
			return (ALDDirectoryString)ALDDataIOManagerSwing.getInstance().readData(
					this.myField, ALDDirectoryString.class, this.dirProvider);
		}
		
		/**
		 * Query flag for recursive processing.
		 * @return	True, if recursive processing is enabled, otherwise false.
		 */
		public boolean recursiveProcessing() {
			return this.recursiveBox.isSelected();
		}
		
		@Override
    public JComponent getJComponent() {
			return this.mainPanel;
    }

		@Override
    public void disableComponent() {
			this.dirProvider.disableComponent();
			this.recursiveBox.setEnabled(false);
    }

		@Override
    public void enableComponent() {
			this.dirProvider.enableComponent();
			this.recursiveBox.setEnabled(true);
    }

		@Override
    public void dispose() {
			this.dirProvider.dispose();
    }
	}
	
	/**
	 * Polygon set iterator class.
	 * 
	 * @author moeller
	 */
	class MTBBatchInputImageDataIterator implements Iterator<Object> {

		/**
		 * Index of iterator in set.
		 */
		private int currentIndex = -1;

		private Vector<MTBImage> images;
		
		/**
		 * Default constructor.
		 * @param directory		Directory to process.
		 * @param rec					Flag for recursive processing of sub-directories.
		 */
		public MTBBatchInputImageDataIterator(String directory, boolean rec) {

			this.images = new Vector<MTBImage>();
			this.currentIndex = -1;
			
			// read the directory
			DirectoryTree dirTree = new DirectoryTree(directory, rec);
			Vector<String> files = dirTree.getFileList();
			
			// process all files you can open like images
			ImageReaderMTB ireader;
			try {
				ireader = new ImageReaderMTB();
				MTBImage nextImage = null;
				for (String s: files) {
					nextImage = null;
					try {
						ireader.setFileName(s);
						ireader.runOp();
						nextImage = ireader.getResultMTBImage();
						if (nextImage != null) {
							this.images.add(nextImage);
						}
					} catch (Exception e) {
						continue;
					}
				}
			} catch (ALDOperatorException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public boolean hasNext() {
			return ( this.currentIndex < this.images.size() - 1);
		}

		@Override
		public MTBImage next() {
			this.currentIndex++;
			return this.images.elementAt(this.currentIndex);
		}

		@Override
		public void remove() {
			this.images.remove(this.currentIndex);
		}	
	}
}
