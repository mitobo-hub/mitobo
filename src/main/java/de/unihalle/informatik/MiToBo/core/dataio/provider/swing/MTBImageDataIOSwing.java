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
 * This class uses the Bio-Formats and LOCI-commons packages/libraries 
 * (see the two licenses at the top) as well as source code from the 
 * LOCI-plugins package (see third license from the top)
 */

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing;

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing.ProviderInteractionLevel;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.events.ALDSwingValueChangeEvent;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.MiToBo.core.dataconverter.MTBImageConverter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Implementation of {@link ALDDataIOSwing} interface for MiToBo images.
 * 
 * @author moeller
 */
@ALDDataIOProvider(priority=10)
public class MTBImageDataIOSwing implements ALDDataIOSwing {
	
	/** 
	 * Index to number untitled images popped-up.
	 */
	private static int idxUntitled = 0;

	/**
	 * Class of image to be returned.
	 */
	protected Class<?> desiredImageClass = null;
	
	/**
	 * Default constructor.
	 */
	public MTBImageDataIOSwing() {
		// nothing to do here...
	}

	/**
	 * Interface method to announce class for which IO is provided for
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
		// ImageJ 1.0 ImagePlus 
		classes.add( ImagePlus.class);
		return classes;
	}
	
	@Override
  public Object getInitialGUIValue(Field field, Class<?> cl, Object obj, 
  		ALDParameterDescriptor descr) {
		if (obj != null)
			return obj;
		if (descr.isRequired()) {
			MTBImage defaultImg = null;
			int[] winIDs = ij.WindowManager.getIDList();
			if (winIDs != null && winIDs.length > 0) {
				defaultImg = MTBImage.createMTBImage(IJ.getImage());
			
				// check if image object is of correct class, if not, try to convert
				if (   !defaultImg.getClass().equals(cl)
						&& !cl.isAssignableFrom(defaultImg.getClass())) {
					try {
						MTBImageConverter converter = new MTBImageConverter();
						return converter.convert(defaultImg, null, cl, null);
					} catch (ALDException e) {
						// if something goes wrong, just return null
						return null;
					}
				}
			
			}
			return defaultImg;
		}
		return obj;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing#createGUIElement(java.lang.reflect.Field, java.lang.Class, java.lang.Object, de.unihalle.informatik.Alida.operator.ALDParameterDescriptor)
	 */
	@Override
	public ALDSwingComponent createGUIElement(
			Field field,Class<?> cl, Object obj, 
			ALDParameterDescriptor descr) {
		this.desiredImageClass = cl;
		String name = null;
		if (obj != null) {
			if (obj instanceof MTBImage) {
				MTBImage img = (MTBImage)obj;
				name = img.getTitle();
			}
			else if (obj instanceof ImagePlus) {
				ImagePlus imgp = (ImagePlus)obj;
				name = imgp.getTitle();
			}
		}
		else {
			// if parameter is required and there is a single image open
			// we use the image as default
			if (descr != null && descr.isRequired()) {
				int [] winIDs = ij.WindowManager.getIDList();
				if (winIDs != null && winIDs.length > 0) {
					name = IJ.getImage().getTitle();
				}
			}
		}
		return new JComboBoxImage(name, descr);
	}

	@Override
  public void setValue(
  		Field f, Class<?> cl, ALDSwingComponent guiElem, Object value){
		if (!(guiElem instanceof JComboBoxImage))
			return;
		JComboBoxImage box  = ((JComboBoxImage)guiElem);
		box.setValue(value);
  }

