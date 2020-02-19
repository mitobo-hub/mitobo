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

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Class to calculate a gradient vector field of a given image. 
 * <p>
 * Some methods to get derivatives on several operators are implemented like 
 * central and forward differences and the Sobel operator.
 * 
 * @author Birgit Möller, Danny Misiak
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.STANDARD)
public class GradientFieldCalculator2D extends MTBOperator {

		/**
		 * Provided calculation modes for calculation the gradient image.
		 * 
		 * @author misiak
		 */
		public static enum GradientMode {
				/**
				 * First order partial derivative in x- and y-direction using central
				 * differences.
				 */
				PARTIAL_DIFF,
				/**
				 * First order partial derivative in x- and y-direction using forward
				 * differences.
				 */
				PARTIAL_DIFF_FORWARD,
				/**
				 * First order partial derivatives approximated by the Sobel operator.
				 */
				SOBEL
		}

		/**
		 * Width of the image.
		 */
		private transient int width;
		/**
		 * Height of the image.
		 */
		private transient int height;

		/**
		 * The used gradient mode for field calculation.
		 */
		@Parameter(label = "Approximation Mode", required = true, 
				direction = Parameter.Direction.IN, dataIOOrder = -8,
				description = "Type of the gradient mode")
		private GradientMode gradientMode = null;
		/**
		 * The input image to get the gradient values and differences.
		 */
		@Parameter(label = "Input Image", required = true, 
				direction = Parameter.Direction.IN, dataIOOrder = -10,
				description = "Input image")
		private transient MTBImage inputImage = null;
		/**
		 * The resulting 2D vector field.
		 */
		@Parameter(label = "Vector Field", 
				direction = Parameter.Direction.OUT, 
				description = "Gradient field of the 2D image.")
		private transient MTBVectorField2D vectorField = null;

		/**
		 * The resulting 2D vector field stored to an image.
		 * <p>
		 * The image has to layers where the first one contains the values of the
		 * x-direction and the second one the values of the y-direction.
		 */
		@Parameter(label = "Vector Field Image", 
				direction = Parameter.Direction.OUT, 
				description = "Gradient field image.")
		private transient MTBImageDouble vectorFieldImage = null;

		/**
		 * Vector field flow in x-direction.
		 */
		protected double[] xFlow;
		/**
		 *Vector field flow in y-direction.
		 */
		protected double[] yFlow;

		/**
		 * Standard constructor. A new empty operator object is initialized.
		 */
		public GradientFieldCalculator2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor. A new operator object is initialized.
		 * 
		 * @param inImg
		 *          image to work on
		 * @param mode
		 *          gradient calculation mode
		 * @throws ALDOperatorException
		 */
		public GradientFieldCalculator2D(MTBImage inImg, GradientMode mode)
		    throws ALDOperatorException {
				this.inputImage = inImg;
				this.gradientMode = mode;
		}

		/**
		 * Get the underlying input image of the gradient field..
		 */
		public MTBImage getInputImage() {
				return this.inputImage;
		}

		/**
		 * Set the underlying input image of the gradient field.
		 */
		public void setInputImage(MTBImage inImg) {
				this.inputImage = inImg;
		}

		/**
		 * Get the used gradient mode for field calculation.
		 */
		public GradientMode getGradientMode() {
				return this.gradientMode;
		}

		/**
		 * Set the used gradient mode for field calculation.
		 */
		public void getGradientMode(GradientMode mode) {
				this.gradientMode = mode;
		}

