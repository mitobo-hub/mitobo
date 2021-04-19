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

package de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.provider;

/**
 * Datatype to handle input configurations for {@link PaCeQuant_FeatureColorMapper}.
 *
 * @author moeller
 */
public class PaCeQuant_FeatureColorMapperInputData {

	/**
	 * Name of directory including full path.
	 */
	protected String dirPath;
		
	/**
	 * Indices of selected column.
	 */
	protected int[] columnIDs;

	/**
	 * Default constructor.
	 * 
	 * @param dir	Name of directory including path.
	 * @param ids	Indices of selected columns.
	 */
	public PaCeQuant_FeatureColorMapperInputData(String dir, int[] ids) {
		this.dirPath = dir;
		this.columnIDs = ids;
	}
		
	/**
	 * Returns current name of directory.
	 * @return Name of directory.
	 */
	public String getDirectoryName() {
		return this.dirPath;
	}

	/**
	 * Returns index of currently selected column(s).
	 * @return	Indices of columns.
	 */
	public int[] getColumnIDs() {
		return this.columnIDs;
	}
	
	@Override
	public String toString() {
		return new String(this.dirPath);
	}
}