	@SuppressWarnings("unused")
  @Override
	public Object readData(Field field, Class<?> cl, 
			ALDSwingComponent guiElem) 
		throws ALDDataIOProviderException {
		if (!(guiElem instanceof JComboBoxImage))
			return null;
		JComboBoxImage box  = ((JComboBoxImage)guiElem);
		return box.readData(field, cl);
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) {
		// make sure that image object has a proper title
		String title = "";
		if (obj instanceof MTBImage) {
			MTBImage img = ((MTBImage)obj);
			if (img.getTitle() == null || img.getTitle().isEmpty()) {
				// new title string
				title = new String("Result Image: Untitled-" + idxUntitled);
				idxUntitled++;
				img.getImagePlus().setTitle(title);
				img.setTitle(title);
			}
			else {
				// copy title of Mitobo image
				title = img.getTitle();
				img.getImagePlus().setTitle(title);
			}
		}
		// check if it is ok to open the image directly;
		// if not, just return a button to open it on request only
		if (   !(ALDDataIOManagerSwing.getInstance().
							getProviderInteractionLevel()
						  	== ProviderInteractionLevel.ALL_FORBIDDEN)
				&& !(ALDDataIOManagerSwing.getInstance().
							getProviderInteractionLevel()
							  == ProviderInteractionLevel.WARNINGS_ONLY)) {
			this.popUpImage(obj);
		}
		// create panel with image title and button
		ImageShowPanel showPanel= 
				new ImageShowPanel(new ImageShowButton(obj),title);
		return showPanel;
	}
	
	/**
	 * Shows an image to the user.
	 * @param obj	Image object to display.
	 */
	protected void popUpImage(Object obj) {
		if (obj instanceof MTBImage) {
			MTBImage img = ((MTBImage)obj);
			// make sure that ImageJ does not delete the data on closing 
			// the image!
			img.getImagePlus().setIgnoreFlush(true);
			if (   ALDDataIOManagerSwing.getInstance().
							getProviderInteractionLevel()
					== ProviderInteractionLevel.ALL_ALLOWED)
				img.show();
		}
		else if (obj instanceof ImagePlus) {
			ImagePlus imgp = (ImagePlus)obj;
			// make sure that ImageJ does not delete the data on closing 
			// the image!
			imgp.setIgnoreFlush(true);
			if (   ALDDataIOManagerSwing.getInstance().
							getProviderInteractionLevel()
					== ProviderInteractionLevel.ALL_ALLOWED)
				imgp.show();
		}
	}
	
