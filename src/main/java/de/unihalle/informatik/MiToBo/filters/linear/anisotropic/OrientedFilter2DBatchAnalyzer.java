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

package de.unihalle.informatik.MiToBo.filters.linear.anisotropic;

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2D.ApplicationMode;

/**
 * Base class for applying oriented filters in different orientations.
 * <p>
 * The operator applies a given oriented filter in different orientations 
 * to the given image. Subsequently all filter responses are merged into 
 * a final result using the specified mode for joining.
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, 
	level=Level.APPLICATION)
public class OrientedFilter2DBatchAnalyzer extends MTBOperator 
	implements StatusReporter {

	/**
	 * Method for joining the results from the various orientations.
	 */
	public static enum JoinMode {
		/**
		 * Result is the pixel-wise maximum over all orientations.
		 */
		JOIN_MAXIMUM,
		/**
		 * Result is the pixel-wise product over all orientations.
		 * <p>
		 * Prior to joining the responses they are all normalized to a common 
		 * interval of [0,1]. Note that this might cause problems if the 
		 * filter responses also include negative values. 
		 */
		JOIN_PRODUCT
	}
	
	/**
	 * Identifier string for this operator class.
	 */
	private static final String operatorID = 
			"[OrientedFilter2DBatchAnalyzer]";

	/** 
	 * Vector of installed StatusListeners.
	 */
	protected transient Vector<StatusListener> statusListeners;

	/**
	 * Input image to process.
	 */
	@Parameter( label= "Input Image", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Input image.", dataIOOrder=0)
	protected transient MTBImage inputImg = null;

	/**
	 * Oriented filter to apply.
	 */
	@Parameter( label= "Oriented Filter", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Oriented Filter to Apply.", dataIOOrder=1)
	protected OrientedFilter2D oFilter = null;

	/**
	 * Minimal orientation from where to start.
	 */
	@Parameter( label= "Minimal Orientation", required = true, 
		dataIOOrder = 2, direction= Parameter.Direction.IN, 
		mode=ExpertMode.ADVANCED, 
	  description = "Minimal orientation to consider (in degrees).")	
	protected int minAngle = 0;

	/**
	 * Maximal orientation where to end.
	 */
	@Parameter( label= "Maximal Orientation", required = true, 
		dataIOOrder = 3, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	  description = "Maximal orientation to consider (in degrees).")	
	protected int maxAngle = 180;

	/**
	 * Angular sampling step size.
	 */
	@Parameter( label= "Angular Sampling Steps", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Angular sampling step size (in degrees).", 
    dataIOOrder = 4)	
	protected int angleSampling = 15;

	/**
	 * Mode for joining results from different orientations.
	 */
	@Parameter( label= "Join Mode", required = true, dataIOOrder = 5,
		direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
    description = "Mode for joining different orientation responses.")	
	protected JoinMode jMode = JoinMode.JOIN_MAXIMUM;
	
	/**
	 * Result image.
	 */
	@Parameter( label= "Result Image", dataIOOrder = 0, 
		direction=Parameter.Direction.OUT, description = "Result image.")
	protected transient MTBImageDouble resultImg = null;
	
	/**
	 * Stack with filter responses of all orientations.
	 */
	@Parameter( label= "Filter Response Stack", dataIOOrder = 1, 
		direction=Parameter.Direction.OUT, 
		description = "Filter response stack.")
	protected transient MTBImageDouble responseStack = null;

	/**
	 * Map of indices of maximal responses.
	 * <p>
	 * The map is available after running the operator.
	 * Note that this is not an operator parameter because it is only 
	 * useful internally for some vessel filters.
	 */
	private transient MTBImageByte maxIndexMap = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public OrientedFilter2DBatchAnalyzer() throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>();
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		int angleRange = this.maxAngle - this.minAngle;
		int steps = angleRange/this.angleSampling;
		int width = this.inputImg.getSizeX();
		int height = this.inputImg.getSizeY();
		
		// apply all convolutions to image
		MTBImageDouble[] filteredImages = new MTBImageDouble[steps];
		this.oFilter.setInputImage(this.inputImg);
		// if our linear filter class is used, register the status listener
		if (this.oFilter.getApplicationMode() == ApplicationMode.STANDARD) {
			for (StatusListener l : this.statusListeners)
				this.oFilter.addStatusListener(l);
		}
		for (int s=0; s<steps; ++s) {
			double angle = 	this.angleSampling * s + this.minAngle;
			// if ImgLib2 FFT is applied, send status messages on our own
			if (this.oFilter.getApplicationMode() == ApplicationMode.FFT) {
				this.notifyListeners(new StatusEvent(
					(int)((double)(s+1)/(double)steps*100.0), 100, 
					operatorID + " analyzing angle of " + angle + " degrees..."));
			}
			this.oFilter.setAngle(angle);
			this.oFilter.runOp();
			filteredImages[s] = this.oFilter.getResultImage();
		}
		
		// figure out maximal response at each pixel position
		MTBImageDouble filterResponse;
		this.maxIndexMap = (MTBImageByte)MTBImage.createMTBImage(
			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.maxIndexMap.setTitle("Map of indices of maximal responses for "
			+ "<" + this.inputImg.getTitle() + ">");
		if (this.jMode == JoinMode.JOIN_MAXIMUM) {
			this.resultImg = (MTBImageDouble)filteredImages[0].duplicate();
			this.maxIndexMap.fillBlack();
			for (int s=1; s<steps; ++s) {
				filterResponse = filteredImages[s];
				for (int y=0; y<height; ++y) {
					for (int x=0; x<width; ++x) {
						if (  filterResponse.getValueDouble(x, y) 
								> this.resultImg.getValueDouble(x, y)) {
							this.resultImg.putValueDouble(x, y, 
									filterResponse.getValueDouble(x, y));
							this.maxIndexMap.putValueDouble(x, y, s);
						}
					}
				}
			}
		}
		else {
			// scale all responses to an interval of [0,1]
			for (int s=0; s<steps; ++s) {
				filteredImages[s] = filteredImages[s].scaleValues(0, 0, 
					filteredImages[s].getMinMaxDouble()[0], 
					filteredImages[s].getMinMaxDouble()[1], 0, 1);				
			}
			// calculate product of all normalized responses
			this.resultImg = (MTBImageDouble)filteredImages[0].duplicate();
			for (int s=1; s<steps; ++s) {
				filterResponse = filteredImages[s];
				for (int y=0; y<height; ++y) {
					for (int x=0; x<width; ++x) {
						this.resultImg.putValueDouble(x, y, 
							  this.resultImg.getValueDouble(x, y) 
							* filterResponse.getValueDouble(x, y));
					}
				}
			}			
		}
		this.resultImg.setTitle("Result of OrientedFilter2DBatchAnalysis for " 
			+ "<" + this.inputImg.getTitle() + ">");
		
		// create output image stack
		this.responseStack = (MTBImageDouble)(MTBImage.createMTBImage(
			width, height, 1, 1, steps, MTBImage.MTBImageType.MTB_DOUBLE));
		for (int s=0; s<steps; ++s) {
			this.responseStack.setImagePart(filteredImages[s], 0, 0, 0, 0, s);
			this.responseStack.setSliceLabel("Angle = " + 
				(this.angleSampling*s+this.minAngle) + " degrees", 0, 0, s);
		}
		this.responseStack.setTitle("Filter Responses of " + 
			"OrientedFilter2DBatchAnalysis for <" + this.inputImg.getTitle() + ">");
	}
	
	/**
	 * Specify the input image.
	 * @param img		Image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inputImg = img;
	}
	
	/**
	 * Specify the oriented filter to apply.
	 * @param filter	Pre-configured filter object.
	 */
	public void setOrientedFilter(OrientedFilter2D filter) {
		this.oFilter = filter;
	}

	/**
	 * Specify minimal angle to apply.
	 * @param min		Value of angle in degrees.
	 */
	public void setMinAngle(int min) {
		this.minAngle = min;
	}
	
	/**
	 * Specify maximal angle to apply.
	 * @param max		Maximal value of angle in degrees.
	 */
	public void setMaxAngle(int max) {
		this.maxAngle = max;
	}
	
	/**
	 * Set angular sampling interval.
	 * @param s		Sampling interval in degrees.
	 */
	public void setAngleSampling(int s) {
		this.angleSampling = s;
	}
	
	/**
	 * Get result image.
	 * @return	Filtered image.
	 */
	public MTBImageDouble getResultImage() {
		return this.resultImg;
	}

	/**
	 * Get individual filter responses.
	 * @return	Stack with filter responses.
	 */
	public MTBImageDouble getFilterResponseStack() {
		return this.responseStack;
	}

	/**
	 * Get map of indices of maximal responses.
	 * @return	Index map.
	 */
	public MTBImageByte getIndexMap() {
		return this.maxIndexMap;
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

