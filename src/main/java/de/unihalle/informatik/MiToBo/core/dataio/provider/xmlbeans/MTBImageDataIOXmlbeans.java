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
 * Most recent change(s):
 * 
 * $Rev: 3901 $
 * $Date: 2011/10/28 15:22:44 $
 * $Author: posch $
 * 
 */

/*
 * This class uses the Bio-Formats and LOCI-commons packages/libraries (see the two licenses at the top)
 * as well as source code from the LOCI-plugins package (see third license from the top)
 */

package de.unihalle.informatik.MiToBo.core.dataio.provider.xmlbeans;

import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOCmdline;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida_xml.ALDXMLAnyType;
import de.unihalle.informatik.Alida_xml.ALDXMLObjectType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

import ij.ImagePlus;

import java.beans.DesignMode;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.xmlbeans.XmlDouble;
import org.apache.xmlbeans.XmlInt;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;

/**
 * Implementation of ALDDataIOCmdline interface for MitoBo images via xmlbeans provider.
 * <p>
 * Note: currently this is just a dummy implementation. No objects are written and
 * a null object is returned.
 * 
 * @author posch
 */
@ALDDataIOProvider(priority=1)
public class MTBImageDataIOXmlbeans implements ALDDataIOXmlbeans {
	
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
	public Object readData(Field field, Class<?> cl, ALDXMLObjectType xmlObject)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ALDXMLObjectType writeData(Object obj)
			throws ALDDataIOManagerException, ALDDataIOProviderException {
		ALDXMLAnyType aldXmlObject = ALDXMLAnyType.Factory.newInstance();
		aldXmlObject.setClassName(obj.getClass().getName());
		aldXmlObject.setValue(null);
		
		return aldXmlObject;
	}
}
