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

import de.lmu.ifi.dbs.jfeaturelib.features.LocalBinaryPatterns;
import ij.process.ImageProcessor;

/**
 * Adapter for JFeatureLib's Local Binary Patterns.
 * <p>
 * This class offers additional functionality to access the data and
 * methods inside the Local Binary Pattern class to allow for easier
 * and more efficient calculations.
 * <p>
 * For details about the implementation in the JFeatureLib, refer to
 * its project page at 
 * <a href="https://github.com/locked-fg/JFeatureLib">GitHub</a>.
 * 
 * @author Birgit Moeller
 */
public class JFeatureLibLBPAdapter extends LocalBinaryPatterns {

	/**
	 * Default constructor.
	 */
	public JFeatureLibLBPAdapter() {
		super();
		this.m_offsets = null;
	}

	@Override
	public void setImageProcessor(ImageProcessor ip) {
		super.setImageProcessor(ip);
	}
	
	/**
	 * Calculate the histogram of LBP codes in the neighborhood of the
	 * specified pixel.
	 * <p>
	 * Note that suitable parameters, e.g., for the size of the 
	 * neighborhood, the radius and the number of circular points have to 
	 * be specified before.
	 * 
	 * @param x		x-coordinate of the pixel under consideration.
	 * @param y		y-coordinate of the pixel under consideration.
	 * @return	Histogram of LBP codes.
	 */
	public double[] getLBPHistogram(int x, int y) {
		// ensure that the offset table is properly initialized
		if (this.m_offsets == null) {
			this.calculateOffsets();
		}
		return this.processPixel(x, y);
	}
}
