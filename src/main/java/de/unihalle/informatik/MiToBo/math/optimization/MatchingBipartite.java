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

package de.unihalle.informatik.MiToBo.math.optimization;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Base class for bipartite matching algorithms.
 * <p>
 * Given two sets of elements S1 and S2, the goal is to find a matching between
 * both sets such that for every element of S1 exactly one matching element in 
 * S2 is found, and vice versa. If the cardinality of the sets is not equal,
 * handling and solution depends on the applied matching algorithm.
 * <p>
 * The matching is done according to a weighting matrix containing costs or 
 * scores for each pair of elements e1 from S1 and e2 from S2. 
 * 
 * @author moeller
 */
public abstract class MatchingBipartite extends MTBOperator {

	/**
	 * Matrix containing pairwise scores of bipartite sets.
	 */
	@Parameter( label= "scoreMatrix", required = true, 
			type = Parameter.Type.INPUT, 
			description = "Scoring matrix.")
	protected double[][] scoreMatrix = null;
	
	/**
	 * Result matrix containing final matching.
	 * <p>
	 * The matrix usually contains only 0 and 1 as entries with 1 indicating
	 * an assignment. In each row and each column there should be exactly one 
	 * single 1 only.
	 */
	@Parameter( label= "resultMatrix",
			type = Parameter.Type.INPUT, 
			description = "Result matrix.")
	protected byte[][] resultMatrix = null;	

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	protected MatchingBipartite() throws ALDOperatorException {
	  super();
  }

  @Override
  protected void operate() {
  	this.calcMatching();
  }
	
	/**
	 * Function calculating actual matching, to be implemented by subclasses.
	 */
	protected abstract void calcMatching(); 

	/**
	 * Returns result matrix with matching result.
	 */
	public byte[][] getMatching() {
		return this.resultMatrix;
	}
	
}
