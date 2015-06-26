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

package de.unihalle.informatik.MiToBo.fields;

import java.util.Iterator;
import java.util.LinkedList;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.color.conversion.HSVToRGBArrayConverter;

/**
 * Class implements several operations on a 2D vector field.
 * 
 * @author Danny Misiak
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.STANDARD, allowBatchMode = false)
public class FieldOperations2D extends MTBOperator {
		/**
		 * Several operation types on a 2D vector field.
		 * 
		 * @author misiak
		 */
		public static enum FieldOperation {
			/**
			 * Calculate the magnitude image of the vector field.
			 */
			MAGNITUDE_IMAGE,
			/**
			 * Calculate the normalized magnitude image of the vector field in a range
			 * [0,1].
			 */
			NORMEDMAG_IMAGE,
			/**
			 * Calculate the difference image in x-direction from the given vector
			 * field.
			 */
			DIFF_X_IMAGE,
			/**
			 * Calculate the difference image in y-direction from the given vector
			 * field.
			 */
			DIFF_Y_IMAGE,
			/**
			 * Calculate a RGB colored image of the vector field to visualize the field.
			 */
			COLOR_PLOT
		}

		/**
		 * U-component-vector of the vector field.
		 */
		private transient double[] U;

		/**
		 * V-component-vector of the vector field.
		 */
		private transient double[] V;

		/**
		 * Vector field size in x-direction.
		 */

		private int fieldSizeX = 0;

		/**
		 * Vector field size in y-direction.
		 */
		private int fieldSizeY = 0;

		/**
		 * The 2D vector field for several operations.
		 */
		@Parameter(label = "Vector Field", required = false, direction = Parameter.Direction.IN, description = "2D vector field input", dataIOOrder = 0)
		private transient MTBVectorField2D vectorField = null;

		/**
		 * The 2D vector field stored to an image.
		 * <p>
		 * The image has to layers where the first one contains the values of the
		 * x-direction and the second one the values of the y-direction.
		 */
		@Parameter(label = "Vector Field Image", required = false, direction = Parameter.Direction.IN, description = "Vector field image.", dataIOOrder = 1)
		private transient MTBImageDouble vectorFieldImage = null;

		/**
		 * The operation which should be applied to the input vector field.
		 */
		@Parameter(label = "Operation Type", required = true, direction = Parameter.Direction.IN, description = "Type of used field operation.", dataIOOrder = 2)
		private FieldOperation operationType = null;

		/**
		 * Several result formats. Only MTBImage's for the moment.
		 */
		@Parameter(label = "Result Image", required = false, direction = Parameter.Direction.OUT, description = "Result image.")
		private transient MTBImage resultImage = null;

		/**
		 * Local vector field variable actually used during operator run.
		 */
		private transient MTBVectorField2D workVectorField = null;
		
		/**
		 * Standard constructor.
		 */
		public FieldOperations2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new field operation operator.
		 * 
		 * @param inField
		 *          input 2D vector field
		 * @param inFieldImage
		 *          input 2D vector field image
		 * @param op
		 *          operator type
		 * @throws ALDOperatorException
		 */
		public FieldOperations2D(MTBVectorField2D inField,
		    MTBImageDouble inFieldImage, FieldOperation op)
		    throws ALDOperatorException {
				this.vectorField = inField;
				this.vectorFieldImage = inFieldImage;
				this.operationType = op;
		}

		/**
		 * Constructor to create a new field operation operator.
		 * 
		 * @param inField
		 *          input 2D vector field
		 * @param op
		 *          operator type
		 * @throws ALDOperatorException
		 */
		public FieldOperations2D(MTBVectorField2D inField, FieldOperation op)
		    throws ALDOperatorException {
				this.vectorField = inField;
				this.vectorFieldImage = null;
				this.operationType = op;
		}

		/**
		 * Get the operation based 2D vector field.
		 */
		public MTBVectorField2D getVectorField() {
				return this.vectorField;
		}

		/**
		 * Set the operation based 2D vector field.
		 */
		public void setVectorField(MTBVectorField2D inField) {
				this.vectorField = inField;
		}

		/**
		 * Get the operation based 2D vector field image.
		 */
		public MTBImageDouble getVectorFieldImage() {
				return this.vectorFieldImage;
		}

		/**
		 * Set the operation based 2D vector field image.
		 */

		public void setVectorFieldImage(MTBImageDouble inFieldImage) {
				this.vectorFieldImage = inFieldImage;
		}

		/**
		 * Get the operation type which should be applied to the input vector field.
		 * 
		 * @return Field operation type.
		 */
		public FieldOperation getOperationType() {
				return this.operationType;
		}

		public void setOperationType(FieldOperation type) {
				this.operationType = type;
		}

		/**
		 * Get the field size in x-direction.
		 */
		public int getFieldSizeX() {
				return this.fieldSizeX;
		}

		/**
		 * Set the field size in x-direction.
		 */
		public void setFieldSizeX(int sizeX) {
				this.fieldSizeX = sizeX;
		}

		/**
		 * Get the field size in y-direction.
		 */
		public int getFieldSizeY() {
				return this.fieldSizeY;
		}

		/**
		 * Set the field size in y-direction.
		 */
		public void setFieldSizeY(int sizeY) {
				this.fieldSizeY = sizeY;
		}

		/**
		 * Get result image of the operation on the vector field.
		 */
		public MTBImage getResultImage() {
				return this.resultImage;
		}

		/**
		 * This method does the actual work.
		 * @throws ALDProcessingDAGException 
		 */
		@Override
		protected void operate() 
				throws ALDOperatorException, ALDProcessingDAGException {

				this.workVectorField = this.vectorField;
				if (this.vectorField == null && this.vectorFieldImage != null) {
						MTBImage tmpImageU = this.vectorFieldImage.getImagePart(0, 0, 0, 0, 0,
						    this.vectorFieldImage.getSizeX(), this.vectorFieldImage.getSizeY(),
						    1, 1, 1);
						MTBImage tmpImageV = this.vectorFieldImage.getImagePart(0, 0, 0, 0, 1,
						    this.vectorFieldImage.getSizeX(), this.vectorFieldImage.getSizeY(),
						    1, 1, 1);

						double[] u = new double[this.vectorFieldImage.getSizeX()
						    * this.vectorFieldImage.getSizeY()];
						double[] v = new double[this.vectorFieldImage.getSizeX()
						    * this.vectorFieldImage.getSizeY()];

						for (int y = 0; y < this.vectorFieldImage.getSizeY(); y++) {
								for (int x = 0; x < this.vectorFieldImage.getSizeX(); x++) {
										u[y * this.vectorFieldImage.getSizeX() + x] = tmpImageU
										    .getValueDouble(x, y);
										v[y * this.vectorFieldImage.getSizeX() + x] = tmpImageV
										    .getValueDouble(x, y);
								}
						}
						this.workVectorField = new MTBVectorField2D(u, v,
					    this.vectorFieldImage.getSizeX(), 
					    this.vectorFieldImage.getSizeY());
				}

				this.fieldSizeX = this.workVectorField.getFieldSizeX();
				this.fieldSizeY = this.workVectorField.getFieldSizeY();
				this.U = this.workVectorField.getU();
				this.V = this.workVectorField.getV();
				// call several operation methods on the vector field
				switch (this.operationType) {
				case MAGNITUDE_IMAGE:
						this.resultImage = getMagImage();
						break;
				case NORMEDMAG_IMAGE:
						this.resultImage = getNormedMagImage();
						break;
				case DIFF_X_IMAGE:
						this.resultImage = getDiffX_image();
						break;
				case DIFF_Y_IMAGE:
						this.resultImage = getDiffY_image();
						break;
				case COLOR_PLOT:
						this.resultImage = colorPlot();
						break;
				}
				this.resultImage.setTitle("FieldOperations-Result");
		}

		/**
		 * Calculate the gradient magnitude image from the vectors U and V.
		 * 
		 * @return Gradient magnitude image.
		 */
		protected MTBImage getMagImage() {
				MTBImage image = MTBImage.createMTBImage(this.fieldSizeX, this.fieldSizeY, 1, 1,
				    1, MTBImageType.MTB_DOUBLE);
				for (int y = 0; y < this.fieldSizeY; y++) {
						for (int x = 0; x < this.fieldSizeX; x++) {
								image.putValueDouble(
								    x,
								    y,
								    (Math.sqrt(Math.pow(this.U[y * this.fieldSizeX + x], 2)
								        + Math.pow(this.V[y * this.fieldSizeX + x], 2))));
						}
				}
				return image;
		}

		/**
		 * Normalize the gradient magnitude values into a range of [0,1].
		 * 
		 * @return MTBimage with normalized gradient magnitude values.
		 */
		protected MTBImage getNormedMagImage() {
				MTBImage img = getMagImage();
				double max = Double.MIN_VALUE;
				double min = Double.MAX_VALUE;
				MTBImage image = MTBImage.createMTBImage(this.fieldSizeX, this.fieldSizeY,
				    1, 1, 1, MTBImageType.MTB_DOUBLE);
				for (int y = 1; y < this.fieldSizeY - 1; y++) {
						for (int x = 1; x < this.fieldSizeX - 1; x++) {
								if (img.getValueDouble(x, y) > max)
										max = img.getValueDouble(x, y);
								if (img.getValueDouble(x, y) < min)
										min = img.getValueDouble(x, y);
						}
				}
				for (int y = 1; y < this.fieldSizeY - 1; y++) {
						for (int x = 1; x < this.fieldSizeX - 1; x++) {
								image.putValueDouble(x, y, (img.getValueDouble(x, y) - min)
								    / (max - min));
						}
				}
				return (image);
		}

		/**
		 * Calculate the image from the first order partial derivative in x-direction
		 * from the vectors U.
		 * 
		 * @return Gradient image in x-direction.
		 */
		protected MTBImage getDiffX_image() {
				MTBImage image = MTBImage.createMTBImage(this.fieldSizeX, this.fieldSizeY, 1, 1, 1,
				    MTBImageType.MTB_DOUBLE);
				for (int y = 0; y < this.fieldSizeY; y++) {
						for (int x = 0; x < this.fieldSizeX; x++) {
								image.putValueDouble(x, y, this.U[y * this.fieldSizeX + x]);
						}
				}
				return image;
		}

		/**
		 * Calculate the image from the first order partial derivative in y-direction
		 * from the vectors V.
		 * 
		 * @return Gradient image in y-direction.
		 */
		protected MTBImage getDiffY_image() {
				MTBImage image = MTBImage.createMTBImage(this.fieldSizeX, this.fieldSizeY, 1, 1, 1,
				    MTBImageType.MTB_DOUBLE);
				for (int y = 0; y < this.fieldSizeY; y++) {
						for (int x = 0; x < this.fieldSizeX; x++) {
								image.putValueDouble(x, y, this.V[y * this.fieldSizeX + x]);
						}
				}
				return image;
		}


		// ------------------------------------------------------
		// ------- Plot the Field in Arrow or Color Style -------
		// ------------------------------------------------------

		/**
		 * <pre>
		 * Plots the field in colored image. The colors derived from the field
		 *    vectors
		 * magnitude (length of the vector) and their direction (degree from x-axis
		 *    to
		 * y-axis). HSV color spaced is used to represent direction and magnitude.
		 * To view the field, HSV color space is converted to RGBcolor space.
		 * 
		 * H: (the used color) is derived from the angle of the vector.
		 * S: (saturation) of color H is derived from the vector magnitude.
		 * V: (brightness value) is set to 1.0 (100%), to get colors only from
		 * 0% saturation of color H (white) to 100% saturation of color H.
		 * 
		 * Ranges of HSV:
		 * H in [0, 360) (representing degrees),
		 * S and I in [0, 1] (representing [0, 100 %]).
		 * </pre>
		 * 
		 * @return RGB image, visualizing the vector field.
		 * @throws ALDOperatorException 
		 * @throws ALDProcessingDAGException 
		 */
		protected MTBImageRGB colorPlot() 
				throws ALDOperatorException, ALDProcessingDAGException {
			// disable history logging temporarily
			int oldHistoryConstructionMode = ALDOperator.getConstructionMode();
			ALDOperator.setConstructionMode(1);

				// image with normalized magnitude values
				MTBImage magValues = getNormedMagImage();
				// vector field plot image
				MTBImageRGB plot = (MTBImageRGB) MTBImage.createMTBImage(
				    this.fieldSizeX, this.fieldSizeY, 1, 1, 1, MTBImageType.MTB_RGB);
				plot.setTitle("Vector Field Plot");
				double[] h = new double[this.fieldSizeX*this.fieldSizeY];
				double[] s = new double[this.fieldSizeX*this.fieldSizeY];
				double[] v = new double[this.fieldSizeX*this.fieldSizeY];
				int c = 0;
				for (int y = 0; y < this.fieldSizeY; y++) {
						for (int x = 0; x < this.fieldSizeX; x++) {
								// set HSV values for current field position (x,y)
								h[c] = Math.toDegrees(this.workVectorField.getDirection(x, y));
								s[c] = magValues.getValueDouble(x, y);
								v[c] = 1.0;
								++c;
						}
				}
				HSVToRGBArrayConverter hsv = new HSVToRGBArrayConverter(h, s, v);
				hsv.runOp(HidingMode.HIDDEN);
				LinkedList<int[]> rgbValues = hsv.getRGBResult();
				c = 0; int[] rgb;
				Iterator<int[]> rgbIt = rgbValues.iterator();
				for (int y = 0; y < this.fieldSizeY; y++) {
						for (int x = 0; x < this.fieldSizeX; x++) {
							// set RGB value for pixel at current position (x,y)
							rgb = rgbIt.next();
							plot.putValueInt(x, y, ((rgb[0] & 0xff) << 16) 
									+ ((rgb[1] & 0xff) << 8) + (rgb[2] & 0xff));
							++c;
						}
				}
				// ensable history logging again
				ALDOperator.setConstructionMode(oldHistoryConstructionMode);
				return plot;
		}
}
