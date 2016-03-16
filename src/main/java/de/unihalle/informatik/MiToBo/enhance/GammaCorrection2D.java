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

package de.unihalle.informatik.MiToBo.enhance;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Gamma correction on a 8- oder 16-bit image for one- or multi-channel images.
 * The gamma value can be set or automatically be computed.
 * 
 * 
 * @author Danny Misiak
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION)
@ALDMetaInfo(export = ExportPolicy.MANDATORY)
public class GammaCorrection2D extends MTBOperator {

		@Parameter(label = "Input Image", required = true, direction = Parameter.Direction.IN, description = "Input image", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImage inputImage = null;

		@Parameter(label = "Gamma Value", required = false, direction = Parameter.Direction.INOUT, description = "Gamma value.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private Double gamma = 0.0;

		@Parameter(label = "Image Channel", required = true, direction = Parameter.Direction.IN, description = "Channel of image.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private Integer channel = 1;

		@Parameter(label = "Auto Gamma", required = false, direction = Parameter.Direction.IN, description = "Automatic gamma value computation.", mode = ExpertMode.STANDARD, callback = "useAutoGamma", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE, dataIOOrder = 3)
		private Boolean auto = false;

		@Parameter(label = "Result Image", direction = Parameter.Direction.OUT, description = "Gamma corrected image.")
		private transient MTBImage resultImage = null;

		/**
		 * Standard constructor.
		 * 
		 * @throws ALDOperatorException
		 */
		public GammaCorrection2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new gamma correction 2D operator.
		 * 
		 * @param inimg
		 *          2D input image for gamma correction
		 * @param _gamma
		 *          gamma value to apply gamma correction
		 * @param _channel
		 *          image channel to apply gamma correction, starts with 1
		 * @throws ALDOperatorException
		 */
		public GammaCorrection2D(MTBImage inimg, Double _gamma, Integer _channel)
		    throws ALDOperatorException {
				this.inputImage = inimg;
				this.gamma = _gamma;
				this.auto = false;
				this.channel = _channel;
		}

		/**
		 * No gamma value is set. The gamma value is computed automatically.
		 * <p>
		 * gamma = log(r/maxInt)/ log(mean/maxInt)
		 * <p>
		 * maxInt is the maximum pixel intensity for all pixels in the image, for
		 * example 65535 for a 16-Bit pixel image
		 * <p>
		 * mean is the mean value over all image pixels
		 * <p>
		 * r is the half value of the pixel intensity range (32768 for 16-bit image)
		 * 
		 * 
		 * @param inimg
		 *          2D input image for gamma correction
		 * @param _channel
		 *          image channel to apply gamma correction
		 * @throws ALDOperatorException
		 */

		public GammaCorrection2D(MTBImage inimg, Integer _channel)
		    throws ALDOperatorException {
				this.inputImage = inimg;
				this.gamma = new Double(0.0);
				this.auto = true;
				this.channel = _channel;
		}

		@Override
		public void validateCustom() throws ALDOperatorException {

				// set valid = false if image type is different from MTB_BYTE and MTB_SHORT
				boolean valid = (this.getInputImage().getType().ordinal() <= 1);

				if (!valid)
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "GammaCorrection2D.validateCustom(): "
						        + "Input image must be of type MTB_BYTE or MTB_SHORT.");

				// test if nuclei channel number is in channel range of the image
				if (this.channel.intValue() < 1
				    || this.channel.intValue() > this.inputImage.getSizeC()) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> GammaCorrection2D: validation failed!"
						        + "\nImage channel number must be in range [1, #ImageChannels].");
				}

		}

