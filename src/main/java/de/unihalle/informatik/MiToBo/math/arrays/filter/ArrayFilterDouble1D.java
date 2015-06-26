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

package de.unihalle.informatik.MiToBo.math.arrays.filter;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This class defines a superclass for filters for 1D arrays. 
 *
 * @author moeller
 */
public abstract class ArrayFilterDouble1D extends MTBOperator {

	/**
	 * Input data array.
	 */
	@Parameter( label= "Input Array", required = true, dataIOOrder = 0, 
			direction = Parameter.Direction.IN, description = "Input array.")
	protected double[] inputArray = null;

	/**
	 * Flag to indicate if data is periodic.
	 * <p>
	 * If the flag is set to false, padding is done with zeros.
	 */
	@Parameter( label= "Array is periodic?", required = true, 
			dataIOOrder = 1, direction = Parameter.Direction.IN, 
			description = "If checked, array data is assumed to be periodic.")
	protected boolean dataIsPeriodic = true;

	/**
	 * Result data array.
	 */
	@Parameter( label= "Output Array", required = true, dataIOOrder = 0, 
			direction = Parameter.Direction.OUT, description = "Output array.")
	protected transient double[] outputArray = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException 
	 * 		Thrown in case of problems during execution.
	 */
	protected ArrayFilterDouble1D() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Set array to process.
	 * @param array	Data array to process.
	 */
	public void setInputArray(double[] array) {
		this.inputArray = array;
	}
	
	/**
	 * Specify if data is periodic or not.
	 * @param flag	If true data is assumed to be periodic.
	 */
	public void setDataIsPeriodic(boolean flag) {
		this.dataIsPeriodic = flag;
	}

	/**
	 * Returns result array.
	 * @return Filtered array.
	 */
	public double[] getResultArray() {
		return this.outputArray;
	}

}
