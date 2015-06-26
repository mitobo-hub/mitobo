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

package de.unihalle.informatik.MiToBo.features;

/**
 * Interface for result data returned from classes extending 
 * {@link FeatureCalculator}.
 * <p>
 * In principal any kind of object could be used as result object. For easier
 * handling of the result object, in particular in a generic fashion, it is 
 * only mandatory to implement the method {@literal toString()} which should
 * return a proper textual representation of the result. This textual value is
 * for example used within the result table shown in the GUI or on writing the
 * results to file.
 * 
 * @author moeller
 */
public interface FeatureCalculatorResult 
{
	/**
	 * Returns the result data object of the calculator.
	 * @return	Result data object.
	 */
	public abstract Object getResult();
	
	/**
	 * Returns the dimension of the result object.
	 * <p>
	 * If a single object or value is calculated the dimension should be one.
	 * In case of, e.g., arrays containing multiple values the length of the 
	 * array should be returned. 
	 * @return Dimensionality of the result data.
	 */
	public abstract int getDimensionality();
	
	/**
	 * Indicates if result data can be 'condensed' to numerical values, e.g., 
	 * for visualization purposes.
	 * <p>
	 * If this method returns true it is expected that for each component of
	 * the result the method {@link #getNumericalValue(int)} returns a 
	 * proper numerical value.
	 * 
	 * @return	True, if result data can be represented numerically.
	 */
	public abstract boolean isConvertableToNumericalData();
	
	/**
	 * Returns a numerical value representing the requested result component.
	 * <p>
	 * This method is linked to {@link #isConvertableToNumericalData()}, i.e.
	 * is only expected to return proper results if that function returns true.
	 * 
	 * @param  dim	Index of the desired component of the result.
	 * @return	Numerical result value.
	 */
	public abstract double getNumericalValue(int dim);

	/**
	 * Returns entry in requested field as string representation.
	 * 
	 * @param  dim	Index of the desired component of the result.
	 * @return	String representation of the value.
	 */
	public abstract String getTableEntry(int dim);

	/**
	 * Method returns an identifier characterizing the operator.
	 * <p>
	 * The string is for example included in the headers of result tables.
	 * @return	Identifier string.
	 */
	public abstract String getOpIdentifier(); 
	
	/**
	 * Method returns an identifier characterizing the related component of 
	 * the result.
	 * @return	Identifier string.
	 */
	public abstract String getResultIdentifier(int dim);
}
