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

package de.unihalle.informatik.MiToBo.fields;

import ij.IJ;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Class to calculate a special 2D vector field, a Gradient Vector Flow Field
 * (GVF) with its x- and y-flows from the given image. The GVF field is used as
 * external energy in the snake calculation.
 * 
 * 
 * @author Danny Misiak
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.NONE, level = Level.STANDARD)
@ALDMetaInfo(export = ExportPolicy.ALLOWED)
public class GVFFieldCalculator2D extends MTBOperator {

		/**
		 * Size of the image and the calculated gvf field in x-direction.
		 */
		private transient int width;
		/**
		 * Size of the image and the calculated gvf field in y-direction.
		 */
		private transient int height;

		/**
		 * Type of the input image and later the type of the calculated 2D vector
		 * field.
		 */
		private transient MTBImageType type;

		/**
		 * Number of iterations for calculating the GVF field.
		 */
		@Parameter(label = "numIterations", required = true, direction = Parameter.Direction.IN, description = "Number of GVF field iterations")
		private int numIterations = 0;
		/**
		 * The input image as base for the GVF field calculation.
		 */
		@Parameter(label = "inputImage", required = true, direction = Parameter.Direction.IN, description = "Input image")
		private transient MTBImage inputImage = null;
		/**
		 * The calculated GVF vector field.
		 */
		@Parameter(label = "vectorField", required = true, direction = Parameter.Direction.IN, description = "Gradient vector field of the 2D image.")
		private transient MTBVectorField2D vectorField = null;


		/**
		 * The flows of the field in x- and y-direction.
		 */
		protected transient double[] myX, myY, xFlow, yFlow;

		/**
		 * Standard constructor. A new empty operator object is initialized.
		 */
		public GVFFieldCalculator2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor. A new operator object is initialized.
		 * 
		 * @param inImg
		 *          image to work on
		 * @param iterations
		 *          number of iterations for the gvf field
		 * @throws ALDOperatorException
		 */
		public GVFFieldCalculator2D(MTBImage inImg, int iterations)
		    throws ALDOperatorException {
				this.inputImage = inImg;
				this.numIterations = iterations;
				this.type = inImg.getType();
				this.width = inImg.getSizeX();
				this.height = inImg.getSizeY();
		}

		// Get the 2D vector field based input image.
		// Get the number of iterations for the field calculation.
		// Get the resulting 2D vector field (the GVF field).

		/**
		 * Get input image.
		 */
		public MTBImage getInputImage() {
				return this.inputImage;
		}

		/**
		 * Set input image.
		 */
		public void setInputImage(MTBImage inImg) {
				this.inputImage = inImg;
				this.type = inImg.getType();
		}

		/**
		 * Get number of iterations for the GVF field.
		 */
		public int getNumIterations() {
				return this.numIterations;
		}

		/**
		 * Set number of iterations for the GVF field.
		 */
		public void setNumIterations(int iterations) {
				this.numIterations = iterations;
		}

		/**
		 * Get input image type.
		 */
		public MTBImageType getType() {
				return this.type;
		}

		/**
		 * Get calculated GVF vector field..
		 */
		public MTBVectorField2D getVectorField() {
				return this.vectorField;
		}

		/**
		 * Get image width.
		 */
		public int getWidth() {
				return this.width;
		}

		/**
		 * Set image width.
		 */
		public void setWidth(int w) {
				this.width = w;
		}

		/**
		 * Get image height.
		 */
		public int getHeight() {
				return this.height;
		}

		/**
		 * Set image height.
		 */
		public void setHeight(int h) {
				this.height = h;
		}

		/**
		 * This method does the actual work. Each subclass needs to overwrite this
		 * method. As parameter a MiToBo parameter object is passed to the function
		 * where all required data and operator configuration information can be
		 * found.
		 * 
		 * @throws ALDOperatorException
		 */
		@Override
		protected void operate() throws ALDOperatorException {
				// calculate the gvf field
				calcGVF();
				/*
				 * Set the output (2D vector field) from the energy flow vectors of the gvf
				 * field.
				 */
				this.vectorField = new MTBVectorField2D(this.xFlow, this.yFlow, this.width,
				    this.height);
		}

