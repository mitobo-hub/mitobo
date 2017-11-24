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

package de.unihalle.informatik.MiToBo.transforms;

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperatorControllable;
import de.unihalle.informatik.MiToBo.filters.linear.LinearFilter;
import de.unihalle.informatik.MiToBo.math.images.MTBImageArithmetics;

/**
 * Undecimated wavelet transform (UWT) operator.
 * @author gress
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.NONE)
public class UndecimatedWaveletTransform 
	extends MTBOperatorControllable implements StatusReporter {
	
	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = 
		"[UndecimatedWaveletTransform] ";
	
	@Parameter(label= "Kernels", required = true, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 1,
	        description = "A set of lowpass kernels interpreted as parts of a separable kernel")
	private MTBImage[] kernels = null;

	@Parameter( label= "Type of transform", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 4,
	        description = "Specifies if forward or backward transform is computed")
	private TransformationMode transform = TransformationMode.UWT;

	@Parameter( label= "Denoise", required = true, direction = Direction.IN,
			mode = ExpertMode.ADVANCED, dataIOOrder = 3,
	        description = "Specifies whether the wavelet coefficients will be denoised using Jeffrey's noninformative prior")
	private Boolean denoise = new Boolean(false);

	@Parameter( label= "Jmax", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 2,
	        description = "Maximum scale index (decimal > 0)")
	private Integer Jmax = null;

	@Parameter( label= " image", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 1,
	        description = "Input image")
	private MTBImage img = null;

	@Parameter( label= "mask image", required = false, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 5,
	        description = "mask image used for denoising: a value of zero INCLUDES a pixel")
	private MTBImage excludeMask = null;

	@Parameter( label= "UWT coefficient images", required = true, direction = Direction.OUT,
			mode = ExpertMode.STANDARD, dataIOOrder = 1,
	        description = "Images with UWT coefficients of the different scales")
	private MTBImageSet uwtImages = null;

	
	/** vector of installed StatusListeners */
	private Vector<StatusListener> m_statusListeners;
	
	/** noise sigma scale factors for different wavelet scales */
	//private final double[] m_sigmaScales = {1.0, 0.08594106332714473, 0.03654245034158243, 0.01764455750822137, 0.008785131705085117};
	private final double[] m_sigmaScales = {1.000000,	0.085954, 0.036694,	0.017764, 0.008837,	0.004465};

	
	protected enum TransformationMode {
		UWT, InverseUWT
	}
	
	
	/**
	 * Constructor with default Gaussian kernel [1/16, 1/4, 3/8, 1/4, 1/16] for dimension x, y and z,
	 * no image and <code>denoise</code> set to false.
	 */
	public UndecimatedWaveletTransform() throws ALDOperatorException {
		this( null, 0, false);
//		this.m_statusListeners = new Vector<StatusListener>(1);
//		
//		double[] kernel = //{1.0/16.0, 1.0/4.0, 3.0/8.0, 1.0/4.0, 1.0/16.0};
//			{0.0002638651, 0.1064507720, 0.7865707259, 0.1064507720, 0.0002638651};
//
//
//		MTBImage[] kernels;
//
//		kernels = new MTBImage[3];
//		kernels[0] = MTBImage.createMTBImage(5, 1, 1, 1, 1, MTBImageType.MTB_DOUBLE);
//		kernels[1] = MTBImage.createMTBImage(1, 5, 1, 1, 1, MTBImageType.MTB_DOUBLE);
//		kernels[2] = MTBImage.createMTBImage(1, 1, 5, 1, 1, MTBImageType.MTB_DOUBLE);
//		
//		for (int i = 0; i < kernel.length; i++) {
//			kernels[0].putValueDouble(i, 0, 0, 0, 0, kernel[i]);
//			kernels[1].putValueDouble(0, i, 0, 0, 0, kernel[i]);
//			kernels[2].putValueDouble(0, 0, i, 0, 0, kernel[i]);
//		}
//		
//		this.setKernels(kernels);
//		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_INIT;
	}
	
	/**
	 * Constructor. Separable lowpass kernels are expected (i.e. Gaussian).
	 * @param _img input image
	 * @param _Jmax maximum scale (2^Jmax - 1)
	 * @param _denoise reduction of gaussian noise
	 * @param _kernels an array of kernels, which are inflated for each scale and used to convolve the image consecutively, because a separable filter is expected to be specified by this array
	 * @throws ALDOperatorException 
	 */
	public UndecimatedWaveletTransform(MTBImage _img, int _Jmax, 
				boolean _denoise, MTBImage[] _kernels) 
			throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		
		this.setImg(_img);
		this.setJmax(_Jmax);
		this.setKernels(_kernels);
		this.setForwardTransform();
		this.setDenoise(_denoise);
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_INIT;
	}
	
	/**
	 * Constructor with default Gaussian kernel [1/16, 1/4, 3/8, 1/4, 1/16] for at most dimension x, y (and z if present).
	 * @param img input image
	 * @param Jmax maximum scale (2^Jmax - 1)
	 * @param denoise reduction of gaussian noise
	 * @throws ALDOperatorException 
	 */
	public UndecimatedWaveletTransform(MTBImage _img, int _Jmax, 
			boolean _denoise) throws ALDOperatorException {
		this( _img, _Jmax, _denoise, true);
	}
	
	/**
	 * Constructor with default Gaussian kernel for sd = 1 [1/16, 1/4, 3/8, 1/4, 1/16] for at most dimension x, y (and z if present)
	 * or sd=0.5 [0.0002638651, 0.1064507720, 0.7865707259, 0.1064507720, 0.0002638651] if 
	 * <code>sigmaEqualsOne</code> is false
	 * @param img input image
	 * @param Jmax maximum scale (2^Jmax - 1)
	 * @param denoise reduction of gaussian noise
	 * @throws ALDOperatorException 
	 */
	public UndecimatedWaveletTransform(MTBImage _img, int _Jmax, 
			boolean _denoise, boolean sigmaEqualsOne) throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		
		this.setImg(_img);
		this.setJmax(_Jmax);
		this.setForwardTransform();
		this.setDenoise(_denoise);
		
		double[] kernel = (sigmaEqualsOne) ? 
				new double[] {1.0/16.0, 1.0/4.0, 3.0/8.0, 1.0/4.0, 1.0/16.0}
		        : new double[] {0.0002638651, 0.1064507720, 0.7865707259, 0.1064507720, 0.0002638651};
		
		
		MTBImage[] kernels;
		
		if ( _img == null || _img.getSizeZ() > 1) {
			kernels = new MTBImage[3];
			kernels[0] = MTBImage.createMTBImage(5, 1, 1, 1, 1, MTBImageType.MTB_DOUBLE);
			kernels[1] = MTBImage.createMTBImage(1, 5, 1, 1, 1, MTBImageType.MTB_DOUBLE);
			kernels[2] = MTBImage.createMTBImage(1, 1, 5, 1, 1, MTBImageType.MTB_DOUBLE);
			
			for (int i = 0; i < kernel.length; i++) {
				kernels[0].putValueDouble(i, 0, 0, 0, 0, kernel[i]);
				kernels[1].putValueDouble(0, i, 0, 0, 0, kernel[i]);
				kernels[2].putValueDouble(0, 0, i, 0, 0, kernel[i]);
			}
			
		}
		else {
			kernels = new MTBImage[2];
			kernels[0] = MTBImage.createMTBImage(5, 1, 1, 1, 1, MTBImageType.MTB_DOUBLE);
			kernels[1] = MTBImage.createMTBImage(1, 5, 1, 1, 1, MTBImageType.MTB_DOUBLE);
			
			for (int i = 0; i < kernel.length; i++) {
				kernels[0].putValueDouble(i, 0, 0, 0, 0, kernel[i]);
				kernels[1].putValueDouble(0, i, 0, 0, 0, kernel[i]);
			}
		}
		
		this.setKernels(kernels);
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_INIT;
	}	

	@Override
  public boolean supportsStepWiseExecution() {
		return false;
	}

	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_RUNNING;

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "running undecimated wavelet transformation...");

		if (this.isInverseTransform()) {
			
			MTBImageSet dwtImgSet = this.getUWT();
			
			MTBImage[] dwtImgs = dwtImgSet.toArray();
			
			if (dwtImgs != null) {
				
				this.setImg(this.inverseATrousDWT(dwtImgs));
			}
		}
		else {
			
			MTBImage[] dwtImgs = 
				this.aTrousDWT(this.getImg(), this.getJmax(), this.getKernels());
			
			// something went wrong, probably operator was stopped...
			if (dwtImgs == null) {
				this.setUWT(null);
				return;
			}
			
			if (this.getDenoise()) {
				
				if (this.verbose.booleanValue())
					System.out.println(opIdentifier	+ "performing denoising...");

				double[] sigmaScales;
				
				
				if (this.getJmax() <= this.m_sigmaScales.length) {
					sigmaScales = this.m_sigmaScales;
				}
				else {
					sigmaScales = this.computeSigmaScales();
				}
				
				// sigma of wavelet-coeffs in scale j=1 
				// (we assume that only noise is present in scale j=1)
				double scaleOneSigma = this.get3SigClippedStdDev(dwtImgs[1]);

				// threshold DWT coefficients
				this.denoiseDWTJeffreys(dwtImgs, scaleOneSigma, sigmaScales);
			}
			
			this.setUWT(new MTBImageSet(dwtImgs));
		}
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier	+ "finished calculations.");
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_TERMINATED;
	}
	
	
	/**
	 * Inverse a trous DWT
	 * @param dwt DWT-coefficient images
	 * @return
	 * @throws ALDOperatorException 
	 */
	protected MTBImage inverseATrousDWT(MTBImage[] dwt) throws ALDOperatorException {
		// get lowpass filtered image
		MTBImage invDWT = dwt[0].duplicate();

		MTBImageArithmetics mia = new MTBImageArithmetics();
		
		for (int j = 1; j < dwt.length; j++) {
			// add DWT images (bandpass filtered images) to lowpass filtered image
			invDWT = mia.add(invDWT, dwt[j]);
		}
		
		return invDWT;
	}
	
	/**
	 * A trous DWT
	 * @return DWT-coefficient images ([0] lowpass coeffs, [1] highpass coeffs, [2] highest bandpass coeffs, ..., [Jmax] lowest bandpass coeffs)
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImage[] aTrousDWT(MTBImage _img, int _Jmax, 
			MTBImage[] _kernels) 
		throws ALDOperatorException, ALDProcessingDAGException {		
		
		// get kernels for scale j=1
		MTBImage[] scaleOneKernels = _kernels;


		// create DWT images array
		MTBImage[] dwtData = new MTBImage[_Jmax+1];
		
		// reference to last smoothed image
		MTBImage lastA = _img;

		// new image arithmetics object
		MTBImageArithmetics mia = new MTBImageArithmetics();
		
		if (this.verbose.booleanValue())
			System.out.print(opIdentifier + "performing convolutions...");

		// iterate over all scales
		for (int j = 1; j <= _Jmax; j++) {

			// check if operator has been paused or interrupted
			if (this.operatorStatus == OperatorControlStatus.OP_STOP) {
				this.operatorExecStatus = 
					OperatorExecutionStatus.OP_EXEC_TERMINATED;
				if (this.verbose.booleanValue())
					System.err.println(opIdentifier + "stopped!");
				return null;
	 		}
			if (this.operatorStatus == OperatorControlStatus.OP_PAUSE) {
				System.err.println(opIdentifier+"paused, waiting to continue...");
				this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_PAUSED;
				do {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// just ignore the exception
					}
				} while (this.operatorStatus != OperatorControlStatus.OP_RESUME);
				this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_RUNNING;
				System.err.println(opIdentifier + "running again...");
			}

			// smooth image at specified scale
			dwtData[0] = conv(lastA, scaleOneKernels, j);
			
			// subtract the smoothed image from the last smoothed image
			dwtData[j] = mia.sub(lastA, dwtData[0]);

			// set last smoothed image to the currently smoothed image
			lastA = dwtData[0];			
		}
		if (this.verbose.booleanValue())
			System.out.println("done.");
		return dwtData;
	}

	/**
	 * Convolve input image with an 'a trous' kernel (zeros inserted) given the original kernel and scale j
	 * @param img input image
	 * @param scaleOneKernels original kernel (without inserted zeros)
	 * @param j scale parameter
	 * @return filtered image
	 * @throws ALDProcessingDAGException 
	 * @throws ALDOperatorException 
	 */
	protected MTBImage conv(MTBImage img, MTBImage[] scaleOneKernels, int j) throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImage tImg = img;
		
		for (int k = 0; k < scaleOneKernels.length; k++) {
			
			if (scaleOneKernels[k] != null) {
		
				// compute size of inflated kernel (depends on scale)
				int kSizeX = scaleOneKernels[k].getSizeX() + (scaleOneKernels[k].getSizeX()-1)*((int)Math.pow(2.0, j-1)-1);
				int kSizeY = scaleOneKernels[k].getSizeY() + (scaleOneKernels[k].getSizeY()-1)*((int)Math.pow(2.0, j-1)-1);
				int kSizeZ = scaleOneKernels[k].getSizeZ() + (scaleOneKernels[k].getSizeZ()-1)*((int)Math.pow(2.0, j-1)-1);
				int kSizeT = scaleOneKernels[k].getSizeT() + (scaleOneKernels[k].getSizeT()-1)*((int)Math.pow(2.0, j-1)-1);
				int kSizeC = scaleOneKernels[k].getSizeC() + (scaleOneKernels[k].getSizeC()-1)*((int)Math.pow(2.0, j-1)-1);
		
				// create kernel image
				MTBImage kernel = MTBImage.createMTBImage(kSizeX, kSizeY, kSizeZ, kSizeT, kSizeC, MTBImageType.MTB_DOUBLE);
				
				// take values from scale one kernel to put into actual scale's kernel at the right position (all other values are zero)
				for (int c = 0; c < kSizeC; c++) {		
					if (c % (int)Math.pow(2.0, j-1) == 0) {
						
						for (int t = 0; t < kSizeT; t++) {
							if (t % (int)Math.pow(2.0, j-1) == 0) {

								for (int z = 0; z < kSizeZ; z++) {
									if (z % (int)Math.pow(2.0, j-1) == 0) {

										for (int y = 0; y < kSizeY; y++) {
											if (y % (int)Math.pow(2.0, j-1) == 0) {
												
												for (int x = 0; x < kSizeX; x++) {
													if (x % (int)Math.pow(2.0, j-1) == 0) {
														kernel.putValueDouble(x, y, z, t, c,  
																			scaleOneKernels[k].getValueDouble(x/(int)Math.pow(2.0, j-1), 
																											  y/(int)Math.pow(2.0, j-1), 
																											  z/(int)Math.pow(2.0, j-1), 
																											  t/(int)Math.pow(2.0, j-1), 
																											  c/(int)Math.pow(2.0, j-1)));
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				
				// compute anchor element of the kernel
				int[] kAnchor = {kSizeX/2, kSizeY/2, kSizeZ/2, kSizeT/2, kSizeC/2};

				// linear filter
				LinearFilter lf = new LinearFilter(tImg, kernel, kAnchor, true, BoundaryPadding.PADDING_BORDER);
				for (int i = 0; i < this.m_statusListeners.size(); i++) {
					lf.addStatusListener(this.m_statusListeners.get(i));
				}
		
				// do linear filtering
				lf.runOp(false);
				tImg = lf.getResultImg();
			}
		}
		
		return tImg;
	}
	
	/**
	 * Denoise wavelet coefficients using Jeffrey's noninformative prior [..]
	 * @param dwt
	 * @param scaleOneSigma
	 * @param sigmaScales
	 */
	protected void denoiseDWTJeffreys(MTBImage[] dwt, double scaleOneSigma, double[] sigmaScales) {
		for (int j = 1; j < dwt.length; j++) {
			
			this.denoise(dwt[j], scaleOneSigma*sigmaScales[j-1]);
		}
	}
	
	/**
	 * Denoise wavelet coefficients using Jeffrey's noninformative prior for a given sigma of noise
	 * @param img input image
	 * @param sigma sigma of noise
	 */
	protected void denoise(MTBImage img, double sigma) {
		int sizeStack = img.getSizeStack();
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		
		double s2 = 3.0*sigma*sigma;
		double val, val2;
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++)	{	
					if ( this.excludeMask == null || this.excludeMask.getValueInt(x, y) == 0){
						val = img.getValueDouble(x, y);

						val2 = (val*val - s2);

						if (val2 < 0.0)
							val2 = 0.0;

						if (val != 0.0) 
							img.putValueDouble(x, y, val2/val);
						else
							img.putValueDouble(x, y, 0.0);
					} else {
						img.putValueDouble(x, y, 0.0);
					}
				}
			}
		}	
		img.setCurrentSliceIndex(0);
	}
	
	/**
	 * Compute scaling factors of noise sigma for wavelet scales
	 * (these scaling factors are estimated using a gaussian noise image with sigma=1 to measure noise in the wavelet scales)
	 * @param p DWT configuration (only Jmax is important here)
	 * @return scaling factor of noise sigma for wavelet scale 1 to Jmax
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected double[] computeSigmaScales() throws ALDOperatorException, ALDProcessingDAGException {
		MTBImage gImg = this.createGaussianNoiseImage(0.0, 1.0, 128.0, 2048, 1000, 1000, 1, 1, 1);
	//	double gSigma = this.getStdDev(gImg);
	//	System.out.println("gSigma: " + gSigma);
		int Jmax = this.getJmax();
		
		MTBImage[] dwtImgs = this.aTrousDWT(gImg, Jmax, this.getKernels());
		
		double[] sigmaScales = new double[Jmax];
	
		for (int j = 1; j <= Jmax; j++) {
			sigmaScales[j-1] = this.getStdDev(dwtImgs[j]);
		}
				for (int j = 1; j <= Jmax; j++) {
						if (this.verbose.booleanValue()) {
								System.out.print("Pure gaussian noise stddev at scale " + j + ": "
								    + sigmaScales[j - 1]);
						}
						sigmaScales[j - 1] /= sigmaScales[0];
						if (this.verbose.booleanValue()) {
								System.out.println(" => sigmaScale at scale " + j + ": "
								    + sigmaScales[j - 1]);
						}
				}
		
		return sigmaScales;
	}
	
	/**
	 * Get the standard deviation (sigma) of an image only using values clipped by 3*sigma from a first estimate of sigma 
	 * @param img input image
	 * @return 3*sigma clipped std dev
	 */
	protected double get3SigClippedStdDev(MTBImage img) {
		
		int sizeStack = img.getSizeStack();
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		
		double val;
		double mu = 0.0;
		double mu2 = 0.0;
		double N = 0.0;
		double sigma;
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);

			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					if ( this.excludeMask == null || this.excludeMask.getValueInt(x, y) == 0){
						val = img.getValueDouble(x, y);
						mu += val;
						mu2 += val*val;
						N++;		
					}
				}
			}
		}
	
		mu /= N;
		mu2 /= N;
		sigma = Math.sqrt(mu2 - mu*mu);

		mu = 0.0;
		mu2 = 0.0;
		N = 0.0;
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);

			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					if ( this.excludeMask == null || this.excludeMask.getValueInt(x, y) == 0){ {
						val = img.getValueDouble(x, y);
						if (Math.abs(val - mu) <= 3.0*sigma) {
							mu += val;
							mu2 += val*val;
							N++;
						}
					}
					}
				}
			}
		}			
		img.setCurrentSliceIndex(0);
		
		mu /= N;
		mu2 /= N;
		sigma = Math.sqrt(mu2 - mu*mu);
		
		return sigma;	
	}
	
	/**
	 * Get standard deviation of the image
	 * @param img input image 
	 * @return sigma
	 */
	protected double getStdDev(MTBImage img) {

		int sizeStack = img.getSizeStack();
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		
		double val;
		double mu = 0.0;
		double mu2 = 0.0;
		double N = 0.0;
		double sigma;
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					val = img.getValueDouble(x, y);
					mu += val;
					mu2 += val*val;
					N++;		
				}
			}
		}
	
		mu /= N;
		mu2 /= N;
		sigma = Math.sqrt(mu2 - mu*mu);
		
		return sigma;	
	}
	
	/**
	 * Create an image with Gaussian noise
	 * @param mean
	 * @param sigma
	 * @param clippingFactor
	 * @param bins
	 * @param sizeX
	 * @param sizeY
	 * @param sizeZ
	 * @param sizeT
	 * @param sizeC
	 * @return
	 */
	protected MTBImage createGaussianNoiseImage(double mean, double sigma, double clippingFactor, int bins, int sizeX, int sizeY, int sizeZ, int sizeT, int sizeC) {
		MTBImage gImg = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, MTBImageType.MTB_DOUBLE);
		int sizeStack = gImg.getSizeStack();
		
		double[] dist = new double[bins];
		double cs = - clippingFactor*sigma;
		double gFactor = 1.0/(Math.sqrt(2.0*Math.PI)*sigma);
		
		double lastVal = 0.0;
		
		double X;
		
		// cumulative distribution
		for (int i = 0; i < bins; i++) {
			X = ((double)i/(double)(bins-1))*2.0*cs - cs;
			
			dist[i] = lastVal + gFactor*Math.exp(-0.5*(X*X)/(sigma*sigma));
			lastVal = dist[i];
		}
		
		// normalization
