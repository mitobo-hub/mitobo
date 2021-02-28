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

import loci.common.StatusListener;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.filters.linear.LinearFilter;

/**
 * 2D Gabor filter.
 * <p>
 * The implementation is based on the following definition of the Gabor family:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
 *      \\begin{eqnarray*} 
 *         g(x,y) &=& g_{\\sigma_x\\sigma_y}(x,y) \\cdot 
 *                e^{2\\pi \\cdot \\imath \\cdot \\phi \\cdot x} \\\\
 *         g_{\\sigma_x\\sigma_y} (x,y) &=& 
 *            \\frac{1}{2 \\cdot \\pi \\cdot \\sigma_x\\sigma_y} \\cdot 
 *               e^{-\\frac{1}{2} \\left( \\frac{x^2}{\\sigma_x^2} + 
 *                                  \\frac{y^2}{\\sigma_y^2} \\right) }\\\\
 *         e^{2\\pi \\cdot \\imath \\cdot \\phi \\cdot x} &=& 
 *             \\cos(2\\pi \\cdot \\phi \\cdot x) 
 *                   + \\imath \\cdot \\sin(2\\pi \\cdot \\phi \\cdot x)
 *      \\end{eqnarray*}}
 * 
 * This definition is basically the one to be found in
 * <i>J.G. Daugmann, "Uncertainty relation for resolution in space, spatial
 * frequency, and orientation optimized by two-dimensional visual cortical
 * filters", J. Opt. Soc. Am. A/Vol. 2, No. 7, July 1985</i>.
 * <p>
 * The only difference is the scaling factor of the Gaussian which is sometimes
 * omitted.
 * <p>
 * The filter mask is rotated by transforming the x and y coordinates as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
 *      \\begin{equation*} 
 *         \\left( \\begin{array}{c}
 *                   x \\\\ y
 *                 \\end{array} \\right) = 
 *                      \\left( \\begin{array}{cc}
 *                          \\cos \\phi & \\sin \\phi \\\\
 *                         -\\sin \\phi & \\cos \\phi 
 *                              \\end{array} \\right) \\cdot
 *         \\left( \\begin{array}{c}
 *                   x^\\prime \\\\ y^\\prime
 *                 \\end{array} \\right) 
 *      \\end{equation*}}
 *
 *
 * @author Birgit Moeller
 */
@ALDDerivedClass
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, level=Level.APPLICATION)
public class GaborFilter2D extends OrientedFilter2D {

	/**
	 * Reponse mode.
	 */
	public static enum ResultType {
		/**
		 * Result is energy of complex filter response.
		 */
		RESPONSE_ENERGY,
		/**
		 * Result is real part of complex filter response.
		 */
		RESPONSE_REALPART,
		/**
		 * Result is imaginary part of complex filter response.
		 */
		RESPONSE_COMPLEXPART
	}

