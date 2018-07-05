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

import java.util.Vector;

/**
 * Class keeping look-up tables for rotation invariant LBP codes.
 * <p>
 * This class basically contains various static arrays with 
 * rotation-invariant LBP codes. The arrays are initialized at the time 
 * of first access. Note that this may take some time depending on the 
 * power of your computer. Furthermore at least 1 GB of RAM should be 
 * available.
 * <p>
 * The theory regarding rotation invariant LBP codes can be found in<br>
 * <i>Ojala et al, Multiresolution Gray-Scale and Rotation Invariant
 * Texture Classification with Local Binary Patterns, PAMI, vol. 24,
 * no. 7, pp. 971-987, July 2002.</i>
 *
 * @see FeatureCalculatorLBPOriginal
 * @see FeatureCalculatorLBP
 *
 * @author Birgit Moeller
 */
public class FeatureCalculatorLBPRIULUTs {
	
	/**
	 * Array containing rotation invariant LBP codes.
	 * <p>
	 * The array has 2^24 rows and 4 columns. Each column contains a 
	 * certain unified code for the index:
	 * <ul>
	 * <li> 1st column: 8-bit representation
	 * <li> 2nd column: 16-bit representation
	 * <li> 3rd column: 12-bit representation
	 * <li> 4th column: 24-bit representation
	 * </ul>
	 * The codes are calculated by shifting the binary representation of 
	 * the index such that a maximal number of zeros is located at the
	 * left end of the code and the overall number encoded is minimal.
	 */
	public static int[][] codeArrayRI = null;

	/**
	 * Array containing uniform rotation invariant LBP codes.
	 * <p>
	 * The array has 2^24 rows and 4 columns. Each column contains a 
	 * certain unified code for the index:
	 * <ul>
	 * <li> 1st column: 8-bit representation
	 * <li> 2nd column: 16-bit representation
	 * <li> 3rd column: 12-bit representation
	 * <li> 4th column: 24-bit representation
	 * </ul>
	 * The codes are calculated by first determining the number U of 
	 * 0-1 or 1-0 changes in the binary representation of a given index.
	 * For indices with U not larger than 2 the code is then given by 
	 * counting the number of ones, all other indices get a code the 
	 * number of bits considered plus one. 
	 */
	public static int[][] codeArrayRIU = null;
	
	/**
	 * Distribution of codes.
	 * <p>
	 * The array has 2^24 rows and 4 columns. Each column contains the 
	 * number of appearances of the index as LBP code given the 
	 * corresponding number of bits: 
	 * <ul>
	 * <li> 1st column: 8-bit representation
	 * <li> 2nd column: 16-bit representation
	 * <li> 3rd column: 12-bit representation
	 * <li> 4th column: 24-bit representation
	 * </ul>
	 */
	public static int[][] codeDistributionRI = null;
	
	/**
	 * Maximal bit number for which the class has already been initialized.
	 * <p>
	 * By storing the maximum bit number already handled we seek to avoid doing 
	 * time-consuming calculations multiple times.
	 */
	private static int maxBitsSoFar = 0;
	
	/**
	 * Get array with LBP rotation invariant codes. 
	 * @param maxBits	Maximal number of bits to consider.
	 * @return	Array with codes.
	 */
	public static int[][] getLBPCodeArrayRI(int maxBits) {
		if (codeArrayRI == null || maxBits > maxBitsSoFar) {
			initArrayRI(maxBits);
			maxBitsSoFar = maxBits;
		}
		return codeArrayRI;
	}
	
	/**
	 * Get array with LBP rotation invariant uniform codes. 
	 * @param maxBits	Maximal number of bits to consider.
	 * @return	Array with codes.
	 */
	public static int[][] getLBPCodeArrayRIU(int maxBits) {
		if (codeArrayRIU == null || maxBits > maxBitsSoFar) {
			initArrayRIU(maxBits);
			maxBitsSoFar = maxBits;
		}
		return codeArrayRIU;
	}

	/**
	 * Get array with LBP-RI code distributions. 
	 * @param maxBits	Maximal number of bits to consider.
	 * @return	Array with code counts.
	 */
	public static int[][] getCodeDistributionRI(int maxBits) {
		if (codeArrayRI == null)
			initArrayRI(maxBits);
		return codeDistributionRI;
	}
	
