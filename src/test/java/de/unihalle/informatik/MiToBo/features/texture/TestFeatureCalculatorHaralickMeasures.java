/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.features.texture;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures.HaralickDirection;

/**
 * JUnit test class for {@link FeatureCalculatorHaralickMeasures}.
 * 
 * @author moeller
 */
public class TestFeatureCalculatorHaralickMeasures {

	/**
	 * Dummy image for tests.
	 */
	protected MTBImageByte dummyImage;
	
	/**
	 * Test operator.
	 */
	protected FeatureCalculatorHaralickMeasures haralickOp;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// create a test image...
		//
		// 0   1   2   3   4   5   6   7   8   9
		// 1   1   2   3   4   5   6   7   8   9
		// 2   2   4   6   8  10  12  14  16  18
		// 3   3   6   9  12  15  18  21  24  27
		// 4   4   8  12  16  20  24  28  32  36
		// 5   5  10  15  20  25  30  35  40  45
		// 6   6  12  18  24  30  36  42  48  54
		// 7   7  14  21  28  35  42  49  56  63
		// 8   8  16  24  32  40  48  56  64  72
		// 9   9  18  27  36  45  54  63  72  81
		//
		this.dummyImage = (MTBImageByte)MTBImage.createMTBImage(10, 10, 1, 1, 1, 
																												MTBImageType.MTB_BYTE);
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (x == 0)
					this.dummyImage.putValueInt(x, y, y);
				else if (y == 0)
					this.dummyImage.putValueInt(x, y, x);
				else
					this.dummyImage.putValueInt(x, y, x*y);
			}
		}
		// init operator object
		try {
			this.haralickOp = new FeatureCalculatorHaralickMeasures();
		} catch (ALDOperatorException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Test if matrix extraction is correct.
	 * @throws ALDOperatorException 
	 */
	@Test
	public void testMatrixExtraction() throws ALDOperatorException {
		this.haralickOp.setParameter("inImg", this.dummyImage);
		this.haralickOp.setParameter("distance", new Integer(1));
		double [][] matrix = this.haralickOp.calcMatrix(HaralickDirection.EAST);
		
		// Result matrix info:
		// - matrix contains 74 entries equal to one
		// - matrix contains  8 entries equal to two
		// - 9 elements on diagonal equal one, rest is zero
		// - lower triangular matrix is completely zero
		
		// check entries in row 0
		assertTrue("Wrong entry at position [0][0]..." + matrix[0][0],
				Math.abs(matrix[0][0]) < 10e-8);
		assertTrue("Wrong entry at position [0][1]..." + matrix[0][1],
				Math.abs(matrix[0][1] - 1.0/90) < 10e-8);
		for (int x=2; x<256; ++x) {
			assertTrue("Wrong entry at position [0][" + x + "]..." + matrix[0][x],
					Math.abs(matrix[0][x]) < 10e-8);
		}
		// check entries in row 1
		for (int x=0; x<256; ++x) {
			if (x == 1)
				assertTrue("Wrong entry at position [1][" + x + "]..." + matrix[1][x],
						Math.abs(matrix[1][x] - 1.0/90) < 10e-8);
			else if (x == 2)
				assertTrue("Wrong entry at position [1][" + x + "]..." + matrix[1][x],
						Math.abs(matrix[1][x] - 2.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [1][" + x + "]..." + matrix[1][x],
						Math.abs(matrix[1][x]) < 10e-8);
		}
		// check entries in row 2
		for (int x=0; x<256; ++x) {
			if (x == 2)
				assertTrue("Wrong entry at position [2][" + x + "]..." + matrix[2][x],
						Math.abs(matrix[2][x] - 1.0/90) < 10e-8);
			else if (x == 3)
				assertTrue("Wrong entry at position [2][" + x + "]..." + matrix[2][x],
						Math.abs(matrix[2][x] - 2.0/90) < 10e-8);
			else if (x == 4)
				assertTrue("Wrong entry at position [2][" + x + "]..." + matrix[2][x],
						Math.abs(matrix[2][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [2][" + x + "]..." + matrix[2][x],
						Math.abs(matrix[2][x]) < 10e-8);
		}
		// check entries in row 3
		for (int x=0; x<256; ++x) {
			if (x == 3)
				assertTrue("Wrong entry at position [3][" + x + "]..." + matrix[3][x],
						Math.abs(matrix[3][x] - 1.0/90) < 10e-8);
			else if (x == 4)
				assertTrue("Wrong entry at position [3][" + x + "]..." + matrix[3][x],
						Math.abs(matrix[3][x] - 2.0/90) < 10e-8);
			else if (x == 6)
				assertTrue("Wrong entry at position [3][" + x + "]..." + matrix[3][x],
						Math.abs(matrix[3][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [3][" + x + "]..." + matrix[3][x],
						Math.abs(matrix[3][x]) < 10e-8);
		}
		// check entries in row 4
		for (int x=0; x<256; ++x) {
			if (x == 4)
				assertTrue("Wrong entry at position [4][" + x + "]..." + matrix[4][x],
						Math.abs(matrix[4][x] - 1.0/90) < 10e-8);
			else if (x == 5)
				assertTrue("Wrong entry at position [4][" + x + "]..." + matrix[4][x],
						Math.abs(matrix[4][x] - 2.0/90) < 10e-8);
			else if (x == 6)
				assertTrue("Wrong entry at position [4][" + x + "]..." + matrix[4][x],
						Math.abs(matrix[4][x] - 1.0/90) < 10e-8);
			else if (x == 8)
				assertTrue("Wrong entry at position [4][" + x + "]..." + matrix[4][x],
						Math.abs(matrix[4][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [4][" + x + "]..." + matrix[4][x],
						Math.abs(matrix[4][x]) < 10e-8);
		}
		// check entries in row 5
		for (int x=0; x<256; ++x) {
			if (x == 5)
				assertTrue("Wrong entry at position [5][" + x + "]..." + matrix[5][x],
						Math.abs(matrix[5][x] - 1.0/90) < 10e-8);
			else if (x == 6)
				assertTrue("Wrong entry at position [5][" + x + "]..." + matrix[5][x],
						Math.abs(matrix[5][x] - 2.0/90) < 10e-8);
			else if (x == 10)
				assertTrue("Wrong entry at position [5][" + x + "]..." + matrix[5][x],
						Math.abs(matrix[5][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [5][" + x + "]..." + matrix[5][x],
						Math.abs(matrix[5][x]) < 10e-8);
		}
		// check entries in row 6
		for (int x=0; x<256; ++x) {
			if (x == 6)
				assertTrue("Wrong entry at position [6][" + x + "]..." + matrix[6][x],
						Math.abs(matrix[6][x] - 1.0/90) < 10e-8);
			else if (x == 7)
				assertTrue("Wrong entry at position [6][" + x + "]..." + matrix[6][x],
						Math.abs(matrix[6][x] - 2.0/90) < 10e-8);
			else if (x == 8)
				assertTrue("Wrong entry at position [6][" + x + "]..." + matrix[6][x],
						Math.abs(matrix[6][x] - 1.0/90) < 10e-8);
			else if (x == 9)
				assertTrue("Wrong entry at position [6][" + x + "]..." + matrix[6][x],
						Math.abs(matrix[6][x] - 1.0/90) < 10e-8);
			else if (x == 12)
				assertTrue("Wrong entry at position [6][" + x + "]..." + matrix[6][x],
						Math.abs(matrix[6][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [6][" + x + "]..." + matrix[6][x],
						Math.abs(matrix[6][x]) < 10e-8);
		}
		// check entries in row 7
		for (int x=0; x<256; ++x) {
			if (x == 7)
				assertTrue("Wrong entry at position [7][" + x + "]..." + matrix[7][x],
						Math.abs(matrix[7][x] - 1.0/90) < 10e-8);
			else if (x == 8)
				assertTrue("Wrong entry at position [7][" + x + "]..." + matrix[7][x],
						Math.abs(matrix[7][x] - 2.0/90) < 10e-8);
			else if (x == 14)
				assertTrue("Wrong entry at position [7][" + x + "]..." + matrix[7][x],
						Math.abs(matrix[7][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [7][" + x + "]..." + matrix[7][x],
						Math.abs(matrix[7][x]) < 10e-8);
		}
		// check entries in row 8
		for (int x=0; x<256; ++x) {
			if (x == 8)
				assertTrue("Wrong entry at position [8][" + x + "]..." + matrix[8][x],
						Math.abs(matrix[8][x] - 1.0/90) < 10e-8);
			else if (x == 9)
				assertTrue("Wrong entry at position [8][" + x + "]..." + matrix[8][x],
						Math.abs(matrix[8][x] - 2.0/90) < 10e-8);
			else if (x == 10)
				assertTrue("Wrong entry at position [8][" + x + "]..." + matrix[8][x],
						Math.abs(matrix[8][x] - 1.0/90) < 10e-8);
			else if (x == 12)
				assertTrue("Wrong entry at position [8][" + x + "]..." + matrix[8][x],
						Math.abs(matrix[8][x] - 1.0/90) < 10e-8);
			else if (x == 16)
				assertTrue("Wrong entry at position [8][" + x + "]..." + matrix[8][x],
						Math.abs(matrix[8][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [8][" + x + "]..." + matrix[8][x],
						Math.abs(matrix[8][x]) < 10e-8);
		}
		// check entries in row 9
		for (int x=0; x<256; ++x) {
			if (x == 9)
				assertTrue("Wrong entry at position [9][" + x + "]..." + matrix[9][x],
						Math.abs(matrix[9][x] - 1.0/90) < 10e-8);
			else if (x == 12)
				assertTrue("Wrong entry at position [9][" + x + "]..." + matrix[9][x],
						Math.abs(matrix[9][x] - 1.0/90) < 10e-8);
			else if (x == 18)
				assertTrue("Wrong entry at position [9][" + x + "]..." + matrix[9][x],
						Math.abs(matrix[9][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [9][" + x + "]..." + matrix[9][x],
						Math.abs(matrix[9][x]) < 10e-8);
		}
		// check entries in row 10
		for (int x=0; x<256; ++x) {
			if (x == 12)
				assertTrue("Wrong entry at position [10][" + x + "]..." + matrix[10][x],
						Math.abs(matrix[10][x] - 1.0/90) < 10e-8);
			else if (x == 15)
				assertTrue("Wrong entry at position [10][" + x + "]..." + matrix[10][x],
						Math.abs(matrix[10][x] - 1.0/90) < 10e-8);
			else
				assertTrue("Wrong entry at position [10][" + x + "]..." + matrix[10][x],
						Math.abs(matrix[10][x]) < 10e-8);
		}
		// lower triangular matrix is completely zero
		for (int y=0; y<256; ++y) {
			for (int x=0; x<y; ++x) {
				assertTrue("Wrong entry at position ["+y+"]["+x+"]..." + matrix[y][x],
						Math.abs(matrix[y][x]) < 10e-8);
			}
		}
		// up to here first ten rows are ok, now just check if all fields that
		// are to be unequal to zero are unequal to zero...
		assertTrue("Wrong entry at position [12][14]..." + matrix[12][14],
				Math.abs(matrix[12][14] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [12][15]..." + matrix[12][15],
				Math.abs(matrix[12][15] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [12][16]..." + matrix[12][16],
				Math.abs(matrix[12][16] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [12][18]..." + matrix[12][18],
				Math.abs(matrix[12][18] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [14][16]..." + matrix[14][16],
				Math.abs(matrix[14][16] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [14][21]..." + matrix[14][21],
				Math.abs(matrix[14][21] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [15][18]..." + matrix[15][18],
				Math.abs(matrix[15][18] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [15][20]..." + matrix[15][20],
				Math.abs(matrix[15][20] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [16][18]..." + matrix[16][18],
				Math.abs(matrix[16][18] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [16][20]..." + matrix[16][20],
				Math.abs(matrix[16][20] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [16][24]..." + matrix[16][24],
				Math.abs(matrix[16][24] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [18][21]..." + matrix[18][21],
				Math.abs(matrix[18][21] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [18][24]..." + matrix[18][24],
				Math.abs(matrix[18][24] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [18][27]..." + matrix[18][27],
				Math.abs(matrix[18][27] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [20][24]..." + matrix[20][24],
				Math.abs(matrix[20][24] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [20][25]..." + matrix[20][25],
				Math.abs(matrix[20][25] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [21][24]..." + matrix[21][24],
				Math.abs(matrix[21][24] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [21][28]..." + matrix[21][28],
				Math.abs(matrix[21][28] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [24][27]..." + matrix[24][27],
				Math.abs(matrix[24][27] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [24][28]..." + matrix[24][28],
				Math.abs(matrix[24][28] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [24][30]..." + matrix[24][30],
				Math.abs(matrix[24][30] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [24][32]..." + matrix[24][32],
				Math.abs(matrix[24][32] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [25][30]..." + matrix[25][30],
				Math.abs(matrix[25][30] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [27][36]..." + matrix[27][36],
				Math.abs(matrix[27][36] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [28][32]..." + matrix[28][32],
				Math.abs(matrix[28][32] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [28][35]..." + matrix[28][35],
				Math.abs(matrix[28][35] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [30][35]..." + matrix[30][35],
				Math.abs(matrix[30][35] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [30][36]..." + matrix[30][36],
				Math.abs(matrix[30][36] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [32][36]..." + matrix[32][36],
				Math.abs(matrix[32][36] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [32][40]..." + matrix[32][40],
				Math.abs(matrix[32][40] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [35][40]..." + matrix[35][40],
				Math.abs(matrix[35][40] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [35][42]..." + matrix[35][42],
				Math.abs(matrix[35][42] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [36][42]..." + matrix[36][42],
				Math.abs(matrix[36][42] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [36][45]..." + matrix[36][45],
				Math.abs(matrix[36][45] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [40][45]..." + matrix[40][45],
				Math.abs(matrix[40][45] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [40][48]..." + matrix[40][48],
				Math.abs(matrix[40][48] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [42][48]..." + matrix[42][48],
				Math.abs(matrix[42][48] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [42][49]..." + matrix[42][49],
				Math.abs(matrix[42][49] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [45][54]..." + matrix[45][54],
				Math.abs(matrix[45][54] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [48][54]..." + matrix[48][54],
				Math.abs(matrix[48][54] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [48][56]..." + matrix[48][56],
				Math.abs(matrix[48][56] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [49][56]..." + matrix[49][56],
				Math.abs(matrix[49][56] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [54][63]..." + matrix[54][63],
				Math.abs(matrix[54][63] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [56][63]..." + matrix[56][63],
				Math.abs(matrix[56][63] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [56][64]..." + matrix[56][64],
				Math.abs(matrix[56][64] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [63][72]..." + matrix[63][72],
				Math.abs(matrix[63][72] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [64][72]..." + matrix[64][72],
				Math.abs(matrix[64][72] - 1.0/90) < 10e-8);
		assertTrue("Wrong entry at position [72][81]..." + matrix[72][81],
				Math.abs(matrix[72][81] - 1.0/90) < 10e-8);
		// rows >= index 82 are all zero
		for (int y=82;y<256;++y) 
			for (int x=0;x<256;++x) 
				assertTrue("Wrong entry at position [" + y + "][" + x + "]..." 
						+ matrix[y][x], Math.abs(matrix[y][x]) < 10e-8);
	}

	/**
	 * Test if feature calculations are correct.
	 * @throws ALDOperatorException 
	 */
	@Test
	public void testStatisticalCalcs() throws ALDOperatorException {
		this.haralickOp.setParameter("inImg", this.dummyImage);
		this.haralickOp.setParameter("distance", new Integer(1));
		double [][] matrix = this.haralickOp.calcMatrix(HaralickDirection.EAST);
		double [] features = this.haralickOp.calcFeatures(matrix);
		
		assertTrue("Homogenity should be " + (74.0/8100.0 + 32.0/8100.0),
			Math.abs(features[0] - (74.0/8100.0 + 32.0/8100.0)) < 10e-8);
		double expectedContrast = 16.0/90.0 + 1.0/90.0 + 284.0 * 8.0/90.0; 
		assertTrue("Contrast should be " + expectedContrast,
			Math.abs(features[1] - expectedContrast) < 10e-8);
		double localHomogenity = 21.2751346578893/90.0;
		assertTrue("Local homogenity should be " + localHomogenity,
			Math.abs(features[2] - localHomogenity) < 10e-8);
		double expectedEntropy = 
				-8.0*2.0/90.0*Math.log10(2.0/90.0) - 74.0*1.0/90.0*Math.log10(1.0/90.0);
			assertTrue("Entropy should be " + expectedEntropy,
				Math.abs(features[3] - expectedEntropy) < 10e-8);
		double correlation = 0.00298791749209032;
		assertTrue("Correlation should be " + correlation,
			Math.abs(features[4] - correlation) < 10e-8);
		double autoCorrelation = 68925.0/90.0;
		assertTrue("Auto-correlation should be " + autoCorrelation,
			Math.abs(features[5] - autoCorrelation) < 10e-8);
		double dissimilarity = 369.0/90.0;
		assertTrue("Dissimilarity should be " + dissimilarity,
			Math.abs(features[6] - dissimilarity) < 10e-8);
		double clusterShade = 4732834.32000000/90.0;
		assertTrue("Cluster shade should be " + clusterShade,
			Math.abs(features[7] - clusterShade) < 10e-8);
		double clusterProminence = 533458943.673000/90.0;
		assertTrue("Cluster prominence should be " + clusterProminence,
			Math.abs(features[8] - clusterProminence) < 10e-8);
		assertTrue("Max. probability should be " + 2.0/90,
			Math.abs(features[9] - 2.0/90) < 10e-8);

		// check if handling of list with directions works as expected 
		this.haralickOp.setParameter("inImg", this.dummyImage);
		this.haralickOp.setParameter("distance", new Integer(1));
		Vector<HaralickDirection> dirs = 
			new Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
		dirs.add(HaralickDirection.EAST);
		this.haralickOp.setDirections(dirs);
		boolean exceptionThrown = false;
		try {
	    this.haralickOp.runOp();
    } catch (ALDProcessingDAGException e) {
    	exceptionThrown = true;
    }
		assertFalse("Call to operate caused an exception!", exceptionThrown);
		FeatureCalculatorHaralickMeasuresResult result = 
			(FeatureCalculatorHaralickMeasuresResult)this.haralickOp.getResultData();
		assertTrue("Expected dimensionality of 10,  got " 
			+ result.getDimensionality(), result.getDimensionality() == 10);
		assertTrue("Feature 0 should be Homogenity, got " 
			+ result.getResultIdentifier(0), 
			result.getResultIdentifier(0).equals("Homogenity"));
		assertTrue("Feature 1 should be Contrast, got " 
				+ result.getResultIdentifier(1), 
				result.getResultIdentifier(1).equals("Contrast"));
		assertTrue("Feature 2 should be Local_Homogenity, got " 
				+ result.getResultIdentifier(2), 
				result.getResultIdentifier(2).equals("Local_Homogenity"));
		assertTrue("Feature 3 should be Entropy, got " 
				+ result.getResultIdentifier(3), 
				result.getResultIdentifier(3).equals("Entropy"));
		assertTrue("Feature 4 should be Correlation, got " 
				+ result.getResultIdentifier(4), 
				result.getResultIdentifier(4).equals("Correlation"));
		assertTrue("Feature 5 should be Auto_Correlation, got " 
				+ result.getResultIdentifier(5), 
				result.getResultIdentifier(5).equals("Auto_Correlation"));
		assertTrue("Feature 6 should be Dissimilarity, got " 
				+ result.getResultIdentifier(6), 
				result.getResultIdentifier(6).equals("Dissimilarity"));
		assertTrue("Feature 7 should be Cluster_Shade, got " 
				+ result.getResultIdentifier(7), 
				result.getResultIdentifier(7).equals("Cluster_Shade"));
		assertTrue("Feature 8 should be Cluster_Prominence, got " 
				+ result.getResultIdentifier(8), 
				result.getResultIdentifier(8).equals("Cluster_Prominence"));
		assertTrue("Feature 9 should be Maximum_Probability, got " 
				+ result.getResultIdentifier(9), 
				result.getResultIdentifier(9).equals("Maximum_Probability"));
		
		features = (double[])result.getResult();
		assertTrue("Homogenity should be " + (74.0/8100.0 + 32.0/8100.0),
			Math.abs(features[0] - (74.0/8100.0 + 32.0/8100.0)) < 10e-8);
		assertTrue("Contrast should be " + expectedContrast,
			Math.abs(features[1] - expectedContrast) < 10e-8);
		assertTrue("Local homogenity should be " + localHomogenity,
			Math.abs(features[2] - localHomogenity) < 10e-8);
		assertTrue("Entropy should be " + expectedEntropy,
			Math.abs(features[3] - expectedEntropy) < 10e-8);
		assertTrue("Correlation should be " + correlation,
			Math.abs(features[4] - correlation) < 10e-8);
		assertTrue("Auto-correlation should be " + autoCorrelation,
			Math.abs(features[5] - autoCorrelation) < 10e-8);
		assertTrue("Dissimilarity should be " + dissimilarity,
			Math.abs(features[6] - dissimilarity) < 10e-8);
		assertTrue("Cluster shade should be " + clusterShade,
			Math.abs(features[7] - clusterShade) < 10e-8);
		assertTrue("Cluster prominence should be " + clusterProminence,
			Math.abs(features[8] - clusterProminence) < 10e-8);
		assertTrue("Max. probability should be " + 2.0/90,
			Math.abs(features[9] - 2.0/90) < 10e-8);

		// check if handling of list with directions works as expected
		this.haralickOp.setParameter("inImg", this.dummyImage);
		this.haralickOp.setParameter("distance", new Integer(1));
		dirs = new Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
		dirs.add(HaralickDirection.EAST);
		dirs.add(HaralickDirection.EAST);
		this.haralickOp.setDirections(dirs);
		exceptionThrown = false;
		try {
	    this.haralickOp.runOp();
    } catch (ALDProcessingDAGException e) {
    	exceptionThrown = true;
    }
		assertFalse("Call to operate caused an exception!", exceptionThrown);
		result = 
			(FeatureCalculatorHaralickMeasuresResult)this.haralickOp.getResultData();
		assertTrue("Expected dimensionality of 20,  got " 
				+ result.getDimensionality(), result.getDimensionality() == 20);
		assertTrue("Feature 0 should be Homogenity, got " 
				+ result.getResultIdentifier(0), 
				result.getResultIdentifier(0).equals("Homogenity"));
		assertTrue("Feature 1 should be Contrast, got " 
				+ result.getResultIdentifier(1), 
				result.getResultIdentifier(1).equals("Contrast"));
		assertTrue("Feature 2 should be Local_Homogenity, got " 
				+ result.getResultIdentifier(2), 
				result.getResultIdentifier(2).equals("Local_Homogenity"));
		assertTrue("Feature 3 should be Entropy, got " 
				+ result.getResultIdentifier(3), 
				result.getResultIdentifier(3).equals("Entropy"));
		assertTrue("Feature 4 should be Correlation, got " 
				+ result.getResultIdentifier(4), 
				result.getResultIdentifier(4).equals("Correlation"));
		assertTrue("Feature 5 should be Auto_Correlation, got " 
				+ result.getResultIdentifier(5), 
				result.getResultIdentifier(5).equals("Auto_Correlation"));
		assertTrue("Feature 6 should be Dissimilarity, got " 
				+ result.getResultIdentifier(6), 
				result.getResultIdentifier(6).equals("Dissimilarity"));
		assertTrue("Feature 7 should be Cluster_Shade, got " 
				+ result.getResultIdentifier(7), 
				result.getResultIdentifier(7).equals("Cluster_Shade"));
		assertTrue("Feature 8 should be Cluster_Prominence, got " 
				+ result.getResultIdentifier(8), 
				result.getResultIdentifier(8).equals("Cluster_Prominence"));
		assertTrue("Feature 9 should be Maximum_Probability, got " 
				+ result.getResultIdentifier(9), 
				result.getResultIdentifier(9).equals("Maximum_Probability"));
		assertTrue("Feature 10 should be Homogenity, got " 
				+ result.getResultIdentifier(10), 
				result.getResultIdentifier(10).equals("Homogenity"));
		assertTrue("Feature 11 should be Contrast, got " 
				+ result.getResultIdentifier(11), 
				result.getResultIdentifier(11).equals("Contrast"));
		assertTrue("Feature 12 should be Local_Homogenity, got " 
				+ result.getResultIdentifier(12), 
				result.getResultIdentifier(12).equals("Local_Homogenity"));
		assertTrue("Feature 13 should be Entropy, got " 
				+ result.getResultIdentifier(13), 
				result.getResultIdentifier(13).equals("Entropy"));
		assertTrue("Feature 14 should be Correlation, got " 
				+ result.getResultIdentifier(14), 
				result.getResultIdentifier(14).equals("Correlation"));
		assertTrue("Feature 15 should be Auto_Correlation, got " 
				+ result.getResultIdentifier(15), 
				result.getResultIdentifier(15).equals("Auto_Correlation"));
		assertTrue("Feature 16 should be Dissimilarity, got " 
				+ result.getResultIdentifier(16), 
				result.getResultIdentifier(16).equals("Dissimilarity"));
		assertTrue("Feature 17 should be Cluster_Shade, got " 
				+ result.getResultIdentifier(17), 
				result.getResultIdentifier(17).equals("Cluster_Shade"));
		assertTrue("Feature 18 should be Cluster_Prominence, got " 
				+ result.getResultIdentifier(18), 
				result.getResultIdentifier(18).equals("Cluster_Prominence"));
		assertTrue("Feature 19 should be Maximum_Probability, got " 
				+ result.getResultIdentifier(19), 
				result.getResultIdentifier(19).equals("Maximum_Probability"));

		features = (double[])result.getResult();
		assertTrue("Homogenity should be " + (74.0/8100.0 + 32.0/8100.0),
			Math.abs(features[0] - (74.0/8100.0 + 32.0/8100.0)) < 10e-8);
		assertTrue("Contrast should be " + expectedContrast,
			Math.abs(features[1] - expectedContrast) < 10e-8);
		assertTrue("Local homogenity should be " + localHomogenity,
			Math.abs(features[2] - localHomogenity) < 10e-8);
		assertTrue("Entropy should be " + expectedEntropy,
			Math.abs(features[3] - expectedEntropy) < 10e-8);
		assertTrue("Correlation should be " + correlation,
			Math.abs(features[4] - correlation) < 10e-8);
		assertTrue("Auto-correlation should be " + autoCorrelation,
			Math.abs(features[5] - autoCorrelation) < 10e-8);
		assertTrue("Dissimilarity should be " + dissimilarity,
			Math.abs(features[6] - dissimilarity) < 10e-8);
		assertTrue("Cluster shade should be " + clusterShade,
			Math.abs(features[7] - clusterShade) < 10e-8);
		assertTrue("Cluster prominence should be " + clusterProminence,
			Math.abs(features[8] - clusterProminence) < 10e-8);
		assertTrue("Max. probability should be " + 2.0/90,
			Math.abs(features[9] - 2.0/90) < 10e-8);
		
		assertTrue("Length of feature vector is expected to be 20!",
			features.length == 20);
		for (int i=0; i<features.length/2; ++i)
			assertTrue("Expecting elements " + i + " and " + (i+features.length) 
				+ " to be equal...!?", 
				Math.abs(features[i] - features[i+features.length/2]) < 10e-8);
	}
}