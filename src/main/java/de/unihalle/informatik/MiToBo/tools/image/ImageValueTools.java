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

package de.unihalle.informatik.MiToBo.tools.image;

import java.awt.Color;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * An operator class for different simple operations on the values of an image
 * like inversion, filling with a constant value, etc.
 * <p>
 * The policy here is to use static functions instead of constructing a new
 * object, set its parameters and then call <code>runOp()</code>. Thus the
 * source code is reduced as the functions of this class are assumed to be
 * called quite often.
 * <p>
 * This class is meant to implement convenience functions on images to keep the
 * image classes from bloating. Feel free to extend this class with your
 * convenience functions. Please implement static methods for any functionality
 * to keep the policy of this class. Be aware that the input image is changed,
 * i.e. the input image is also the result image. (You don't need to get the
 * result image, if you still have a reference to the input image, but this
 * method is implemented for completeness)
 * <p>
 * 
 * @author Oliver Gress
 * 
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.NONE)
public class ImageValueTools extends MTBOperator {

		/**
		 * Available image value modification methods.
		 * 
		 * @author Oliver Gress
		 * 
		 */
		public enum ImageValueModification {
				FILL, INVERT, NORM_TO, NORM_FROM_TO;
		}

		@Parameter(label = "inputImage", required = true, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Input image")
		private MTBImage inputImage = null;

		@Parameter(label = "resultImage", required = true, direction = Parameter.Direction.OUT, 
				 mode=ExpertMode.STANDARD, dataIOOrder=1, description = "Result image")
		private MTBImage resultImage = null;

		@Parameter(label = "modificationMode", required = true, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=2, description = "Image value modification mode")
		private ImageValueModification modificationMode = null;

		@Parameter(label = "rgbValue", required = false, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=3, description = "RGB color value to fill the image")
		private Color rgbValue = null;

		@Parameter(label = "grayValue", required = false, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=4, description = "Floating point value to fill the image")
		private Double grayValue = null;

		@Parameter(label = "lowerBound", required = false, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=5, description = "Floating point value for lower normalization bound")
		private double lowerBound = 0.0;

		@Parameter(label = "upperBound", required = false, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=6, description = "Floating point value for upper normalization bound")
		private double upperBound = 0.0;

		@Parameter(label = "fromMinVal", required = false, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=7, description = "Floating point reference minimum image value")
		private double fromMinVal = 0.0;

		@Parameter(label = "fromMaxVal", required = false, direction = Parameter.Direction.IN, 
				 mode=ExpertMode.STANDARD, dataIOOrder=8, description = "Floating point reference maximum image value")
		private double fromMaxVal = 0.0;

		@Override
		public void validateCustom() throws ALDOperatorException {

				if (this.modificationMode == ImageValueModification.FILL) {
						if (!((this.grayValue != null) ^ (this.rgbValue != null))) {
								throw new ALDOperatorException(
								    OperatorExceptionType.VALIDATION_FAILED,
								    "ImageValueTools.validateCustom(): For image filling, "
								        + "whether a gray or an RGB value has to be specified (exclusively).");
						}
				}

				else if (this.modificationMode == ImageValueModification.NORM_TO) {
						if (this.lowerBound >= this.upperBound) {
								throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
								    "ImageValueTools.validateCustom(): For image normalization, "
								        + "lower bound must be below the upper bound value.");
						}
				}
		}

		public ImageValueTools() throws ALDOperatorException {
				super();
		}

		/**
		 * Constructor. Use set-functions to set the parameters.
		 * 
		 * @param img
		 * @throws ALDOperatorException
		 */
		protected ImageValueTools(MTBImage img) throws ALDOperatorException {
				this.inputImage = img;
				// this.setInvert();
		}

