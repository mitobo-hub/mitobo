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

import ij.process.ImageProcessor;

import java.util.List;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;

/**
 * Abstract super class for LBP operators building on the JFeatureLib.
 * <p>
 * Operators implementing LBPs based on the JFeatureLib are expected to 
 * support neighborhoods with a given radius and number of neighboring pixels.
 * In addition they are expected to either return LBP code histograms for each
 * pixel in an image or for the image as a whole.
 * 
 * @author Alexander Weiss
 * @author Birgit Moeller
 */

public abstract class FeatureCalculatorLBPJFeatureLib 
	extends FeatureCalculator { 	
	
	/**
	 * Type of feature to be extracted.
	 */
	public static enum FeatureType {
		/**
		 * Returns a histogram of LBP codes for each pixel.
		 */
		PER_PIXEL_HISTOS,
		/**
		 * Returns a single histogram of all the LBP codes in the image.
		 * <p>
		 * Note: the parameter 'histoRegionSize' is ignored! And be 
		 * careful, if the image is large, the calculation takes a very 
		 * long time since the implementation is not yet optimized for 
		 * this use case. 
		 */
		IMAGE_HISTO
	}
	
	/**
	 * Type of result features to be returned.
	 */
	@Parameter(label = "Type of Result", required = true,
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Kind of feature vectors to be returned.", 
		dataIOOrder = 1)
	protected FeatureType resultType = FeatureType.PER_PIXEL_HISTOS;

	/**
	 * Number of neighboring pixels to consider for LBP codes.
	 */
	@Parameter(label = "Number of Neighbor Pixels", required = true,
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Number of neighbor pixels to consider.", 
		dataIOOrder = 2)
	protected int numberNeighbors = 8;
	
	/**
	 * Neighborhood radius for LBP code calculation.
	 */
	@Parameter(label = "Radius of Neighborhood", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Radius of neighborhood around center.", 
		dataIOOrder = 3)
	protected double radius = 1.0;
	
	/**
	 * Size of region for histogram binning.
	 */
	@Parameter(label = "Size of Histogram Region", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Size of region used for histogram binning.", 
		dataIOOrder = 4)
	protected int histoRegionSize = 1;
	
	/**
	 * Number of histogram bins.
	 */
	@Parameter(label = "Number of Histograms Bins", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Number of Histogram Bins.", dataIOOrder = 5)
	protected int histBins = 8;
	
	/**
	 * Baseline constant.
	 */
	@Parameter(label = "Baseline Constant in Thresholding", 
		required = true, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 6, 
		description = "Baseline constant used in thresholding.")
	protected double baselineConstant = 0.0;

	/**
	 * Get instance of actual feature calculator.
	 * @return	Feature calculator object.
	 */
	protected abstract JFeatureLibLBPAdapter getFeatureOp();
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureCalculatorLBPJFeatureLib() throws ALDOperatorException {
		// nothing to do here
	}

	@Override
	public void operate() {

		// extract ImageJ image of input image
		ImageProcessor ip = this.inImg.getImagePlus().getProcessor();

		// init feature extractor
		JFeatureLibLBPAdapter fOp = this.getFeatureOp();
		if (this.resultType == FeatureType.PER_PIXEL_HISTOS) {
			fOp.setNeighborhoodSize(this.histoRegionSize);
			fOp.run(ip);
		}
		else {
			fOp.setNeighborhoodSize((int)(this.inImg.getSizeX()/2.0));
		}
		List<double[]> features = fOp.getFeatures();

		if (this.resultType == FeatureType.PER_PIXEL_HISTOS) {
			// note: the first two features are the x-/y-coordinates which 
			//       are to be ignored...
			double[] resultVec = 
					new double[features.size()*(features.get(0).length-2)];
			int i=0;
			for (double[] fVec: features) {
				for (int j=2; j<fVec.length; ++j) {
					resultVec[i] = fVec[j];
					++i;
				}
			}		
			this.resultObj = new FeatureCalculatorLBPResult(resultVec);
		}
		else {
			// just extract histogram of image center pixel
			int centerX = (int)(this.inImg.getSizeX()/2.0 + 0.5);
			int centerY = (int)(this.inImg.getSizeY()/2.0 + 0.5);
			fOp.setImageProcessor(ip);
			double[] featureVec = fOp.getLBPHistogram(centerX, centerY);
			double[] resultVec = new double[featureVec.length-2];
			System.arraycopy(featureVec, 2, resultVec, 0, featureVec.length-2);
			this.resultObj = new FeatureCalculatorLBPResult(resultVec);			
		}
	}
	
	/**
	 * Specify region for histogram binning.
	 * @param n	Size of region.
	 */
	public void setHistoRegionSize(int n) {
		this.histoRegionSize = n;
	}
	
	/**
	 * Specify number of histogram bins.
	 * @param n	Number of bins to use.
	 */
	public void setHistBins(int n) {
		this.histBins = n;
	}
	
	
	/**
	 * Specify number of neighbors to consider for LBP code calculation.
	 * @param n	Number of neighbors.
	 */
	public void setNumberNeighbors(int n) {
		this.numberNeighbors = n;
	}
	
	/**
	 * Specify radius of neighborhood for LBP code calculation.
	 * @param r	Radius of neighborhood.
	 */
	public void setRadius(double r) {
		this.radius = r;
	}
	
	/**
	 * Specify baseline constant for thresholding.
	 * @param c	Constant to apply.
	 */
	public void setBaselineConstant(double c) {
		this.baselineConstant = c;
	}
	
	/**
	 * Specify type of features to be calculated.
	 * @param t	Feature type to extract.
	 */
	public void setFeatureType(FeatureType t) {
		this.resultType = t;
	}
	
	@Override
	protected FeatureCalculatorLBPResult getResultDataObjectInvalid(
			int dim) {
		double[] nanResult = new double[dim];
		for (int i=0; i<dim; ++i)
			nanResult[i] = Double.NaN;
		return new FeatureCalculatorLBPResult(nanResult);
	}
}
