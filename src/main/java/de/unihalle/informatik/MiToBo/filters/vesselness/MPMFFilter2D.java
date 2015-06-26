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

package de.unihalle.informatik.MiToBo.filters.vesselness;

import java.util.Vector;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxxFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2DBatchAnalyzer;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.HysteresisThresholding;

/**
 * Multiscale Production of the Matched Filter (MPMF) implementation.
 * <p>
 * This operator applies matched filters on multiple scales. The results are 
 * then combined by pairwise multiplication followed by hysteresis thresholding
 * of each resulting product. Finally, all binary images are combined into a
 * single result map applying logical OR operations.
 * <p>
 * As matched filters a second order Gaussian derivative filter, i.e., a 
 * Mexican hat filter function is applied. Here we use MiToBo's implementation 
 * of such a filter which is to be found in class {@link GaussPDxxFilter2D}.
 * <p> 
 * Related publication:<br>
 * Q. Li, J. You, and D. Zhang, <i>"Vessel Segmentation and Width Estimation in 
 * Retinal Images using Multiscale Production of Matched Filter Responses"</i>,
 * Expert Systems with Applications, 39 (2012), pp. 7600-7610.
 * <p>
 * Here we assume vessels to exhibit a Gaussian profile (while in the paper
 * a box profile is assumed). The width of the vessels is defined as two-times 
 * the standard deviation of the Gaussian kernel mask. To normalize the filter
 * responses of different scales we follow the scheme proposed in 
 * <p>
 * Sofka and Stewart, <i>"Retinal Vessel Centerline Extraction Using Multiscale
 * Matched Filters, Confidence and Edge Measures"</i>, IEEE TMI, vol. 25, no. 12,
 * pp. 1531-1546, December 2006,
 * <p>
 * i.e. apply {@latex.inline $(\\sigma \\cdot \\sigma)^{\\frac{3}{4}}$} 
 * as normalization factor at scale {@latex.inline $\\sigma$}.
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, level=Level.APPLICATION)
public class MPMFFilter2D extends MTBOperator 
	implements StatusReporter {

	/**
	 * Factor to normalize filter responses of different scales appropriately.
	 * <p>
	 * See Sofka and Stewart, 2006, for details.
	 */
	private final static double scaleNormalizationFactor = 3.0/4.0;
	
	/**
	 * Factor to choose higher threshold in hysteresis thresholding automatically.
	 * <p>
	 * The threshold is determined by multiplying this factor with the intensity
	 * maximum to be found in the image to threshold. Its value has been chosen
	 * according to some empirical experiments.
	 */
	private final static double higherThresholdRatio = 0.05;
	
	/** 
	 * Vector of installed StatusListeners.
	 */
	protected transient Vector<StatusListener> statusListeners;

	/**
	 * Detection scenario mode.
	 */
	public static enum VesselMode {
		/**
		 * Detect dark vessels on bright background.
		 */
		DARK_ON_BRIGHT_BACKGROUND,
		/**
		 * Detect bright vessels on dark background.
		 */
		BRIGHT_ON_DARK_BACKGROUND
	}

	/**
	 * Input image to be processed.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = -10,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Input image.")
	protected transient MTBImage inputImg = null;

	/**
	 * Scenario for detecting vessels, i.e. relation of foreground to background.
	 */
	@Parameter( label= "Scenario", required = true, dataIOOrder = 1,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Relation of vessels to background.")
	protected VesselMode mode = VesselMode.DARK_ON_BRIGHT_BACKGROUND;
	
	/**
	 * Expected width of thin vessels.
	 */
	@Parameter( label= "Thin Vessel Width", required = true, dataIOOrder = 2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Expected width of thin vessels.")
	protected double minWidth = 1.0;

	/**
	 * Expected width of thick vessels.
	 */
	@Parameter( label= "Thick Vessel Width", required = true, dataIOOrder = 3,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Expected width of thick vessels.")
	protected double maxWidth = 7.0;

	/**
	 * Sampling step size for orientations considered.
	 */
	@Parameter( label= "Angular Sampling Steps", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Angular sampling step size (in degrees).", dataIOOrder = 5)	
	protected int angleSampling = 15;
	
	/**
	 * Higher threshold for binarization.
	 */
	@Parameter( label= "(Upper) Threshold", required = false, dataIOOrder = 0, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Binarization threshold, if set to -1 a threshold is " + 
    	"determined automatically.")	
	protected double threshold = -1;

	/**
	 * Resulting binary vessel map.
	 */
	@Parameter( label= "Result Map", dataIOOrder = 0, 
		direction=Parameter.Direction.OUT, description = "Resulting vessel map.")
	protected transient MTBImageByte resultVesselMap = null;

	/**
	 * Result stack with intermediate results.
	 * <p>
	 * Only generated in verbose mode.
	 */
	@Parameter( label= "Filter Response Stack", dataIOOrder = 1, 
		direction=Parameter.Direction.OUT, description = "Filter response stack.")
	private transient MTBImageDouble responseStack = null;

	/**
	 * Default constructor.
	 */
	public MPMFFilter2D() throws ALDOperatorException {
		super();
		this.statusListeners = new Vector<StatusListener>(1);
	}
	
	/**
	 * Returns the final binary map.
	 * @return	Binary result map.
	 */
	public MTBImageByte getBinaryResultMap() {
		return this.resultVesselMap;
	}

	/**
	 * Returns stack with intermediate (non-binary) results.
	 * <p>
	 * Note that the stack is only created if the verbose mode is activated.
	 * 
	 * @return	Stack with intermediate results.
	 */
	public MTBImageDouble getResponseStack() {
		return this.responseStack;
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.core.operator.MTBOperator#readResolve()
	 */
	@Override
	protected Object readResolve() {
		super.readResolve();
		this.statusListeners = new Vector<StatusListener>(1);
		return this;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.maxWidth < this.minWidth)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[MPMFFilter2D] thin vessels are wider than thick ones...?!");
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		int width = this.inputImg.getSizeX();
		int height = this.inputImg.getSizeY();
		
		// init Gaussian filter
		GaussPDxxFilter2D gFilter = new GaussPDxxFilter2D();
		gFilter.setInputImage(this.inputImg);
		if (this.mode == VesselMode.DARK_ON_BRIGHT_BACKGROUND)
			gFilter.setInvertMask(false);
		else
			gFilter.setInvertMask(true);
		for (StatusListener l : this.statusListeners)
			gFilter.addStatusListener(l);	
		
		// run the filter for the different scales
		MTBImageDouble[] filterResponses = new MTBImageDouble[3];
		OrientedFilter2DBatchAnalyzer bOp = new OrientedFilter2DBatchAnalyzer();
		for (StatusListener l : this.statusListeners)
			bOp.addStatusListener(l);
		bOp.setInputImage(this.inputImg);
		bOp.setOrientedFilter(gFilter);
		bOp.setAngleSampling(this.angleSampling);
		
		// first scale for thin vessels
		double minSigma = this.minWidth / 2.0;
		gFilter.setStandardDeviation(minSigma);
		bOp.runOp();
		filterResponses[0] = bOp.getResultImage();
		double normFactor = Math.pow(minSigma*minSigma, scaleNormalizationFactor);
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				filterResponses[0].putValueDouble(x, y, 
					filterResponses[0].getValueDouble(x, y) * normFactor);
			}
		}
		// third scale for thick vessels
		double maxSigma = this.maxWidth / 2.0;
		gFilter.setStandardDeviation(maxSigma);
		bOp.runOp();
		filterResponses[2] = bOp.getResultImage();
		normFactor = Math.pow(maxSigma*maxSigma, scaleNormalizationFactor);
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				filterResponses[2].putValueDouble(x, y, 
					filterResponses[2].getValueDouble(x, y) * normFactor);
			}
		}
		// medial scale
		double middleScale = this.findMiddleScale(minSigma, maxSigma);
		gFilter.setStandardDeviation(middleScale);		
		bOp.runOp();
		filterResponses[1] = bOp.getResultImage();
		normFactor = Math.pow(middleScale*middleScale, scaleNormalizationFactor);
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				filterResponses[1].putValueDouble(x, y, 
					filterResponses[1].getValueDouble(x, y) * normFactor);
			}
		}
		
		// product of first two scales
		MTBImageDouble p12 = (MTBImageDouble)filterResponses[0].duplicate();
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				p12.putValueDouble(x, y, 
					p12.getValueDouble(x, y) * filterResponses[1].getValueDouble(x, y));
			}
		}
		// product of second and third scale
		MTBImageDouble p23 = (MTBImageDouble)filterResponses[1].duplicate();
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				p23.putValueDouble(x, y, 
					p23.getValueDouble(x, y) * filterResponses[2].getValueDouble(x, y));
			}
		}
		
		HysteresisThresholding hOp = new HysteresisThresholding();

		// binarize both productions using Canny hysteresis thresholding;
		// unfortunately the paper does not state how to choose the higher 
		// threshold, just its relation to the lower threshold is specified...
		double threshHigh = this.threshold;
		if (threshHigh < 0) {
			// automatically find the threshold (empirical heuristic!)
			double maxVal = p12.getMinMaxDouble()[1];
			threshHigh = higherThresholdRatio * maxVal;
		}
		double threshLow = threshHigh / 2.0;		
		hOp.setLowerThreshold(threshLow);
		hOp.setHigherThreshold(threshHigh);
		hOp.setInputImage(p12);
		hOp.runOp();
		MTBImageByte p12_thresholded = hOp.getResultImage();
		
		threshHigh = this.threshold;
		if (threshHigh < 0) {
			// automatically find the threshold (empirical heuristic!)
			double maxVal = p23.getMinMaxDouble()[1];
			threshHigh = higherThresholdRatio * maxVal;
			this.threshold = threshHigh;
		}
		threshLow = threshHigh / 2.0;		
		hOp.setLowerThreshold(threshLow);
		hOp.setHigherThreshold(threshHigh);
		hOp.setInputImage(p23);
		hOp.runOp();
		MTBImageByte p23_thresholded = hOp.getResultImage();
		
		// logical OR of both binarized images
		this.resultVesselMap = (MTBImageByte)MTBImage.createMTBImage(
			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.resultVesselMap.fillBlack();
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				if (   p12_thresholded.getValueInt(x, y) > 0
						|| p23_thresholded.getValueInt(x, y) > 0)
					this.resultVesselMap.putValueInt(x, y, 255);
			}
		}
		this.resultVesselMap.setTitle("Result of MFFDOGMultiScale Filter for " 
				+ "<" + this.inputImg.getTitle() + ">");
		
		// create output image stack
		if (this.verbose.booleanValue()) {
			this.responseStack = (MTBImageDouble)(MTBImage.createMTBImage(
					width, height, 1, 1, 5, MTBImage.MTBImageType.MTB_DOUBLE));
			this.responseStack.setImagePart(filterResponses[0], 0, 0, 0, 0, 0);
			this.responseStack.setSliceLabel("Filter response scale 1", 0, 0, 0);
			this.responseStack.setImagePart(filterResponses[1], 0, 0, 0, 0, 1);
			this.responseStack.setSliceLabel("Filter response scale 2", 0, 0, 1);
			this.responseStack.setImagePart(filterResponses[2], 0, 0, 0, 0, 2);
			this.responseStack.setSliceLabel("Filter response scale 3", 0, 0, 2);
			this.responseStack.setImagePart(p12, 0, 0, 0, 0, 3);
			this.responseStack.setSliceLabel("Scale product P_12", 0, 0, 3);
			this.responseStack.setImagePart(p23, 0, 0, 0, 0, 4);
			this.responseStack.setSliceLabel("Scale product P_23", 0, 0, 4);
			this.responseStack.setTitle("Scale Responses of MPMF scheme " 
				+ "for <" + this.inputImg.getTitle() + ">");
		}
	}
	
	/**
	 * Numerically finds the middle scale between to given scales.
	 * 
	 * @param minScale	Scale of thin vessels (given as standard deviation). 
	 * @param maxScale Scale of thick vessels (given as standard deviation).
	 * @return	Standard deviation of middle scale.
	 */
	protected double findMiddleScale(double minScale, double maxScale) {
		try {
			UnivariateFunction function = new ResponseDifference(minScale, maxScale);
			final double relativeAccuracy = 1.0e-12;
			final double absoluteAccuracy = 1.0e-8;
			final int    maxOrder         = 5;
			UnivariateSolver solver   = new BracketingNthOrderBrentSolver(
					relativeAccuracy, absoluteAccuracy, maxOrder);
			return solver.solve(100, function, minScale, maxScale);
		} catch(Exception ex) {
			System.err.println("[MPMFFilter2D] searching middle scale failed, " + 
				"falling back to simple average!");
			// if calculations fail just take the average of both scales...
			return (maxScale + minScale) / 2.0;
		}
	}
	
	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#addStatusListener(loci.common.StatusListener)
	 */
	@Override
	public void addStatusListener(StatusListener statuslistener) {
		this.statusListeners.add(statuslistener);
	}

	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#notifyListeners(loci.common.StatusEvent)
	 */
	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.statusListeners.size(); i++) {
			this.statusListeners.get(i).statusUpdated(e);
		}
	}

	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#removeStatusListener(loci.common.StatusListener)
	 */
	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.statusListeners.remove(statuslistener);
	}
	
	/**
	 * Function to find roots during middle scale calculation.
	 * <p>
	 * This function basically calculates the difference between the filter
	 * responses of the Gaussian model vessel to the matched filter kernel 
	 * given a certain standard deviation of the kernel.
	 * 
	 * @author moeller
	 */
	private class ResponseDifference implements UnivariateFunction {
		
		/**
		 * Lower vessel scale.
		 */
		private double minScale;
		
		/**
		 * Upper vessel scale.
		 */
		private double maxScale;
		
		/**
		 * Default constructor.
		 * @param minS	Lower vessel scale.
		 * @param maxS Upper vessel scale.
		 */
		public ResponseDifference(double minS, double maxS) {
			this.maxScale = maxS;
			this.minScale = minS;
		}
		
    /* (non-Javadoc)
     * @see org.apache.commons.math3.analysis.UnivariateFunction#value(double)
     */
    @Override
    public double value(double filterStdDev) {
    	// optimal filter response for minimal scale
    	double minScaleResponse = 
    		MPMFFilter2D.normalizedFilterResponse(this.minScale, filterStdDev);
    	double maxScaleResponse = 
     		MPMFFilter2D.normalizedFilterResponse(this.maxScale, filterStdDev);
    	// result value is the difference of both values
    	return minScaleResponse - maxScaleResponse;
    }
	}
	
  /**
   * Computes the (ideal) filter response to the Gaussian derivative kernel.
   * <p>
   * The function returns the result of convolving a Gaussian function 
   * centered at {@latex.inline $x=0$} and having a standard deviation of 
   * {@latex.inline %preamble{\\usepackage{amssymb, amsmath}} 
   * $\\sigma=\\text{vScale}$} (defining the model of a vessel)
   * with a second derivative Gaussian matched filter with standard deviation 
   * {@latex.inline %preamble{\\usepackage{amssymb, amsmath}} 
   * $\\sigma=\\text{fScale}$}.The result is normalized as 
   * proposed in Sofka et al.
   * 
   * @param vScale	Standard deviation of the Gaussian vessel function.
   * @param fScale	Standard deviation of the matched filter kernel.
   * @return	Normalized filter response.
   */
  protected static double normalizedFilterResponse(	
  		double vScale,double fScale) {
  	double fScale_2 = fScale * fScale;
  	double vScale_2 = vScale * vScale;

  	double normFactor = Math.pow(vScale_2, scaleNormalizationFactor);
  	double firstFactor = -1 / (2*Math.PI*vScale*Math.pow(fScale,5.0));

  	double alpha = (2 * vScale_2 * fScale_2) / ( vScale_2 + fScale_2);

  	double minuend = 1.0/2.0 * Math.sqrt(Math.PI * Math.pow(alpha, 3.0));
  	double subtrahend = fScale_2 * Math.sqrt(Math.PI * alpha);
  	return normFactor * firstFactor * ( minuend - subtrahend);
  }
}

