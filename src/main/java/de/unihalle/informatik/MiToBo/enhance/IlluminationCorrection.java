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

package de.unihalle.informatik.MiToBo.enhance;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;

import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.math.images.ImageArithmetics;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology.maskShape;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology.opMode;

import de.unihalle.informatik.MiToBo.tools.image.ImageConverter;

/**
 * <pre>
 * 
 * This class corrects the uneven illumination (e.g. background gradient) of
 * especially gray value bright filed or DIC images.
 * The original image is transformed by a morphological closing
 * (or an other morphological transformation). The correction of the uneven
 * illumination is obtained by dividing the original image by the transformed
 * image. This approach works well for DIC images, maybe also for bright field
 * or other illumination/contrast based images. Mask size of the structuring
 * element should be large to remove the cells or objects but preserve the
 * (uneven) illumination function.
 * 
 * 
 * The approach is adapted from:
 * 
 * author = {Soille, Pierre},
 * title = {Morphological Image Analysis: Principles
 * and Applications},
 * year = {2010},
 * isbn = {9783642076961},
 * edition = {2},
 * pages = {124 -- 126},
 * publisher = {Springer Berlin Heidelberg}. 
 * 
 * 
 * Also other morphological operations can be used instead of the closing
 * followed with a division of the images. Approach maybe depends on the image
 * data.
 * 
 * Feel free to extend this class!
 * 
 * @author Danny Misiak
 * 
 * </pre>
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.STANDARD, allowBatchMode = true)
public class IlluminationCorrection extends MTBOperator {

		// --- input parameters ---

		@Parameter(label = "Input Image", required = true, direction = Parameter.Direction.IN, description = "Uneven illuminated input image.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImage inputImage = null;

		@Parameter(label = "Morphology", required = true, direction = Parameter.Direction.IN, description = "Morphological operator for correction.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private BasicMorphology morphOp = new BasicMorphology(21, opMode.CLOSE,
		    maskShape.SQUARE);

		@Parameter(label = "Output Image Type", required = true, direction = Parameter.Direction.IN, description = "Image type of illumination corrected output image.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private MTBImageType outputType = MTBImageType.MTB_BYTE;

		// --- supplemental parameters ---

		// --- output parameters ---

		@Parameter(label = "Output Image", required = true, direction = Parameter.Direction.OUT, description = "Illumination corrected output image.")
		private transient MTBImage outputImage = null;

		/**
		 * Standard constructor.
		 */
		public IlluminationCorrection() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new IlluminationCorrection object.
		 * 
		 * @param image
		 *          uneven illuminated input image
		 * @param operator
		 *          morphological operator for correction, e.g. closing
		 * @param type
		 *          MTBImageType of illumination corrected output image
		 * 
		 * @throws ALDOperatorException
		 */
		public IlluminationCorrection(MTBImage _inputImage, BasicMorphology _morphOp,
		    MTBImageType _outputType) throws ALDOperatorException {
				this.inputImage = _inputImage;
				this.morphOp = _morphOp;
				this.outputType = _outputType;
		}

		/**
		 * Get uneven illuminated input image.
		 */
		public MTBImage getInputImage() {
				return inputImage;
		}

		/**
		 * Set uneven illuminated input image.
		 */
		public void setInputImage(MTBImage _inputImage) {
				this.inputImage = _inputImage;
		}

		/**
		 * Get morphological operation for illumination correction, default is
		 * closing.
		 */
		public BasicMorphology getMorphOp() {
				return morphOp;
		}

		/**
		 * Set morphological operation for illumination correction, default is
		 * closing.
		 */
		public void setMorphOp(BasicMorphology _morphOp) {
				this.morphOp = _morphOp;
		}

		/**
		 * Get output image type.
		 */
		public MTBImageType getImageType() {
				return outputType;
		}

		/**
		 * Set output image type.
		 */
		public void setImageType(MTBImageType _outputType) {
				this.outputType = _outputType;
		}

		/**
		 * Get illumination corrected result image.
		 */
		public MTBImage getResultImage() {
				return outputImage;
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				// set input image for basic morphology operator
				morphOp.setInImg(inputImage);
				// run basic morphology
				morphOp.runOp();
				// get transformed image
				MTBImage transformedImage = morphOp.getResultImage();
				// divide original image by morphological transformed image
				MTBImage divImage = this.div(inputImage, transformedImage);
				// convert result image to user defined image type
				ImageConverter imageConv = null;
				try {
						imageConv = new ImageConverter(divImage, outputType, true, false);
						imageConv.runOp();
				} catch (ALDOperatorException e) {
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						e.printStackTrace();
				}
				// set illumination corrected result image
				outputImage = imageConv.getResultImg();
		}

		/**
		 * Method to divide two images. Method is copied and modified from
		 * {@link ImageArithmetics}.
		 */
		private MTBImage div(MTBImage img1, MTBImage img2) {
				int sizeStack = img1.getSizeStack();
				int sizeY = img1.getSizeY();
				int sizeX = img1.getSizeX();

				int idx1 = img1.getCurrentSliceIndex();
				int idx2 = img2.getCurrentSliceIndex();

				MTBImageType type = (img1.getType().ordinal() >= img2.getType().ordinal() ? img1
				    .getType()
				    : img2.getType());

				if (type == MTBImageType.MTB_BYTE || type == MTBImageType.MTB_SHORT
				    || type == MTBImageType.MTB_INT) {
						type = MTBImageType.MTB_DOUBLE;
				}

				MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(),
				    img1.getSizeT(), img1.getSizeC(), type);

				newImg.setTitle(img1.getTitle());
				newImg.setStepsizeX(img1.getStepsizeX());
				newImg.setStepsizeY(img1.getStepsizeY());
				newImg.setStepsizeZ(img1.getStepsizeZ());
				newImg.setStepsizeT(img1.getStepsizeT());
				newImg.setUnitX(img1.getUnitX());
				newImg.setUnitY(img1.getUnitY());
				newImg.setUnitZ(img1.getUnitZ());
				newImg.setUnitT(img1.getUnitT());

				for (int i = 0; i < sizeStack; i++) {
						img1.setCurrentSliceIndex(i);
						img2.setCurrentSliceIndex(i);
						newImg.setCurrentSliceIndex(i);

						// handle division by 0
						for (int y = 0; y < sizeY; y++) {
								for (int x = 0; x < sizeX; x++) {
										if (img2.getValueDouble(x, y) < MTBConstants.epsilon) {
												newImg.putValueDouble(x, y, new Double(0.0));
										} else {
												newImg.putValueDouble(x, y, img1.getValueDouble(x, y)
												    / img2.getValueDouble(x, y));
										}
								}
						}
				}

				// restore actual slice index
				img1.setCurrentSliceIndex(idx1);
				img2.setCurrentSliceIndex(idx2);
				newImg.setCurrentSliceIndex(0);

				return newImg;
		}
}