	/**
	 * Standard deviation of the Gaussian in x.
	 * <p>
	 * The mask width is derived from the standard deviation of the Gaussian,<br>
	 * i.e. the width is given by
	 * {@latex.inline %preamble{\\usepackage{amssymb, amsmath}}
	 * 	$w = 2 \\cdot (\\text{int})(2 \\cdot \\sigma + 0.5)+1$}.<br>
	 * Note that the minimum width is 3.
	 */
	@Parameter( label= "\u03C3 of Gaussian in x", required = false, dataIOOrder=2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Std. deviation of Gaussian in x.")
	protected double gaussStdDevX = 1.0;

	/**
	 * Standard deviation of the Gaussian in x.
	 * <p>
	 * The mask width is derived from the standard deviation of the Gaussian,<br>
	 * i.e. the width is given by 
	 * {@latex.inline %preamble{\\usepackage{amssymb, amsmath}} 
	 * $w = 2 \\cdot (\\text{int})(2 \\cdot \\sigma + 0.5)+1$}.<br>
	 * Note that the minimum width is 3.
	 */
	@Parameter( label= "\u03C3 of Gaussian in y", required = false, dataIOOrder=3,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Std. deviation of Gaussian in y.")
	protected double gaussStdDevY = 1.0;

	/**
	 * Filter frequency.
	 */
	@Parameter( label= "Filter Frequency \u03C6", required = true, 
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Frequency of the filter.", dataIOOrder = 3)
	protected double frequency = 1.0/8.0;

	/**
	 * Flag to invert filter mask.
	 */
	@Parameter( label= "Invert Mask", required = false, dataIOOrder = 4,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "If true, filter mask is inverted.")
	protected boolean invertMask = false;

	/**
	 * Desired type of result.
	 */
	@Parameter( label= "Result Type", required = false, dataIOOrder = 6,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Type of desired result.")
	protected ResultType resultType = ResultType.RESPONSE_ENERGY;

	/**
	 * Size of the (squared) kernel window.
	 * <p>
	 * If set to -1, the size is automatically derived from the given 
	 * standard deviations in x and y.
	 */
	@Parameter( label= "Kernel Size", required = false, dataIOOrder = 5,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Size of kernel window, " + 
			"if set to -1 it's determined automatically.")
	protected int kernelSize = -1;
	
	/**
	 * Allows to request kernel masks for real and complex parts independently.
	 */
	protected static enum KernelPart {
		/**
		 * Triggers generation of real part only. 
		 */
		REAL,
		/**
		 * Triggers generation of complex part only.
		 */
		COMPLEX
	}
	
	/**
	 * Variable to configure which part of kernel mask is calculated.
	 * <p>
	 * This variable is mainly used internally for generating parts of the
	 * Gabor filter mask independently, and for unit testing. 
	 * Due to this the variable is not annotated as parameter.
	 */
	protected KernelPart kPart;
	
	/**
	 * Default constructor.
	 */
	public GaborFilter2D() throws ALDOperatorException {
		super();
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.core.operator.MTBOperator#readResolve()
	 */
	@Override
	protected Object readResolve() {
		return super.readResolve();
	}

	@Override
	public GaborFilter2D clone() {
		GaborFilter2D newOp;
		try {
			newOp = new GaborFilter2D();
			// super class fields
			newOp.inputImg = this.inputImg;
			newOp.angle = this.angle;
			newOp.mode = this.mode;
			newOp.statusListeners = this.statusListeners;
			// local fields
			newOp.gaussStdDevX = this.gaussStdDevX;
			newOp.gaussStdDevY = this.gaussStdDevY;
			newOp.frequency = this.frequency;
			newOp.invertMask = this.invertMask; 
			newOp.resultType = this.resultType;
			newOp.kernelSize = this.kernelSize;
			newOp.kPart = this.kPart;
			return newOp;
		} catch (ALDOperatorException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@SuppressWarnings("null")
  @Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImageDouble kernel;
		MTBImageDouble realResponse = null;
		MTBImageDouble compResponse = null;
		LinearFilter lf;
		
		// apply real part of kernel to image
		if (  this.resultType == ResultType.RESPONSE_REALPART
				|| this.resultType == ResultType.RESPONSE_ENERGY) {
			this.kPart = KernelPart.REAL;
			kernel = this.getKernel(this.angle.doubleValue());
			lf = new LinearFilter();
			for (StatusListener l : this.statusListeners)
				lf.addStatusListener(l);
			lf.setInputImg(this.inputImg);
			lf.setKernelImg(kernel); 
			lf.setKernelNormalization(false);
			lf.setResultImageType(MTBImageType.MTB_DOUBLE);
			lf.runOp();
			realResponse = (MTBImageDouble)lf.getResultImg();
		}
		
		// if just the response to the real part is requested, we are done
		if (this.resultType == ResultType.RESPONSE_REALPART) {
			this.resultImg = realResponse;
			return;
		}

		// apply complex part of kernel to image
		// apply real part of kernel to image
		if (  this.resultType == ResultType.RESPONSE_COMPLEXPART
				|| this.resultType == ResultType.RESPONSE_ENERGY) {
			this.kPart = KernelPart.COMPLEX;
			kernel = this.getKernel(this.angle.doubleValue());
			lf = new LinearFilter();
			for (StatusListener l : this.statusListeners)
				lf.addStatusListener(l);
			lf.setInputImg(this.inputImg);
			lf.setKernelImg(kernel); 
			lf.setKernelNormalization(false);
			lf.setResultImageType(MTBImageType.MTB_DOUBLE);
			lf.runOp();
			compResponse = (MTBImageDouble)lf.getResultImg();
		}
		
		// if just the response to the complex part is requested, we are done
		if (this.resultType == ResultType.RESPONSE_COMPLEXPART) {
			this.resultImg = compResponse;
			return;
		}

		// result is in this case the energy of real and complex parts
		this.resultImg = (MTBImageDouble)realResponse.duplicate();
		for (int y=0; y<realResponse.getSizeY(); ++y) {
			for (int x=0; x<realResponse.getSizeX(); ++x) {
				this.resultImg.putValueDouble(x, y, Math.sqrt(
					realResponse.getValueDouble(x, y)*realResponse.getValueDouble(x, y) +
					compResponse.getValueDouble(x, y)*compResponse.getValueDouble(x, y)));
			}
		}
	}

	/**
	 * Calculates kernel for given angle and pre-defined sigmas.
	 * <p>
	 * Dependend on the value of this.kPart either the real or the complex 
	 * part of the kernel mask is returned.
	 * 
	 * @param _angle		Rotation angle in degrees.
	 * @return	Kernel image.
	 */
	@SuppressWarnings("null")
  @Override
  public MTBImageDouble getKernel(double _angle) {
		
		// init some local variables.
		int kernelWidth, kernelHeight;
		MTBImageDouble gaussKernel, cosKernel, sinKernel = null;
		
		if (this.kernelSize == -1) {
			kernelWidth = 2 * (int)(2 * this.gaussStdDevX + 0.5) + 1;
			kernelHeight = 2 * (int)(2 * this.gaussStdDevY + 0.5) + 1;
			this.kernelSize = 
				(kernelWidth > kernelHeight) ? kernelWidth : kernelHeight;
		}
		if (this.kernelSize < 3)
			this.kernelSize = 3;
		
		// Gaussian kernel
		gaussKernel = this.getGaussianKernel(_angle);
		// real part of sinusoidal kernel
		cosKernel = this.getRealKernel(_angle);
		// complex part of sinusoidal kernel
		if (this.kPart == KernelPart.COMPLEX)
			sinKernel = this.getComplexKernel(_angle);
			
		MTBImageDouble kernelImg = (MTBImageDouble)MTBImage.createMTBImage(
			this.kernelSize, this.kernelSize, 1, 1, 1,	MTBImageType.MTB_DOUBLE);
		double response;
		for (int x = 0; x< this.kernelSize; ++x) {
			for (int y = 0; y< this.kernelSize; ++y) {
				// real kernel part requested
				if (this.kPart == KernelPart.REAL) {
					response = 
							gaussKernel.getValueDouble(x, y) * cosKernel.getValueDouble(x, y);
					if (!this.invertMask)
						kernelImg.putValueDouble(x, y,  response);
					else
						kernelImg.putValueDouble(x, y, -response);
				}
				// complex kernel part requested
				else {
					response = 
							gaussKernel.getValueDouble(x, y) * sinKernel.getValueDouble(x, y);
					if (!this.invertMask)
						kernelImg.putValueDouble(x, y,  response);
					else
						kernelImg.putValueDouble(x, y, -response);
				}
			}
		}
		return kernelImg;
	}
	
	/**
	 * Generates the Gaussian part of the Gabor kernel for a given orientation.
	 * @param _angle	Filter orientation.
	 * @return	Kernel image.
	 */
	protected MTBImageDouble getGaussianKernel(double _angle) {
		
		// init some local variables.
		int kernelSizeHalf, kernelWidth, kernelHeight;

		if (this.kernelSize == -1) {
			kernelWidth = 2 * (int)(2 * this.gaussStdDevX + 0.5) + 1;
			kernelHeight = 2 * (int)(2 * this.gaussStdDevY + 0.5) + 1;
			this.kernelSize = 
				(kernelWidth > kernelHeight) ? kernelWidth : kernelHeight;
		}
		if (this.kernelSize < 3)
			this.kernelSize = 3;

		kernelSizeHalf = this.kernelSize / 2;
		double radAngle = _angle / 180.0 * Math.PI;
		
		MTBImageDouble kernelImg = (MTBImageDouble)MTBImage.createMTBImage(
			this.kernelSize, this.kernelSize, 1, 1, 1,	MTBImageType.MTB_DOUBLE);
		double gaussVal, gaussX, gaussY;
		for (int x = -kernelSizeHalf; x<= kernelSizeHalf; ++x) {
			for (int y = -kernelSizeHalf; y<= kernelSizeHalf; ++y) {
				double x_orig = Math.cos(radAngle)*x + Math.sin(radAngle)*y;
				double y_orig = -Math.sin(radAngle)*x + Math.cos(radAngle)*y;
				gaussX = x_orig * x_orig / (this.gaussStdDevX * this.gaussStdDevX); 
				gaussY = y_orig * y_orig / (this.gaussStdDevY * this.gaussStdDevY); 
				gaussVal = Math.exp( - 0.5 * ( gaussX + gaussY ) );
				kernelImg.putValueDouble(x+kernelSizeHalf,y+kernelSizeHalf,gaussVal);
			}
		}
		return kernelImg;
	}
	
  /**
   * Generates the cosine filter mask of the complex part of the Gabor filter.
   * @param _angle		Orientation of the filter.
   * @return	Kernel image.
   */
  protected MTBImageDouble getRealKernel(double _angle) {
		
		// init some local variables.
		int kernelSizeHalf, kernelWidth, kernelHeight;

		if (this.kernelSize == -1) {
			kernelWidth = 2 * (int)(2 * this.gaussStdDevX + 0.5) + 1;
			kernelHeight = 2 * (int)(2 * this.gaussStdDevY + 0.5) + 1;
			this.kernelSize = 
				(kernelWidth > kernelHeight) ? kernelWidth : kernelHeight;
		}
		if (this.kernelSize < 3)
			this.kernelSize = 3;

		kernelSizeHalf = this.kernelSize / 2;
		double radAngle = _angle / 180.0 * Math.PI;
		
		MTBImageDouble kernelImg = (MTBImageDouble)MTBImage.createMTBImage(
			this.kernelSize, this.kernelSize, 1, 1, 1,	MTBImageType.MTB_DOUBLE);
		double cosVal;
		for (int x = -kernelSizeHalf; x<= kernelSizeHalf; ++x) {
			for (int y = -kernelSizeHalf; y<= kernelSizeHalf; ++y) {
				double x_orig = Math.cos(radAngle)*x + Math.sin(radAngle)*y;
				cosVal =	Math.cos( 2 * Math.PI * this.frequency * x_orig);
				kernelImg.putValueDouble(x+kernelSizeHalf,y+kernelSizeHalf,cosVal);
			}
		}
		return kernelImg;
	}

  /**
   * Generates the sinus filter mask of the complex part of the Gabor filter.
   * @param _angle	Filter orientation.
   * @return	Kernel image.
   */
  protected MTBImageDouble getComplexKernel(double _angle) {
		
		// init some local variables.
		int kernelSizeHalf, kernelWidth, kernelHeight;

		if (this.kernelSize == -1) {
			kernelWidth = 2 * (int)(2 * this.gaussStdDevX + 0.5) + 1;
			kernelHeight = 2 * (int)(2 * this.gaussStdDevY + 0.5) + 1;
			this.kernelSize = 
				(kernelWidth > kernelHeight) ? kernelWidth : kernelHeight;
		}
		if (this.kernelSize < 3)
			this.kernelSize = 3;

		kernelSizeHalf = this.kernelSize / 2;
		double radAngle = _angle / 180.0 * Math.PI;
		
		MTBImageDouble kernelImg = (MTBImageDouble)MTBImage.createMTBImage(
			this.kernelSize, this.kernelSize, 1, 1, 1,	MTBImageType.MTB_DOUBLE);
		double sinVal, x_orig;
		for (int x = -kernelSizeHalf; x<= kernelSizeHalf; ++x) {
			for (int y = -kernelSizeHalf; y<= kernelSizeHalf; ++y) {
				x_orig = Math.cos(radAngle)*x + Math.sin(radAngle)*y;
				sinVal =	Math.sin( 2 * Math.PI * this.frequency * x_orig);
				kernelImg.putValueDouble(x+kernelSizeHalf,y+kernelSizeHalf,sinVal);
			}
		}
		return kernelImg;
	}

  /**
	 * Specify Gaussian standard deviation in x.
	 * @param sx	Standard deviation.
	 */
	public void setSigmaX(double sx) {
		this.gaussStdDevX = sx;
	}

	/**
	 * Specify Gaussian standard deviation in y.
	 * @param sy	Standard deviation.
	 */
	public void setSigmaY(double sy) {
		this.gaussStdDevY = sy;
	}
	
	/**
	 * Specify frequency.
	 * @param f		Frequency.
	 */
	public void setFrequency(double f) {
		this.frequency = f;
	}

	/**
	 * Specify result type.
	 * @param t		Desired type of result.
	 */
	public void setResultType(ResultType t) {
		this.resultType = t;
	}

	/**
	 * Enable/disable inversion of mask.
	 * @param b		Flag for inversion.
	 */
	public void setInvertMask(boolean b) {
		this.invertMask = b;
	}

	/**
	 * Specify size of the kernel.
	 * @param s		Size to apply.
	 */
	public void setKernelSize(int s) {
		this.kernelSize = s;
	}
	
	/**
	 * Specify which kernel part should be generated.
	 * @param k		Part of kernel to apply, i.e. real or complex part.
	 */
	protected void setKernelPart(KernelPart k) {
		this.kPart = k;
	}
}

