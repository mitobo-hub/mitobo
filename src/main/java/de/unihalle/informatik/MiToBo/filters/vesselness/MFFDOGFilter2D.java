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
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.linear.MeanFilter;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.ChaudhuriMatchedFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2DBatchAnalyzer;
import de.unihalle.informatik.MiToBo.math.images.ImageStatistics;
import de.unihalle.informatik.MiToBo.math.images.ImageStatistics.StatValue;

/**
 * MF-FDOG filter for vessel segmentation.
 * <p>
 * This filter, the matched filter with first-order derivative of Gaussian,
 * targets at segmenting vessel-like structures. Related publication:
 * <p>
 * B. Zhang, L. Zhang, L. Zhang and F. Karray, 
 * "Retinal vessel extraction by matched filter with first-order derivative
 *  of Gaussian",<br>
 * Comp. in Biology and Medicine, vol. 40 (2010), pp. 438-445.  
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, level=Level.APPLICATION)
public class MFFDOGFilter2D extends MTBOperator implements StatusReporter {

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
	 * Image to process.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = -10,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Input image.")
	protected transient MTBImage inputImg = null;

	/**
	 * Mode for detecting vessels.
	 */
	@Parameter( label= "Scenario", required = false, dataIOOrder = 1,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Relation of vessels to background.")
	protected VesselMode mode = VesselMode.DARK_ON_BRIGHT_BACKGROUND;
	
	/**
	 * Expected width of vessels.
	 * <p>
	 * The width is used to select the standard deviation of the Gaussian,
	 * i.e. {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
	 * $\\sigma = 2 \\cdot \\text{width}$}.
	 */
	@Parameter(label= "Expected Vessel Width", required = false, dataIOOrder = 2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Expected size of vessels.")
	protected Double expectedWidth = new Double(4.0);

	/**
	 * Expected length of vessel segments.
	 * <p>
	 * The length of the vessels is used to set the height of the filter masks.
	 */
	@Parameter( label= "Length", required = false, dataIOOrder = 3,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
    description = "Length of vessel segments.")	
	protected Integer length = new Integer(9);

	/**
	 * Angular sampling interval.
	 */
	@Parameter( label= "Angular Sampling Steps", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Angular sampling step size (in degrees).", dataIOOrder = 4)	
	protected int angleSampling = 15;
	
	/**
	 * Size of mean filter for Gaussian derivative image.
	 */
	@Parameter( label= "Size of Mean Filter", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Size of mean filter.", dataIOOrder = 5)	
	protected int meanFilterSize = 31;
	
	/**
	 * Threshold scaling constant.
	 */
	@Parameter( label= "Threshold Scaling Constant", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	  description = "Constant for scaling local threshold, " + 
	  	"usually from the interval [2,3].", dataIOOrder = 6)	
	protected double threshScalingConstant = 2;

	/**
	 * Final vessel map.
	 */
	@Parameter( label= "Result Map", dataIOOrder = 0, 
			direction=Parameter.Direction.OUT, description = "Resulting vessel map.")
	protected transient MTBImageByte resultVesselMap = null;

	/**
	 * Default constructor.
	 */
	public MFFDOGFilter2D() throws ALDOperatorException {
		super();
		this.statusListeners = new Vector<StatusListener>(1);
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
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		int width = this.inputImg.getSizeX();
		int height = this.inputImg.getSizeY();
		
		// check the mode
		boolean invertMatchedFilter = false;
		boolean invertGaussFilter = false;
		if (this.mode == VesselMode.DARK_ON_BRIGHT_BACKGROUND) {
			invertMatchedFilter = false;
			invertGaussFilter = false;
		}
		else {
			invertMatchedFilter = true;
			invertGaussFilter = true;
		}
		
		// apply MF filter to image
		ChaudhuriMatchedFilter2D mFilter = new ChaudhuriMatchedFilter2D();
		mFilter.setInputImage(this.inputImg);
		mFilter.setInvertMask(invertMatchedFilter);
		mFilter.setStandardDeviation(this.expectedWidth.doubleValue()/2.0);
		mFilter.setHeight(this.length.intValue());
		OrientedFilter2DBatchAnalyzer bOp = new OrientedFilter2DBatchAnalyzer();
		for (StatusListener l : this.statusListeners)
			bOp.addStatusListener(l);		
		bOp.setInputImage(this.inputImg);
		bOp.setOrientedFilter(mFilter);
		bOp.setAngleSampling(this.angleSampling);
		bOp.runOp();
		MTBImageDouble mfResult = bOp.getResultImage();
		
		// apply FDOG filter to image
		GaussPDxFilter2D gFilter = new GaussPDxFilter2D();
		gFilter.setInputImage(this.inputImg);
		gFilter.setInvertMask(invertGaussFilter);
		gFilter.setStandardDeviation(this.expectedWidth.doubleValue()/2.0);
		gFilter.setHeight(this.length.intValue());
		bOp = new OrientedFilter2DBatchAnalyzer();
		for (StatusListener l : this.statusListeners)
			bOp.addStatusListener(l);		
		bOp.setInputImage(this.inputImg);
		bOp.setOrientedFilter(gFilter);
		// for this filter orientations alpha and alpha + 180 are not the same!
		bOp.setMaxAngle(360);
		bOp.setAngleSampling(this.angleSampling);
		bOp.runOp();
		MTBImageDouble fdogResult = bOp.getResultImage();

		// apply mean filter to FDOG result
		Integer filterSize = new Integer(this.meanFilterSize);
		MeanFilter meanF = new MeanFilter(fdogResult , filterSize,
				filterSize, new Integer(1), new Integer(1), new Integer(1),
				BoundaryPadding.PADDING_BORDER);
		for (StatusListener l : this.statusListeners)
			meanF.addStatusListener(l);		
		meanF.runOp();
		MTBImageDouble meanImage = (MTBImageDouble)meanF.getResultImg();
			
		// scale image to [0,1]
		MTBImageDouble scaledMeanImage = meanImage.scaleValues(0, 0, 
				meanImage.getMinMaxDouble()[0],	meanImage.getMinMaxDouble()[1], 0, 1);

		// threshold matched filtered image to produce final binary map
		this.resultVesselMap = (MTBImageByte)MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		ImageStatistics statsOp = 
				new ImageStatistics(mfResult, StatValue.INTENSITY_MEAN);
		statsOp.runOp();
		double mu_H = statsOp.getResultValue();
		double T_c = this.threshScalingConstant * mu_H;
		double T;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				T = (1 + scaledMeanImage.getValueDouble(x,y)) * T_c;
				if (mfResult.getValueDouble(x, y) >= T)
					this.resultVesselMap.putValueInt(x, y, 255);
				else
					this.resultVesselMap.putValueInt(x, y, 0);
			}
		}
	}
	
	/**
	 * Specify input image.
	 * @param img		Image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inputImg = img;
	}

	/**
	 * Specify detection scenario.
	 * @param m		Mode to assume.
	 */
	public void setVesselMode(VesselMode m) {
		this.mode = m;
	}

	/**
	 * Specify width of vessels.
	 * @param w		Expected width of vessels.
	 */
	public void setWidth(Double w) {
		this.expectedWidth = w;
	}
	
	/**
	 * Specify length of vessels.
	 * @param l		Length value to apply.
	 */
	public void setLength(Integer l) {
		this.length = l;
	}
	
	/**
	 * Specify angular sampling.
	 * @param s		Size of angular sampling interval.
	 */
	public void setSampling(int s) {
		this.angleSampling = s;
	}

	/**
	 * Get result.
	 * @return	Binary vessel map.
	 */
	public MTBImageByte getResultVesselMap() {
		return this.resultVesselMap;
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
}

