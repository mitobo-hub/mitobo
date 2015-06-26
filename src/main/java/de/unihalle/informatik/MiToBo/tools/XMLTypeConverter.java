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

package de.unihalle.informatik.MiToBo.tools;

import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiStateFactory;
import de.unihalle.informatik.MiToBo_xml.MTBXMLMatrixType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLMultiStateMMIDType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLMultiStateMMIDVectorType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLStateMMIDType;

/**
 * A class providing static methods to convert specific object to and from their corresponding xml-types
 * provided by xml-beans.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class XMLTypeConverter {
	
	// ---- JAMA matrix ----
	
	/**
	 * Convert a JAMA matrix to its xml-beans type
	 */
	public static MTBXMLMatrixType toXMLType(Matrix matrix) {
		MTBXMLMatrixType m_xml = MTBXMLMatrixType.Factory.newInstance();
		
		int nr = matrix.getRowDimension();
		int nc = matrix.getColumnDimension();
		
		m_xml.setNumRows(nr);
		m_xml.setNumColumns(nc);
		
		double[] mElems = new double[nr*nc];
		
		for (int r = 0; r < nr; r++) {
			for (int c = 0; c < nc; c++) {
				mElems[r*nc + c] = matrix.get(r, c);
			}
		}
		
		m_xml.setMatrixElementsArray(mElems);
		return m_xml;
	}
	
	/**
	 * Obtain a JAMA matrix from its xml-beans type
	 */
	public static Matrix fromXMLType(MTBXMLMatrixType xmlmatrix) {
		
		int nr = xmlmatrix.getNumRows();
		int nc = xmlmatrix.getNumColumns();
		
		Matrix m = new Matrix(nr, nc);
		double[] mXmlElems = xmlmatrix.getMatrixElementsArray();
		
		for (int r = 0; r < nr; r++) {
			for (int c = 0; c < nc; c++) {
				m.set(r, c, mXmlElems[r*nc + c]);
			}
		}
		
		return m;
	}
	
	
	// ---- MultiState<MotionModelID> ----
	
	/**
	 * Convert a MultiState object to its xml-beans representation
	 */
	public static MTBXMLMultiStateMMIDType toXMLType(MultiState<MotionModelID> mstate) {
		
		MTBXMLMultiStateMMIDType mstate_xml = MTBXMLMultiStateMMIDType.Factory.newInstance();
		
		MTBXMLStateMMIDType[] states = new MTBXMLStateMMIDType[mstate.getNumberOfStates()];
		
		for (int n = 0; n < states.length; n++) {
			states[n] = MTBXMLStateMMIDType.Factory.newInstance();
			states[n].setStateCont(XMLTypeConverter.toXMLType(mstate.getStateContinuous(n)));
			states[n].setStateDisr(mstate.getStateDiscrete(n).toXMLType());
		}
		
		mstate_xml.setStatesArray(states);
		mstate_xml.setCDOF(mstate.getFactory().getContinuousDOF());
		
		return mstate_xml;
	}
	
	/**
	 * Obtain a MultiState object from its xml-beans representation
	 */
	public static MultiState<MotionModelID> fromXMLType(MTBXMLMultiStateMMIDType mstate_xml) {
		MultiStateFactory<MotionModelID> factory = new MultiStateFactory<MotionModelID>(mstate_xml.getCDOF());
		
		MTBXMLStateMMIDType[] states = mstate_xml.getStatesArray();
		
		MultiState<MotionModelID> mstate = (MultiState<MotionModelID>) factory.createEmptyMultiState();
		
		for (int n = 0; n < states.length; n++) {
			mstate.insertState(XMLTypeConverter.fromXMLType(states[n].getStateCont()),
					new MotionModelID(states[n].getStateDisr()));
		}
		
		return mstate;
	}
	
	
	// ---- Vector<MultiState<MotionModelID>> ----
	
	/**
	 * Convert a Vector<MultiState<MotionModelID>> object to its xml-beans representation
	 */
	public static MTBXMLMultiStateMMIDVectorType toXMLType(Vector<MultiState<MotionModelID>> mstates) {
		
		MTBXMLMultiStateMMIDVectorType mstates_xml = MTBXMLMultiStateMMIDVectorType.Factory.newInstance();
		MTBXMLMultiStateMMIDType[] mstates_xml_array = new MTBXMLMultiStateMMIDType[mstates.size()];
		
		for (int t = 0; t < mstates.size(); t++) {
			mstates_xml_array[t] = toXMLType(mstates.get(t));
		}
		
		mstates_xml.setMultiStatesArray(mstates_xml_array);
		
		return mstates_xml;
	}
	
	/**
	 * Obtain a Vector<MultiState<MotionModelID>> object from its xml-beans representation
	 */
	public static Vector<MultiState<MotionModelID>> fromXMLType(MTBXMLMultiStateMMIDVectorType mstates_xml) {
		MTBXMLMultiStateMMIDType[] mstates_xml_array = mstates_xml.getMultiStatesArray();
		
		Vector<MultiState<MotionModelID>> mstates = new Vector<MultiState<MotionModelID>>(mstates_xml_array.length);
		
		for (int t = 0; t < mstates_xml_array.length; t++) {
			mstates.add(fromXMLType(mstates_xml_array[t]));
		}
		
		return mstates;
	}

}
