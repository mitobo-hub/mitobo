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

package de.unihalle.informatik.MiToBo.core.datatypes.wrapper;

import de.unihalle.informatik.Alida.operator.ALDData;

/**
 * A boolean to be used as input or output for MTB operators.
 * <p>
 * This wrapper types overcomes the limitations of Java's built-in
 * wrappers in automatic parameter documentation. While references of 
 * type {@link Boolean} aren't necessarily unique and, hence, might not
 * be properly traced through the processing history, this wrapper class
 * overcomes the limitations.
 * 
 * @author Birgit Moeller
 */
public class MTBBooleanData extends ALDData {
	
		/**
		 * Boolean value of this object.
		 */
		private boolean value;

		/**
		 * Construct an empty boolean data object from the given boolean value.
		 * 
		 * @param value	Boolean value for the object.
		 */
		public MTBBooleanData(boolean _value) {
				this.value = _value;
		}

		/**
		 * Returns the boolean value.
		 * 
		 * @return The boolean data value.
		 */
		public boolean getValue() {
				return this.value;
		}

		/**
		 * Set the value of the boolean data object.
		 * 
		 * @param value	Boolean value to set.
		 */
		public void setValue(boolean _value) {
				this.value = _value;
		}
		
		@Override
		public String toString() {
		    return Boolean.toString(this.value);
		}

}
