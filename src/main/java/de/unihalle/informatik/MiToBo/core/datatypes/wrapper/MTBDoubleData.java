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
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.core.datatypes.wrapper;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.operator.ALDData;

/**
 * A double to be used as input or output for MTB operators.
 * <p>
 * This wrapper types overcomes the limitations of Java's built-in
 * wrappers in automatic parameter documentation. While references of 
 * type {@link Double} aren't necessarily unique and, hence, might not
 * be properly traced through the processing history, this wrapper class
 * overcomes the limitations.
 * 
 * @author Stefan Posch
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBDoubleData extends ALDData {
	
		/**
		 * Double value for these object.
		 */
		private Double value;

		/**
		 * Construct an empty double data object from the given double value.
		 * 
		 * @param value	Double value for the object.
		 */
		public MTBDoubleData(Double _value) {
				this.value = _value;
		}

		/**
		 * Returns the double value.
		 * 
		 * @return The double data value.
		 */
		public Double getValue() {
				return this.value;
		}

		/**
		 * Set the value of the double datat object.
		 * 
		 * @param value	Double value to set.
		 */
		public void setValue(Double _value) {
				this.value = _value;
		}
		
		@Override
		public String toString() {
		    return Double.toString(value.doubleValue());
		}

}
