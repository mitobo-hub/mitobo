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
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.linear.LinearFilter;

/**
 * Base class for anisotropic linear filters in 2D.
 * 
 * @author Birgit Moeller
 */
public abstract class OrientedFilter2D extends MTBOperator 
	implements StatusReporter {

	/**
	 * Input image to process.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = -10,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Input image.")
	protected transient MTBImage inputImg = null;

	/**
	 * Orientation of the filter.
	 */
	@Parameter( label= "Orientation", required = true, dataIOOrder = -9,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
    description = "Orientation for which to apply the filter (in degrees).")	
	protected Double angle = new Double(0.0);

	/**
	 * Filtered image.
	 */
	@Parameter( label= "Result Image", dataIOOrder = -10, 
			direction=Parameter.Direction.OUT, description = "Result image.")
	protected transient MTBImageDouble resultImg = null;

	/** 
	 * Vector of installed objects of type {@link StatusListener}.
	 */
	protected transient Vector<StatusListener> statusListeners;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public OrientedFilter2D() throws ALDOperatorException {
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

	/**
	 * Calculates kernel for the given orientation.
	 * @param _angle		Orientation of filter (in degrees).
	 * @return	Kernel image.
	 */
	public abstract MTBImageDouble getKernel(double _angle);
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// apply convolution mask to image
		MTBImageDouble kernel;
		kernel = this.getKernel(this.angle.doubleValue());
		LinearFilter lf = new LinearFilter();
		for (StatusListener l : this.statusListeners)
			lf.addStatusListener(l);
		lf.setInputImg(this.inputImg);
		lf.setKernelImg(kernel); 
		lf.setKernelNormalization(false);
		lf.setResultImageType(MTBImageType.MTB_DOUBLE);
		lf.runOp();
		this.resultImg = (MTBImageDouble)lf.getResultImg();
	}

	/**
	 * Set input image to analyze.
	 * @param img		Input image.
	 */
	public void setInputImage(MTBImage img) {
		this.inputImg = img;
	}

	/**
	 * Set orientation of filter.
	 * @param _angle	Orientation to use.
	 */
	public void setAngle(double _angle) {
		this.angle = new Double(_angle);
	}

	/**
	 * Get result image.
	 * @return	Filtered image.
	 */
	public MTBImageDouble getResultImage() {
		return this.resultImg;
	}

	// ----- StatusReporter interface
	@Override
	public void addStatusListener(StatusListener statuslistener) {
		this.statusListeners.add(statuslistener);
	}

	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.statusListeners.size(); i++) {
			this.statusListeners.get(i).statusUpdated(e);
		}
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.statusListeners.remove(statuslistener);
	}
	
}

