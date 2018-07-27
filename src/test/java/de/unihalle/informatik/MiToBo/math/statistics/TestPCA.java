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

package de.unihalle.informatik.MiToBo.math.statistics;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.math.statistics.PCA.ReductionMode;

/**
 * JUnit test class for {@link PCA}.
 * 
 * @author Birgit Moeller
 */
public class TestPCA {

	/**
	 * Numerical accuracy for tests.
	 */
	private final static double accuracy = 1.0e-4;

	/*
	 * You can find the definitions of the test data at the end of this file.
	 */
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}

	/**
	 * Test PCA calculations if sample count is larger than data dimensionality.
	 */
	@Test
	public void testPCA_N_larger_D() {
		
		/*
		 * The test data used here can be found at the end of the file.
		 */
		
		PCA pcaOp = null;
		
		boolean exceptionThrown = false;
		try {
	    pcaOp = new PCA();
    } catch (ALDOperatorException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] constructor threw an exception!?", exceptionThrown);
		if (pcaOp == null)
			return;
		
		pcaOp.setDataset(this.testData);
		pcaOp.setReductionMode(ReductionMode.NUMBER_COMPONENTS);
		pcaOp.setNumberOfComponents(5);
		pcaOp.setMeanFreeData(false);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check global variables
		assertTrue("[TestPCA] sample size is 20, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 20);
		assertTrue("[TestPCA] data dimension is 10, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 10);
		assertTrue("[TestPCA] subspace dimension should be 5, got " + pcaOp.subDim, 
			pcaOp.subDim == 5);
		
		// check calculation of mean vector
		for (int i=0; i<10; ++i) {
			assertTrue("[TestPCA] got unexpected value in mean[" + i + "] = " 
				+  pcaOp.mean[i] + ", expected " + this.testMean[i],
					Math.abs(pcaOp.mean[i] - this.testMean[i]) < TestPCA.accuracy); 
		}
		// check calculation of mean-free data
		for (int i=0; i<10; ++i) {
			for (int j=0; j<20; ++j) {
				assertTrue("[TestPCA] got unexpected value in mean-free data[" 
					+ i + "][" + j + "]= " + pcaOp.meanfreeData[i][j] 
						+  ", expected " + this.testDataMeanFree[i][j],
								Math.abs(pcaOp.meanfreeData[i][j] - this.testDataMeanFree[i][j]) 
							< TestPCA.accuracy); 
			}
		}
		// check calculation of covariance matrix
		for (int i=0; i<10; ++i) {
			for (int j=0; j<10; ++j) {
				assertTrue("[TestPCA] got unexpected value in C["	+ i + "][" + j + "]= " 
					+ pcaOp.C.get(i,j) +  ", expected " + this.C[i][j],
					Math.abs(pcaOp.C.get(i,j) - this.C[i][j]) < TestPCA.accuracy); 
			}
		}
		// check eigenvalues
		for (int i=0; i<10; ++i) {
			assertTrue("[TestPCA] got unexpected eigenvalue in mean[" + i + "] = " 
				+  pcaOp.eigenVals[i] + ", expected " + this.eigenvals[i],
					Math.abs(pcaOp.eigenVals[i] - this.eigenvals[i]) < TestPCA.accuracy); 
		}
		// check eigenvectors column-wise
		double sign = 1.0;
		String signString = "";
		for (int j=0; j<10; ++j) {
			if (  Math.abs(pcaOp.eigenVects.get(0,j)*(-1) - this.eigenvects[0][j])
					< TestPCA.accuracy) {
				sign = -1.0;
				signString = "-";
			}
			else {
				sign = 1.0;
				signString = "";
			}
			for (int i=0; i<10; ++i) {
				assertTrue("[TestPCA] got unexpected eigenvector in V["+i+"]["+j+"]= "
					+ signString
						+ pcaOp.eigenVects.get(i,j) + ", expected " + this.eigenvects[i][j],
								Math.abs(pcaOp.eigenVects.get(i,j)*sign - this.eigenvects[i][j]) 
							< TestPCA.accuracy); 
			}
		}
		// check projection matrix
		for (int i=0; i<5; ++i) {
			if (  Math.abs(pcaOp.P_t.get(i,0)*(-1) - this.projectMatrixTrans_05[i][0])
					< TestPCA.accuracy) {
				sign = -1.0;
				signString = "-";
			}
			else {
				sign = 1.0;
				signString = "";
			}
			for (int j=0; j<10; ++j) {
				assertTrue("[TestPCA] got unexpected value in P^T["+i+"]["+j+"]= " 
					+ signString + pcaOp.P_t.get(i,j) + ", expected " 
						+ this.projectMatrixTrans_05[i][j],
							Math.abs(pcaOp.P_t.get(i,j)*sign-this.projectMatrixTrans_05[i][j]) 
						< TestPCA.accuracy); 
			}
		}
		// check sub-space data by transforming the data
		double[][] resultData = pcaOp.getResultData();
		double[][] normedProjectionMatrix = new double[5][10];
		for (int i=0; i<5; ++i) {
			if (  Math.abs(this.projectMatrixTrans_05[i][0]*(-1) - pcaOp.P_t.get(i,0))
					< TestPCA.accuracy) {
				for (int j=0; j<10; ++j) {
					normedProjectionMatrix[i][j] = this.projectMatrixTrans_05[i][j]*(-1);
				}
			}
			else {
				for (int j=0; j<10; ++j) {
					normedProjectionMatrix[i][j] = this.projectMatrixTrans_05[i][j];
				}
			}
		}
		double[][] transformedData = new double[5][20];
		for (int j=0; j<20; ++j) {
			for (int i=0; i<5; ++i) {
				double sum=0;
				for (int k=0; k<10; ++k)
					sum+= this.testDataMeanFree[k][j]*normedProjectionMatrix[i][k];
				transformedData[i][j] = sum;
			}
		}
		for (int j=0; j<20; ++j) {
			for (int i=0; i<5; ++i) {
				assertTrue("[TestPCA] got unexpected data item at ["+i+"]["+j+"]= " 
					+ resultData[i][j] + ", expected " 
						+ transformedData[i][j],
								Math.abs(resultData[i][j] - transformedData[i][j]) 
							< TestPCA.accuracy); 
			}
		}

		// change the mode how to determine sub-space dimensionality
		pcaOp.setReductionMode(ReductionMode.PERCENTAGE_VARIANCE);
		pcaOp.setMeanFreeData(false);
		pcaOp.setPercentageOfVariance(0.65);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check if correct dimensionality was determined
		assertTrue("[TestPCA] expected a dimensionality of 4, got " + pcaOp.subDim,
			pcaOp.subDim == 4);
		// check if correct subset of eigenvectors is selected
		for (int i=0; i<4; ++i) {
			if (  Math.abs(pcaOp.P_t.get(i,0)*(-1) - this.projectMatrixTrans_05[i][0])
					< TestPCA.accuracy) {
				sign = -1.0;
				signString = "-";
			}
			else {
				sign = 1.0;
				signString = "";
			}
			for (int j=0; j<10; ++j) {
				assertTrue("[TestPCA] got unexpected value in P^T["+i+"]["+j+"]= " 
					+ signString + pcaOp.P_t.get(i,j) + ", expected " 
						+ this.projectMatrixTrans_05[i][j],
							Math.abs(pcaOp.P_t.get(i,j)*sign-this.projectMatrixTrans_05[i][j]) 
						< TestPCA.accuracy); 
			}
		}

		pcaOp.setPercentageOfVariance(0.95);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check if correct dimensionality was determined
		assertTrue("[TestPCA] expected a dimensionality of 8, got " + pcaOp.subDim,
			pcaOp.subDim == 8);
	}
	
	/**
	 * Test PCA calculations if dimensionality exceeds sample count.
	 */
	@Test
	public void testPCA_D_larger_N() {
		
		PCA pcaOp = null;
		
		boolean exceptionThrown = false;
		try {
	    pcaOp = new PCA();
    } catch (ALDOperatorException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] constructor threw an exception!?", exceptionThrown);
		if (pcaOp == null)
			return;

		// now, use data where dimension is larger than sample count, i.e., use
		// accelerated calculations
		double[][] testDataD_larger_N = new double[][]{
			{	1,    8,   12},
			{13,    8,    5},
			{ 2,   10,   18},
			{ 8,    9,   17},
			{ 7,    3,   14}};

		pcaOp.setDataset(testDataD_larger_N);
		pcaOp.setReductionMode(ReductionMode.NUMBER_COMPONENTS);
		pcaOp.setNumberOfComponents(2);
		pcaOp.setMeanFreeData(false);
		
		// determine number of samples and sample dimension
		pcaOp.examineDataset();
		assertTrue("[TestPCA] sample size is 3, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 3);
		assertTrue("[TestPCA] data dimension is 5, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 5);

		// check calculation of mean-free data
		pcaOp.calculateMeanFreeData();
		double[] mean = new double[]{7.0000, 8.6667, 10.0000, 11.3333, 8.0000};

		for (int i=0; i<5; ++i) {
			assertTrue("[TestPCA] got unexpected value in mean[" + i + "] = " 
				+  pcaOp.mean[i] + ", expected " + mean[i],
					Math.abs(pcaOp.mean[i] - mean[i]) < TestPCA.accuracy); 
		}
		// check calculation of mean-free data
		double[][] meanFreeData = new double[][]{
				{-6.00000,   1.00000,   5.00000},
				{ 4.33333,  -0.66667,  -3.66667},
				{-8.00000,   0.00000,   8.00000},
				{-3.33333,  -2.33333,   5.66667},
				{-1.00000,  -5.00000,   6.00000}};
		for (int i=0; i<5; ++i) {
			for (int j=0; j<3; ++j) {
				assertTrue("[TestPCA] got unexpected value in mean-free data[" 
					+ i + "][" + j + "]= " + pcaOp.meanfreeData[i][j] 
						+  ", expected " + meanFreeData[i][j],
								Math.abs(pcaOp.meanfreeData[i][j] - meanFreeData[i][j]) 
							< TestPCA.accuracy); 
			}
		}

		// check calculation of covariance matrix, eigenvalues and -vectors
		pcaOp.calculateCovarianceMatrixAndEigenstuff();
		
		double[][] covMatrix = {{ 130.8889,		3.8889,  -134.7778},
														{   3.8889,  31.8889,   -35.7778},
														{-134.7778, -35.7778,   170.5556}};

		for (int i=0; i<3; ++i) {
			for (int j=0; j<3; ++j) {
				assertTrue("[TestPCA] got unexpected value in C["	+ i + "][" + j + "]= " 
					+ pcaOp.C.get(i,j) +  ", expected " + covMatrix[i][j],
						Math.abs(pcaOp.C.get(i,j) - covMatrix[i][j]) < TestPCA.accuracy); 
			}
		}
		// check eigenvalues
		double[] eigVals = new double[]{
			2.10538593122572e-14, 4.29676592153357e+01, 2.90365674117998e+02};
		for (int i=0; i<3; ++i) {
			assertTrue("[TestPCA] got unexpected eigenvalue in mean[" + i + "] = " 
				+  pcaOp.eigenVals[i] + ", expected " + eigVals[i],
					Math.abs(pcaOp.eigenVals[i] - eigVals[i]) < TestPCA.accuracy); 
		}
		// check eigenvectors column-wise
		double[][] eigVects = new double[][]{
			{0.0,  -0.351405960935564,   0.441871767570761},
			{0.0,   0.244356114285587,  -0.321972174712668},
			{0.0,  -0.242051367885063,   0.657383911971846},
			{0.0,   0.295502611765346,   0.393297684940557},
			{0.0,   0.819080754123200,   0.338003821037097}};
		double sign = 1.0;
		String signString = "";
		for (int j=0; j<3; ++j) {
			if (  Math.abs(pcaOp.eigenVects.get(0,j)*(-1) - eigVects[0][j])
					< TestPCA.accuracy) {
				sign = -1.0;
				signString = "-";
			}
			else {
				sign = 1.0;
				signString = "";
			}
			for (int i=0; i<5; ++i) {
				assertTrue("[TestPCA] got unexpected eigenvector in V["+i+"]["+j+"]= "
					+ signString
						+ pcaOp.eigenVects.get(i,j) + ", expected " + eigVects[i][j],
								Math.abs(pcaOp.eigenVects.get(i,j)*sign - eigVects[i][j]) 
							< TestPCA.accuracy); 
			}
		}
	
		// determine and check subspace dimension
		pcaOp.determineSubspaceDimension();
		assertTrue("[TestPCA] subspace dimension should be 2, got " + pcaOp.subDim, 
			pcaOp.subDim == 2);

		// do the actual dimension reduction
		pcaOp.doDimensionReduction();		
		
		// check projection matrix
		double[][] projMatrix = new double[][] {
			{ 0.441871767570761,  -0.321972174712668,   0.657383911971846,   
				0.393297684940557,   0.338003821037097},
			{-0.351405960935564,   0.244356114285587,  -0.242051367885063,   
				0.295502611765346,   0.819080754123200}};
		for (int i=0; i<2; ++i) {
			if (  Math.abs(pcaOp.P_t.get(i,0)*(-1) - projMatrix[i][0])
					< TestPCA.accuracy) {
				sign = -1.0;
				signString = "-";
			}
			else {
				sign = 1.0;
				signString = "";
			}
			for (int j=0; j<5; ++j) {
				assertTrue("[TestPCA] got unexpected value in P^T["+i+"]["+j+"]= " 
					+ signString + pcaOp.P_t.get(i,j) + ", expected " 
						+ projMatrix[i][j],
							Math.abs(pcaOp.P_t.get(i,j)*sign - projMatrix[i][j]) 
						< TestPCA.accuracy); 
			}
		}
		
		// check sub-space data by transforming the data
		double[][] resultData = pcaOp.getResultData();
		double[][] normedProjectionMatrix = new double[2][5];
		for (int i=0; i<2; ++i) {
			if (  Math.abs(projMatrix[i][0]*(-1) - pcaOp.P_t.get(i,0))
					< TestPCA.accuracy) {
				for (int j=0; j<5; ++j) {
					normedProjectionMatrix[i][j] = projMatrix[i][j]*(-1);
				}
			}
			else {
				for (int j=0; j<5; ++j) {
					normedProjectionMatrix[i][j] = projMatrix[i][j];
				}
			}
		}
		double[][] transformedData = new double[2][3];
		for (int j=0; j<3; ++j) {
			for (int i=0; i<2; ++i) {
				double sum=0;
				for (int k=0; k<5; ++k)
					sum+= normedProjectionMatrix[i][k] * meanFreeData[k][j];
				transformedData[i][j] = sum;
			}
		}
		for (int i=0; i<2; ++i) {
			for (int j=0; j<3; ++j) {
				assertTrue("[TestPCA] got unexpected data item at ["+i+"]["+j+"]= " 
					+ resultData[i][j] + ", expected " 
						+ transformedData[i][j],
								Math.abs(resultData[i][j] - transformedData[i][j]) 
							< TestPCA.accuracy); 
			}
		}

		// change the mode how to determine sub-space dimensionality
		pcaOp.setReductionMode(ReductionMode.PERCENTAGE_VARIANCE);
		pcaOp.setMeanFreeData(false);
		pcaOp.setPercentageOfVariance(0.85);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check if correct dimensionality was determined
		assertTrue("[TestPCA] expected a dimensionality of 1, got " + pcaOp.subDim,
			pcaOp.subDim == 1);
		// check if correct subset of eigenvectors is selected
		for (int i=0; i<1; ++i) {
			if (  Math.abs(pcaOp.P_t.get(i,0)*(-1) - projMatrix[i][0])
					< TestPCA.accuracy) {
				sign = -1.0;
				signString = "-";
			}
			else {
				sign = 1.0;
				signString = "";
			}
			for (int j=0; j<5; ++j) {
				assertTrue("[TestPCA] got unexpected value in P^T["+i+"]["+j+"]= " 
					+ signString + pcaOp.P_t.get(i,j) + ", expected " 
						+ projMatrix[i][j],
							Math.abs(pcaOp.P_t.get(i,j)*sign - projMatrix[i][j]) 
						< TestPCA.accuracy); 
			}
		}
	}

	/**
	 * Test data, randomly generated with Octave (size 10 x 20).
	 */
	private final transient double[][] testData = {
		{5.623896, 7.658024, 1.785343, 4.351788, 6.041751, 8.916370, 1.168178,
		 2.997397, 0.524554, 2.500465, 8.414985, 5.055817, 5.326396, 7.975129,
		 3.248365, 0.474611, 0.851832, 5.414299, 7.192981, 0.107289},
		{3.991966, 4.021994, 0.289701, 9.253908, 7.815675, 4.387053, 6.559013,
		 8.117330, 8.783682, 8.454524, 1.354133, 3.190070, 7.055342, 4.597015,
		 9.344895, 2.869549, 7.747136, 4.415799, 8.068055, 2.696707},
		{1.685400, 1.691610, 1.843487, 2.667106, 5.919382, 8.862517, 8.086338,
		 4.920342, 5.808431, 4.810840, 8.399790, 6.433908, 2.295266, 0.990470,
		 3.294702, 4.995507, 3.621357, 5.445709, 6.425891, 3.862074},
		{1.517468, 1.209385, 5.732721, 7.206048, 3.508089, 8.340118, 2.199101,
		 9.787558, 7.810280, 8.382364, 9.320867, 8.673217, 8.364548, 4.888171,
		 9.819978, 4.867933, 2.569892, 7.684214, 0.449565, 2.482030},
		{1.768901, 8.350337, 2.185225, 6.455986, 2.077825, 3.599986, 1.005673,
		 4.478796, 3.013819, 8.593278, 0.709584, 2.533578, 6.514926, 0.926635,
		 1.724240, 9.087804, 0.971541, 8.496685, 2.190004, 7.588493},
		{9.950321, 1.061910, 6.835215, 0.910112, 7.016299, 7.630106, 0.667760,
		 6.398383, 1.292088, 4.539831, 1.558355, 6.625835, 1.086466, 6.948620,
		 1.569215, 2.678043, 3.371158, 6.849678, 6.341344, 7.356553},
		{7.411174, 8.291020, 2.206987, 7.485583, 2.181438, 1.785413, 4.871112,
		 1.719993, 0.978294, 4.594610, 4.910523, 6.157796, 9.000286, 2.714079,
		 8.453020, 4.460758, 1.005826, 3.157651, 9.354883, 1.490334},
		{1.341773, 7.266244, 7.954417, 1.196684, 8.003860, 3.131583, 7.370043,
		 3.416579, 5.001972, 9.601653, 2.620201, 9.821992, 3.182486, 8.582025,
		 6.559720, 2.271241, 5.780631, 3.005897, 6.640361, 9.692715},
		{9.291073, 0.846572, 1.909630, 1.202719, 9.438694, 1.885125, 7.929127,
		 6.314945, 2.012328, 6.991239, 4.402431, 4.130599, 3.755627, 0.059640,
		 8.079447, 5.778226, 5.562358, 2.360595, 9.705156, 0.105139},
		{4.328129, 9.757062, 0.840140, 0.120317, 3.378764, 8.785999, 7.413436,
		 5.014873, 3.660243, 3.792819, 0.165348, 1.938250, 9.638948, 4.782784,
		 0.532768, 9.720796, 5.820479, 1.700625, 0.308036, 2.496307}};
	
	/**
	 * Mean vector of the test data.
	 */
	private final transient double[] testMean = 
		{4.2815,5.6507,4.6030,5.7407,4.1137,4.5344,4.6115,5.6221,4.5880,4.2098};
	
	/**
	 * Mean-free test data.
	 */
	private final transient double[][] testDataMeanFree = {
		{ 1.3424224,  3.3765505, -2.4961302,  0.0703147,  1.7602774,  4.6348965,
		 -3.1132958, -1.2840763, -3.7569199, -1.7810087,  4.1335118,  0.7743432,
		  1.0449222,  3.6936559, -1.0331086, -3.8068620, -3.4296415,  1.1328254,
		  2.9115079, -4.1741849},
		{-1.6587112, -1.6286836, -5.3609762,  3.6032311,  2.1649977, -1.2636243,
		  0.9083355,  2.4666527,  3.1330044,  2.8038465, -4.2965441, -2.4606071,
		  1.4046643, -1.0536622,  3.6942176, -2.7811283,  2.0964588, -1.2348785,
		  2.4173775, -2.9539705},
		{-2.9176065, -2.9113962, -2.7595193, -1.9359005,  1.3163758,  4.2595105,
		  3.4833318,  0.3173354,  1.2054242,  0.2078341,  3.7967839,  1.8309020,
		 -2.3077405, -3.6125366, -1.3083041,  0.3925004, -0.9816492,  0.8427029,
		  1.8228846, -0.7409328},
		{-4.2232097, -4.5312921, -0.0079567,  1.4653709, -2.2325884,  2.5994410,
		 -3.5415766,  4.0468809,  2.0696028,  2.6416862,  3.5801892,  2.9325397,
		  2.6238706, -0.8525060,  4.0793002, -0.8727442, -3.1707852,  1.9435371,
		 -5.2911124, -3.2586473},
		{-2.3447645,  4.2366710, -1.9284409,  2.3423203, -2.0358408, -0.5136801,
		 -3.1079929,  0.3651300, -1.0998468,  4.4796125, -3.4040814, -1.5800883,
		  2.4012603, -3.1870307, -2.3894263,  4.9741379, -3.1421249,  4.3830195,
		 -1.9236614,  3.4748274},
		{ 5.4159564, -3.4724549,  2.3008501, -3.6242522,  2.4819340,  3.0957415,
		 -3.8666043,  1.8640183, -3.2422767,  0.0054666, -2.9760092,  2.0914702,
		 -3.4478991,  2.4142558, -2.9651492, -1.8563212, -1.1632065,  2.3153131,
		  1.8069789,  2.8221884},
		{ 2.7996347,  3.6794809, -2.4045521,  2.8740443, -2.4301013, -2.8261257,
		  0.2595726, -2.8915459, -3.6332452, -0.0169290,  0.2989841,  1.5462574,
		  4.3887474, -1.8974601,  3.8414808, -0.1507811, -3.6057128, -1.4538881,
		  4.7433442, -3.1212052},
		{-4.2803304,  1.6441402,  2.3323129, -4.4254200,  2.3817560, -2.4905206,
		  1.7479396, -2.2055250, -0.6201318,  3.9795487, -3.0019030,  4.1998881,
		 -2.4396177,  2.9599210,  0.9376162, -3.3508624,  0.1585267, -2.6162071,
		  1.0182569,  4.0706115},
		{ 4.7030395, -3.7414610, -2.6784034, -3.3853145,  4.8506609, -2.7029085,
		  3.3410935,  1.7269114, -2.5757059,  2.4032052, -0.1856025, -0.4574342,
		 -0.8324065, -4.5283934,  3.4914136,  1.1901926,  0.9743241, -2.2274383,
		  5.1171221, -4.4828946},
		{ 0.1183226,  5.5472555, -3.3696663, -4.0894888, -0.8310418,  4.5761924,
		  3.2036295,  0.8050671, -0.5495631, -0.4169871, -4.0444587, -2.2715564,
		  5.4291415,  0.5729782, -3.6770379,  5.5109901,  1.6106730, -2.5091807, 
		 -3.9017701, -1.7134989}};

	/**
	 * Covariance matrix.
	 */
	private final transient double[][] C = {
		{159.57397, -23.43495,  12.84930,   7.66478, -29.85553,  34.93623,  55.76869,
		 -27.56492, -10.19341,  -1.88221},
		{-23.43495, 147.46776,   0.49161,  19.51641,  -5.12987, -51.12148,  25.20082,    
			-7.56008,  65.85236,  -8.29534},
		{ 12.84930,   0.49161, 105.51338,  30.65128, -24.74998,  -6.14398, -28.56119,
			-7.00500,  38.02203,  -6.89458},
		{  7.66478,  19.51641,  30.65128, 191.31605,  14.84971, -32.09086, -18.15092,   
		 -38.40101, -36.54095, -39.12692},
		{-29.85553,  -5.12987, -24.74998,  14.84971, 173.44847, -21.85429,  17.03270,   
		 -19.55609, -61.16484,	58.81742},
		{ 34.93623, -51.12148,  -6.14398, -32.09086, -21.85429, 165.63003, -50.84581,
			25.67943,  16.18171, -37.50324},
		{ 55.76869,  25.20082, -28.56119, -18.15092,  17.03270, -50.84581, 158.41775,   
		 -25.25610,  52.55222,  -5.57124},
		{-27.56492,  -7.56008,  -7.00500, -38.40101, -19.55609,  25.67943, -25.25610,   
		 160.01206,  -6.57770, -27.00892},
		{-10.19341,  65.85236,  38.02203, -36.54095, -61.16484,  16.18171,  52.55222,    
			-6.57770, 199.38226, -17.95078},
		{ -1.88221,  -8.29534,  -6.89458, -39.12692,  58.81742, -37.50324,  -5.57124,   
		 -27.00892, -17.95078, 214.14731}};
	
	/**
	 * Eigenvalues, sorted in ascending order.
	 */
	private final transient double[] eigenvals = {
	   16.7337448997470,	    66.9393635935891,	    91.7790117233634,
	  105.9227174713888,	   129.3325218337630,	   174.0960316025935,
	  223.2641668187497,	   253.8326932971480,	   291.5892096422733,
	  321.4195839118851};

	/**
	 * Eigenvectors, sorted ascending with regard to corresponding eigenvalues.
	 */
	private final transient double[][] eigenvects = {
		{ 0.38887964799474167,  -0.25941832067515913,  -0.22564251083858564,   
			0.31741328198202218,  -0.22105577603798737,  -0.04382114454153705,
			0.75160798172087939,  -0.04194807805381383,   0.00701219911626706,
		 -0.11090401612813232},
		{-0.19159283519626769,  -0.38875820187425431,  -0.65613693697925746,   
			0.18681128661787486,   0.05306606235942980,   0.05216331685167547,
		 -0.32643271110613603,  -0.09661079256814840,   0.44438563700879496,
		 -0.16684584502120789},
		{-0.36600733724608764,  -0.47690530629881694,   0.51986830531489103,   
			0.28729057320305734,  -0.13020830475358730,  -0.46540372652054185,
		 -0.03675044108636462,  -0.17394820907795958,   0.02399548776894310,
		 -0.14163982668370576},
		{-0.01163787841966517,   0.49877965718524803,  -0.07454773816441927,   
			0.31138855983798247,   0.04369970602404002,  -0.14749637241149949,   
			0.00221001230954187,  -0.77711181447575772,   0.08038230764053537,
			0.12053794832201770},
		{ 0.22967517443636740,  -0.26323506734532776,   0.21763009916926065,   
			0.37420655151006377,   0.57965785174806828,   0.23324853098073020, 
		 -0.03860739429535935,   0.03952016430326220,   0.11632759281405262,   
		  0.53251349737001796},
		{-0.40540781952782601,   0.10956070664472085,  -0.26772555777600143,   
		  0.20044964399099091,   0.55964040729897679,  -0.15704935954053584,
		  0.21751810915144629,   0.15703253918126961,  -0.49920397229476732,
		 -0.22966078991274905},
		{-0.51765261022484155,   0.22678030156126985,   0.22367268773210805,   
			0.09700102138210176,  -0.01163765195645515,   0.47029741798628460,
			0.38307918931759760,   0.11544856418271826,   0.47873194756328752,
		 -0.10290962238425025},
		{ 0.05007289851775416,   0.13915210251158922,   0.04101367590022507,   
			0.65469561406265342,  -0.37919653047161345,   0.35063941196595905,  
		 -0.33931941016664435,   0.20295531788352664,  -0.32461686475400348,  
		 -0.13920592844992796},
		{ 0.41495922349654868,   0.26844799682917003,   0.19823064885498304,   
			0.14993442979944285,   0.31000105719064869,  -0.25602797603924565,  
		 -0.12428297913757017,   0.22431495018789160,   0.38108753163031123,
		 -0.56685681607398308},
		{-0.13490415358600685,   0.28494944962883317,  -0.18083843549702838,   
			0.21005989987087484,  -0.20056859244028816,  -0.51793390069817702,   
			0.03214658377554360,   0.47295715578959774,   0.23002245064260038,   
			0.48750537595818771}};
	
	/**
	 * Projection matrix for a sub-space dimensionality of 5.
	 */
	private final transient double[][] projectMatrixTrans_05 = {
		{-0.11090401612813232,  -0.16684584502120789,  -0.14163982668370576,
		  0.12053794832201770,   0.53251349737001796,  -0.22966078991274905,
		 -0.10290962238425025,  -0.13920592844992796,  -0.56685681607398308,
		  0.48750537595818771},
	  { 0.00701219911626706,   0.44438563700879496,   0.02399548776894310,
		  0.08038230764053537,   0.11632759281405262,  -0.49920397229476732,
		  0.47873194756328752,  -0.32461686475400348,   0.38108753163031123,
		  0.23002245064260038},
		{-0.04194807805381383,  -0.09661079256814840,  -0.17394820907795958,
		 -0.77711181447575772,   0.03952016430326220,   0.15703253918126961,
		  0.11544856418271826,   0.20295531788352664,   0.22431495018789160,
		  0.47295715578959774},
		{ 0.75160798172087939,  -0.32643271110613603,  -0.03675044108636462,
			0.00221001230954187,  -0.03860739429535935,   0.21751810915144629,   
			0.38307918931759760,  -0.33931941016664435,  -0.12428297913757017,
			0.03214658377554360},
		{-0.04382114454153705,   0.05216331685167547,  -0.46540372652054185,
		 -0.14749637241149949,   0.23324853098073020,  -0.15704935954053584,
			0.47029741798628460,   0.35063941196595905,  -0.25602797603924565,
		 -0.51793390069817702}};
	
	/**
	 * Test singular cases.
	 */
	@Test
	public void testPCA_singular() {
		
		PCA pcaOp = null;
		
		boolean exceptionThrown = false;
		try {
	    pcaOp = new PCA();
    } catch (ALDOperatorException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] constructor threw an exception!?", exceptionThrown);
		if (pcaOp == null)
			return;

		/*
		 * just one vector
		 */
		double[][] singularData = new double[][] {
			{1},{2},{3}
		};
		double[] singularDataMean = new double[] {1,2,3};
		
		pcaOp.setDataset(singularData);
		pcaOp.setMeanFreeData(false);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check global variables
		assertTrue("[TestPCA] sample size is 1, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 1);
		assertTrue("[TestPCA] data dimension is 3, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 3);
		assertTrue("[TestPCA] subspace dimension should be 3, got " + pcaOp.subDim, 
			pcaOp.subDim == 3);
		
		// check calculation of mean vector
		for (int i=0; i<3; ++i) {
			assertTrue("[TestPCA] got unexpected value in mean[" + i + "] = " 
				+  pcaOp.mean[i] + ", expected " + this.testMean[i],
					Math.abs(pcaOp.mean[i] - singularDataMean[i]) < TestPCA.accuracy); 
		}
		// check calculation of mean-free data
		for (int i=0; i<3; ++i) {
			for (int j=0; j<1; ++j) {
				assertTrue("[TestPCA] got unexpected value in mean-free data[" 
					+ i + "][" + j + "]= " + pcaOp.meanfreeData[i][j] 
						+  ", expected " + 0,
								Math.abs(pcaOp.meanfreeData[i][j]) < TestPCA.accuracy); 
			}
		}
		// check eigenvalues
		assertTrue("[TestPCA] got unexpected number of eigenvalues, expected 0...", 	
				Math.abs(pcaOp.eigenVals.length - 1) < TestPCA.accuracy); 
		assertTrue("[TestPCA] got unexpected eigenvalue, expected 0...", 	
				Math.abs(pcaOp.eigenVals[0]) < TestPCA.accuracy); 
		double[][] resultData = pcaOp.getResultData();
		// check transformed data
		for (int j=0; j<1; ++j) {
			for (int i=0; i<3; ++i) {
				assertTrue("[TestPCA] got unexpected data item at ["+i+"]["+j+"]= " 
					+ resultData[i][j] + ", expected " + singularData[i][j],
								Math.abs(resultData[i][j] - singularData[i][j]) 
							< TestPCA.accuracy); 
			}
		}
		
		/*
		 * Check alternative mode for determining sub-space dimension.
		 */
		pcaOp.setDataset(singularData);
		pcaOp.setMeanFreeData(false);
		pcaOp.setReductionMode(ReductionMode.NUMBER_COMPONENTS);
		pcaOp.setNumberOfComponents(2);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check global variables
		assertTrue("[TestPCA] sample size is 1, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 1);
		assertTrue("[TestPCA] data dimension is 3, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 3);
		assertTrue("[TestPCA] subspace dimension should be 3, got " + pcaOp.subDim, 
			pcaOp.subDim == 3);
		
		/*
		 * two vectors
		 */
		singularData = new double[][] {	{1,2},{2,3},{3,4}	};
		double[][] singularDataMeanFree = 
				new double[][] {	{-0.5,0.5},{-0.5,0.5},{-0.5,0.5}	};
		singularDataMean = new double[] {1.5, 2.5, 3.5};
		
		pcaOp.setDataset(singularData);
		pcaOp.setMeanFreeData(false);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check global variables
		assertTrue("[TestPCA] sample size is 2, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 2);
		assertTrue("[TestPCA] data dimension is 3, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 3);
		assertTrue("[TestPCA] subspace dimension should be 1, got " + pcaOp.subDim, 
			pcaOp.subDim == 1);
		
		// check calculation of mean vector
		for (int i=0; i<3; ++i) {
			assertTrue("[TestPCA] got unexpected value in mean[" + i + "] = " 
				+  pcaOp.mean[i] + ", expected " + this.testMean[i],
					Math.abs(pcaOp.mean[i] - singularDataMean[i]) < TestPCA.accuracy); 
		}
		// check calculation of mean-free data
		for (int i=0; i<3; ++i) {
			for (int j=0; j<2; ++j) {
				assertTrue("[TestPCA] got unexpected value in mean-free data[" 
					+ i + "][" + j + "]= " + pcaOp.meanfreeData[i][j] 
						+  ", expected " + singularDataMeanFree[i][j],
								Math.abs(pcaOp.meanfreeData[i][j] - singularDataMeanFree[i][j]) 
						  < TestPCA.accuracy); 
			}
		}
		
		/*
		 * two linear-dependent vectors
		 */
		singularData = new double[][] {	{1,2},{2,4},{3,6}	};
		singularDataMeanFree = new double[][] {	{-0.5,0.5},{-1,1},{-1.5,1.5}	};
		singularDataMean = new double[] {1.5, 3, 4.5};
		
		pcaOp.setDataset(singularData);
		pcaOp.setMeanFreeData(false);
		pcaOp.setReductionMode(ReductionMode.PERCENTAGE_VARIANCE);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check global variables
		assertTrue("[TestPCA] sample size is 2, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 2);
		assertTrue("[TestPCA] data dimension is 3, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 3);
		assertTrue("[TestPCA] subspace dimension should be 3, got " + pcaOp.subDim, 
			pcaOp.subDim == 3);
		
		// check calculation of mean vector
		for (int i=0; i<3; ++i) {
			assertTrue("[TestPCA] got unexpected value in mean[" + i + "] = " 
				+  pcaOp.mean[i] + ", expected " + this.testMean[i],
					Math.abs(pcaOp.mean[i] - singularDataMean[i]) < TestPCA.accuracy); 
		}
		// check calculation of mean-free data
		for (int i=0; i<3; ++i) {
			for (int j=0; j<2; ++j) {
				assertTrue("[TestPCA] got unexpected value in mean-free data[" 
					+ i + "][" + j + "]= " + pcaOp.meanfreeData[i][j] 
						+  ", expected " + singularDataMeanFree[i][j],
								Math.abs(pcaOp.meanfreeData[i][j] - singularDataMeanFree[i][j]) 
						  < TestPCA.accuracy); 
			}
		}
		
		pcaOp.setDataset(singularData);
		pcaOp.setMeanFreeData(false);
		pcaOp.setReductionMode(ReductionMode.NUMBER_COMPONENTS);
		pcaOp.setNumberOfComponents(2);
		exceptionThrown = false;
		try {
	    pcaOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[TestPCA] running operator failed!?", exceptionThrown);
		
		// check global variables
		assertTrue("[TestPCA] sample size is 2, got " + pcaOp.sampleCount, 
			pcaOp.sampleCount == 2);
		assertTrue("[TestPCA] data dimension is 3, got " + pcaOp.dataDim, 
			pcaOp.dataDim == 3);
		assertTrue("[TestPCA] subspace dimension should be 1, got " + pcaOp.subDim, 
			pcaOp.subDim == 1);
	}

}
