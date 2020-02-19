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

package de.unihalle.informatik.MiToBo.segmentation.evaluation;

import java.util.HashMap;

/**
 * Data object containing evaluation data from groundtruth comparison.
 * @author moeller
 */
public class MTBGroundtruthEvaluationData {
	
	/**
	 * Result data object.
	 * <p>
	 * The map contains for each string key (relating to a certain measure)  
	 * a map of < GT-ID , measure value > - mappings for the different
	 * groundtruth regions. 
	 */
	protected HashMap< String, HashMap<Integer, Double> > evalData = null;

	/**
	 * Default constructor.
	 */
	public MTBGroundtruthEvaluationData(
			HashMap< String, HashMap<Integer, Double> > data) {
		this.evalData = data;
	}

	/**
	 * Returns result data.
	 * @return	Result data map.
	 */
	public HashMap< String, HashMap<Integer, Double> > getResultData() {
		return this.evalData;
	}
}