	/**
	 * Initializes the array with rotation invariant and uniform codes.
	 * <p>
	 * Note that particularly the 24-bit codes take a rather long time
	 * for initialization and at least 1 GB RAM.
	 * 
	 * @param maxBits Maximal number of bits to consider.
	 */
	protected static void initArrayRI(int maxBits) {
		
		// bit encodings for which to calculate the LBP codes
		int[] bits = new int[]{8, 12, 16, 24};

		// initialize arrays
		codeArrayRI = new int[(int)Math.pow(2, 24)][4];
		codeDistributionRI = new int[(int)Math.pow(2, 24)][4];

		Vector<Integer> maxPos = new Vector<Integer>();
		int c, i, j, n, t, w, x, y; 
		int bitID, code, max, shift, shifter, zeroRunLength, patternCount;
		byte s;
		
		// iterate over all bit representations
		bitID = -1;
		for (int b: bits) {
			
			// do not calculate too much...
			if (b > maxBits)
				break;
			
			++bitID;

			// init some local variables
			byte[] array = new byte[b];
			byte[] shiftArray = new byte[b];
			byte[] bufferArray = new byte[b];
			int[] zeroRuns = new int[b];
			
			// calculate number of patterns
			patternCount = (int)Math.pow(2, b);
			
			// iterate over all numbers
			for (i=0; i<patternCount; ++i) {
				shifter = 1;

				// convert pattern to byte array
				for (s=0; s<b; ++s) {
					array[s] = (i & shifter) == Math.pow(2, s) ? (byte)1 : 0;
					shifter <<= 1;
				}			

				/* convert to unique representation by rotating code to 
				 * have a maximal number of leading zeros and a minimum value
				 */
				
				// determine zero runs
				for (w = 0; w<b; ++w) {
					zeroRuns[w] = 0;
					zeroRunLength = 0;
					for (c=0; c<b; ++c) {
						n = w - c;
						if (n < 0)
							n = b + n;
						if (array[n] == 0)
							++zeroRunLength;
						if (array[n] == 1) {
							zeroRunLength = 0;
							break;
						}
						zeroRuns[w] = zeroRunLength;
					}
				}
				
				// find longest run with minimal value
				maxPos.clear();
				max = 0;
				for (j=0; j<b; ++j)
					if (zeroRuns[j] > max)
						max = zeroRuns[j];
				for (j=0; j<b; ++j)
					if (zeroRuns[j] == max)
						maxPos.add(new Integer(j));

				code = 0;
				if (maxPos.size() == 1) {
					// unique position for longest zero run
					shift = b - 1 - maxPos.get(0).intValue();

					System.arraycopy( array, 0, shiftArray, 0, array.length );
					System.arraycopy( array, 0, bufferArray, 0, array.length );

					// shift array to unqiue representation
					for (x=0; x<shift; ++x) {
						shiftArray[0] = bufferArray[b-1];
						for (w = 1; w<b; ++w)
							shiftArray[w] = bufferArray[w-1];
						for (y=0; y<b; ++y) {
							bufferArray[y] = shiftArray[y];
						}
					}
					// calculate code
					t = 0;
					for (w = 0; w<b-1; ++w)
						t += shiftArray[w] * Math.pow(2, w);
					code = t;
				}
				else {
					// there are several positions with maximal zero runs,
					// search for minimal value
					code = Integer.MAX_VALUE;
					for (int m: maxPos) {
						shift = b - 1 - m;
						
						System.arraycopy( array, 0, shiftArray, 0, array.length );
						System.arraycopy( array, 0, bufferArray, 0, array.length );

						// shift array to unqiue representation
						for (x=0; x<shift; ++x) {
							shiftArray[0] = bufferArray[b-1];
							for (w = 1; w<b; ++w)
								shiftArray[w] = bufferArray[w-1];
							for (y=0; y<b; ++y) {
								bufferArray[y] = shiftArray[y];
							}
						}						
						// calculate code
						t = 0;
						for (w = 0; w<b; ++w)
							t += shiftArray[w] * Math.pow(2, w);
						// search for minimal code
						if (t < code)
							code = t;
					}
				}
				codeArrayRI[i][bitID] = code;

				// update code distribution
				codeDistributionRI[code][bitID]++;
			}
			int count = 0;
			for (i=0;i<patternCount;++i)
				if (codeDistributionRI[i][bitID] >= 1) {
					++count;
				}
			System.err.println("Found " + count + " combinations.");
		}
	}

	/**
	 * Initializes the array with rotation invariant and uniform codes.
	 * 
	 * @param maxBits Maximal number of bits to consider.
	 */
	protected static void initArrayRIU(int maxBits) {
		
		System.out.println("\n Initializing look-up table for " + maxBits);
		
		// bit encodings for which to calculate the LBP codes
		int[] bits = new int[]{8, 12, 16, 24};
		
		codeArrayRIU = new int[(int)Math.pow(2, 24)][4];

		// some local variables
		int bitID = -1, i, j, changes, ones, patternCount, shifter;
		byte s;
		byte[] array;
		
		// iterate over all bit numbers
		for (int b: bits) {

			// do not calculate too much...
			if (b > maxBits)
				break;

			++bitID;
			array = new byte[b];
			
			// calculate the number of possible bit patterns
			patternCount = (int)Math.pow(2, b);
			
			// calculate U for all seen patterns
			for (i=0; i<patternCount; ++i) {

				// reset variables
				changes = 0;
				shifter = 1;

				// convert to byte array
				for (s=0; s<b; ++s) {
					array[s] = (i & shifter) == Math.pow(2, s) ? (byte)1 : 0;
					shifter <<= 1;
				}			

				// count 0-1 and 1-0 changes in bit representation
				for (j=1; j<b;++j) {
					if (array[j] != array[j-1])
						++changes;
				}
				if (array[b-1] != array[0])
					++changes;

				if (changes > 2) {
					codeArrayRIU[i][bitID] = b + 1;					
				}
				else {
					// count ones in bit representation
					ones = 0;
					for (j=0; j<b;++j) {
						if (array[j] == 1)
							++ones;
					}
					codeArrayRIU[i][bitID] = ones;
				}
			}	
		}
//		for (i=0;i<200;++i) {
//			for (j=0; j<4; ++j) {
//				System.out.print(codeArrayRIU[i][j] + "\t");
//			}
//			System.out.println();
//		}
	}
	
	/**
	 * Test function.
	 * @param args	Command-line arguments.
	 */
	public static void main(String[] args) {
		int maxBits = 24;
		System.out.println("Initializing up to " + maxBits 
				+ "-bit LBP-RI codes...");
		initArrayRI(maxBits);
		System.out.println("Done!");
		System.out.println("Initializing up to " + maxBits 
				+ "-bit LBP-RIU codes...");
		initArrayRIU(maxBits);
		System.out.println("Done!");
		for (int i=0;i<200;++i) {
			System.out.print(i + " : ");
			for (int j=0; j<4; ++j) {
				System.out.print(codeArrayRI[i][j] + "\t");
			}
			for (int j=0; j<4; ++j) {
				System.out.print(codeArrayRIU[i][j] + "\t");
			}
			System.out.println();
		}
	}
}