		/**
		 * Get the resulting 2D vector field.
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
		 */
		@Override
		protected void operate() {

			this.width = this.inputImage.getSizeX();
			this.height = this.inputImage.getSizeY();

			this.xFlow = new double[this.width * this.height];
			this.yFlow = new double[this.width * this.height];

			// call methods to calculate the gradient, 
			// according to given gradient mode
			switch (this.gradientMode) {
			case PARTIAL_DIFF:
				getPartialDiffX();
				getPartialDiffY();
				break;
			case PARTIAL_DIFF_FORWARD:
				getPartialDiffX_forward();
				getPartialDiffY_forward();
				break;
			case SOBEL:
				getPartialDiffX_sobel();
				getPartialDiffY_sobel();
				break;
			}
			this.vectorField = new MTBVectorField2D(this.xFlow, this.yFlow, 
					this.width,this.height);
			// write data also into an image for visualization
			String mode = new String();
			switch (this.gradientMode)
			{
			case PARTIAL_DIFF:
				mode = "symmetric";
				break;
			case PARTIAL_DIFF_FORWARD:
				mode = "forward";
				break;
			case SOBEL:
				mode = "sobel";
				break;
			}
			this.vectorFieldImage = 
					(MTBImageDouble)MTBImage.createMTBImage(this.width, this.height, 
							1, 1, 2, MTBImageType.MTB_DOUBLE);
			this.vectorFieldImage.setTitle("Gradient Vector Field for <" 
				+ this.inputImage.getTitle() + "> - mode: " + mode);
			this.vectorFieldImage.setSliceLabel("component: dx", 0, 0, 0);
			this.vectorFieldImage.setSliceLabel("component: dy", 0, 0, 1);
			for (int y = 0; y<this.height; ++y) {
				for (int x = 0; x<this.width; ++x) {
					this.vectorFieldImage.putValueDouble(x, y, 0, 0, 0, 
							this.xFlow[y*this.width+x]);
					this.vectorFieldImage.putValueDouble(x, y, 0, 0, 1, 
							this.yFlow[y*this.width+x]);
				}
			}
		}

		/**
		 * Calculate first order partial derivative in x-direction using central
		 * differences.
		 */
		protected void getPartialDiffX() {
				for (int y = 0; y < this.height; y++) {
						for (int x = 1; x < this.width - 1; x++) {
								this.xFlow[y * this.width + x] = this.inputImage.getValueDouble(x + 1,
								    y)
								    - this.inputImage.getValueDouble(x - 1, y);
						}
				}
		}

		/**
		 * Calculate first order partial derivative in y-direction using central
		 * differences.
		 */
		protected void getPartialDiffY() {
				for (int y = 1; y < this.height - 1; y++) {
						for (int x = 0; x < this.width; x++) {
								this.yFlow[y * this.width + x] = this.inputImage.getValueDouble(x,
								    y + 1)
								    - this.inputImage.getValueDouble(x, y - 1);
						}
				}
		}

		/**
		 * Calculate first order partial derivative in x-direction using forward
		 * differences.
		 */
		protected void getPartialDiffX_forward() {
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width - 1; x++) {
								this.xFlow[y * this.width + x] = this.inputImage.getValueDouble(x + 1,
								    y)
								    - this.inputImage.getValueDouble(x, y);
						}
				}
		}

		/**
		 * Calculate first order partial derivative in y-direction using forward
		 * differences.
		 */
		protected void getPartialDiffY_forward() {
				for (int y = 0; y < this.height - 1; y++) {
						for (int x = 0; x < this.width; x++) {
								this.yFlow[y * this.width + x] = this.inputImage.getValueDouble(x,
								    y + 1)
								    - this.inputImage.getValueDouble(x, y);
						}
				}
		}
		
		/**
		 * Calculate first order partial derivative in x-direction using Sobel.
		 */
		protected void getPartialDiffX_sobel() {
			double diff_1, diff_2, diff_3;
			for (int y = 1; y < this.height-1; y++) {
				for (int x = 1; x < this.width - 1; x++) {
					diff_1 =  this.inputImage.getValueDouble(x + 1, y - 1)
		              - this.inputImage.getValueDouble(x - 1, y - 1);
					diff_2 =  2 * this.inputImage.getValueDouble(x + 1, y)
						      - 2 * this.inputImage.getValueDouble(x - 1, y);
					diff_3 =  this.inputImage.getValueDouble(x + 1, y + 1)
                  - this.inputImage.getValueDouble(x - 1, y + 1);
					this.xFlow[y * this.width + x] = diff_1 + diff_2 + diff_3;
				}
			}
		}

		/**
		 * Calculate first order partial derivative in y-direction using Sobel.
		 */
		protected void getPartialDiffY_sobel() {
			double diff_1, diff_2, diff_3;
			for (int y = 1; y < this.height - 1; y++) {
				for (int x = 1; x < this.width - 1; x++) {
					diff_1 =  this.inputImage.getValueDouble(x - 1, y + 1)
                  - this.inputImage.getValueDouble(x - 1, y - 1);
					diff_2 =  2 * this.inputImage.getValueDouble(x, y + 1)
	     			      - 2 * this.inputImage.getValueDouble(x, y - 1);
					diff_3 =  this.inputImage.getValueDouble(x + 1, y + 1)
                  - this.inputImage.getValueDouble(x + 1, y - 1);	
					this.yFlow[y * this.width + x] = diff_1 + diff_2 + diff_3;
				}
			}
		}
}