	/**
	 * Class for handling images opened in GUI.
	 * 
	 * @author moeller
	 */
	private class JComboBoxImage extends ALDSwingComponent
		implements ImageListener, ItemListener {

		/**
		 * Combobox to select images.
		 */
		private JComboBox selectBox;
		
		/**
		 * Recently selected image.
		 */
		private String selectedImage = "none selected";
		
		/**
		 * (Operator) parameter descriptor of associated parameter.
		 */
		private ALDParameterDescriptor paramDescr;
		
		/**
		 * Default constructor.
		 * 
		 * @param objName		Optional name of default object, may be null.
		 * @param d					(Operator) parameter descriptor.
		 */
		public JComboBoxImage(String objName, ALDParameterDescriptor d) {
			this.selectBox = new JComboBox();
			this.selectBox.addItemListener(this);
			this.paramDescr = d;
			// register in ImageJ as listener
			ImagePlus.addImageListener(this);
			this.updateImageList(objName);
		}

		@Override
    public JComponent getJComponent() {
			return this.selectBox;
		}
		
	  /**
	   * Selects the given image object in combobox.
	   * @param value 	Image object to be selected.
	   */
	  public void setValue(Object value){
	  	if (value == null) {
	  		updateImageList(null);
	  	}
			MTBImageDataIOSwing.this.popUpImage(value);
			if (value instanceof MTBImage) {
				updateImageList(((MTBImage)value).getTitle());
			}
			else if (value instanceof ImagePlus) {
				updateImageList(((ImagePlus)value).getTitle());
			}
	  }

		/**
		 * Function to get selected image from GUI.
		 * 
		 * @param field	Field of requested object.
		 * @param cl		Class of selected object.
		 * @return	Image object.
		 */
		public Object readData(@SuppressWarnings("unused") Field field, 
														Class<?> cl) {
			ImagePlus selectedImg = null;
			
			// get currently selected item
			String imgName = (String)(this.selectBox.getSelectedItem());

			// find image with selected title in GUI
			int[] ids = ij.WindowManager.getIDList();
			if (ids == null) {
				return null;
			}
			for ( int id : ids ) {
				ImagePlus img = ij.WindowManager.getImage( id);
				if ( imgName.equals( img.getTitle()) ) {
					selectedImg = img;
				}
			}
			// safety check, if something went wrong, return nothing
			if ( selectedImg == null ) {
				return null;
			}
			
			// remember the selected image name
			this.selectedImage = imgName;
			
			// return result
			if (cl.equals(ImagePlus.class))
				return selectedImg;
//			return MTBImage.createMTBImage( selectedImg);
			MTBImage image = MTBImage.createMTBImage( selectedImg);
			// check image type, if necessary cast the type
			if (   !image.getClass().equals(
							MTBImageDataIOSwing.this.desiredImageClass)
					&& !MTBImageDataIOSwing.this.desiredImageClass.
							isAssignableFrom(image.getClass())) {
				try {
					MTBImageConverter converter = new MTBImageConverter();
	        return converter.convert(image, null, 
	        		MTBImageDataIOSwing.this.desiredImageClass, null);
        } catch (ALDException e) {
        	return null;
        }
			}
			return image;
		}

		/**
		 * Updates image list and GUI.
		 * @param preselect		Optional name of object to be pre-selected.
		 * @return True, if selected image was really changed.
		 */
		private boolean updateImageList(String preselect) {
			
			// remember the name of the currently active image
			String activeImage = this.selectedImage;
			
			Vector<String> allImgNames = new Vector<String>();
			allImgNames.add("none selected");
			int idx = 0;
			
			// remember which image to be selected
			this.selectedImage = "none selected";
			
			// get list of current images
			boolean preselectedFound = false;
			int[] ids = ij.WindowManager.getIDList();
			if ( ids != null ) {
				int i = 0;
				for ( int id : ids ) {
					ImagePlus img = ij.WindowManager.getImage( id);
					if (!imageTypeIsOk(img))
						continue;
					allImgNames.add( img.getTitle());
					i++;
					// check if image name matches desired pre-selection
					if (preselect != null && preselect.equals( img.getTitle()) ) {
						idx = i;
						this.selectedImage = preselect;
						preselectedFound = true;
					}
				}
			}
			this.selectBox.removeAllItems();
			for (String image: allImgNames) {
				this.selectBox.addItem(image);
			}
			// if preselected image was not found, assume that it was renamed
			// and select currently active window
			if (   !preselectedFound 
					&& ij.WindowManager.getCurrentImage() != null) {
				if (!this.selectBox.getSelectedItem().equals("none selected"))
					this.selectBox.setSelectedItem(
							ij.WindowManager.getCurrentImage().getTitle());
			}
			else
				this.selectBox.setSelectedIndex(idx);
			
			// indicate if image selection really changed
			if (this.selectedImage == activeImage)
				return false;
			return true;
		}

		private boolean imageTypeIsOk(ImagePlus img) {
			MTBImage tmpImage = MTBImage.createMTBImage(img);
			if (  tmpImage.getClass().equals(
						 MTBImageDataIOSwing.this.desiredImageClass)
					|| MTBImageDataIOSwing.this.desiredImageClass.
							isAssignableFrom(tmpImage.getClass()))
				return true;
			MTBImageConverter converter;
      try {
	      converter = new MTBImageConverter();
				if (converter.supportConversion(tmpImage.getClass(), null, 
						MTBImageDataIOSwing.this.desiredImageClass, null))
					return true;
				return false;
      } catch (ALDOperatorException e) {
      	return false;
      }
		}
		
		@Override
		public void imageClosed(ImagePlus arg0) {
			boolean selectedImgChanged = 
					this.updateImageList(this.selectedImage);
			// notify listener of potential changes in parameter values
			if (selectedImgChanged)
				this.fireALDSwingValueChangeEvent(
					new ALDSwingValueChangeEvent(this, this.paramDescr));
		}

		@Override
		public void imageOpened(ImagePlus arg0) {
			boolean selectedImgChanged = 
					this.updateImageList(this.selectedImage);
			// notify listener of potential changes in parameter values
			if (selectedImgChanged)
				this.fireALDSwingValueChangeEvent(
					new ALDSwingValueChangeEvent(this, this.paramDescr));
		}

		@Override
		public void imageUpdated(ImagePlus img) {
			// we are only interested in images that are currently displayed...
			if (!img.isVisible())
				return;
			
			// check if image was renamed, should be the case if title is not 
			// in the list of available images so far...
			// Note: ImageJ does not guarantee unique names!
			boolean imageTitleIsNew = true;
			for (int i=0;i<this.selectBox.getItemCount();++i) {
				String name = (String)this.selectBox.getItemAt(i);
				if (img.getTitle().equals(name)) {
					imageTitleIsNew = false;
					break;
				}
			}
			boolean selectedImgChanged = false;
			if (imageTitleIsNew) {
				selectedImgChanged = this.updateImageList(this.selectedImage);
			}
			// notify listener of potential changes in parameter values
			if (selectedImgChanged)
				this.fireALDSwingValueChangeEvent(
					new ALDSwingValueChangeEvent(this, this.paramDescr));
		}
		
		@Override
    public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (this.selectBox.getItemCount() == 1) {
					return;
				}
				// get currently selected item
				String imgName = (String)(this.selectBox.getSelectedItem());
				if (!(imgName.equals(this.selectedImage))) {
					this.selectedImage = imgName;
					this.fireALDSwingValueChangeEvent(
						new ALDSwingValueChangeEvent(this, this.paramDescr));
				}
			}
    }

		@Override
    public void disableComponent() {
			this.selectBox.setEnabled(false);
    }

		@Override
    public void enableComponent() {
			this.selectBox.setEnabled(true);
    }
		
		@Override
		public void dispose() {
			// nothing to do here
		}
	}
	
	/**
	 * Panel containing the button to display an image on demand.
	 * 
	 * @author moeller
	 */
	protected class ImageShowPanel extends JPanel { 
		
		/**
		 * Button associated with the panel.
		 */
		private ImageShowButton button;
		
		/**
		 * Constructor.
		 * @param _obj	Button to be associated with the panel.
		 * @param title Image title to be shown on button.
		 */
		protected ImageShowPanel(ImageShowButton _obj, String title) {
			this.button = _obj;
			this.add(new JLabel(" < " + title + " >   "));
			this.add(this.button);
		}

		/**
		 * Returns the button object associated with the panel.
		 * @return	Button associated with panel.
		 */
		public ImageShowButton getImageShowButton() {
			return this.button;
		}
	}

	/**
	 * Button to display an image on demand.
	 * 
	 * @author moeller
	 */
	protected class ImageShowButton extends JButton 
		implements ActionListener {
		
		/**
		 * Image object associated with the button.
		 * <p>
		 * Supported types are {@link MTBImage} and {@link ImagePlus}.
		 */
		Object obj;
		
		/**
		 * Constructor.
		 * @param _obj	Image to be associated with the button.
		 */
		protected ImageShowButton(Object _obj) {
			super("Show Image");
			this.obj = _obj;
			this.setActionCommand("show");
			this.addActionListener(this);
		}
		
		@Override
    public void actionPerformed(ActionEvent ev) {
			String cmd = ev.getActionCommand();
			if (cmd.equals("show")) {
				MTBImageDataIOSwing.this.popUpImage(this.obj);
			}
		}
		
		/**
		 * Returns the image object associated with the button.
		 * @return	Image object shown on clicking the button.
		 */
		public Object getImageObject() {
			return this.obj;
		}
	}
}
