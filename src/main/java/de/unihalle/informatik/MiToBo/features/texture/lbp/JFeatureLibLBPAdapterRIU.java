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

package de.unihalle.informatik.MiToBo.features.texture.lbp;

/**
 * Implementation of rotation invariant uniform LBPs.
 * <p>
 * This class builds upon the Local Binary Pattern implementation of
 * JFeatureLib, i.e., extends the class for rotation invariance.
 * For details refer to its project page at 
 * <a href="https://github.com/locked-fg/JFeatureLib">GitHub</a>.
 * <p>
 * This implementation follows the paper by<br>
 * <i>Ojala et al, Multiresolution Gray-Scale and Rotation Invariant
 * Texture Classification with Local Binary Patterns, PAMI, vol. 24,
 * no. 7, pp. 971-987, July 2002.</i>
 * 
 * @author Birgit Moeller
 */
public class JFeatureLibLBPAdapterRIU extends JFeatureLibLBPAdapter {

	/**
	 * Maximal neighbor number for which array has been initialized so far.
	 */
	protected static int maxValidNeighborCount = 0;
	
	/**
	 * Rotation invariant uniform LBP code array.
	 */
	protected static int[][] codeArrayRIU = null; 

	/**
	 * Default constructor.
	 * @param n		Number of neighbors, i.e. bits, to consider.
	 */
	public JFeatureLibLBPAdapterRIU(int n) {
		super();
		if (codeArrayRIU == null || maxValidNeighborCount < n) {
			maxValidNeighborCount = n;
			codeArrayRIU = FeatureCalculatorLBPRIULUTs.getLBPCodeArrayRIU(n);
		}
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.jfeaturelib.features.LocalBinaryPatterns#getDescription()
	 */
	@Override
	public String getDescription() {
		return "MiToBo Local Binary Patterns RIU";
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.jfeaturelib.features.LocalBinaryPatterns#getMaxBinaryPattern()
	 */
	@Override
	protected int getMaxBinaryPattern() {
		return this.getNumPoints()+2;
	}

	/* (non-Javadoc)
	 * @see de.lmu.ifi.dbs.jfeaturelib.features.LocalBinaryPatterns#getBinaryPattern(int, int)
	 */
	@Override
	protected int getBinaryPattern(final int x, final int y) {

		// first get conventional binary pattern
		int pattern = super.getBinaryPattern(x, y);

		// check if array has properly been initialized
		if (this.getNumPoints() > maxValidNeighborCount) {
			maxValidNeighborCount = this.getNumPoints();
			codeArrayRIU = 
				FeatureCalculatorLBPRIULUTs.getLBPCodeArrayRIU(this.getNumPoints());
		}
		
		// on the first call determine correct column index
		int columnIndex = -1;
		switch(this.getNumPoints())
		{
			case  8: columnIndex = 0; break;
			case 12: columnIndex = 1; break;
			case 16: columnIndex = 2; break;
			case 24: columnIndex = 3; break;
		}
		return codeArrayRIU[pattern][columnIndex];
	}
}