//		for (int i = 0; i < bins; i++) {
//			dist[i] /= dist[bins-1];
//		}		
		double sample;
		for (int i = 0; i < sizeStack; i++) {
			gImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					sample = getSample(dist);
					sample = sample*2.0*cs - cs - mean;
					gImg.putValueDouble(x, y, sample);
				}
			}
		}
		gImg.setCurrentSliceIndex(0);	
		
		return gImg;
	}
	
	/**
	 * Returns a sample in the range [0, 1] from a cumulative distribution given by the array cdf
	 * @param cdf cumulative distribution array
	 * @return
	 */
	protected double getSample(double[] cdf) {
		double x = Math.random();
		
		int i = 0;
		while (i < cdf.length && cdf[i] < x) {
			i++;
		}
		
		return (double)i/(double)cdf.length;
	}
	
	
	
	// ---- Getter/Setter methods -----------------
	
	/**
	 * Get the denoising flag. true if denoising is activated.
	 */
	public boolean getDenoise()  {
		return this.denoise;
	}
	
	/**
	 * Set the denoising flag. true to activate denoising.
	 */
	protected void setDenoise(boolean denoise) {
		this.denoise = denoise;
	}	
	
	/**
	 * Get reference to the (input) image.
	 * 
	 * @return	Image domain image
	 */
	public MTBImage getImg() {
		return this.img;
	}
	
	/**
	 * Set the image domain image
	 */
	public void setImg(MTBImage img) {
		this.img = img;
	}
	
	/**
	 * @return the excludeMask
	 */
	public MTBImage getExcludeMask() {
		return excludeMask;
	}

	/**
	 * @param excludeMask the excludeMask to set
	 */
	public void setExcludeMask(MTBImage excludeMask) {
		this.excludeMask = excludeMask;
	}

	/**
	 * Get Jmax (maximum scale 2^Jmax - 1)
	 */
	public int getJmax() {
		return this.Jmax;
	}
	
	/**
	 * Set Jmax (maximum scale 2^Jmax - 1)
	 * @param Jmax
	 */
	protected void setJmax(int Jmax) {
		this.Jmax = Jmax;
	}	
	
	/**
	 * Get smoothing kernels
	 */
	protected MTBImage[] getKernels() {
		return this.kernels;
	}
	
	/**
	 * Set smoothing kernels
	 */
	protected void setKernels(MTBImage[] kernels) {
		this.kernels = kernels;
	}
	
	/**
	 * Test if parameter object is set to (forward) transform (image to wavelet)
	 * @return
	 * @throws ALDOperatorException 
	 */
	public boolean isForwardTransform() {
		return (this.transform == TransformationMode.UWT);
	}
	
	/**
	 * Toggle computation of the (forward) transform (image to wavelet)
	 */
	public void setForwardTransform() {
		this.transform = TransformationMode.UWT;
	}
	
	/**
	 * Test if parameter object is set to Inverse Transform (wavelet to image)
	 */
	public boolean isInverseTransform() throws ALDOperatorException {
		return (this.transform == TransformationMode.InverseUWT);
	}
	
	/**
	 * Set computation of the inverse transform (wavelet to image)
	 */
	public void setInverseTransform() throws ALDOperatorException {
		this.transform = TransformationMode.InverseUWT;
	}
	
	/**
	 * Get the UWT coefficient images. At index 0, the lowpass image stored. Indices j = 1,...,Jmax correspond to
	 * the UWT coefficients at scales 2^j - 1, meaning highpass at j=1, highest bandpass at j=2 and lowest bandpass at j=Jmax
	 * 
	 * @return	Reference to UWT images
	 */
	public MTBImageSet getUWT() {
		return this.uwtImages;
	}
	
	/**
	 * Set UWT coefficient images
	 * @params uwtImages (see getUWT() for description)
	 */
	public void setUWT(MTBImageSet uwtImages) {
		this.uwtImages = uwtImages;
	}
	
	
	
	// ----- StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener statuslistener) {	
		this.m_statusListeners.add(statuslistener);	
	}


	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.m_statusListeners.remove(statuslistener);
	}
	
	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			this.m_statusListeners.get(i).statusUpdated(e);
		}
	}
}
