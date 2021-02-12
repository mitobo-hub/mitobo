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
import java.util.concurrent.ExecutorService;
import net.imglib2.Cursor;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.real.FloatType;
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
 * <p>
 * This operator supports two modes how to compute the convolution. 
 * The first mode 'STANDARD' is based on the plain implementation of 
 * a convolution, i.e. relies on class {@link LinearFilter}.
 * The second mode 'FFT' makes use of the fast implementations of 
 * convolutions available in ImgLib2. In particular, we apply FFT 
 * convolution which is almost two times faster than the implementation 
 * of linear filtering from scratch to be found in 
 * {@link OrientedFilter2D}. 
 * <p>
 * Please note that the energy of the input image might be changed by 
 * the convolution if mode 'FFT' is applied since the masks of the 
 * anisotropic kernels are normalized to a sum of zero rather than 1 as 
 * usual.
 * <p>
 * The code for convolving the input image is leaned on example 6b
 * to be found at the ImgLib2  
 * <a href="http://fiji.sc/ImgLib2_Examples">web page</a>.
 * 
 * @author Birgit Moeller
 */
public abstract class OrientedFilter2D extends MTBOperator 
	implements StatusReporter {

	/**
	 * Modes how to apply the filter to an image.
	 */
	public static enum ApplicationMode {
		/**
		 * Use standard implementation of convolution.
		 */
		STANDARD,
		/**
		 * Use ImgLib2 FFT convolution (faster). 
		 */
		FFT
	}
	
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
    description = "Orientation of the filter to apply (in degrees).")	
	protected Double angle = new Double(0.0);

	/**
	 * Mode of application.
	 */
	@Parameter( label= "Application Mode", required = true, 
		dataIOOrder = -8, direction= Parameter.Direction.IN, 
		mode=ExpertMode.ADVANCED, description = "Computation mode.")	
	protected ApplicationMode mode = ApplicationMode.FFT;	
	
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
	 * @throws ALDOperatorException Thrown in case of failure.
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
		
		switch(this.mode) 
		{
		case STANDARD:
			LinearFilter lf = new LinearFilter();
			for (StatusListener l : this.statusListeners)
				lf.addStatusListener(l);
			lf.setInputImg(this.inputImg);
			lf.setKernelImg(kernel); 
			lf.setKernelNormalization(false);
			lf.setResultImageType(MTBImageType.MTB_DOUBLE);
			lf.runOp();
			this.resultImg = (MTBImageDouble)lf.getResultImg();
			break;
		case FFT:
			int iWidth = this.inputImg.getSizeX();
			int iHeight = this.inputImg.getSizeY();

			// create an image factory that will instantiate the ImgLib2 images
			final ImgFactory<FloatType> imgFactory = 
				new CellImgFactory<FloatType>( 1 );
	    final Img<FloatType> iLibImg = imgFactory.create( 
	      	new long[]{ iWidth, iHeight, 1, 1},	new FloatType() );
			// copy data from input image to ImgLib2 image
	    Cursor<FloatType> iLibCursor = iLibImg.cursor();
	    for (int y=0; y<iHeight; ++y) {
	    	for (int x=0; x<iWidth; ++x) {
	    		iLibCursor.fwd();
	    		iLibCursor.get().set((float)this.inputImg.getValueDouble(x,y));
	    	}
	    }

	    // create ImgLib2 kernel image
	    int kernelWidth = kernel.getSizeX();
	    int kernelHeight = kernel.getSizeY();
	    final Img<FloatType> iLibKernel = imgFactory.create( 
	    		new long[]{ kernelWidth, kernelHeight, 1, 1},	new FloatType());
	    // set data
	    iLibCursor = iLibKernel.cursor();
	    for (int y=0; y<kernelHeight; ++y) {
	    	for (int x=0; x<kernelWidth; ++x) {
	    		iLibCursor.fwd();
	    		iLibCursor.get().set((float)kernel.getValueDouble(x,y));
	    	}
	    }

	    // compute fourier convolution (in-place)
	    ExecutorService exs = FFTConvolution.createExecutorService();
	    FFTConvolution<FloatType> fftc = 
	    	new FFTConvolution<FloatType>( iLibImg, iLibKernel, exs );
	    fftc.convolve();
	    // Important: we need to terminate all threads explicitly, since 
	    // 		automatic termination seems not work if operator is run 
	    //    from commandline...
	    exs.shutdown();

	    // copy data to result image
	    this.resultImg = (MTBImageDouble)MTBImage.createMTBImage(
	    		iWidth, iHeight, 1, 1, 1, MTBImageType.MTB_DOUBLE);
	    iLibCursor = iLibImg.cursor();
	    for (int y=0; y<iHeight; ++y) {
	    	for (int x=0; x<iWidth; ++x) {
	    		iLibCursor.fwd();
	    		this.resultImg.putValueDouble(x, y, iLibCursor.get().get());   				
	    	}
	    }
	    break;
		}
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
	 * Set application mode for the filter.
	 * @param m	Mode in which the filter should run.
	 */
	public void setApplicationMode(ApplicationMode m) {
		this.mode = m;
	}

	/**
	 * Get application mode.
	 * @return	Application mode.
	 */
	public ApplicationMode getApplicationMode() {
		return this.mode;
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