		/**
		 * Calculate the GVF field with the given number of iterations on the given
		 * input image.
		 */
		protected void calcGVF() {
				double[] values = getValues();
				// BoundMirrorExpansion, enhance image borders and mirror border values
				values = BoundExpand(values, this.width, this.height);
				this.width = this.width + 2;
				this.height = this.height + 2;
				// partial derivative of gradient-magnitude-image in x- and y-direction
				this.myX = Abl_X(values, this.width, this.height);
				this.myY = Abl_Y(values, this.width, this.height);
				// calculate factor b for calculation of the x- and y-flow
				double[] b = new double[this.myX.length];
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; x++) {
								double dx = this.myX[y * this.width + x];
								double dy = this.myY[y * this.width + x];
								b[y * this.width + y] = Math.pow(dx, 2) + Math.pow(dy, 2);
						}
				}
				double mu = 0.2;
				double dt = 1.0;
				double r = mu * dt;
				this.xFlow = this.myX;
				this.yFlow = this.myY;
				for (int n = 0; n < this.numIterations; n++) {
						double proz = ((double) (n + 1)) / ((double) this.numIterations);
						IJ.showProgress(proz);
						this.xFlow = BoundEnsure(this.xFlow, this.width, this.height);
						this.yFlow = BoundEnsure(this.yFlow, this.width, this.height);
						double[] tmpU = new double[this.width * this.height];
						double[] tmpV = new double[this.width * this.height];
						for (int j = 1; j < this.height - 1; j++) {
								for (int i = 1; i < this.width - 1; i++) {

										double tu1 = this.xFlow[j * this.width + i];
										double tu2 = r
										    * ((this.xFlow[j * this.width + (i + 1)])
										        + (this.xFlow[(j + 1) * this.width + i])
										        + (this.xFlow[j * this.width + (i - 1)])
										        + (this.xFlow[(j - 1) * this.width + i]) - 4 * (this.xFlow[j
										        * this.width + i]));
										double tu3 = b[j * this.width + i]
										    * ((dt * tu1) - this.myX[j * this.width + i]);
										tmpU[j * this.width + i] = (tu1 + tu2 - tu3);
										double tv1 = this.yFlow[j * this.width + i];
										double tv2 = r
										    * ((this.yFlow[j * this.width + (i + 1)])
										        + (this.yFlow[(j + 1) * this.width + i])
										        + (this.yFlow[j * this.width + (i - 1)])
										        + (this.yFlow[(j - 1) * this.width + i]) - 4 * (this.yFlow[j
										        * this.width + i]));
										double tv3 = b[j * this.width + i]
										    * ((dt * tv1) - this.myY[j * this.width + i]);
										tmpV[j * this.width + i] = (tv1 + tv2 - tv3);
								}
						}
						for (int y = 2; y < this.height - 2; y++) {
								for (int x = 2; x < this.width - 2; x++) {
										this.xFlow[y * this.width + x] = tmpU[y * this.width + x];
										this.yFlow[y * this.width + x] = tmpV[y * this.width + x];
								}
						}
				}
				// shrink the border
				this.xFlow = BoundShrink(this.xFlow, this.width, this.height);
				this.yFlow = BoundShrink(this.yFlow, this.width, this.height);
				// adapt image size to basic values from the given image
				this.width = this.width - 2;
				this.height = this.height - 2;
		}

		/**
		 * Get the double values of the given image.
		 * 
		 * @return Double values of the given image in a one dimensional double array.
		 */
		protected double[] getValues() {
				double[] tmpValues = new double[this.width * this.height];
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; x++) {
								tmpValues[y * this.width + x] = this.inputImage.getValueDouble(x, y);
						}
				}
				return tmpValues;
		}

		/**
		 * Normalize the values in the gvf field in a range of [-1,1].
		 */
		protected void normValues() {
				double maxX = 0.0;
				double maxY = 0.0;
				for (int i = 0; i < this.xFlow.length; i++) {
						if (Math.abs(this.xFlow[i]) > maxX)
								maxX = Math.abs(this.xFlow[i]);
						if (Math.abs(this.yFlow[i]) > maxY)
								maxY = Math.abs(this.yFlow[i]);
				}
				for (int i = 0; i < this.xFlow.length; i++) {
						this.xFlow[i] = (this.xFlow[i] / maxX);
						this.yFlow[i] = (this.yFlow[i] / maxY);
				}
		}

		/**
		 * Calculate the x-derivative of the gradient image.
		 * 
		 * @param values
		 *          gradient values of the image
		 * @param w
		 *          width of the given image
		 * @param h
		 *          height of the given image
		 * @return A one dimensional array including the x-derivative values
		 */
		protected double[] Abl_X(double[] values, int w, int h) {
				int width = w;
				int height = h;
				double[] ablX = new double[values.length];
				for (int y = 1; y < height - 1; y++) {
						for (int x = 1; x < width - 1; x++) {
								double dx = (values[y * width + (x + 1)])
								    - (values[y * width + (x - 1)]);
								ablX[y * width + x] = dx;
						}
				}
				return (ablX);
		}

		/**
		 * Calculate the y-derivative of the gradient image.
		 * 
		 * @param values
		 *          gradient values of the image
		 * @param w
		 *          width of the given image
		 * @param h
		 *          height of the given image
		 * @return A one dimensional array including the y-derivative values
		 */
		protected double[] Abl_Y(double[] values, int w, int h) {
				int width = w;
				int height = h;
				double[] ablY = new double[values.length];
				for (int y = 1; y < height - 1; y++) {
						for (int x = 1; x < width - 1; x++) {
								double dy = (values[(y + 1) * width + x])
								    - (values[(y - 1) * width + x]);
								ablY[y * width + x] = dy;
						}
				}
				return (ablY);
		}

		/**
		 * Expand the image boundary. It's necessary to calculate the derivatives in
		 * the border pixels of the image.
		 * 
		 * @param A
		 *          gradient image of the given image
		 * @param w
		 *          width of the given image
		 * @param h
		 *          height of the given image
		 */
		protected double[] BoundExpand(double[] A, int w, int h) {
				int new_w = w + 2;
				int new_h = h + 2;
				double[] B = new double[new_w * new_h];
				// copy A into B
				for (int y = 0; y < h; y++) {
						for (int x = 0; x < w; x++) {
								B[(y + 1) * new_w + (x + 1)] = A[y * w + x];
						}
				}
				// mirroring 1th and last row
				for (int x = 1; x < new_w - 1; x++) {
						B[x] = B[2 * new_w + x];
						B[(new_h - 1) * new_w + x] = B[(new_h - 3) * new_w + x];
				}
				// mirroring 1th and last column
				for (int y = 0; y < new_h; y++) {
						B[y * new_w] = B[(y * new_w) + 2];
						B[y * new_w + (new_w - 1)] = B[(y * new_w + (new_w - 1)) - 2];
				}
				return (B);
		}

		/**
		 * Test the image boundary. It's necessary to calculate the derivatives in the
		 * border pixels of the image.
		 * 
		 * @param A
		 *          gradient image of the given image
		 * @param w
		 *          width of the given image
		 * @param h
		 *          height of the given image
		 */
		protected double[] BoundEnsure(double[] A, int w, int h) {
				double[] B = A;
				if (w < 3 || h < 3) {
						System.out
						    .println("\n error (in BoundEnsure): the number of rows ore columns is smaler than 3\n");
				} else {
						// mirroring 1th and last row
						for (int x = 1; x < w - 1; x++) {
								B[x] = B[2 * w + x];
								B[(h - 1) * w + x] = B[(h - 3) * w + x];
						}
						// mirroring 1th and last column
						for (int y = 0; y < h; y++) {
								B[y * w] = B[(y * w) + 2];
								B[y * w + (w - 1)] = B[(y * w + (w - 1)) - 2];
						}
				}
				return (B);
		}

		/**
		 * Shrink the image boundary. It's necessary to calculate the derivatives in
		 * the border pixels of the image.
		 * 
		 * @param A
		 *          gradient image of the given image
		 * @param w
		 *          width of the given image
		 * @param h
		 *          height of the given image
		 */
		protected double[] BoundShrink(double[] A, int w, int h) {
				int new_w = w - 2;
				int new_h = h - 2;
				double[] B = new double[new_w * new_h];
				// copy sub matrix of A into B
				for (int y = 1; y < h - 1; y++) {
						for (int x = 1; x < w - 1; x++) {
								B[(y - 1) * new_w + (x - 1)] = A[y * w + x];
						}
				}
				return (B);
		}
}
