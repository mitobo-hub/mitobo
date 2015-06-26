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
 * LOCI-plugins package (see third license from the top).
 */

package de.unihalle.informatik.MiToBo.core.batch.provider.output.swing;

import de.unihalle.informatik.Alida.annotations.ALDBatchOutputProvider;
import de.unihalle.informatik.Alida.batch.ALDBatchRunResultInfo;
import de.unihalle.informatik.Alida.batch.provider.input.swing.ALDBatchInputIteratorSwing;
import de.unihalle.informatik.Alida.batch.provider.output.swing.ALDBatchOutputSummarizerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JComponent;

/**
 * Implementation of {@link ALDBatchInputIteratorSwing} for MitoBo images.
 * 
 * @author moeller
 */
@ALDBatchOutputProvider(priority=1)
public class MTBSummarizerImageData
	implements ALDBatchOutputSummarizerSwing {
	
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
	
	@SuppressWarnings("null")
	@Override
	public JComponent writeData(ALDBatchRunResultInfo batchInfo,
																ALDParameterDescriptor descr) {
		Vector<Object> resultDataCollection = batchInfo.getResultDataVec();
		int numImages = resultDataCollection.size();
  	MTBImage tmpImg;

		// discover maximal parameters
		int maxWidth = 0, maxHeight = 0, maxDepth = 0, maxChannels = 0;
		for (int i=0; i<numImages; i++) {
  		tmpImg = (MTBImage)resultDataCollection.get(i);
  		if (tmpImg.getSizeX() > maxWidth)
  			maxWidth = tmpImg.getSizeX();
  		if (tmpImg.getSizeY() > maxHeight)
  			maxHeight = tmpImg.getSizeY();
  		if (tmpImg.getSizeZ() > maxDepth)
  			maxDepth = tmpImg.getSizeZ();
  		if (tmpImg.getSizeC() > maxChannels)
  			maxChannels = tmpImg.getSizeC();
		}
		int width = maxWidth;
		int height = maxHeight;
		int depth = maxDepth;
		int channels = maxChannels;
  	MTBImage mtbStack= null;
  	MTBImage refImage = (MTBImage)resultDataCollection.get(0);
  	if (refImage instanceof MTBImageByte) {
  		mtbStack = MTBImage.createMTBImage(width, height, depth, 1,
  												numImages*channels, MTBImage.MTBImageType.MTB_BYTE);
  	}
  	else if (refImage instanceof MTBImageDouble) {
  		mtbStack = MTBImage.createMTBImage(width, height, depth, 1,
  												numImages*channels, MTBImage.MTBImageType.MTB_DOUBLE);
  	}
  	else if (refImage instanceof MTBImageFloat) {
  		mtbStack = MTBImage.createMTBImage(width, height, depth, 1,
  												numImages*channels, MTBImage.MTBImageType.MTB_FLOAT);
  	}
  	else if (refImage instanceof MTBImageInt) {
  		mtbStack = MTBImage.createMTBImage(width, height, depth, 1,
  												numImages*channels, MTBImage.MTBImageType.MTB_INT);
  	}
  	else if (refImage instanceof MTBImageRGB) {
  		mtbStack = MTBImage.createMTBImage(width, height, depth, 1,
  												numImages*channels, MTBImage.MTBImageType.MTB_RGB);
  	}
  	else if (refImage instanceof MTBImageShort) {
  		mtbStack = MTBImage.createMTBImage(width, height, depth, 1,
  												numImages*channels, MTBImage.MTBImageType.MTB_SHORT);
  	}
  	
  	// copy result images to stack
  	Vector<Object> paramSettings = batchInfo.getParameterValueVec();
  	for (int i=0; i<numImages; i++) {
  		tmpImg = (MTBImage)resultDataCollection.get(i);
  		String title = "Input parameter = " + paramSettings.get(i);
  		tmpImg.setTitle(title);
  		mtbStack.setImagePart(tmpImg, 0, 0, 0, 0, i*channels);
  		mtbStack.setSliceLabel(title, 0, 0, i*channels);
  	}
  	
  	// return data I/O provider for image
  	try {
			ALDDataIOSwing imageProvider =
					(ALDDataIOSwing)ALDDataIOManagerSwing.getInstance().getProvider(
																			MTBImage.class, ALDDataIOSwing.class);
			return imageProvider.writeData(mtbStack, descr);
		} catch (ALDDataIOManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ALDDataIOProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	return null;
	}
}
