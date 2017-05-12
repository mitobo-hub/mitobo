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

package de.unihalle.informatik.MiToBo.core.dataio.provider.cmdline;

import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

import ij.ImagePlus;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Implementation of ALDDataIOCmdline interface for MitoBo images.
 * 
 * @author moeller
 */
@ALDDataIOProvider(priority=1)
public class MTBImageDataIO implements ALDDataIOCmdline {
	
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
		classes.add( ImagePlus.class);
		return classes;
	}
	@Override
	public Object readData(Field field, Class<?> cl, String iname) 
			throws ALDDataIOProviderException {
		try {
			String desiredClassName = cl.getSimpleName();
			
			ImageReaderMTB reader = new ImageReaderMTB(iname);
			reader.runOp(false);
			MTBImage img = reader.getResultMTBImage();
			
			// check image type and convert if necessary
			if (desiredClassName.equals("ImagePlus"))
				return img.getImagePlus();
			if (desiredClassName.equals("MTBImage"))
				return img;
			if (desiredClassName.equals("MTBImageByte")) {
				if (img.getType() == MTBImageType.MTB_BYTE)
					return img;
				return img.convertType(MTBImageType.MTB_BYTE, true);
			}
			if (desiredClassName.equals("MTBImageDouble")) {
				if (img.getType() == MTBImageType.MTB_DOUBLE)
					return img;
				return img.convertType(MTBImageType.MTB_DOUBLE, true);
			}
			if (desiredClassName.equals("MTBImageFloat")) {
				if (img.getType() == MTBImageType.MTB_FLOAT)
					return img;
				return img.convertType(MTBImageType.MTB_FLOAT, true);
			}
			if (desiredClassName.equals("MTBImageInt")) {
				if (img.getType() == MTBImageType.MTB_INT)
					return img;
				return img.convertType(MTBImageType.MTB_INT, true);
			}
			if (desiredClassName.equals("MTBImageRGB")) {
				if (img.getType() == MTBImageType.MTB_RGB)
					return img;
				return img.convertType(MTBImageType.MTB_RGB, true);
			}
			if (desiredClassName.equals("MTBImageShort")) {
				if (img.getType() == MTBImageType.MTB_SHORT)
					return img;
				return img.convertType(MTBImageType.MTB_SHORT, true);
			}
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
					"MTBImageDataIO::readData unkwon image type <" +  desiredClassName +">");
		} catch (Exception e) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
					"MTBImageDataIO::readData error reading MTBImage of type <" +  cl.getSimpleName() +
					"from file <" + iname + ">");
		}
	}

	@Override
	public String writeData(Object obj, String oname) 
			throws ALDDataIOProviderException {
		
		if ((!(obj instanceof MTBImage)) && (!(obj instanceof ImagePlus)))
			return null;
		
		if (obj instanceof MTBImage) {
			MTBImage img = (MTBImage)obj;
			
			// check if output file name starts with '-', if so, show image directly
			if (oname.startsWith("-")) {
				img.show();
				return null;
			}
			
			try {
				ImageWriterMTB writer = new ImageWriterMTB(img, oname);
				writer.runOp(false);
				return( null);
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBImageDataIO::writeData error writing MTBImage to file <" + oname + ">" +
						"\n" + e.getMessage());
			} catch (ALDProcessingDAGException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBImageDataIO::writeData error writing MTBImage to file <" + oname + ">" +
						"\n" + e.getMessage());
			}
		}
		else if (obj instanceof ImagePlus) {
			ImagePlus imgp = (ImagePlus)obj;
			
			// check if output file name starts with '-', if so, show image directly
			if (oname.startsWith("-")) {
				MTBImage.createMTBImage(imgp).show();
				return null;
			}

			try {
				ImageWriterMTB writer = 
						new ImageWriterMTB(MTBImage.createMTBImage(imgp), oname);
				writer.runOp(false);
				return( null);
			} catch (ALDOperatorException e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
						"MTBImageDataIO::writeData error writing MTBImage to file <" + oname + ">" +
						"\n" + e.getMessage());
			} catch (ALDProcessingDAGException e) {
				e.printStackTrace();
			} 
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.FILE_IO_ERROR,
					"MTBImageDataIO::writeData error writing MTBImage to file <" + oname + ">");
		}
		throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
				"MTBImageDataIO::writeData unknown image type <" +
						obj.getClass().getCanonicalName() + ">");
	}
}