		// /**
		// * Constructor for filling an image with a gray value
		// * @param img
		// * @param grayvalue
		// * @throws MTBOperatorException
		// */
		// public ImageValueTools(MTBImage img, double grayvalue) throws
		// MTBOperatorException {
		// this.setInputImage(img);
		// this.setFill(grayvalue);
		// }
		//	
		// /**
		// * Constructor for filling an image with a color (if the input image is RGB,
		// otherwise the RGB value is averaged to a gray value)
		// * @param img
		// * @param grayvalue
		// * @throws MTBOperatorException
		// */
		// public ImageValueTools(MTBImage img, Color rgbvalue) throws
		// MTBOperatorException {
		// this.setInputImage(img);
		// this.setFill(rgbvalue);
		// }

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				if (this.modificationMode == ImageValueModification.INVERT) {
						this.invert(this.inputImage);
						this.resultImage = this.inputImage;
				} else if (this.modificationMode == ImageValueModification.FILL) {
						if (this.grayValue != null) {
								this.fill(this.inputImage, this.grayValue);
								this.resultImage = this.inputImage;
						} else if (this.rgbValue != null) {
								this.fill(this.inputImage, this.rgbValue);
								this.resultImage = this.inputImage;
						} else
								throw new ALDOperatorException(
								    OperatorExceptionType.OPERATE_FAILED,
								    "ImageValueTools.operate(): For image filling, "
								        + "whether a gray or an RGB value has to be specified (exclusively). This exception should never occur, because validateCustom()"
								        + " should account for this error.");
				} else if (this.modificationMode == ImageValueModification.NORM_TO) {
						this.normTo(this.inputImage, this.lowerBound, this.upperBound);
						this.resultImage = this.inputImage;
				} else if (this.modificationMode == ImageValueModification.NORM_FROM_TO) {
						this.normFromTo(this.inputImage);
						this.resultImage = this.inputImage;
				} else {
						throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
						    "ImageValueTools.operate(): Unknown image value modification method "
						        + this.modificationMode.toString() + ".");
				}

		}

		// -------- static methods for easy use of this class (one-line code) ------

		/**
		 * Invert the specified image. If the input image is not of type MTB_BYTE or
		 * MTB_RGB, the minimum and maximum image value are used to compute the
		 * inverted values to keep the values in the same range.
		 */
		public static void invertImage(MTBImage img, MTBOperator callingOperator)
		    throws ALDOperatorException, ALDProcessingDAGException {
				ImageValueTools ivt = new ImageValueTools(img);
				ivt.setInvert();
				ivt.runOp(null);
		}

		/**
		 * Fill the specified image with a gray value.
		 * 
		 * @param img
		 * @param grayvalue
		 * @param callingOperator
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public static void fillImage(MTBImage img, double grayvalue,
		    MTBOperator callingOperator) throws ALDOperatorException,
		    ALDProcessingDAGException {
				ImageValueTools ivt = new ImageValueTools(img);
				ivt.setFill(grayvalue);
				ivt.runOp(null);
		}

		/**
		 * Fill the image with a RGB color value. If the input image is a gray value
		 * image, the RGB value is averaged for a gray value.
		 * 
		 * @param img
		 * @param rgbvalue
		 * @param callingOperator
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public static void fillImage(MTBImage img, Color rgbvalue,
		    MTBOperator callingOperator) throws ALDOperatorException,
		    ALDProcessingDAGException {
				ImageValueTools ivt = new ImageValueTools(img);
				ivt.setFill(rgbvalue);
				ivt.runOp(null);
		}

		/**
		 * Normalize the image values to range [lowerBound, upperBound]. This method
		 * is useful mostly for gray value images.
		 */
		public static void normImageTo(MTBImage img, double lowerBound,
		    double upperBound, MTBOperator callingOperator)
		    throws ALDOperatorException, ALDProcessingDAGException {
				ImageValueTools ivt = new ImageValueTools(img);
				ivt.setNormTo(lowerBound, upperBound);
				ivt.runOp(null);
		}

		/**
		 * Normalize the image values to range [lowerBound, upperBound]. This method
		 * is useful mostly for gray value images.
		 * 
		 * @param img
		 * @param lowerBound
		 * @param upperBound
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public static void normImageFromTo(MTBImage img, double oldMinVal,
		    double oldMaxVal, double newMinVal, double newMaxVal,
		    MTBOperator callingOperator) throws ALDOperatorException,
		    ALDProcessingDAGException {

				ImageValueTools ivt = new ImageValueTools(img);
				ivt.setNormFromTo(oldMinVal, oldMaxVal, newMinVal, newMaxVal);
				ivt.runOp(null);
		}

		// -------- getter/setter methods --------------

		public MTBImage getInputImage() {
				return this.inputImage;
		}

		public void setInputImage(MTBImage _inputImage) {
				this.inputImage = _inputImage;
		}

		public MTBImage getResultImage() {
				return this.resultImage;
		}

		public void setInvert() {
				this.modificationMode = ImageValueModification.INVERT;
		}

		public void setFill(Color _rgbValue) throws ALDOperatorException {
				this.modificationMode = ImageValueModification.FILL;
				this.rgbValue = _rgbValue;
				this.grayValue = null;
		}

		public void setFill(double _grayValue) throws ALDOperatorException {
				this.modificationMode = ImageValueModification.FILL;
				this.grayValue = _grayValue;
				this.rgbValue = null;
		}

		public void setNormTo(double _lowerBound, double _upperBound)
		    throws ALDOperatorException {
				this.modificationMode = ImageValueModification.NORM_TO;
				this.lowerBound = _lowerBound;
				this.upperBound = _upperBound;
		}

		public void setNormFromTo(double minVal, double maxVal, double minVal_new,
		    double maxVal_new) {
				this.modificationMode = ImageValueModification.NORM_FROM_TO;
				this.fromMinVal = minVal;
				this.fromMaxVal = maxVal;
				this.lowerBound = minVal_new;
				this.upperBound = maxVal_new;
		}

		public void setModification(ImageValueModification mod) {
				this.modificationMode = mod;
		}

		protected ImageValueModification getModification() {
				return this.modificationMode;
		}

		protected Double getGrayValue() {
				return this.grayValue;
		}

		protected Color getRGBValue() {
				return this.rgbValue;
		}

		/**
		 * Get lower bound of the normalization to range [lowerBound, upperBound].
		 * 
		 * @return Lower bound.
		 * @throws ALDOperatorException
		 */
		protected double getLowerBound() {
				return this.lowerBound;
		}

		/**
		 * Get upper bound of the normalization to range [lowerBound, upperBound].
		 * 
		 * @return Upper bound.
		 * @throws ALDOperatorException
		 */
		protected double getUpperBound() {
				return this.upperBound;
		}

		// -------- image modification methods --------------

		protected void invert(MTBImage img) {
				int sizeX = img.getSizeX();
				int sizeY = img.getSizeY();
				int sizeStack = img.getSizeStack();

				int sliceIdx = img.getCurrentSliceIndex();

				if (img.getType() == MTBImageType.MTB_RGB) {
						MTBImageRGB rgbImg = (MTBImageRGB) img;

						for (int i = 0; i < sizeStack; i++) {
								rgbImg.setCurrentSliceIndex(i);

								for (int y = 0; y < sizeY; y++)
										for (int x = 0; x < sizeX; x++)
												rgbImg.putValue(x, y, 255 - rgbImg.getValueR(x, y), 255 - rgbImg
												    .getValueG(x, y), 255 - rgbImg.getValueB(x, y));
						}
				} else {

						double[] minmax = img.getMinMaxDouble();

						if (img.getType() == MTBImageType.MTB_BYTE) {
								minmax[0] = 0;
								minmax[1] = 255;
						}

						for (int i = 0; i < sizeStack; i++) {
								img.setCurrentSliceIndex(i);

								for (int y = 0; y < sizeY; y++)
										for (int x = 0; x < sizeX; x++)
												img.putValueDouble(x, y, minmax[1] - img.getValueDouble(x, y)
												    + minmax[0]);
						}

				}

				img.setCurrentSliceIndex(sliceIdx);
		}

		protected void fill(MTBImage img, double grayvalue) {
				int sizeX = img.getSizeX();
				int sizeY = img.getSizeY();
				int sizeStack = img.getSizeStack();

				int sliceIdx = img.getCurrentSliceIndex();

				if (img.getType() == MTBImageType.MTB_RGB) {
						MTBImageRGB rgbImg = (MTBImageRGB) img;

						for (int i = 0; i < sizeStack; i++) {
								rgbImg.setCurrentSliceIndex(i);

								for (int y = 0; y < sizeY; y++)
										for (int x = 0; x < sizeX; x++)
												rgbImg.putValue(x, y, (int) grayvalue, (int) grayvalue,
												    (int) grayvalue);
						}
				} else {

						for (int i = 0; i < sizeStack; i++) {
								img.setCurrentSliceIndex(i);

								for (int y = 0; y < sizeY; y++)
										for (int x = 0; x < sizeX; x++)
												img.putValueDouble(x, y, grayvalue);
						}

				}

				img.setCurrentSliceIndex(sliceIdx);
		}

		protected void fill(MTBImage img, Color rgbvalue) {
				int sizeX = img.getSizeX();
				int sizeY = img.getSizeY();
				int sizeStack = img.getSizeStack();

				int sliceIdx = img.getCurrentSliceIndex();

				if (img.getType() == MTBImageType.MTB_RGB) {
						MTBImageRGB rgbImg = (MTBImageRGB) img;

						for (int i = 0; i < sizeStack; i++) {
								rgbImg.setCurrentSliceIndex(i);

								for (int y = 0; y < sizeY; y++)
										for (int x = 0; x < sizeX; x++)
												rgbImg.putValue(x, y, rgbvalue.getRed(), rgbvalue.getGreen(),
												    rgbvalue.getBlue());
						}
				} else {

						for (int i = 0; i < sizeStack; i++) {
								img.setCurrentSliceIndex(i);

								for (int y = 0; y < sizeY; y++)
										for (int x = 0; x < sizeX; x++)
												img
												    .putValueDouble(x, y,
												        (rgbvalue.getRed() + rgbvalue.getGreen() + rgbvalue
												            .getBlue()) / 3.0);
						}

				}

				img.setCurrentSliceIndex(sliceIdx);
		}

		protected void normTo(MTBImage img, double lowerBound, double upperBound) {
				double maxB = upperBound;
				double minB = lowerBound;
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				int width = img.getSizeX();
				int height = img.getSizeY();

				int sliceIdx = img.getCurrentSliceIndex();

				// get minimum and maximum value
				for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
								if (img.getValueDouble(x, y) < min)
										min = img.getValueDouble(x, y);
								if (img.getValueDouble(x, y) > max)
										max = img.getValueDouble(x, y);
						}
				}
				// System.out.println(minB + ", " + maxB + ", " + min + ", " + max);
				// normalize the values in the energy
				for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
								img.putValueDouble(x, y, (img.getValueDouble(x, y) - min)
								    * ((maxB - minB) / (max - min)) + minB);
						}
				}
				img.setCurrentSliceIndex(sliceIdx);
		}

		protected void normFromTo(MTBImage img) {
				double oldMin = this.fromMinVal;
				double oldMax = this.fromMaxVal;

				double newMin = this.lowerBound;
				double newMax = this.upperBound;

				double min = oldMin;
				double max = oldMax;
				int width = img.getSizeX();
				int height = img.getSizeY();

				int sliceIdx = img.getCurrentSliceIndex();

				// normalize the values
				for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
								img.putValueDouble(x, y, (img.getValueDouble(x, y) - min)
								    * ((newMax - newMin) / (max - min)) + newMin);
						}
				}
				img.setCurrentSliceIndex(sliceIdx);
		}
}