		/**
		 * Callback routine to change parameters.
		 */
		@SuppressWarnings("unused")
		private void useAutoGamma() {
				try {
						if (this.auto.booleanValue() == true) {
								if (this.hasParameter("gamma")) {
										this.removeParameter("gamma");
								}
						} else {
								if (!this.hasParameter("gamma")) {
										this.addParameter("gamma");
								}
						}
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
		}

		/**
		 * Get the input image.
		 */
		public MTBImage getInputImage() {
				return this.inputImage;
		}

		/**
		 * Set the input image.
		 */
		public void setInputImage(MTBImage inImg) {
				this.inputImage = inImg;
		}

		/**
		 * Get the gamma value.
		 */
		public Double getGamma() {
				return this.gamma;
		}

		/**
		 * Set the gamma value.
		 */
		public void setGamma(Double _gamma) {
				this.gamma = _gamma;
		}

		/**
		 * Get the image channel.
		 */
		public Integer getChannel() {
				return this.channel;
		}

		/**
		 * Set the image channel.
		 */
		public void setChannel(Integer _channel) {
				this.channel = _channel;
		}

		/**
		 * Get the gamma corrected image.
		 */
		public MTBImage getResultImage() {
				return this.resultImage;
		}

		/**
		 * Set if gamma should be calculated automatically or not.
		 */
		public void setGammaToAuto(boolean a) {
				this.auto = a;
		}

		/**
		 * Get if gamma should be calculated automatically or not.
		 */
		public boolean getGammaToAuto() {
				return this.auto;
		}

		/**
		 * This method does the actual work.
		 * 
		 * @throws ALDProcessingDAGException
		 */
		@Override
		protected void operate() throws ALDOperatorException {
				if (auto) {
						this.resultImage = this.correctGammaAuto(this.inputImage,
						    this.channel.intValue());
				} else {
						this.resultImage = this.correctGamma(this.inputImage,
						    this.gamma.doubleValue(), this.channel.intValue());
				}
				this.resultImage.setTitle("GammaCorrection-Result");
		}

		/**
		 * Perform gamma correction to the specified channel of the input image, using
		 * the given gamma value.
		 * 
		 * @return Gamma corrected image.
		 */
		private MTBImage correctGamma(MTBImage inputImg, double gamma, int channel) {
				int sizeX = inputImg.getSizeX();
				int sizeY = inputImg.getSizeY();
				MTBImageType type = inputImg.getType();
				MTBImage outputImg = inputImg.duplicate();

				double w_max;
				int[] lut;

				if (type == MTBImageType.MTB_BYTE) // 8 Bit Image
				{
						w_max = 255;
						lut = new int[256];
				} else // 16 Bit Image
				{
						w_max = 65535;
						lut = new int[65536];
				}

				for (int i = 0; i <= w_max; i++) {
						lut[i] = (int) (w_max * Math.pow(((double) i) / w_max, gamma));
				}

				for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
								int value = lut[inputImg.getValueInt(x, y, 0, 0, (channel - 1))];
								outputImg.putValueInt(x, y, 0, 0, (channel - 1), value);
						}
				}

				return outputImg;
		}

		/**
		 * Perform gamma correction to the specified channel of the input image. The
		 * gamma value is calculated automatically.
		 * 
		 * @return Gamma corrected image.
		 */
		private MTBImage correctGammaAuto(MTBImage inputImg, int channel) {
				int sizeX = inputImg.getSizeX();
				int sizeY = inputImg.getSizeY();
				MTBImageType type = inputImg.getType();
				MTBImage outputImg = inputImg.duplicate();

				double w_max;
				int[] lut;

				if (type == MTBImageType.MTB_BYTE) // 8 Bit Image
				{
						w_max = 255;
						lut = new int[256];
				} else // 16 Bit Image
				{
						w_max = 65535;
						lut = new int[65536];
				}

				double mean = 0.0;
				for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
								mean += inputImg.getValueDouble(x, y, 0, 0, (channel - 1));
						}
				}
				mean = mean / (sizeX * sizeY);
				this.gamma = new Double(Math.log((w_max / 2) / (w_max))
				    / Math.log(mean / (w_max)));

				for (int i = 0; i <= w_max; i++) {
						lut[i] = (int) (w_max * Math.pow(((double) i) / w_max, this.gamma));
				}

				for (int y = 0; y < sizeY; y++) {
						for (int x = 0; x < sizeX; x++) {
								int value = lut[inputImg.getValueInt(x, y, 0, 0, (channel - 1))];
								outputImg.putValueInt(x, y, 0, 0, (channel - 1), value);
						}
				}
				return outputImg;
		}
}
