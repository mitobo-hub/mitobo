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

import de.unihalle.informatik.MiToBo.core.datatypes.MTBStructuringElement;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology.opMode;

/**
 * <pre>
 * 
 * This class enhances the contrast by top-hat operations, especially for gray
 * value bright filed or DIC images. A white top-hat is added to the original
 * image (enhance bright objects) and subsequent a black top-hat is subtracted
 * (enhance dark objects).
 * This approach works well for DIC images, maybe also for bright field or other
 * illumination/contrast based images. Mask size of the structuring element
 * should be small to preserve small structures, like neurites.
 * 
 * NOTE: maybe the result image must be re-scaled, since output gray values can
 *       fall outside the dynamic range of the input image!
 * 
 * Feel free to extend this class!
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
 * pages = {126 -- 127},
 * publisher = {Springer Berlin Heidelberg}. 
 * 
 * 
 * @author Danny Misiak
 * 
 * </pre>
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.STANDARD, allowBatchMode = true,
	shortDescription="Enhances the contrast by top-hat operations.")
public class TopHatContrastEnhancement extends MTBOperator {

		// --- input parameters ---

		@Parameter(label = "Input Image", required = true, direction = Parameter.Direction.IN, description = "Low contrast input image.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImage inputImage = null;

		@Parameter(label = "WTH Mask Size", required = true, direction = Parameter.Direction.IN, description = "Mask size of white top-hat.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private Integer wthMaskSize = new Integer(5);

		@Parameter(label = "BTH Mask Size", required = true, direction = Parameter.Direction.IN, description = "Mask size of black top-hat.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private Integer bthMaskSize = new Integer(5);

		// --- supplemental parameters ---

		// --- output parameters ---

		@Parameter(label = "Output Image", required = true, direction = Parameter.Direction.OUT, description = "Contrast enhanced output image.")
		private transient MTBImage outputImage = null;

		/**
		 * White top-hat image.
		 */
		private MTBImage wthImage;

		/**
		 * Black top-hat image.
		 */
		private MTBImage bthImage;

		/**
		 * Standard constructor.
		 */
		public TopHatContrastEnhancement() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new IlluminationCorrection object.
		 * 
		 * @param image
		 *          uneven illuminated input image
		 * @param _wthMaskSize
		 *          mask size of white top-hat
		 * @param _bthMaskSize
		 *          mask size of black top-hat
		 * 
		 * @throws ALDOperatorException
		 */
		public TopHatContrastEnhancement(MTBImageByte _inputImage,
		    Integer _wthMaskSize, Integer _bthMaskSize) throws ALDOperatorException {
				this.inputImage = _inputImage;
				this.wthMaskSize = _wthMaskSize;
				this.bthMaskSize = _bthMaskSize;
		}

		/**
		 * Get low contrast input image.
		 */
		public MTBImage getInputImage() {
				return inputImage;
		}

		/**
		 * Set low contrast input image.
		 */
		public void setInputImage(MTBImage _inputImage) {
				this.inputImage = _inputImage;
		}

		/**
		 * Get mask size of white top-hat.
		 */
		public Integer getWthMaskSize() {
				return wthMaskSize;
		}

		/**
		 * Set mask size of white top-hat.
		 */
		public void setWthMaskSize(Integer _wthMaskSize) {
				this.wthMaskSize = _wthMaskSize;
		}

		/**
		 * Get mask size of black top-hat.
		 */
		public Integer getBthMaskSize() {
				return bthMaskSize;
		}

		/**
		 * Set mask size of black top-hat.
		 */
		public void setBthMaskSize(Integer _bthMaskSize) {
				this.bthMaskSize = _bthMaskSize;
		}

		/**
		 * Get white top-hat image.
		 */
		public MTBImage getWthImage() {
				return wthImage;
		}

		/**
		 * Get black top-hat image.
		 */
		public MTBImage getBthImage() {
				return bthImage;
		}

		/**
		 * Get contrast enhanced result image.
		 */
		public MTBImage getResultImage() {
				return outputImage;
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				// generate white top-hat
				BasicMorphology morph = new BasicMorphology(inputImage,
				    MTBStructuringElement.createQuadraticElement(wthMaskSize));
				morph.setMode(opMode.WHITE_TOPHAT);
				morph.runOp();
				// get result image of white top-hat
				this.wthImage = morph.getResultImage();
				// generate black top-hat
				morph = new BasicMorphology(inputImage,
				    MTBStructuringElement.createQuadraticElement(bthMaskSize));
				morph.setMode(opMode.BLACK_TOPHAT);
				morph.runOp();
				// get result image of black top-hat
				this.bthImage = morph.getResultImage();
				// add white top-hat to original image
				outputImage = this.add(inputImage, this.wthImage);
				// subtract black top-hat from addition image
				outputImage = this.sub(outputImage, this.bthImage);

				// if verbose flag is set, show intermediate top-hat images
				if (this.verbose) {
						this.wthImage.show();
						this.bthImage.show();
				}

		}

		/**
		 * Method to add two images. Method is copied and modified from
		 * {@link ImageArithmetics}.
		 */
		private MTBImage add(MTBImage img1, MTBImage img2) {
				int sizeStack = img1.getSizeStack();
				int sizeY = img1.getSizeY();
				int sizeX = img1.getSizeX();

				int idx1 = img1.getCurrentSliceIndex();
				int idx2 = img2.getCurrentSliceIndex();
				MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(),
				    img1.getSizeT(), img1.getSizeC(), MTBImageType.MTB_DOUBLE);

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

						for (int y = 0; y < sizeY; y++) {
								for (int x = 0; x < sizeX; x++) {
										newImg.putValueDouble(x, y,
										    img1.getValueDouble(x, y) + img2.getValueDouble(x, y));
								}
						}
				}

				// restore actual slice index
				img1.setCurrentSliceIndex(idx1);
				img2.setCurrentSliceIndex(idx2);
				newImg.setCurrentSliceIndex(0);
				return newImg;
		}

		/**
		 * Method to sub two images. Method is copied and modified from
		 * {@link ImageArithmetics}.
		 */
		private MTBImage sub(MTBImage img1, MTBImage img2) {
				int sizeStack = img1.getSizeStack();
				int sizeY = img1.getSizeY();
				int sizeX = img1.getSizeX();

				int idx1 = img1.getCurrentSliceIndex();
				int idx2 = img2.getCurrentSliceIndex();

				MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(),
				    img1.getSizeT(), img1.getSizeC(), MTBImageType.MTB_DOUBLE);

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

						for (int y = 0; y < sizeY; y++) {
								for (int x = 0; x < sizeX; x++) {
										newImg.putValueDouble(x, y,
										    img1.getValueDouble(x, y) - img2.getValueDouble(x, y));
								}
						}
				}

				// restore actual slice index
				img1.setCurrentSliceIndex(idx1);
				img2.setCurrentSliceIndex(idx2);
				newImg.setCurrentSliceIndex(0);

				return newImg;
		}
		
		@Override
		public String getDocumentation() {
			return "\n" + 
					"<p>This class enhances the contrast by top-hat operations, especially for gray value bright filed or DIC images. A white top-hat is added to the original image (enhance bright objects) and subsequent a black top-hat is subtracted (enhance dark objects).</p>\n" + 
					"\n" + 
					"<p>This approach works well for DIC images, maybe also for bright field or other illumination/contrast based images. Mask size of the structuring element should be small to preserve small structures, like neurites.</p>\n" + 
					"\n" + 
					"<p><b>NOTE:</b> maybe the result image must be re-scaled, since output gray values can       fall outside the dynamic range of the input image!</p>\n" + 
					"\n" + 
					"<p>The approach is adapted from:  \n" + 
					"<ul><li>\n" + 
					"<p>author = {Soille, Pierre},</p>\n" + 
					"</li><li>\n" + 
					"<p>title = {Morphological Image Analysis: Principles</p>\n" + 
					"</li><li>\n" + 
					"<p>and Applications},</p>\n" + 
					"</li><li>\n" + 
					"<p>year = {2010},</p>\n" + 
					"</li><li>\n" + 
					"<p>isbn = {9783642076961},</p>\n" + 
					"</li><li>\n" + 
					"<p>edition = {2},</p>\n" + 
					"</li><li>\n" + 
					"<p>pages = {126 -- 127},</p>\n" + 
					"</li><li>\n" + 
					"<p>publisher = {Springer Berlin Heidelberg}. </p>\n" + 
					"<br>\n" + 
					"</li></ul>\n" + 
					"</p>\n" + 
					"\n" + 
					"<p>--------------------------------------------------------------------------------</p>\n" + 
					"<h2>Usage (standard view)</h2>\n" + 
					"<h3>Required parameters:</h3>\n" + 
					"\n" + 
					"<ul><li>\n" + 
					"<p><tt><b>Input Image</b></tt>\n" + 
					"<ul><li>\n" + 
					"<p>Low contrast input image</p>\n" + 
					"</li></ul>\n" + 
					"</p>\n" + 
					"</li><li>\n" + 
					"<p><tt><b>WTH Mask Size</b></tt>\n" + 
					"<ul><li>\n" + 
					"<p>Mask size of white top-hat</p>\n" + 
					"</li><li>\n" + 
					"<p>default: <i><b>5</b></i></p>\n" + 
					"</li></ul>\n" + 
					"</p>\n" + 
					"</li><li>\n" + 
					"<p><tt><b>BTH Mask Size</b></tt>\n" + 
					"<ul><li>\n" + 
					"<p>ask size of black top-hat</p>\n" + 
					"</li><li>\n" + 
					"<p>default: <i><b>5</b></i></p>\n" + 
					"</li></ul>\n" + 
					"</p>\n" + 
					"</li></ul>\n" + 
					"<h3>Supplemental parameters:</h3>\n" + 
					"\n" + 
					"<ul><li>\n" + 
					"<p><tt><b>None</b></tt></p>\n" + 
					"</li></ul>";
		}
}
