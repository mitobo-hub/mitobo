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

package de.unihalle.informatik.MiToBo.features.texture;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;

/**
 * Calculates set of Haralick co-occurrence texture features from an image. 
 * <p>
 * The operator expects that the input image does not contain more than 256 
 * different gray-values, color images are completely rejected. For saving 
 * memory the operator first checks the maximal gray-value in the image and 
 * updates the size of the gray-level co-occurence matrices accordingly.
 * Subsequently, the following measures are calculated:
 * <ul>
 * <li> homogenity: 
 * 	{@latex.inline $\\sum_i \\sum_j p(i,j)^2$}
 * <li> contrast: 
 * 	{@latex.inline $\\sum_i \\sum_j |i-j|^2 \\cdot p(i,j)$}
 * <li> local homogeneity: 
 * 	{@latex.inline $\\sum_i \\sum_j \\frac{1}{1 + (i-j)^2} p(i,j)$}
 * <li> entropy: 
 * 	{@latex.inline $- \\sum_i \\sum_j p(i,j) \\cdot \\log p(i,j)$}
 * <li> correlation: 
 * 	{@latex.inline $\\sum_i \\sum_j \\frac{i \\cdot j \\cdot p(i,j) 
 * 		- \\mu_x \\cdot \\mu_y}{\\sigma_x \\cdot \\sigma_y}$}
 * <li> auto-correlation:
 * 	{@latex.inline $\\sum_i \\sum_j i \\cdot j \\cdot p(i,j)$}
 * <li> dissimilarity:
 * 	{@latex.inline $\\sum_i \\sum_j |i - j| \\cdot p(i,j)$}
 * <li> cluster shade:
 * 	{@latex.inline $\\sum_i \\sum_j (i + j - \\mu_x - \\mu_y)^3 \\cdot  p(i,j)$}
 * <li> cluster prominence:
 * 	{@latex.inline $\\sum_i \\sum_j (i + j - \\mu_x - \\mu_y)^4 \\cdot p(i,j)$}
 * <li> maximum probability:
 * 	{@latex.inline $\\max p(i,j)$}
 * </ul>
 * For details about these measures, refer to one of the following papers:
 * <ul>
 * <li>R.M. Haralick, K. Shanmugan, and I. Dinstein, <i>Textural Features for 
 * Image Classification</i>, IEEE Trans. on Systems, Man and Cybernetics, 
 * vol. SMC-3, no. 6, pp. 610-621, 1973.
 * <li>F. Albregtsen, <i>Statistical Texture Measures Computed from Gray Level
 * Cooccurence Matrices</i>, Technical Report, Image Processing Laboratory,
 * Department of Informatics, University of Oslo, November 5, 2008.
 * <li>L.-K. Soh and C. Tsatsoulis, <i>Texture Analysis of SAR Sea Ice 
 * Imagery Using Gray Level Co-Occurence Matrices</i>, IEEE Trans. on 
 * Geoscience and Remote Sensing, vol. 37, no. 2, pp. 779-795, 1999. 
 * 
 * @author Birgit Moeller
 * @author Elisabeth Piltz
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD, allowBatchMode=false,
	shortDescription="Calculates set of Haralick co-occurrence texture features from an image.")
@ALDDerivedClass
public class FeatureCalculatorHaralickMeasures extends FeatureCalculator
{
	
	/**
	 * Directions available for matrix calculations. 
	 * @author moeller
	 */
	public static enum HaralickDirection {
		/**
		 * Vector direction [0, -1].
		 */
		NORTH,
		/**
		 * Vector direction [1, -1].
		 */
		NORTH_EAST,
		/**
		 * Vector direction [1,  0].
		 */
		EAST,
		/**
		 * Vector direction [1,  1].
		 */
		SOUTH_EAST,
		/**
		 * Vector direction [0,  1].
		 */
		SOUTH,
		/**
		 * Vector direction [-1, 1].
		 */
		SOUTH_WEST,
		/**
		 * Vector direction [-1, 0].
		 */
		WEST,
		/**
		 * Vector direction [-1,-1].
		 */
		NORTH_WEST,
	}

	/**
	 * Directions.
	 * <p>
	 * Note that no checks are done if directions appear several times.
	 */
	@Parameter( label= "Directions", required = true,
			direction = Parameter.Direction.IN, dataIOOrder=0,  
			mode=ExpertMode.STANDARD, description = "Desired directions.")
	private Vector<HaralickDirection> directions = 
		new Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();

	/**
	 * Flag for isotropic calculations.
	 * <p>
	 * If flag is true the average of all given directions is calculated.
	 */
	@Parameter( label= "Isotropic calculations", required = true,
			direction = Parameter.Direction.IN, dataIOOrder=1,  
			mode=ExpertMode.STANDARD, 
			description = "Flag to enable isotropic calculations.")
	private boolean isotropicCalcs = false;

	/**
	 * Distance.
	 */
	@Parameter( label= "Distance", required = true,
			direction = Parameter.Direction.IN, dataIOOrder=2,  
			mode=ExpertMode.STANDARD, description = "Desired distance.")
	private int distance= 1;

	/**
	 * Flag for thrinking matrix if possible.
	 */
	@Parameter( label= "Reduce matrix size", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=4,  
		mode=ExpertMode.STANDARD, description = "Thrinks the matrix if not " 
			+ "the whole range of gray-values is actually used.")
	private boolean thrinkMatrix= false;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureCalculatorHaralickMeasures() throws ALDOperatorException {
		// nothing to do here
	}
	
	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.directions == null || this.directions.isEmpty())
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[FeatureCalculatorHaralickMeasures] no directions given!");
		if (   this.inImg.getType().equals(MTBImageType.MTB_RGB)
				|| this.inImg.getType().equals(MTBImageType.MTB_FLOAT)
				|| this.inImg.getType().equals(MTBImageType.MTB_DOUBLE)) 
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[FeatureCalculatorHaralickMeasures] wrong input image type, "
					+ "color images and images with rational values not supported!");
		int[] minmax = this.inImg.getMinMaxInt();
		if (minmax[0] < 0 || minmax[1] > 256)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[FeatureCalculatorHaralickMeasures] image contains negative values "
					+ "or the admissible range [0,255] of gray-values is exceeded!");		
	}
	
	@Override
  public void operate() {
		double[][] comatrix;
		double[] features;
		Vector<double[]> featureVec = new Vector<double[]>();
		for (HaralickDirection dir: this.directions) {
			comatrix = this.calcMatrix(dir);
			featureVec.add(this.calcFeatures(comatrix));
		}
		int featureNum = featureVec.firstElement().length;
		if (this.isotropicCalcs) {
			// calculate average features
			features = new double[featureNum];
			for (int i=0; i<featureNum; ++i) {
				for (double[] fElem: featureVec) {
					features[i] += fElem[i];
				}
				features[i] /= featureNum;
			}			
		}
		else {
			// just concatenate all features
			features = new double[featureNum*this.directions.size()];
			int i=-1;
			for (double[] fElem: featureVec) {
				++i;
				for (int j=0; j<fElem.length; ++j) {
					features[i*featureNum + j] = fElem[j];
				}
			}
		}
		this.resultObj = new FeatureCalculatorHaralickMeasuresResult(features);
  }

	/**
	 * Extracts the co-occurence matrix for the given distance and direction.
	 * @param dir Haralick direction to consider.
	 * @return	Resulting (normalized) matrix.
	 */
	protected double[][] calcMatrix(HaralickDirection dir) 	{
		
		// get image dimensions
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();
		
		// extract number of gray-values and allocate matrix accordingly
		int matrixSize = 256;
		if (this.thrinkMatrix) {
			matrixSize = this.inImg.getMinMaxInt()[1] + 1;
		}
		double[][] comatrix = new double[matrixSize][matrixSize]; 
		
		int firstVal = 0, secondVal = 0;
		int x2 = 0, y2 = 0;
		int dx = 0, dy = 0;
		switch(dir)
		{
		case NORTH:      dx =  0; dy = -this.distance; break;
		case NORTH_EAST: dx =  this.distance; dy = -this.distance; break;
		case EAST:       dx =  this.distance; dy =  0; break;
		case SOUTH_EAST: dx =  this.distance; dy =  this.distance; break;
		case SOUTH:      dx =  0; dy =  this.distance; break;
		case SOUTH_WEST: dx = -this.distance; dy =  this.distance; break;
		case WEST:       dx = -this.distance; dy =  0; break;
		case NORTH_WEST: dx = -this.distance; dy = -this.distance; break;
		}
		
		int pixCount = 0;
		for(int y=0; y<height; y++) {
			y2 = y + dy;
			if (y2 < 0 || y2 >= height)
				continue;
			for(int x=0; x<width; x++) {
				x2 = x + dx;
				if(x2 < 0 || x2 >= width)
					continue;

				// get intensities
				firstVal = this.inImg.getValueInt(x, y);
				secondVal = this.inImg.getValueInt(x2, y2);

				// increment matrix cell
				comatrix[firstVal][secondVal]++;
				// count overall number of entries or pixel pairs, respectively
				pixCount++;
			}
		}
		// normalize matrix
		for (int y=0;y<matrixSize;++y)
			for (int x=0;x<matrixSize;++x)
				comatrix[y][x] /= pixCount;
		return comatrix;
	}
		
	/**
	 * Extracts Haralick features from given matrix.
	 * <p>
	 * Order of the features in the result vector is as follows:
	 * 0 = homogenity (also known as energy)
	 * 1 = contrast (|i-j|² * p(i,j))
	 * 2 = local homogeneity
	 * 3 = entropy
	 * 4 = correlation
	 * 5 = auto-correlation
	 * 6 = dissimilarity
	 * 7 = cluster shade
	 * 8 = cluster prominence
	 * 9 = maximum probability
	 *
	 * @param matrix	Normalized co-occurence matrix.
	 * @return	Vector of features.
	 */
	protected double[] calcFeatures(double[][] matrix) {

		// matrix dimensions
		int height = matrix.length;
		int width = matrix[0].length;
		
		double[] features = new double[10];
		
		double myx = 0;
		double myy = 0;
		double sigmax = 0;
		double sigmay = 0;
		
		for(int i=0; i<height; i++) {
			for(int j=0; j<width; j++) {
				myx += i*matrix[i][j];
				myy += j*matrix[i][j];
			}
		}
		
		for(int i=0; i<height; i++) {
			for(int j=0; j<width; j++) 	{
				sigmax += (i-myx)*(i-myx)*matrix[i][j];
				sigmay += (j-myy)*(j-myy)*matrix[i][j];
			}
		}
		
		// fill in the result vector 
		for(int i=0; i<height; i++) {
			for(int j=0; j<width; j++) 	{
				// homogenity/energy
				features[0] += matrix[i][j]*matrix[i][j];
				// contrast
				features[1] += (i-j)*(i-j)*matrix[i][j];
				// local homogeneity
				features[2] += 1.0/(1.0+((i-j)*(i-j)))*matrix[i][j];
				// avoid numerical instabilities in logarithmic evaluation
				if(matrix[i][j] > MTBConstants.epsilon)	{
					// entropy
					features[3] -= matrix[i][j]* Math.log10(matrix[i][j]);
				}
				// correlation
//				features[4] += (((i*j)*matrix[i][j]) - (myx*myy)) / (sigmax * sigmay);
				features[4] += (((i*j)*matrix[i][j]));
				// auto-correlation
				features[5] += i*j*matrix[i][j];
				// dissimilarity
				features[6] += Math.abs(i-j)*matrix[i][j];
				// cluster shade
				features[7] += Math.pow((i+j-myx-myy), 3.0)*matrix[i][j];
				// cluster prominence
				features[8] += Math.pow((i+j-myx-myy), 4.0)*matrix[i][j];
				// maximum probability
				if(matrix[i][j] > features[9]) {
					features[9] = matrix[i][j];
				}
			}
		}
		features[4] = ( features[4] - (myx*myy) ) / (sigmax * sigmay);
		return features;
	}
	
	/**
	 * Set flag for isotropic calculations.
	 * @param flag	If true, isotropic calculations are done.
	 */
	public void doIsotropicCalcutations(boolean flag) {
		this.isotropicCalcs = flag;
	}
	
	/**
	 * Specify distance to be used in calculations.
	 * @param dist	Distance to be used (in pixels).
	 */
	public void setDistance(int dist) {
		this.distance = dist;
	}
	
	/**
	 * Directions to be analyzed.
	 * @param dirs		Set of directions to consider.
	 * @see FeatureCalculatorHaralickMeasures.HaralickDirection
	 */
	public void setDirections(Vector<HaralickDirection> dirs) {
		this.directions = dirs;
	}
	
	/**
	 * Flag for thrinking matrix if possible.
	 * @param b		If true, matrix size is reduced, otherwise it remains 256x256.
	 */
	public void setFlagThrinkMatrix(boolean b) {
		this.thrinkMatrix = b;
	}
	
	@Override
	protected FeatureCalculatorHaralickMeasuresResult getResultDataObjectInvalid(
			int dim) {
		double[] nanResult = new double[dim];
		for (int i=0; i<dim; ++i)
			nanResult[i] = Double.NaN;
		return new FeatureCalculatorHaralickMeasuresResult(nanResult);
	}

	@Override
	public String getDocumentation() {
		return "This operator calculates a co-occurence matrix on the given image applying the\r\n" + 
				"specified parameters. Subsequently the following Haralick measures are \r\n" + 
				"calculated from the matrix:\r\n" + 
				"<ul>\r\n" + 
				"<li> homogenity\r\n" + 
				"<li> contrast\r\n" + 
				"<li> local homogeneity \r\n" + 
				"<li> entropy \r\n" + 
				"<li> correlation \r\n" + 
				"<li> auto-correlation\r\n" + 
				"<li> dissimilarity\r\n" + 
				"<li> cluster shade\r\n" + 
				"<li> cluster prominence\r\n" + 
				"<li> maximum probability\r\n" + 
				"</ul>\r\n" + 
				" \r\n" + 
				"<ul>\r\n" + 
				"<li><p><b>input:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Input image</i>: the (gray-scale) image to analyze</p></li>\r\n" + 
				"<li><p><i>Directions</i>: \r\n" + 
				"set of directions to consider, for each direction the 10 features are extracted,\r\n" + 
				"i.e., the resulting feature vector has a dimension of 10 times the number of\r\n" + 
				"directions\r\n" + 
				"</p></li>\r\n" + 
				"<li><p><i>Isotropic calculations</i>: if selected the various directions are \r\n" + 
				"not treated indipendently, but the extracted measures are averaged over all\r\n" + 
				"directions; in this case the result data vector has always only 10 dimensions\r\n" + 
				"</p></li>\r\n" + 
				"<li><p><i>Distance</i>: pixel distance between pixel pairs to consider</p></li>\r\n" + 
				"</ul>\r\n" + 
				"</p>\r\n" + 
				"</li>\r\n" + 
				"<li><p><b>output:</b><br>\r\n" + 
				"The result of the operator is represented as a table with the extracted \r\n" + 
				"feature measures. Each row of the table refers to a single measure. In case of\r\n" + 
				"isotropic calculations there are 10 entries in the table referring to the 10\r\n" + 
				"Haralick measures as listed above. In the non-isotropic case for each direction\r\n" + 
				"10 values are present where the first 10 entries refer to the first direction, \r\n" + 
				"the second 10 entries to the second direction and so on.\r\n" + 
				"</p>\r\n" + 
				"</li>\r\n" + 
				"</ul>\r\n";
	}
}
