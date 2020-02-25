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

package de.unihalle.informatik.MiToBo.filters.linear;

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;


/**
 * Generic linear filter operation class
 * 
 * @author gress
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		shortDescription="Convolves an image with a user-specified kernel.")
public class LinearFilter extends MTBOperator implements StatusReporter {
	
	/** vector of installed StatusListeners */
	private Vector<StatusListener> m_statusListeners;

	@Parameter( label= "Input image", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Input image")
	private MTBImage inputImg = null;
	
	@Parameter( label= "Kernel image", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=2, description = "Convolution kernel (image)")
	private MTBImage kernelImg = null;
	
	@Parameter( label= "Kernel origin", required = false, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=3, 
			 description = "Coordinate of the kernel's origin in the kernel image. If not specified (null) it defaults to the center of the kernel image rounded down.")
	private int[] kernelOrigin = null;

	@Parameter( label= "Kernel normalization", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=4, description = "If true, kernel values are normalized to sum to 1 (Default)")
	private boolean kernelNormalization = true;
	
	@Parameter( label= "Boundary padding", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=5, description = "Image is padded by the specified method (Default: 0s are assumed outside the image)")
	private BoundaryPadding boundaryPadding = BoundaryPadding.PADDING_ZERO;

	@Parameter( label= "Type of result image", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=6, description = "Type of result image (Default: MTB_DOUBLE)")
	private MTBImage.MTBImageType resultImageType = MTBImageType.MTB_DOUBLE;

	@Parameter( label= "Result image", required = true, direction = Parameter.Direction.OUT, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Result image")
	private MTBImage resultImg = null;

	
	
	// ----- Constructors
	/**
	 * Empty constructor
	 */
	public LinearFilter() throws ALDOperatorException {
		m_statusListeners = new Vector<StatusListener>(1);
	}
	
	/**
	 * Constructor
	 */
	public LinearFilter(MTBImage inImg, 
						MTBImage kernelImg) throws IllegalArgumentException,ALDOperatorException {

	    this.setInputImg(inImg);
	    this.setKernelImg(kernelImg);

		m_statusListeners = new Vector<StatusListener>(1);
	}
	
	
	/**
	 * Constructor
	 */
	public LinearFilter(MTBImage inImg, 
						MTBImage kernelImg, 
						int[] kernelOrigin, 
						boolean kernelNormalization,
						BoundaryPadding boundaryPadding) throws IllegalArgumentException,ALDOperatorException {

	    this.setInputImg(inImg);
	    this.setKernelImg(kernelImg);
		this.setKernelOrigin(kernelOrigin);
		this.setKernelNormalization(kernelNormalization);
		this.boundaryPadding = boundaryPadding;


		m_statusListeners = new Vector<StatusListener>(1);
	}
	
	
	public void validateCustom() throws ALDOperatorException {
		
		if (this.kernelOrigin != null && this.kernelOrigin.length != 5)
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "LinearFilter.validateCustom(): Kernel origin array must have size 5.");
		
	}
	
	@Override
	protected void operate() throws ALDOperatorException {
		
		MTBImage resultImg = null;
		
		if (this.kernelOrigin == null) {
		    int[] ko = new int[5];
		    ko[0] = this.kernelImg.getSizeX()/2;
		    ko[1] = this.kernelImg.getSizeY()/2;
		    ko[2] = this.kernelImg.getSizeZ()/2;
		    ko[3] = this.kernelImg.getSizeT()/2;
		    ko[4] = this.kernelImg.getSizeC()/2;
		    this.kernelOrigin = ko;
		}
		
		// convolve operation
		resultImg = convolve(this.getInputImg(),
										  this.getKernelImg(),
										  this.getKernelOrigin(),
										  this.getKernelNormalization(),
										  this.getBoundaryPadding());
		

		if (resultImg != null) {

			this.resultImg= resultImg;
		}
		else {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "LinearFilter.operate() failed: Result image is 'null'");
		}
	}
	
	
	
	// ---- linear filter functions ----
	/**
	 * Convolve an image with a specified kernel.
	 * @param img input image
	 * @param kernel kernel image
	 * @param kernelAnchor array of length 5 to specify the kernel element, under which the result value is stored ([0]=x-position, [1]=y-pos, [2]=z-pos, [3]=t-pos, [4]=c-pos)
	 * @param normalize flag for kernel normalization (kernel weights sum to 1)
	 * @param boundaryPadding set the padding method outside the boundaries of the underlying image (see static final fields)
	 */
	protected MTBImage convolve(MTBImage img, MTBImage kernel, int[] kernelAnchor, boolean normalize, BoundaryPadding boundaryPadding) {
		
		MTBImage resultImg;
		
		int[] kA = kernelAnchor;
		
		int ksizeX = kernel.getSizeX();
		int ksizeY = kernel.getSizeY();
		int ksizeZ = kernel.getSizeZ();
		int ksizeT = kernel.getSizeT();
		int ksizeC = kernel.getSizeC();
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		int sizeZ = img.getSizeZ();
		int sizeT = img.getSizeT();
		int sizeC = img.getSizeC();
		
		
		double normFactor = 1.0;
		
		if (normalize) {
			double sum = 0.0;
			
			for (int kc = 0; kc < ksizeC; kc++) {
				for (int kt = 0; kt < ksizeT; kt++) {
					for (int kz = 0; kz < ksizeZ; kz++) {
						for (int ky = 0; ky < ksizeY; ky++) {
							for (int kx = 0; kx < ksizeX; kx++) {
								sum += kernel.getValueDouble(kx, ky, kz, kt, kc);
							}
						}
					}
				}
			}
			
			normFactor /= sum;
		}
		
		
		// create result image
		resultImg = img.convertType(this.getResultImageType(), false);

		
		MTBImageWindow win = new MTBImageWindow(ksizeX, ksizeY, ksizeZ, ksizeT, ksizeC, img, boundaryPadding);
		double val;
		
		// progress bar update
		int ctzy_minus = sizeC*sizeT*sizeZ*sizeY;
	//	int ctz_minus = m_sizeC*m_sizeT*m_sizeZ;
		int tzy = sizeT*sizeZ*sizeY;
		int zy = sizeZ*sizeY;
		String msg = "Convolution... (kernelsize: " + ksizeX + "x" + ksizeY + "x" + ksizeZ + "x" + ksizeT + "x" + ksizeC + ")";	
		this.notifyListeners(new StatusEvent(0, ctzy_minus, msg));
		
		// compute convolution
		for (int c = 0; c < sizeC; c++) {
			win.setPositionC(c-kA[4]);
			
			for (int t = 0; t < sizeT; t++) {
				win.setPositionT(t-kA[3]);
				
				for (int z = 0; z < sizeZ; z++) {
					win.setPositionZ(z-kA[2]);
					
					for (int y = 0; y < sizeY; y++) {
						win.setPositionY(y-kA[1]);
						
						for (int x = 0; x < sizeX; x++) {	
							win.setPositionX(x-kA[0]);
							
							val = 0.0;
							for (int kc = 0; kc < ksizeC; kc++) {
								for (int kt = 0; kt < ksizeT; kt++) {
									for (int kz = 0; kz < ksizeZ; kz++) {
										for (int ky = 0; ky < ksizeY; ky++) {
											for (int kx = 0; kx < ksizeX; kx++) {	
												val += kernel.getValueDouble(kx, ky, kz, kt, kc) * win.getValueDouble(kx, ky, kz, kt, kc);
											}
										}
									}
								}
							}
							
							resultImg.putValueDouble(x, y, z, t, c, val*normFactor);
							
						}
						this.notifyListeners(new StatusEvent(c*tzy + t*zy + z*sizeY + y, ctzy_minus, msg));
					}
				}
			}
		}

		return resultImg;
	}	
	
	
	/** Get value of Parameter argument kernelOrigin.
	  * @return int array with five elements specifying the image coordinate [x,y,z,t,c] considered as the origin (0,0,0,0,0) of the kernel 
	  */
	public int[] getKernelOrigin() {
		
		return this.kernelOrigin;
	}
	
	/** Set value of Parameter argument kernelOrigin.
	  * @param int array with five elements specifying the image coordinate [x,y,z,t,c] considered as the origin (0,0,0,0,0) of the kernel 
	  */
	public void setKernelOrigin(int[] kernelOrigin) {

		this.kernelOrigin = kernelOrigin;
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

	/** Get value of Parameter argument kernelNormalization.
	  * @return value of kernelNormalization
	  */
	public boolean getKernelNormalization() {
		return this.kernelNormalization;
	}

	/** Set value of Parameter argument kernelNormalization.
	  * @param value of kernelNormalization
	  */
	public void setKernelNormalization(boolean kernelNormalization) {
		this.kernelNormalization = kernelNormalization;
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
	
	/** Get convolution kernel (image).
	  * @return value of kernelImg
	  */
	public MTBImage getKernelImg() {
		return this.kernelImg;
	}
	
	/** Set convolution kernel (image).
	  */
	public void setKernelImg(MTBImage kernelImg) {
		this.kernelImg = kernelImg;
	}
	
	/**
	 * @return the resultImageType
	 */
	public MTBImage.MTBImageType getResultImageType() {
		return resultImageType;
	}

	/**
	 * @param resultImageType the resultImageType to set
	 */
	public void setResultImageType(MTBImage.MTBImageType resultImageType) {
		this.resultImageType = resultImageType;
	}

	/** Get resulting image.
	  */
	public MTBImage getResultImg() {
		return this.resultImg;
	}
	
	
	// ----- StatusReporter interface

	@Override
	public void addStatusListener(StatusListener statuslistener) {
		m_statusListeners.add(statuslistener);
	}


	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < m_statusListeners.size(); i++) {
			m_statusListeners.get(i).statusUpdated(e);
		}
	}


	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		m_statusListeners.remove(statuslistener);
	}
	
	@Override
	public String getDocumentation() {
		return "<p>Convolution of an input image with an arbitrary kernel (image).</p>\n" + 
				"<h3>Required input:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Input image</b>:</p>\n" + 
				"\n" + 
				"<p>Image to be filtered</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Kernel image</b>:</p>\n" + 
				"\n" + 
				"<p>The kernel used for convolution (the given kernel is used as it is in the convolution and NOT mirrored at its origin)</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Kernel normalization</b>: </p>\n" + 
				"\n" + 
				"<p>Flag to normalize the kernel image. If true the kernel image's pixel values are normalized to sum to 1.</p>\n" + 
				"</li><li>\n" + 
				"<p><b>Boundary padding</b>: </p>\n" + 
				"\n" + 
				"<p>Padding of image: Method of how to simulate pixel values outside the image domain.</p>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p>PADDING_ZERO: Values outside the image domain are assumed to be zero.</p>\n" + 
				"</li><li>\n" + 
				"<p>PADDING_BORDER: Values outside the image domain correspond to value of nearest pixel in the image domain. </p>\n" + 
				"</li><li>\n" + 
				"<p>PADDING_MIRROR: Values of the image are mirrored outside of the image domain along the image border.</p>\n" + 
				"</li><li>\n" + 
				"<p>PADDING_PERIODIC: Values are repeated, i.e. the image is assumed to be periodical with period equal to the image dimensions (as assumed for DFT) </p>\n" + 
				"</li></ul>\n" + 
				"</li></ul>\n" + 
				"<h3>Optional input:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Kernel origin</b>: </p>\n" + 
				"\n" + 
				"<p>The coordinate of the kernel image used as origin. If not specified, the center of the kernel image rounded down is used as origin.</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Output:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Result image</b></p>\n" + 
				"\n" + 
				"<p>The filtered image of type MTBImageType.MTB_DOUBLE</p>\n" + 
				"</li></ul>\n";
	}
}
