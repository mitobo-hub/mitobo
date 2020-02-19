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
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Multidimensional Gauss filter class.
 * The Gauss filter is implemented as separable filter, i.e. only Gaussians with a diagonal
 * covariance matrix can be represented.
 * 
 * The standard deviations (sigma) for each dimension x,y,z,t,c are specified individually.
 * If any sigma is set to 0, no filtering in the corresponding dimension is applied.
 * 
 * It can be specified, in which way the given sigmas are interpreted:
 * The sigmas are whether interpreted to be specified in pixels or in physical pixel size (stepsize).
 * The latter is simplifies the specification of (e.g. isotropic) kernels in the case, that pixel
 * size differs in the different dimensions. Filtering in the c-dimension always assumes interpretation in
 * pixels, because it is assumed that no spatial or temporal order exists between channels.
 * By default, sigmas are interpreted to be specified in physical pixel size.
 * 
 * 
 * @author gress
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class GaussFilter extends MTBOperator implements StatusReporter {

	/** vector of installed StatusListeners */
	private transient Vector<StatusListener> statusListeners;

	@Parameter( label= "Input Image", required = true, 
			direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
			description = "Input image", dataIOOrder=1)
	private transient MTBImage inputImg = null;
	
	@Parameter( label= "Result Image", required = true, 
			direction=Parameter.Direction.OUT, description = "Result image", dataIOOrder=1)
	private transient MTBImage resultImg = null;
	
	@Parameter( label= "sigmaX", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Standard deviation of the Gaussian in x-dimension (Default: 1 micron)", dataIOOrder=3)
	private double sigmaX = 1;

	@Parameter( label= "sigmaY", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Standard deviation of the Gaussian in y-dimension (Default: 1 micron)", dataIOOrder=4)	
	private double sigmaY = 1;

	@Parameter( label= "sigmaZ", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Standard deviation of the Gaussian in z-dimension (Default: 0)", dataIOOrder=5)
	private double sigmaZ = 0;

	@Parameter( label= "sigmaT", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Standard deviation of the Gaussian in t-dimension (Default: 0)", dataIOOrder=6)
	private double sigmaT = 0;

	@Parameter( label= "sigmaC", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Standard deviation of the Gaussian in c-dimension (Default: 0)", dataIOOrder=7)
	private double sigmaC = 0;
	
	@Parameter( label= "Kernel truncation", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Factor to determine where the kernel is truncated: sigma*kernelTruncation", dataIOOrder=8)
	private double kernelTrunctation = 3;

	@Parameter( label= "sigma interpretation", required = true,
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
	    description = "Interpretation of sigmas, whether in pixels or physical pixel size (stepsize)", dataIOOrder=2)
	private SigmaInterpretation sigmaInterpretation = SigmaInterpretation.PHYSICALSIZE;
	
	@Parameter( label= "Boundary padding", required = true, 
			direction= Parameter.Direction.IN, mode=ExpertMode.ADVANCED, 
            description = "Image is padded by the specified method", dataIOOrder=9)
  private BoundaryPadding boundaryPadding = BoundaryPadding.PADDING_BORDER;
	
	@Parameter( label= "Type of result image", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.ADVANCED, dataIOOrder=10, description = "Type of result image (Default: MTB_DOUBLE)")
	private MTBImage.MTBImageType resultImageType = MTBImageType.MTB_DOUBLE;

	/**
	 * Indicator how to interpret the specified standard deviations:
	 * - PIXEL: Sigma is given in pixels
	 * - PHYSICALSIZE: Sigma is given in terms of the physical pixel size (see MTBImage.getStepsizeX/Y/Z/T, in c-dimension always pixel interpretation is used)
	 * @author Oliver Gress
	 *
	 */
	public enum SigmaInterpretation {
		PIXEL, PHYSICALSIZE
	}
	
	
	/**
	 * Constructor with default parameters sigmaX=1, sigmaY=1, sigmaZ=0, sigmaT=0, sigmaC=0, kernelTruncation=3, sigmaInterpretation=PHYSICALSIZE.
	 * An input image must be specified before running this operator.
	 *
	 */
	public GaussFilter() throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>(1);
	}
	
	
	/**
	 * Constructor for Gaussian filtering in x- and y-dimension.
	 * Default for other parameters: sigmaZ=0, sigmaT=0, sigmaC=0, kernelTruncation=3, sigmaInterpretation=PHYSICALSIZE,
	 * boundaryPadding=PADDING_BORDER
	 * @param img input image
	 * @param sigmaX standard deviation in x-dimension
	 * @param sigmaY standard deviation in y-dimension
	 */
	public GaussFilter(MTBImage img,
						double sigmaX,
						double sigmaY) throws ALDOperatorException {

		this.statusListeners = new Vector<StatusListener>(1);
		
		this.setInputImg(img);
		this.setSigmaX(sigmaX);
		this.setSigmaY(sigmaY);
		this.setBoundaryPadding(BoundaryPadding.PADDING_BORDER);
	}
	
	/**
	 * Constructor for Gaussian filtering in x-, y- and z-dimension.
	 * Default for other parameters: sigmaT=0, sigmaC=0, kernelTruncation=3, sigmaInterpretation=PHYSICALSIZE
	 * boundaryPadding=PADDING_BORDER
	 * @param img input image
	 * @param sigmaX standard deviation in x-dimension
	 * @param sigmaY standard deviation in y-dimension
	 * @param sigmaZ standard deviation in z-dimension
	 */
	public GaussFilter(MTBImage img,
						double sigmaX,
						double sigmaY,
						double sigmaZ) throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>(1);
		
		this.setInputImg(img);
		this.setSigmaX(sigmaX);
		this.setSigmaY(sigmaY);
		this.setSigmaZ(sigmaZ);
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
	 * Constructor for Gaussian filtering. Set any sigma to 0 to prevent filtering in the corresponding dimension
	 * @param img input image
	 * @param sigmaX standard deviation in x-dimension
	 * @param sigmaY standard deviation in y-dimension
	 * @param sigmaZ standard deviation in z-dimension
	 * @param sigmaT standard deviation in t-dimension
	 * @param sigmaC standard deviation in c-dimension
	 * @param boundaryPadding determines how pixel values outside the image domain are treated (see {@link LinearFilter})
	 * @param kernelTruncation the Gaussian kernel is truncated at sigma*kernelTruncation
	 * @param sigmaInterpretation how the specified standard deviations are interpreted
	 */
	public GaussFilter(MTBImage img,
					  	  double sigmaX,
						  double sigmaY,
						  double sigmaZ,
						  double sigmaT,
						  double sigmaC,
						  BoundaryPadding boundaryPadding,
						  double kernelTruncation,
						  SigmaInterpretation sigmaInterpretation) throws ALDOperatorException {
		this.statusListeners = new Vector<StatusListener>(1);
		
	    this.setInputImg(img);
	    this.setSigmaX(sigmaX);
	    this.setSigmaY(sigmaY);
		this.setSigmaZ(sigmaZ);
		this.setSigmaT(sigmaT);
		this.setSigmaC(sigmaC);
		this.setBoundaryPadding(boundaryPadding);
		this.setKernelTruncation(kernelTruncation);
		this.setSigmaInterpretation(sigmaInterpretation);
	}

	
	/**
	 * This function calls the actual Gauss filtering method. 
	 * @return Indicates success/failure of the operator
	 */
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException {		
		resultImg = gaussFilter();
	}
	
	/**  The validation of parameters and inputs.
 	 */
	@Override
	public void validateCustom() throws ALDOperatorException {
		

		boolean valid = !(this.getSigmaX() < 0.0 
						|| this.getSigmaY() < 0.0
						|| this.getSigmaZ() < 0.0
						|| this.getSigmaT() < 0.0
						|| this.getSigmaC() < 0.0);
		

		if (!valid)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "GaussFilter.validateCustom(): " +
						     													"Sigma must not be negative.");

	}
	
	
	/**
	 * compute the Gaussian filter
	 * @return resulting image
	 */
	protected MTBImage gaussFilter() throws ALDOperatorException, ALDProcessingDAGException {
		
		// sigmas
		double sx = this.getSigmaX();
		double sy = this.getSigmaY();
		double sz = this.getSigmaZ();
		double st = this.getSigmaT();
		double sc = this.getSigmaC();
				
		// kernel truncation factor
		double ktrunc = this.getKernelTruncation();
		
		MTBImage inImg = this.getInputImg();
		
		double px = 1;
		double py = 1;
		double pz = 1;
		double pt = 1;
		
    	// physical pixel sizes
		if (this.sigmaInterpretation == SigmaInterpretation.PHYSICALSIZE) {
			px = inImg.getStepsizeX();
			py = inImg.getStepsizeY();
			pz = inImg.getStepsizeZ();
			pt = inImg.getStepsizeT();
		}
		

		
		// kernel sizes
		int ksizeX = (int)(Math.ceil(ktrunc*sx/px)) * 2 + 1;
		int ksizeY = (int)(Math.ceil(ktrunc*sy/py)) * 2 + 1;		
		int ksizeZ = (int)(Math.ceil(ktrunc*sz/pz)) * 2 + 1;
		int ksizeT = (int)(Math.ceil(ktrunc*st/pt)) * 2 + 1;	
		int ksizeC = (int)(Math.ceil(ktrunc*sc)) * 2 + 1;
		
		// kernel anchor (midpoint)
		int[] kAnchor = new int[5];

		MTBImage gaussKernel;
		
		LinearFilter lf = new LinearFilter();
		lf.setResultImageType(this.getResultImageType());
		for (StatusListener l : this.statusListeners)
			lf.addStatusListener(l);
		
		if (sx > 0.0) {
			
			gaussKernel = this.getGaussKernelX();

			kAnchor[0] = (ksizeX-1)/2;
			kAnchor[1] = 0;
			kAnchor[2] = 0;
			kAnchor[3] = 0;
			kAnchor[4] = 0;
			
			lf.setInputImg(inImg);
			lf.setKernelImg(gaussKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (sy > 0.0) {
			
			gaussKernel = this.getGaussKernelY();

			kAnchor[0] = 0;
			kAnchor[1] = (ksizeY-1)/2;
			kAnchor[2] = 0;
			kAnchor[3] = 0;
			kAnchor[4] = 0;
			
			lf.setInputImg(inImg);
			lf.setKernelImg(gaussKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (sz > 0.0) {
			
			gaussKernel = this.getGaussKernelZ();
			
			kAnchor[0] = 0;
			kAnchor[1] = 0;
			kAnchor[2] = (ksizeZ-1)/2;
			kAnchor[3] = 0;
			kAnchor[4] = 0;

			lf.setInputImg(inImg);
			lf.setKernelImg(gaussKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (st > 0.0) {
			
			gaussKernel = this.getGaussKernelT();

			kAnchor[0] = 0;
			kAnchor[1] = 0;
			kAnchor[2] = 0;
			kAnchor[3] = (ksizeT-1)/2;
			kAnchor[4] = 0;

			lf.setInputImg(inImg);
			lf.setKernelImg(gaussKernel);
			lf.setKernelOrigin(kAnchor);
			lf.setBoundaryPadding(this.boundaryPadding);
			lf.setKernelNormalization(true);
			
			lf.runOp(false);
			
			inImg = lf.getResultImg();
		}
		
		if (sc > 0.0) {
			
			gaussKernel = this.getGaussKernelC();

			kAnchor[0] = 0;
			kAnchor[1] = 0;
			kAnchor[2] = 0;
			kAnchor[3] = 0;
			kAnchor[4] = (ksizeC-1)/2;

			lf.setInputImg(inImg);
			lf.setKernelImg(gaussKernel);
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
	
	/** Get kernel truncation factor. The Gaussian kernel is truncated at sigma*kernelTruncation.
	  */
	public double getKernelTruncation() {
		return this.kernelTrunctation;
	}
	
	/** Set kernel truncation factor. The Gaussian kernel is truncated at sigma*kernelTruncation.
	  */
	public void setKernelTruncation(double kernelTruncation) {
		 this.kernelTrunctation = kernelTruncation;
	}

	/** Get standard deviation (sigma) in c-dimension.
	  */
	public double getSigmaC() {
		return this.sigmaC;
	}
	
	/** Set standard deviation (sigma) in c-dimension.
	  */
	public void setSigmaC(double sigmaC) {
		 this.sigmaC = sigmaC;
	}

	/** Get standard deviation (sigma) in z-dimension.
	  */
	public double getSigmaZ() {
		return this.sigmaZ;
	}
	
	/** Set standard deviation (sigma) in z-dimension.
	  */
	public void setSigmaZ(double sigmaZ) {
		 this.sigmaZ = sigmaZ;
	}

	/** Get standard deviation (sigma) in y-dimension.
	  */
	public double getSigmaY() {
		return this.sigmaY;
	}
	
	/** Set standard deviation (sigma) in y-dimension.
	  */
	public void setSigmaY(double sigmaY) {
		 this.sigmaY = sigmaY;
	}

	/** Get standard deviation (sigma) in x-dimension.
	  */
	public double getSigmaX() {
		return this.sigmaX;
	}
	
	/** Set standard deviation (sigma) in x-dimension.
	  */
	public void setSigmaX(double sigmaX) {
		 this.sigmaX = sigmaX;
	}

	/** Get standard deviation (sigma) in t-dimension.
	  */
	public double getSigmaT() {
		return this.sigmaT;
	}
	
	/** Set standard deviation (sigma) in t-dimension.
	  */
	public void setSigmaT(double sigmaT) {
		 this.sigmaT = sigmaT;
	}
	
	/** Get the mode of how sigmas are interpreted.
	  */
	public SigmaInterpretation getSigmaInterpretation() {
		return this.sigmaInterpretation;
	}
	
	/** Set the mode of how sigmas are interpreted.
	  */
	public void setSigmaInterpretation(SigmaInterpretation sigmaInterpretation) {
		 this.sigmaInterpretation = sigmaInterpretation;
	}
	
	/**
	 * Get Gaussian kernel image (non-normalized) in x-dimension for given parameters, null if sigma is zero in x-dimension
	 */
	public MTBImage getGaussKernelX() {
		
				
		if (this.sigmaX > 0.0) {
			MTBImage inImg = this.getInputImg();
	
			double px = 1;
			
			if (this.sigmaInterpretation == SigmaInterpretation.PHYSICALSIZE)
				px = inImg.getStepsizeX();
			
			// kernel truncation factor
			double ktrunc = this.getKernelTruncation();
			
			// kernel size
			int ksizeX = (int)(Math.ceil(ktrunc*this.sigmaX/px)) * 2 + 1;
			
			int kAnchor = (ksizeX-1)/2;
			
			MTBImage gaussKernel = MTBImage.createMTBImage(ksizeX, 1, 1, 1, 1, MTBImageType.MTB_DOUBLE);
	
			for (int x = 0; x < ksizeX; x++) {
				gaussKernel.putValueDouble(x, 0, 0, 0, 0, Math.exp(-0.5*(x-kAnchor)*(x-kAnchor)*px*px/(this.sigmaX*this.sigmaX)));
			}
			
			return gaussKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get Gaussian kernel image (non-normalized) in y-dimension for given parameters, null if sigma is zero in y-dimension
	 */
	public MTBImage getGaussKernelY() {
				
		if (this.sigmaY > 0.0) {
			MTBImage inImg = this.getInputImg();
	
			double py = 1;

			if (this.sigmaInterpretation == SigmaInterpretation.PHYSICALSIZE)
				py = inImg.getStepsizeY();
			
			// kernel truncation factor
			double ktrunc = this.getKernelTruncation();
			
			// kernel size
			int ksizeY = (int)(Math.ceil(ktrunc*this.sigmaY/py)) * 2 + 1;
			
			int kAnchor = (ksizeY-1)/2;
			
			MTBImage gaussKernel = MTBImage.createMTBImage(1, ksizeY, 1, 1, 1, MTBImageType.MTB_DOUBLE);
	
			for (int y = 0; y < ksizeY; y++) {
				gaussKernel.putValueDouble(0, y, 0, 0, 0, Math.exp(-0.5*(y-kAnchor)*(y-kAnchor)*py*py/(this.sigmaY*this.sigmaY)));
			}
			
			return gaussKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get Gaussian kernel image (non-normalized) in c-dimension for given parameters, null if sigma is zero in c-dimension
	 */
	public MTBImage getGaussKernelC() {
				
		if (this.sigmaC > 0.0) {
			
			// kernel truncation factor
			double ktrunc = this.getKernelTruncation();
			
			// kernel size
			int ksizeC = (int)(Math.ceil(ktrunc*this.sigmaC)) * 2 + 1;
			
			int kAnchor = (ksizeC-1)/2;
			
			MTBImage gaussKernel = MTBImage.createMTBImage(1, 1, 1, 1, ksizeC, MTBImageType.MTB_DOUBLE);
	
			for (int c = 0; c < ksizeC; c++) {
				gaussKernel.putValueDouble(0, 0, 0, 0, c, Math.exp(-0.5*(c-kAnchor)*(c-kAnchor)/(this.sigmaC*this.sigmaC)));
			}
			
			return gaussKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get Gaussian kernel image (non-normalized) in t-dimension for given parameters, null if sigma is zero in t-dimension
	 */
	public MTBImage getGaussKernelT() {

		if (this.sigmaT > 0.0) {
			MTBImage inImg = this.getInputImg();
	
			double pt = 1;
			
			if (this.sigmaInterpretation == SigmaInterpretation.PHYSICALSIZE)
				pt = inImg.getStepsizeT();
			
			// kernel truncation factor
			double ktrunc = this.getKernelTruncation();
			
			// kernel size
			int ksizeT = (int)(Math.ceil(ktrunc*this.sigmaT/pt)) * 2 + 1;
			
			int kAnchor = (ksizeT-1)/2;
			
			MTBImage gaussKernel = MTBImage.createMTBImage(1, 1, 1, ksizeT, 1, MTBImageType.MTB_DOUBLE);
	
			for (int t = 0; t < ksizeT; t++) {
				gaussKernel.putValueDouble(0, 0, 0, t, 0, Math.exp(-0.5*(t-kAnchor)*(t-kAnchor)*pt*pt/(this.sigmaT*this.sigmaT)));
			}
			
			return gaussKernel;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Get Gaussian kernel image (non-normalized) in z-dimension for given parameters, null if sigma is zero in z-dimension
	 */
	public MTBImage getGaussKernelZ() {
				
		if (this.sigmaZ > 0.0) {
			MTBImage inImg = this.getInputImg();
	
			double pz = 1;
			
			if (this.sigmaInterpretation == SigmaInterpretation.PHYSICALSIZE)
				pz = inImg.getStepsizeZ();
			
			// kernel truncation factor
			double ktrunc = this.getKernelTruncation();
			
			// kernel size
			int ksizeZ = (int)(Math.ceil(ktrunc*this.sigmaZ/pz)) * 2 + 1;
			
			int kAnchor = (ksizeZ-1)/2;
			
			MTBImage gaussKernel = MTBImage.createMTBImage(1, 1, ksizeZ, 1, 1, MTBImageType.MTB_DOUBLE);
	
			for (int z = 0; z < ksizeZ; z++) {
				gaussKernel.putValueDouble(0, 0, z, 0, 0, Math.exp(-0.5*(z-kAnchor)*(z-kAnchor)*pz*pz/(this.sigmaZ*this.sigmaZ)));
			}
			
			return gaussKernel;
		}
		else {
			return null;
		}
	}
	
	// ----- StatusReporter interface

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

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/filters/linear/GaussFilter.html">API</a></p>

<p>Convolution of an input image with a Gaussian kernel. Only Gaussian kernels with a diagonal covariance matrix are considered.</p>
<h3>Required input:</h3>

<ul><li>
<p><b>Input image</b>:</p>

<p>Image to be filtered</p>
</li><li>
<p><b>sigmaX</b>:</p>

<p>Standard deviation of Gaussian kernel in x-dimension</p>
</li><li>
<p><b>sigmaY</b>: </p>

<p>Standard deviation of Gaussian kernel in y-dimension</p>
</li><li>
<p><b>sigmaZ</b>: </p>

<p>Standard deviation of Gaussian kernel in z-dimension</p>
</li><li>
<p><b>sigmaT</b>: </p>

<p>Standard deviation of Gaussian kernel in t-dimension</p>
</li><li>
<p><b>sigmaC</b>: </p>

<p>Standard deviation of Gaussian kernel in c-dimension</p>
</li><li>
<p><b>sigma interpretation</b>: </p>

<p>Standard deviations are interpreted to be given in pixels or in physical pixel size</p>
</li><li>
<p><b>Kernel truncation</b>:</p>

<p>A factor to truncate the tails of the Gaussian function in terms of standard deviation, e.g. if kernel truncation is set to 2 then the Gaussian kernel is truncated (set to zero) for values farther than 2*(standard deviation) from the kernel's mean.</p>
</li><li>
<p><b>Boundary padding</b>: </p>

<p>Padding of image: Method of how to simulate pixel values outside the image domain.</p>

<ul><li>
<p>PADDING_ZERO: Values outside the image domain are assumed to be zero.</p>
</li><li>
<p>PADDING_BORDER: Values outside the image domain correspond to value of nearest pixel in the image domain. </p>
</li><li>
<p>PADDING_MIRROR: Values of the image are mirrored outside of the image domain along the image border.</p>
</li><li>
<p>PADDING_PERIODIC: Values are repeated, i.e. the image is assumed to be periodical with period equal to the image dimensions (as assumed for DFT) </p>
</li></ul>
</li></ul>
<h3>Output:</h3>

<ul><li>
<p><b>Result image</b></p>

<p>The filtered image of type MTBImageType.MTB_DOUBLE</p>
</li></ul>
END_MITOBO_ONLINE_HELP*/