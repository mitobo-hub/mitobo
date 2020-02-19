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
 * $Rev: 4605 $
 * $Date: 2011-11-28 11:00:29 +0100 (Mo, 28 Nov 2011) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatools;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.tools.XMLTypeConverter;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo_xml.MTBXMLMultiStateMMIDVectorDocument;
import de.unihalle.informatik.MiToBo_xml.MTBXMLMultiStateMMIDVectorType;

/**
 * File-IO for (vectors of) MultiState objects.
 * 
 * @author Oliver Gress
 *
 */
public class MultiStateIO {

	/**
	 * Write a vector of MultiState objects to file in its xml-beans representation.
	 * @param mstates Vector of MultiState objects
	 * @param filename filename
	 */
	public static void writeMultiStates(Vector<MultiState<MotionModelID>> mstates, String filename) 
											throws IOException, ALDProcessingDAGException, ALDOperatorException {
		
		MTBXMLMultiStateMMIDVectorDocument doc = MTBXMLMultiStateMMIDVectorDocument.Factory.newInstance();
		
		doc.setMTBXMLMultiStateMMIDVector(XMLTypeConverter.toXMLType(mstates));
		
		doc.save(new File(filename));
		
		MTBOperator.writeHistory(mstates, filename);
	}
	
	/**
	 * Read a vector of MultiState objects from a file with xml-beans representation.
	 * @param filename filename
	 * @return Vector of MultiState objects
	 */
	public static Vector<MultiState<MotionModelID>> readMultiStates(String filename) throws XmlException, IOException {

		MTBXMLMultiStateMMIDVectorType mstates_xml = MTBXMLMultiStateMMIDVectorDocument.Factory.parse(new File(filename)).getMTBXMLMultiStateMMIDVector();
		
		Vector<MultiState<MotionModelID>> mstates = XMLTypeConverter.fromXMLType(mstates_xml);
		
		MTBOperator.readHistory(mstates, filename);
		
		return mstates;
	}
}
