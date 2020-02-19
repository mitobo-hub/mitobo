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

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D.FieldOperation;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter.SigmaInterpretation;
import de.unihalle.informatik.MiToBo.math.images.MTBImageArithmetics;
import de.unihalle.informatik.MiToBo.tools.image.ImageValueTools;

/**
 * <pre>
 * 
 * 2D multi-scale medialness filter for vessel segmentation.
 * 
 * The filter detects vessel-like structures in 2D space and is derived from a
 * medialness function, used to detect tubular structures in 3D space.
 * 
 * Related publication:
 * 
 * author =  {Moghimirad, Elahe and Rezatofighi, Seyed Hamid  and Soltanian-Zadeh, Hamid},
 * title =   {Retinal vessel segmentation using a multi-scale medialness function},
 * journal = {Computers in Biology and Medicine},
 * year =    {2012},
 * volume =  {42},
 * number =  {1},
 * pages =   {50--60},
 * 
 * 
 * @author Danny Misiak
 * 
 * </pre>
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION, allowBatchMode = true)
public class MedialnessMultiScaleFilter2D extends MTBOperator {

		/**
		 * Settings.
		 * 
		 * @author moeller
		 */
		public static enum FilterMode {
				/**
				 * Detect dark entities on bright background.
				 */
				DARK_ON_BRIGHT_BACKGROUND,
				/**
				 * Detect bright entities on dark background.
				 */
				BRIGHT_ON_DARK_BACKGROUND
		}

		// --- input parameters ---

		@Parameter(label = "Input Image", required = true, direction = Parameter.Direction.IN, description = "Input image.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImage inputImage = null;

		@Parameter(label = "Number of Scales", required = true, direction = Parameter.Direction.IN, description = "Number of scales.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private Integer scaleNum = new Integer(7);

		@Parameter(label = "Minimum of Scales", required = true, direction = Parameter.Direction.IN, description = "Minimum of scales.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private Double scaleMin = new Double(0.4);

		@Parameter(label = "Maximum of Scales", required = true, direction = Parameter.Direction.IN, description = "Maximum of scales.", mode = ExpertMode.STANDARD, dataIOOrder = 3)
		private Double scaleMax = new Double(4.0);

		@Parameter(label = "Coefficient", required = true, direction = Parameter.Direction.IN, description = "Coefficient to relate vessel radius and scale.", mode = ExpertMode.STANDARD, dataIOOrder = 4)
		private Double theta = new Double(1.0);

		@Parameter(label = "Filter Mode", required = true, direction = Parameter.Direction.IN, description = "Mode to set relation of foreground to background.", mode = ExpertMode.STANDARD, dataIOOrder = 5)
		private FilterMode filterMode = FilterMode.DARK_ON_BRIGHT_BACKGROUND;

		@Parameter(label = "Adaptive Thresholding", required = true, direction = Parameter.Direction.IN, description = "Use adaptive thresholding to reduce background noise.", mode = ExpertMode.STANDARD, dataIOOrder = 6)
		private Boolean useThresh = new Boolean(true);

		// --- supplemental parameters ---

		// --- output parameters ---

		@Parameter(label = "Output Image", required = true, direction = Parameter.Direction.OUT, description = "Medialness filtered output image.")
		private transient MTBImageDouble outputImage = null;

		// processed input image
		private transient MTBImage processedImage;
		// second partial derivative of input image (x- and x-direction)
		private transient MTBImageDouble ImageXX;
		// second partial derivative of input image (x- and y-direction)
		private transient MTBImageDouble ImageXY;
		// second partial derivative of input image (y- and x-direction)
		private transient MTBImageDouble ImageYX;
		// second partial derivative of input image (y- and y-direction)
		private transient MTBImageDouble ImageYY;

		/**
		 * Standard constructor.
		 */
		public MedialnessMultiScaleFilter2D() throws ALDOperatorException {
				super();
		}

		/**
		 * Constructor to create a new 2D multi-scale medialness filter.
		 * 
		 * @param _inputImage
		 *          input image
		 * @param _scaleNum
		 *          number of scales
		 * @param _scaleMin
		 *          minimum of scales
		 * @param _scaleMax
		 *          maximum of scales
		 * @param _theta
		 *          coefficient to relate vessel radius and scale
		 * @param _filterMode
		 *          filter mode to define relation of foreground to background
		 * @param _useThresh
		 *          flag for adaptive thresholding to reduce background noise
		 * @throws ALDOperatorException
		 */
		public MedialnessMultiScaleFilter2D(MTBImage _inputImage, Integer _scaleNum,
		    Double _scaleMin, Double _scaleMax, Double _theta,
		    FilterMode _filterMode, Boolean _useThresh) throws ALDOperatorException {

				this.inputImage = _inputImage;
				this.scaleNum = _scaleNum;
				this.scaleMin = _scaleMin;
				this.scaleMax = _scaleMax;
				this.theta = _theta;
				this.filterMode = _filterMode;
				this.useThresh = _useThresh;

		}

		/**
		 * Get input image.
		 */
		public MTBImage getInputImage() {
				return inputImage;
		}

		/**
		 * Set input image.
		 */
		public void setInputImage(MTBImage inputImage) {
				this.inputImage = inputImage;
		}

		/**
		 * Get number of scales.
		 */
		public Integer getScaleNum() {
				return scaleNum;
		}

		/**
		 * Set number of scales.
		 */
		public void setScaleNum(Integer scaleNum) {
				this.scaleNum = scaleNum;
		}

		/**
		 * Get minimum of scales.
		 */
		public Double getScaleMin() {
				return scaleMin;
		}

		/**
		 * Set minimum of scales.
		 */
		public void setScaleMin(Double scaleMin) {
				this.scaleMin = scaleMin;
		}

		/**
		 * Get maximum of scales.
		 */
		public Double getScaleMax() {
				return scaleMax;
		}

		/**
		 * Set maximum of scales.
		 */
		public void setScaleMax(Double scaleMax) {
				this.scaleMax = scaleMax;
		}

		/**
		 * Get coefficient to relate vessel radius and scale.
		 */
		public Double getTheta() {
				return theta;
		}

		/**
		 * Set coefficient to relate vessel radius and scale.
		 */
		public void setTheta(Double theta) {
				this.theta = theta;
		}

		/**
		 * Get filter mode to define relation of foreground to background.
		 */
		public FilterMode getFilterMode() {
				return filterMode;
		}

		/**
		 * Set filter mode to define relation of foreground to background.
		 */
		public void setFilterMode(FilterMode _filterMode) {
				this.filterMode = _filterMode;
		}

		/**
		 * Get adaptive thresholding tag.
		 */
		public Boolean getUseThresh() {
				return useThresh;
		}

		/**
		 * Set adaptive thresholding tag.
		 */
		public void setUseThresh(Boolean _useThresh) {
				this.useThresh = _useThresh;
		}

		/**
		 * Get medialness filter image.
		 */
		public MTBImageDouble getOutputImage() {
				return outputImage;
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				/*
				 * Copy input image to processed image to not change the input image if
				 * bright structures on dark background are extracted and the input image is
				 * inverted.
				 */
				this.processedImage = this.inputImage.duplicate();

				// invert image if filter mode is changed
				if (this.filterMode == FilterMode.BRIGHT_ON_DARK_BACKGROUND) {
						ImageValueTools.invertImage(this.processedImage, this);
				}

				/*
				 * Calculate gradient of image convolved with Gaussian kernel.
				 */
				GradientFieldCalculator2D gradient = new GradientFieldCalculator2D(
				    this.processedImage,
				    GradientFieldCalculator2D.GradientMode.PARTIAL_DIFF);
				gradient.runOp();
				MTBVectorField2D gradientField = gradient.getVectorField();

				// get gradient magnitude image from vector field
				FieldOperations2D fieldOps = new FieldOperations2D(gradientField,
				    FieldOperation.MAGNITUDE_IMAGE);
				fieldOps.runOp();
				MTBImageDouble gradientImage = (MTBImageDouble) fieldOps.getResultImage();

				// generate empty image for medialness function results
				MTBImageDouble medialnessImage = (MTBImageDouble) MTBImage.createMTBImage(
				    this.processedImage.getSizeX(), this.processedImage.getSizeY(),
				    this.processedImage.getSizeZ(), this.processedImage.getSizeT(),
				    this.processedImage.getSizeC(), MTBImageType.MTB_DOUBLE);

				// generate empty image for eigenvalues at each pixel
				MTBImageDouble eigenvalueImage = (MTBImageDouble) MTBImage.createMTBImage(
				    this.processedImage.getSizeX(), this.processedImage.getSizeY(),
				    this.processedImage.getSizeZ(), this.processedImage.getSizeT(),
				    this.processedImage.getSizeC(), MTBImageType.MTB_DOUBLE);

				// run multi-scale detection
				for (int s = 1; s <= this.scaleNum; s++) {

						/*
						 * Calculate current scale, depending on minimum, maximum and number of
						 * scales.
						 */
						double scale = Math
						    .exp(Math.log(this.scaleMin)
						        + (((s - 1) * (Math.log(this.scaleMax) - Math.log(this.scaleMin))) / (this.scaleNum - 1)));

						// use gaussian filter with sigma = scale and 3x3 mask
						GaussFilter gauss = new GaussFilter(gradientImage, scale, scale);
						gauss.setKernelTruncation(3.0);
						gauss.setSigmaInterpretation(SigmaInterpretation.PIXEL);
						gauss.runOp();
						MTBImageDouble convolvedGradientImg = (MTBImageDouble) gauss
						    .getResultImg();

						// calculate second partial derivatives of input image for Hessian matrix
						this.getImageDerivatives();

						// calculate weighted medialness at each image pixel for different scales
						for (int y = 0; y < this.processedImage.getSizeY(); y++) {
								for (int x = 0; x < this.processedImage.getSizeX(); x++) {

										/*
										 * Get eigenvalue decomposition of of the Hessian matrix to get
										 * largest eigenvalue at pixel (x,y) to finally get the vector
										 * perpendicular to the vessel path and eigenvalues.
										 */
										EigenvalueDecomposition ed = getEigenvalueDecompositionAt(x, y);
										// get eigenvector matrix
										Matrix eigVectors = ed.getV();
										// get block diagonal eigenvalue matrix
										Matrix eigValues = ed.getD();

										/*
										 * Calculate values of 2D functions b1 and b2 with eigenvector
										 * corresponding to largest eigenvalue (last eigenvector column)
										 */
										int factorX = (int) Math.round(scale * theta.doubleValue()
										    * eigVectors.get(0, 1));
										int factorY = (int) Math.round(scale * theta.doubleValue()
										    * eigVectors.get(1, 1));
										double b1 = 0.0;
										if ((x + factorX) < this.processedImage.getSizeX()
										    && (x + factorX) >= 0
										    && (y + factorY) < this.processedImage.getSizeY()
										    && (y + factorY) >= 0) {
												b1 = convolvedGradientImg.getValueDouble((x + factorX),
												    (y + factorY));
										}
										double b2 = 0.0;
										if ((x - factorX) >= 0
										    && (x - factorX) < this.processedImage.getSizeX()
										    && (y - factorY) >= 0
										    && (y - factorY) < this.processedImage.getSizeY()) {
												b2 = convolvedGradientImg.getValueDouble((x - factorX),
												    (y - factorY));
										}

										/*
										 * Calculate values of exponential function p for b1 and b2. This
										 * weighting function models the vessel by a Gaussian function and
										 * emphasizes the vessel structures and weakens other structures.
										 */
										double p1 = Math.exp(-(1 - (b1 / ((b1 + b2) / 2)))
										    / (2 * (scale * scale)));
										double p2 = Math.exp(-(1 - (b2 / ((b1 + b2) / 2)))
										    / (2 * (scale * scale)));

										// calculate weighted medialness function m
										double m = 0.5 * ((b1 * p1) + (b2 * p2));

										// adaptive thresholding to reduce background noise
										if (this.useThresh.booleanValue()) {
												if (m > (convolvedGradientImg.getValueDouble(x, y))) {
														m = (m - convolvedGradientImg.getValueDouble(x, y));
												} else {
														m = 0.0;
												}
										}

										/*
										 * Maximize response of medialness function at different scales and
										 * put values for each pixel in the resulting image of the medialness
										 * detection filter.
										 */
										if (m > medialnessImage.getValueDouble(x, y)) {
												medialnessImage.putValueDouble(x, y, m);
										}

										// put eigenvalues for current position in eigenvalue image
										double eigenvalue = eigValues.trace();
										eigenvalueImage.putValueDouble(x, y, eigenvalue);
								}
						}
				}
				// use gaussian filter with sigma = 1.0 and 3x3 mask
				GaussFilter gauss = new GaussFilter(eigenvalueImage, 1.0, 1.0);
				gauss.setKernelTruncation(3.0);
				gauss.setSigmaInterpretation(SigmaInterpretation.PIXEL);
				gauss.runOp();
				MTBImageDouble convolvedEigenvalues = (MTBImageDouble) gauss.getResultImg();
				// generate final result of vessel detection filter
				MTBImageArithmetics IA = new MTBImageArithmetics();
				this.outputImage = (MTBImageDouble) IA.mult(medialnessImage,
				    convolvedEigenvalues);
				this.outputImage.setTitle("MedialnessFiltered-Image");
		}

		/**
		 * Get eigenvalue decomposition of of the Hessian matrix to get largest
		 * eigenvalue at pixel (x,y) to get the vector perpendicular to the vessel
		 * path and eigenvalues.
		 * 
		 * @param x
		 *          pixel coordinate in x-direction
		 * @param y
		 *          pixel coordinate in y-direction
		 * @return EigenvalueDecomposition
		 */
		private EigenvalueDecomposition getEigenvalueDecompositionAt(int x, int y) {

				// calculate 2x2 Hessian matrix for current pixel position
				Matrix H = new Matrix(2, 2);
				H.set(0, 0, this.ImageXX.getValueDouble(x, y));
				H.set(0, 1, this.ImageXY.getValueDouble(x, y));
				H.set(1, 0, this.ImageYX.getValueDouble(x, y));
				H.set(1, 1, this.ImageYY.getValueDouble(x, y));

				// get eigenvalue decomposition of Hessian matrix at point (x,y)
				EigenvalueDecomposition ed = H.eig();
				return ed;
		}

		/**
		 * Calculate second partial derivatives of the input image to generate Hessian
		 * matrix at each pixel position and getting the vector perpendicular to the
		 * vessel path.
		 */
		private void getImageDerivatives() {

				GradientFieldCalculator2D gradient;
				try {

						/*
						 * Calculate first derivatives of image.
						 */
						gradient = new GradientFieldCalculator2D(this.processedImage,
						    GradientFieldCalculator2D.GradientMode.PARTIAL_DIFF);
						gradient.runOp();
						MTBVectorField2D gradientField = gradient.getVectorField();

						// get first derivative in x-direction
						FieldOperations2D fieldOps = new FieldOperations2D(gradientField,
						    FieldOperation.DIFF_X_IMAGE);
						fieldOps.runOp();
						MTBImageDouble Dx = (MTBImageDouble) fieldOps.getResultImage();
						// get first derivative in y-direction
						fieldOps = new FieldOperations2D(gradientField,
						    FieldOperation.DIFF_Y_IMAGE);
						fieldOps.runOp();
						MTBImageDouble Dy = (MTBImageDouble) fieldOps.getResultImage();

						/*
						 * Calculate second derivatives of first derivative in x-direction.
						 */
						gradient = new GradientFieldCalculator2D(Dx,
						    GradientFieldCalculator2D.GradientMode.PARTIAL_DIFF);
						gradient.runOp();
						gradientField = gradient.getVectorField();

						// get second derivative in x-direction of first derivative in x-direction
						fieldOps = new FieldOperations2D(gradientField,
						    FieldOperation.DIFF_X_IMAGE);
						fieldOps.runOp();
						MTBImageDouble Dxx = (MTBImageDouble) fieldOps.getResultImage();

						// get second derivative in y-direction of first derivative in x-direction
						fieldOps = new FieldOperations2D(gradientField,
						    FieldOperation.DIFF_Y_IMAGE);
						fieldOps.runOp();
						MTBImageDouble Dxy = (MTBImageDouble) fieldOps.getResultImage();

						/*
						 * Calculate second derivatives of first derivative in y-direction.
						 */
						gradient = new GradientFieldCalculator2D(Dy,
						    GradientFieldCalculator2D.GradientMode.PARTIAL_DIFF);
						gradient.runOp();
						gradientField = gradient.getVectorField();

						// get second derivative in x-direction of first derivative in y-direction
						// note symmetry of second derivatives (Schwarz' theorem)
						MTBImageDouble Dyx = (MTBImageDouble) Dxy.duplicate();

						// get second derivative in y-direction of first derivative in y-direction
						fieldOps = new FieldOperations2D(gradientField,
						    FieldOperation.DIFF_Y_IMAGE);
						fieldOps.runOp();
						MTBImageDouble Dyy = (MTBImageDouble) fieldOps.getResultImage();

						this.ImageXX = Dxx;
						this.ImageXY = Dxy;
						this.ImageYX = Dyx;
						this.ImageYY = Dyy;

				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}

		}

}
