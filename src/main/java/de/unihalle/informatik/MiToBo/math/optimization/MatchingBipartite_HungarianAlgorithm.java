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
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.*;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;

/**
 * Bipartite matching with Hungarian algorithm.
 * <p>
 * This implementation is done according to:
 * 
 * Grosche, Ziegler, Ziegler and Zeidler,
 * Teubner-Taschenbuch der Mathematik, Teil II, pp. 219 ff
 * 7th edition, B.G. Teubner Verlagsgesellschaft Leipzig, 1995
 * 
 * It assumes a squared score matrix and tries to find the matching that
 * optimizes the score. The scores should all be positive and per default
 * the algorithm minimizes the overall score. Anyway, the operator can be
 * configured so as to interprete the scores inversely, i.e. searching for
 * the matching that maximizes the sum of scores. 
 * <p>
 * Note, that this implementation is not well-suited for large matrices.
 * The implementation is greedy, so do not expect too much in case of 
 * a large number of elements to be matched...
 * 
 * @author moeller
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MatchingBipartite_HungarianAlgorithm extends MatchingBipartite {

	/**
	 * Matrix scores interpretation.
	 * 
	 * @author moeller
	 */
	public static enum ScoreInterpretation {
		/**
		 * Indicates that scores are better if smaller.
		 */
		MINIMUM_IS_BEST,
		/**
		 * Indicates that scores are better if larger. 
		 */
		MAXIMUM_IS_BEST
	}
	
	/**
	 * Array containing row marks.
	 */
	protected boolean [] rowMarkers = null;
	
	/**
	 * Array containing column marks.
	 */
	protected boolean [] colMarkers = null;

	/**
	 * Matrix for elements markers: 0 = no mark, 1 = starred, 2 = primed
	 */
	protected byte [][] elementMarkers = null;
	
	/**
	 * Local copy of matrix modified during calculations.
	 */
	protected double[][] workingMatrix = null;
	
	/**
	 * Number of rows and columns, respectively.
	 */
	protected int matrixSize;
	
	/**
	 * Helper matrix for exchange chain extraction: 0 = unselected, 1 = selected.
	 */
	protected byte [][] chainMarkers = null;

	/**
	 * Internal flag for interrupting recursive calls in unit testing.
	 */
	protected boolean junitTest = false;
	
	/**
	 * Helper to make matrix minimum in stage three externally accessible.
	 */
	protected double stageThreeMin = 0;
	
	/**
	 * Score interpretation.
	 */
	@Parameter( label= "scoreInterpretation", required = true, 
			type = Parameter.Type.INPUT, 
			description = "How to interpret the scores.")
	protected ScoreInterpretation matrixScore = 
		ScoreInterpretation.MINIMUM_IS_BEST;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	protected MatchingBipartite_HungarianAlgorithm() 
		throws ALDOperatorException {
	  super();
  }

	/**
	 * Default constructor with parameters.
	 * <p>
	 * The rows of the matrix should refer to one set of elements, and the 
	 * cols to the second one. Matrix elements then give the matching scores 
	 * for all pairs of possible matchings. Note that for this technique the
	 * score matrix needs to be square, otherwise an exception is thrown.
	 * Also it is assumed that all scores are larger than or equal to zero.
	 * 
	 * @param smatrix		Square matrix with pairwise scores.
	 * @throws ALDOperatorException
	 */
	public MatchingBipartite_HungarianAlgorithm(double[][] smatrix, 
			ScoreInterpretation o) 
		throws ALDOperatorException {
		this.scoreMatrix = smatrix;
		this.matrixScore = o;
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.scoreMatrix.length != this.scoreMatrix[0].length)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"MatchingBipartite_Hungarian: score matrix is not square!");
		for (int r=0;r<this.scoreMatrix.length;++r)
			for (int c=0;c<this.scoreMatrix.length;++c)
				if (this.scoreMatrix[r][c] < 0)
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"MatchingBipartite_Hungarian: score matrix contains negative scores!");
		return;
	}

	@Override
  protected void calcMatching() {
	
		// init algorithm
		this.init();
		
		// allocate memory, init working copy of matrix, etc.
		this.prepareMatching();

		// call main test routine
		this.calcMatching_mainTest();
		
	}
	
	/**
	 * Initializes the operator, i.e. allocates memory.
	 */
	protected void init() {
		// allocate arrays for row and column markers, and element marker matrix
		this.matrixSize = this.scoreMatrix.length;
		
		this.colMarkers = new boolean[this.matrixSize];
		for (int i=0;i<this.matrixSize;++i)
			this.colMarkers[i] = false;
		this.rowMarkers = new boolean[this.matrixSize];
		for (int i=0;i<this.matrixSize;++i)
			this.rowMarkers[i] = false;
		this.elementMarkers = new byte[this.matrixSize][this.matrixSize];
		for (int j=0;j<this.matrixSize;++j)
			for (int i=0;i<this.matrixSize;++i)
				this.elementMarkers[j][i] = 0;
		this.workingMatrix = new double[this.matrixSize][this.matrixSize];
		for (int r=0;r<this.matrixSize;++r)
			for (int c=0;c<this.matrixSize;++c)
				this.workingMatrix[r][c]= this.scoreMatrix[r][c];
		this.chainMarkers = new byte[this.matrixSize][this.matrixSize];		
	}
	
	/**
	 * Preprocess the matrix.
	 * <p>
	 * The method first inverts all entries if large scores are better.
	 * Subsequently zeros are starred in the matrix, preferably so that each
	 * row and each column contains exactly one star. If this is possible a 
	 * solution has already been found.
	 */
	protected void prepareMatching() {
				
		// check if best score is minimal one, if not, preprocess data
		if (this.matrixScore == ScoreInterpretation.MAXIMUM_IS_BEST) {
			double maximum = 0;
			for (int r= 0; r<this.matrixSize; ++r) {
				for (int c= 0; c<this.matrixSize; ++c) {
					if (this.workingMatrix[r][c] > maximum)
						maximum = this.workingMatrix[r][c];
				}
			}			
			for (int r= 0; r<this.matrixSize; ++r) {
				for (int c= 0; c<this.matrixSize; ++c) {
					this.workingMatrix[r][c] = maximum - this.workingMatrix[r][c];
				}
			}
		}
		// search for minimum in each row and subtract it from each element
		double minimum = Double.MAX_VALUE;
		for (int r= 0; r<this.matrixSize; ++r) {
			minimum = Double.MAX_VALUE;
			for (int c= 0; c<this.matrixSize; ++c) {
				if (minimum > this.workingMatrix[r][c]) 
					minimum = this.workingMatrix[r][c];
			}
			for (int c= 0; c<this.matrixSize; ++c) {
				this.workingMatrix[r][c] -= minimum; 
			}				
		}
		// search for minimum in each col and subtract it from each element
		minimum = Double.MAX_VALUE;
		for (int c= 0; c<this.matrixSize; ++c) {
			minimum = Double.MAX_VALUE;
			for (int r= 0; r<this.matrixSize; ++r) {
				if (minimum > this.workingMatrix[r][c]) 
					minimum = this.workingMatrix[r][c];
			}
			for (int r= 0; r<this.matrixSize; ++r) {
				this.workingMatrix[r][c] -= minimum; 
			}				
		}

		// init algorithm: iteratively select zeros and mark rows and cols
		for (int r=0;r<this.matrixSize;++r) {
			if (this.rowMarkers[r])
				continue;
			for (int c=0;c<this.matrixSize;++c) {
				if (this.colMarkers[c])
					continue;
				// check if we have a zero in the current entry,
				// if so, then mark row and column and star the element
				if (   this.workingMatrix[r][c] < MTBConstants.epsilon
						&& !this.colMarkers[c] && !this.rowMarkers[r]) {
					this.rowMarkers[r] = true;
					this.colMarkers[c] = true;
					this.elementMarkers[r][c] = 1;
				}
			}
		}
		// reset all row markers
		for (int r=0;r<this.matrixSize;++r)
			this.rowMarkers[r] = false;
	}
	
	/**
	 * Checks whether a valid solution has been found.
	 */
	protected void calcMatching_mainTest() {
		
		// main test: all columns marked? 
		//   -> if so, set result matrix,
		//      if not, go to stage 1
		boolean solutionFound = true;
		for (int c=0;c<this.matrixSize;++c) {
			if (!this.colMarkers[c]) {
				solutionFound = false;
				break;
			}
		}
		if (!solutionFound) {
			// if we are in junit test mode, do not proceed recursively
			if (!this.junitTest)
				this.calcMatching_stageOne();
			else
				System.out.println("MATCHER: Would have called stage one...");
		}
		else {
			if (this.junitTest)
				System.out.println("MATCHER: All work done, going to bed...");
				
			// extract all starred entries into the result matrix
			this.resultMatrix = new byte[this.matrixSize][this.matrixSize];
			for (int r=0;r<this.matrixSize;++r) {
				for (int c=0;c<this.matrixSize;++c) {
					if (this.elementMarkers[r][c] == 1)
						this.resultMatrix[r][c] = 1;
					else
						this.resultMatrix[r][c] = 0;
				}
			}
			return;
		}
	}
	
	/**
	 * Implements stage one: searching for new candidate matches.
	 */
	protected void calcMatching_stageOne() {
		
		if (this.junitTest) {
			System.out.println("Working matrix:");
			for (int r=0;r<this.matrixSize;++r) {
				for (int c=0;c<this.matrixSize;++c) {
					System.out.print(this.workingMatrix[r][c] + "\t");
				}
				System.out.println();
			}
		}		
		
		// check if there are 0's not border-marked
		boolean zeroFound = false;
		int r= 0, c= 0;
		for (r=0;r<this.matrixSize && !zeroFound;++r) {
			if (this.rowMarkers[r])
				continue;
			for (c=0;c<this.matrixSize && !zeroFound;++c) {
				if (this.colMarkers[c])
					continue;
				if (this.workingMatrix[r][c] < MTBConstants.epsilon) 
					zeroFound = true;
			}
		}
		// decrease row and column counter to get correct position
		--r; --c;
		// if no zero is found, go to stage 3
		if (!zeroFound) {
			if (!this.junitTest)
				this.calcMatching_stageThree();
			else
				System.out.println("MATCHER: Would have called stage three...");
		}
		else {
			// prime the zero
			this.elementMarkers[r][c] = 2;
			// is there is a starred 0 in the same row?
			boolean starredZeroFound = false;
			int cc = 0;
			for (cc=0; cc<this.matrixSize && !starredZeroFound; ++cc) {
				if (this.elementMarkers[r][cc] == 1)
					starredZeroFound = true;
			}
			// no starred zero in that row, so go to stage 2
			if (!starredZeroFound) {
				if (!this.junitTest)
					this.calcMatching_stageTwo(r, c);
				else
					System.out.println("MATCHER: Would have called stage two...");
			}
			// starred zero found
			else {
				// decrease col count by one to get correct index
				--cc;
				// delete column mark of starred zero
				this.colMarkers[cc] = false;
				// mark row of starred zero
				this.rowMarkers[r] = true;
				// go to stage 1
				if (!this.junitTest)
					this.calcMatching_stageOne();
				else
					System.out.println("MATCHER: Would have called stage one again...");
			}
		}
	}

	/**
	 * Implements stage two: extracting exchange chain.
	 */
	protected void calcMatching_stageTwo(int r, int c) {

		if (this.junitTest)
			System.out.println(" - called stage two with r= " + r + " , c = " + c);
		
		// reset chain matrix
		for (int rr=0;rr<this.matrixSize;++rr)
			for (int cc=0;cc<this.matrixSize;++cc)
				this.chainMarkers[rr][cc] = 0;
		
		// mark begin of chain
		this.chainMarkers[r][c] = 1;
		
		// given last primed zero at entry (r,c),
		// extract exchange chain:
		// - go from a primed zero to a starred zero in the same column
		// - go from a starred zero to a primed zero in the same row
		boolean chainContinuable = true;
		boolean searchingColumn = true;
		int currentRow = r;
		int currentCol = c;
		while (chainContinuable) {
			chainContinuable = false;
			if (searchingColumn) {
				// check elements in current column
				for (int rr = 0; rr<this.matrixSize; ++rr) {
					if (   this.elementMarkers[rr][currentCol] == 1 
							&& this.chainMarkers[rr][currentCol] != 1) {
						this.chainMarkers[rr][currentCol] = 1;
						currentRow = rr;
						searchingColumn = false;
						chainContinuable = true;
						break;
					}
				}
			}
			else {
				// check elements in current row
				for (int cc = 0; cc<this.matrixSize; ++cc) {
					if (   this.elementMarkers[currentRow][cc] == 2 
							&& this.chainMarkers[currentRow][cc] != 1) {
						this.chainMarkers[currentRow][cc] = 1;
						currentCol = cc;
						searchingColumn = true;
						chainContinuable = true;
						break;
					}
				}					
			}
		}
		// delete all border marks
		for (int n = 0; n<this.matrixSize; ++n) {
			this.rowMarkers[n] = false;
			this.colMarkers[n] = false;
		}
		// process markers
		for (int rr=0; rr<this.matrixSize; ++rr) {
			for (int cc=0; cc<this.matrixSize; ++cc) {
				// safety-check only
				if (this.chainMarkers[rr][cc] == 1 && this.elementMarkers[rr][cc] == 0)
					System.err.println("Warning! Element in chain without marker!!!");

				// delete stars of elements in the chain
				if (   this.chainMarkers[rr][cc] == 1 
						&& this.elementMarkers[rr][cc] == 1) {
					this.elementMarkers[rr][cc] = 0;
				}
				// change primes into stars 
				else if (   this.chainMarkers[rr][cc] == 1 
						     && this.elementMarkers[rr][cc] == 2) {
					this.elementMarkers[rr][cc] = 1;
				}
				// delete primes of elements not in the chain
				else if (   this.chainMarkers[rr][cc] == 0 
								 && this.elementMarkers[rr][cc] == 2) {
					this.elementMarkers[rr][cc] = 0;
				}
			}
		}
		// mark all cols that contain stars now
		for (int cc=0; cc<this.matrixSize; ++cc) {
			for (int rr=0; rr<this.matrixSize; ++rr) {
				if (this.elementMarkers[rr][cc] == 1) {
					this.colMarkers[cc] = true;
					break;
				}
			}
		}
		// go to main test
		if (!this.junitTest)
			this.calcMatching_mainTest();
		else
			System.out.println("MATCHER: Would have called main test...");
	}

	/**
	 * Implements stage three: decrease scores to generate new zero entries.
	 */
	protected void calcMatching_stageThree() {
		// search minimum h of all non-border-marked elements
		double h = Double.MAX_VALUE;
		for (int r=0;r<this.matrixSize;++r) {
			if (this.rowMarkers[r])
				continue;
			for (int c=0;c<this.matrixSize;++c) {
				if (this.colMarkers[c])
					continue;
				if (this.workingMatrix[r][c] < h) 
					h = this.workingMatrix[r][c];
			}
		}
		this.stageThreeMin = h;
		// add h to all elements in marked columns
		for (int c=0;c<this.matrixSize;++c) {
			if (!this.colMarkers[c])
				continue;
			for (int r=0;r<this.matrixSize;++r) {
				this.workingMatrix[r][c] += h;
			}
		}
		// subtract h from all elements in non-marked rows
		for (int r=0;r<this.matrixSize;++r) {
			if (this.rowMarkers[r])
				continue;
			for (int c=0;c<this.matrixSize;++c) {
				this.workingMatrix[r][c] -= h;
			}
		}
		// go to stage 1
		if (!this.junitTest)
			this.calcMatching_stageOne();
		else
			System.out.println("MATCHER: Would have called stage one...");
	}
}
