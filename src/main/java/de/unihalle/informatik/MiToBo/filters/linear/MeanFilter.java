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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.filters.linear;

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Multidimensional mean filter. * 
 * 
 * @author posch
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class MeanFilter extends MTBOperator implements StatusReporter {

	/** vector of installed StatusListeners */
	private transient Vector<StatusListener> statusListeners;

	@Parameter( label= "Input Image", required = true, 
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
			description = "Input image", dataIOOrder=1)
	private transient MTBImage inputImg = null;
	
	@Parameter( label= "Result Image", required = true, 
			direction=Parameter.Direction.OUT, description = "Result image", dataIOOrder=1)
	private transient MTBImage resultImg = null;
	
	@Parameter( label= "sizeX", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	    description = "Size in x-dimension (Default: 1 micron)", dataIOOrder=3)
	private Integer sizeX = 3;

	@Parameter( label= "sizeY", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	    description = "Size in y-dimension (Default: 1 micron)", dataIOOrder=4)	
	private Integer sizeY = 3;

	@Parameter( label= "sizeZ", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	    description = "Size in z-dimension (Default: 0)", dataIOOrder=5)
	private Integer sizeZ = 0;

	@Parameter( label= "sizeT", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	    description = "Size in t-dimension (Default: 0)", dataIOOrder=6)
	private Integer sizeT = 0;

	@Parameter( label= "sizeC", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	    description = "Size in c-dimension (Default: 0)", dataIOOrder=7)
	private Integer sizeC = 0;
	
	@Parameter( label= "Boundary padding", required = true, 
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
            description = "Image is padded by the specified method", dataIOOrder=8)
  private BoundaryPadding boundaryPadding = BoundaryPadding.PADDING_BORDER;
	
	@Parameter( label= "Type of result image", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=9, description = "Type of result image (Default: MTB_DOUBLE)")
	private MTBImage.MTBImageType resultImageType = MTBImageType.MTB_DOUBLE;

	/**
	 * Constructor with default parameters sizeX=1, sizeY=1, sizeZ=0, sizeT=0, sizeC=0, kernelTruncation=3, sizeInterpretation=PHYSICALSIZE.
	 * An input image must be specified before running this operator.
	 *
	 */
	public MeanFilter() throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>(1);
	}
	
	
	/**
	 * Constructor for mean filtering in x- and y-dimension.
	 * Default for other parameters: sizeZ=0, sizeT=0, sizeC=0, kernelTruncation=3, sizeInterpretation=PHYSICALSIZE,
	 * boundaryPadding=PADDING_BORDER
	 * @param img input image
	 * @param sizeX standard deviation in x-dimension
	 * @param sizeY standard deviation in y-dimension
	 */
	public MeanFilter(MTBImage img,
			Integer sizeX,
			Integer sizeY) throws ALDOperatorException {

		this.statusListeners = new Vector<StatusListener>(1);
		
		this.setInputImg(img);
		this.setSizeX(sizeX);
		this.setSizeY(sizeY);
		this.setBoundaryPadding(BoundaryPadding.PADDING_BORDER);
	}
	
	/**
	 * Constructor for mean filtering in x-, y- and z-dimension.
	 * Default for other parameters: sizeT=0, sizeC=0, kernelTruncation=3, sizeInterpretation=PHYSICALSIZE
	 * boundaryPadding=PADDING_BORDER
	 * @param img input image
	 * @param sizeX standard deviation in x-dimension
	 * @param sizeY standard deviation in y-dimension
	 * @param sizeZ standard deviation in z-dimension
	 */
	public MeanFilter(MTBImage img,
			Integer sizeX,
			Integer sizeY,
			Integer sizeZ) throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>(1);
		
		this.setInputImg(img);
		this.setSizeX(sizeX);
		this.setSizeY(sizeY);
		this.setSizeZ(sizeZ);
		this.setBoundaryPadding(BoundaryPadding.PADDING_BORDER);
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
	 * Constructor for mean filtering. Set any size to 0 to prevent filtering in the corresponding dimension
	 * @param img input image
	 * @param sizeX standard deviation in x-dimension
	 * @param sizeY standard deviation in y-dimension
	 * @param sizeZ standard deviation in z-dimension
	 * @param sizeT standard deviation in t-dimension
	 * @param sizeC standard deviation in c-dimension
	 * @param boundaryPadding determines how pixel values outside the image domain are treated (see {@link LinearFilter})
	 */
	public MeanFilter(MTBImage img,
			Integer sizeX,
			Integer sizeY,
			Integer sizeZ,
			Integer sizeT,
			Integer sizeC,
						  BoundaryPadding boundaryPadding) throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>(1);
		
	    this.setInputImg(img);
	    this.setSizeX(sizeX);
	    this.setSizeY(sizeY);
		this.setSizeZ(sizeZ);
		this.setSizeT(sizeT);
		this.setSizeC(sizeC);
		this.setBoundaryPadding(boundaryPadding);
	}

	
	/**
	 * This function calls the actual mean filtering method. 
	 * @return Indicates success/failure of the operator
	 */
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException {
		resultImg = meanFilter();
	}
	
	/**  The validation of parameters and inputs.
 	 */
	@Override
	public void validateCustom() throws ALDOperatorException {
		

		boolean valid = !(this.getSizeX() < 0 
						|| this.getSizeY() < 0
						|| this.getSizeZ() < 0
						|| this.getSizeT() < 0
						|| this.getSizeC() < 0);
		

		if (!valid)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "MeanFilter.validateCustom(): " +
						     													"Size must not be negative.");

	}
	
	
	/**
	 * compute the mean filter
	 * @return resulting image
	 */
	protected MTBImage meanFilter() throws ALDOperatorException, ALDProcessingDAGException {
		
		// sizes
		Integer sx = this.getSizeX();
		Integer sy = this.getSizeY();
		Integer sz = this.getSizeZ();
		Integer st = this.getSizeT();
		Integer sc = this.getSizeC();
				
		MTBImage inImg = this.getInputImg();
		
		// kernel anchor (midpoint)
		int[] kAnchor = new int[5];

		MTBImage meanKernel;
		
		LinearFilter lf = new LinearFilter();
		lf.setResultImageType(this.getResultImageType());
		for (StatusListener l : this.statusListeners)
			lf.addStatusListener(l);
		
		if (sx > 0) {
			
			meanKernel = this.getMeanKernelX();

			kAnchor[0] = (sizeX-1)/2;
			kAnchor[1] = 0;
			kAnchor[2] = 0;
			kAnchor[3] = 0;
			kAnchor[4] = 0;
			
			lf.setInputImg(inImg);
			lf.setKernelImg(meanKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (sy > 0) {
			
			meanKernel = this.getMeanKernelY();

			kAnchor[0] = 0;
			kAnchor[1] = (sizeY-1)/2;
			kAnchor[2] = 0;
			kAnchor[3] = 0;
			kAnchor[4] = 0;
			
			lf.setInputImg(inImg);
			lf.setKernelImg(meanKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (sz > 0) {
			
			meanKernel = this.getMeanKernelZ();
			
			kAnchor[0] = 0;
			kAnchor[1] = 0;
			kAnchor[2] = (sizeZ-1)/2;
			kAnchor[3] = 0;
			kAnchor[4] = 0;

			lf.setInputImg(inImg);
			lf.setKernelImg(meanKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (st > 0) {
			
			meanKernel = this.getMeanKernelT();

			kAnchor[0] = 0;
			kAnchor[1] = 0;
			kAnchor[2] = 0;
			kAnchor[3] = (sizeT-1)/2;
			kAnchor[4] = 0;

			lf.setInputImg(inImg);
			lf.setKernelImg(meanKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (sc > 0) {
			
			meanKernel = this.getMeanKernelC();

			kAnchor[0] = 0;
			kAnchor[1] = 0;
			kAnchor[2] = 0;
			kAnchor[3] = 0;
			kAnchor[4] = (sizeC-1)/2;

			lf.setInputImg(inImg);
			lf.setKernelImg(meanKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
	
		return inImg;
	}

	/** Get input image.
	  */
	public MTBImage getInputImg() {
		return this.inputImg;
	}
	
	/** Set input image.
	  */
	public void setInputImg(MTBImage img) {
		this.inputImg = img;
	}
	
	/** Get resulting image.
	  */
	public MTBImage getResultImg() {
		return this.resultImg;
	}
	
	/** Get value of Parameter argument boundaryPadding.
	  * @return value of boundaryPadding
	  */
	public BoundaryPadding getBoundaryPadding() {
		return this.boundaryPadding;
	}
	
	/** Set value of Parameter argument boundaryPadding.
	  * @param value of boundaryPadding
	  */
	public void setBoundaryPadding(BoundaryPadding boundaryPadding) {
		this.boundaryPadding = boundaryPadding;
	}
	
	/** Get standard deviation (size) in c-dimension.
	  */
	public Integer getSizeC() {
		return this.sizeC;
	}
	
	/** Set standard deviation (size) in c-dimension.
	  */
	public void setSizeC(Integer sizeC) {
		 this.sizeC = sizeC;
	}

	/** Get standard deviation (size) in z-dimension.
	  */
	public Integer getSizeZ() {
		return this.sizeZ;
	}
	
	/** Set standard deviation (size) in z-dimension.
	  */
	public void setSizeZ(Integer sizeZ) {
		 this.sizeZ = sizeZ;
	}

	/** Get standard deviation (size) in y-dimension.
	  */
	public Integer getSizeY() {
		return this.sizeY;
	}
	
	/** Set standard deviation (size) in y-dimension.
	  */
	public void setSizeY(Integer sizeY) {
		 this.sizeY = sizeY;
	}

	/** Get standard deviation (size) in x-dimension.
	  */
	public Integer getSizeX() {
		return this.sizeX;
	}
	
	/** Set standard deviation (size) in x-dimension.
	  */
	public void setSizeX(Integer sizeX) {
		 this.sizeX = sizeX;
	}

	/** Get standard deviation (size) in t-dimension.
	  */
	public Integer getSizeT() {
		return this.sizeT;
	}
	
	/** Set standard deviation (size) in t-dimension.
	  */
	public void setSizeT(Integer sizeT) {
		 this.sizeT = sizeT;
	}
	
	/**
	 * Get mean kernel image (non-normalized) in x-dimension for given parameters, null if size is zero in x-dimension
	 */
	public MTBImage getMeanKernelX() {
		
				
		if (this.sizeX > 0) {	
			MTBImage meanKernel = MTBImage.createMTBImage(sizeX, 1, 1, 1, 1, MTBImageType.MTB_DOUBLE);
	
			for (int x = 0; x < sizeX; x++) {
				meanKernel.putValueDouble(x, 0, 0, 0, 0, 1.0/(this.sizeX*this.sizeX));
			}
			
			return meanKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get mean kernel image (non-normalized) in y-dimension for given parameters, null if size is zero in y-dimension
	 */
	public MTBImage getMeanKernelY() {
				
		if (this.sizeY > 0.0) {
			MTBImage meanKernel = MTBImage.createMTBImage(1, sizeY, 1, 1, 1, MTBImageType.MTB_DOUBLE);
	
			for (int y = 0; y < sizeY; y++) {
				meanKernel.putValueDouble(0, y, 0, 0, 0, 1.0/(this.sizeY*this.sizeY));
			}
			
			return meanKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get mean kernel image (non-normalized) in c-dimension for given parameters, null if size is zero in c-dimension
	 */
	public MTBImage getMeanKernelC() {
				
		if (this.sizeC > 0) {
			MTBImage meanKernel = MTBImage.createMTBImage(1, 1, 1, 1, sizeC, MTBImageType.MTB_DOUBLE);
	
			for (int c = 0; c < sizeC; c++) {
				meanKernel.putValueDouble(0, 0, 0, 0, c, 1.0/(this.sizeC*this.sizeC));
			}
			
			return meanKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get mean kernel image (non-normalized) in t-dimension for given parameters, null if size is zero in t-dimension
	 */
	public MTBImage getMeanKernelT() {

		if (this.sizeT > 0) {
			MTBImage meanKernel = MTBImage.createMTBImage(1, 1, 1, sizeT, 1, MTBImageType.MTB_DOUBLE);
	
			for (int t = 0; t < sizeT; t++) {
				meanKernel.putValueDouble(0, 0, 0, t, 0, 1.0/(this.sizeT*this.sizeT));
			}
			
			return meanKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get mean kernel image (non-normalized) in z-dimension for given parameters, null if size is zero in z-dimension
	 */
	public MTBImage getMeanKernelZ() {
				
		if (this.sizeZ > 0) {
			MTBImage meanKernel = MTBImage.createMTBImage(1, 1, sizeZ, 1, 1, MTBImageType.MTB_DOUBLE);
	
			for (int z = 0; z < sizeZ; z++) {
				meanKernel.putValueDouble(0, 0, z, 0, 0, 1.0/(this.sizeZ*this.sizeZ));
			}
			
			return meanKernel;
		}
		else {
			return null;
		}
	}
	

	/**
	 * @return the resultImageType
	 */
	public MTBImage.MTBImageType getResultImageType() {
		return resultImageType;
	}

	// ----- StatusReporter interface
	@Override
	public void addStatusListener(StatusListener statuslistener) {
		statusListeners.add(statuslistener);
	}


	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < statusListeners.size(); i++) {
			statusListeners.get(i).statusUpdated(e);
		}
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		statusListeners.remove(statuslistener);
	}
}

