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
 * $Rev: 4413 $
 * $Date: 2011-10-24 18:39:30 +0200 (Mo, 24 Okt 2011) $
 * $Author: posch $
 * 
 */

package de.unihalle.informatik.MiToBo.enhance;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.morphology.ImgDilate;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.CalcGlobalThreshOtsu;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;

/**
 * This class implements contrast enhancement for microscopy images. 
 * <p>
 * Reference:<br>
 * Jyh-Ying Peng, Chun-Nan Hsu, Chung-Chih Lin,<br>
 * "Adaptive Image Enhancement for Fluorescence Microscopy",<br>
 * Int. Conf. on Technologies and Applications of Artificial Intelligence,
 * pp. 9-16, 2010.
 * <p>
 * The basic idea of this algorithm is to enhance contrast by normalizing
 * each pixel's intensity according to the standard intensity deviation in a 
 * local region around the pixel. Subsequently it should be easier to 
 * distinguish image background and relevant foreground structures from each
 * other. Although in principal arbitrary segmentation methods could be 
 * applied after contrast enhancement, the algorithm is optimized for 
 * subsequent binarization, e.g. by Otsu thresholding. In addition, note that
 * the algorithm does not work very well on images showing small structures
 * on very noisy background (like P-bodies or stress granules). It is much 
 * better suited for larger structures like DAPI-stained nuclei which can 
 * more easily be distinguished from clutter, at least visually.
 * <p>
 * The size of each local region is adaptive and chosen so that the standard 
 * deviation in the region exceeds a certain fraction of
 * the overall image standard deviation, where the fraction is usually lying 
 * in the range of 0.2 to 0.8. The ratio can be adapted by the corresponding 
 * parameter for the standard deviation ratio. The size of the local region is 
 * bounded to a maximum radius which is to be selected according to the size 
 * of the entities shown in the image.
 * <p>
 * The actual image enhancement is based on the following equations.<br>
 * The radius for each pixel region is first of all calculated as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 *              \\begin{equation*} 
 *              r(x,y) =  min \\{ R_{max} ,
 *              	\\underset{r}{min} 
 *              		\\{ r > 0 | StD[R_r(x,y)] \\geq T_{StD} \\} \\}
 *              \\end{equation*}}
 * where {@latex.inline $R_{max}$} is the maximally allowed radius,     
 * {@latex.inline $T_{StD}$} is the threshold for local standard deviation 
 * calculated as fraction of the image intensity standard deviation, and
 * {@latex.inline $StD[R_r(x,y)]$} is the standard deviation in a region 
 * {@latex.inline $R_r(x,y)$} with radius {@latex.inline $r$} around the 
 * current pixel {@latex.inline $(x,y)$}. 
 * <p>
 * To avoid numerical instabilities the local standard deviation for 
 * normalization is calculated as follows, given the selected local region size
 * {@latex.inline $r^\\ast$}:
 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
 *              \\begin{equation*} 
 *              I_{StD}(x,y) =  max \\{ T_{StD} , StD[R_{r^\\ast}(x,y)] \\}
 *              \\end{equation*}}
 * Finally the normalization for contrast enhancement, i.e. the calculation of 
 * the new image intensity value {@latex.inline $I_N(x,y)$} is done as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
 *     \\begin{eqnarray*}
 *       I_E(x,y) &=& E[R_{r^\\ast}(x,y)] \\\\
 *       I_{StD}(x,y) &=& StD[R_{r^\\ast}(x,y)] \\\\
 *       I_N(x,y) &=& \\frac{I(x,y) - I_E(x,y)}{I_{StD}(x,y)}
 *     \\end{eqnarray*}}
 * <p>
 * As an extension to the original paper this operator features a mode for
 * component-wise application of the algorithm. This means that the image is
 * first of all thresholded and connected components are extracted. Then the
 * contrast enhancement is applied to each component's bounding box separately.
 * Finally, the result image is generated from all enhanced patches after they
 * have been thresholded, i.e. the result image in this case is already a 
 * binary segmentation of foreground and background. This inherent binarization
 * is done as the contrast-enhanced image does contain only fractions of 
 * reasonable information and, thus, is difficult to post-process without 
 * specific knowledge only available inside of this operator.
 * <p>
 * The basics of this operator have been implemented in the course of the 
 * bachelor thesis of Joachim Schumann in SS 2011. 
 * 
 * @author Joachim Schumann
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.APPLICATION)
@ALDMetaInfo(export = ExportPolicy.MANDATORY)
public class LocallyAdaptiveContrastEnhancement extends MTBOperator {

	/**
	 * Input image to be processed.
	 */
	@Parameter(label = "Input Image", required = true, 
			direction = Direction.IN, supplemental = false, 
			description = "Input image", mode = ExpertMode.STANDARD)
	private transient MTBImage inImg = null;

	/**
	 * Enhanced or segmented result image, either of type double or binary.
	 * <p>
	 * Note that the enhanced, non-binarized image is scaled to [0,1].
	 */
	@Parameter(label = "Result Image", required = false,
			direction = Direction.OUT, supplemental = false, 
			description = "Result image")
	private transient MTBImage resultImg = null;

	/**
	 * Maximal radius of region to be considered for intensity normalization.
	 * <p>
	 * This radius is supposed to be chosen according to the size of the
	 * entities of interest in the image.
	 */
	@Parameter(label = "Maximal Region Radius", required = false, 
			direction = Direction.IN, supplemental = false, dataIOOrder = -20,
			description = "Max. Radius of Region", mode = ExpertMode.ADVANCED)
	private int maxradius = 100;

	/**
	 * Ratio of image standard deviation used as threshold for radius calculation.
	 * <p>
	 * For each pixel the radius of the region used for intensity normalization
	 * is calculated as to be the radius of the smallest region showing a 
	 * standard deviation that exceeds a threshold. This threshold is calculated
	 * as the given ratio of the image's standard deviation. The larger this 
	 * ratio is set, the larger the regions will become in tendency.
	 */
	@Parameter(label = "Std. Deviation Ratio", required = false, 
			direction = Direction.IN, mode = ExpertMode.ADVANCED, dataIOOrder = -18,
			description = "Percentage of StD used as threshold value.")
	private double stdDevRatio = 0.7;

	/**
	 * Flag to activate component-wise application, yields a binary result image.
	 * <p>
	 * Usually for enhancing an image average intensity values and standard
	 * deviations are calculated over the complete image. But, if there are
	 * large regions without structure in an image, calculated values are 
	 * probably misleading, resulting in image degradation rather than image
	 * enhancement. 
	 * <p>
	 * One possibility to overcome this problem is to apply the intensity 
	 * normalization only locally. In detail, the image is initially thresholded
	 * by Otsu, then dilated and connected components are extracted. 
	 * Subsequently for each connected component the bounding box is extracted 
	 * (slightly enlarged), and the normalization is only applied to the image 
	 * region corresponding to the bounding box. At the end all enhanced regions 
	 * are merged again to yield the final, enhanced image.
	 */
	@Parameter(label = "Apply component-wise", required = false, 
			direction = Direction.IN, mode = ExpertMode.ADVANCED, 
			description = "Flag to enable component-wise processing, " +
					"results in binary image.", dataIOOrder = -16)
	private boolean applyComponentWise = true;
	
	/**
	 * Flag to enable calculation and display of radius image (only in 
	 * non-component mode).
	 */
	@Parameter(label = "Calculate Radius Image", required = false, 
			direction = Direction.IN, mode = ExpertMode.ADVANCED,
			supplemental = true, description = "Flag for calculating radius image.")
	private boolean calcRadiusImage = false;

	/**
	 * Optional radius image.
	 */
	@Parameter(label = "Radius Image", required = false, 
			direction = Direction.OUT, supplemental = true,
			mode = ExpertMode.ADVANCED,	description = "Radius image")
	private transient MTBImage radiusImg = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public LocallyAdaptiveContrastEnhancement() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor with given image.
	 * @param inimg	Input image.
	 * @throws ALDOperatorException
	 */
	public LocallyAdaptiveContrastEnhancement(MTBImage inimg)
			throws ALDOperatorException {
		this.inImg = inimg;
	}

	/**
	 * Constructor with given image and parameters.
	 * @param inimg	Input image.
	 * @throws ALDOperatorException
	 */
	public LocallyAdaptiveContrastEnhancement(MTBImage inimg, double pc, int maxr)
			throws ALDOperatorException {
		this.inImg = inimg;
		this.stdDevRatio = pc;
		this.maxradius = maxr;
	}

	/**
	 * Specify the input image.
	 * @param inimg	Input image to be processed.
	 */
	public void setInputImage(MTBImage inimg) {
		this.inImg = inimg;
	}
	
	/**
	 * Returns the input image, null if not set.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}

	/**
	 * Returns result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}

	/**
	 * Returns supplemental radius image, null if not generated.
	 */
	public MTBImage getRadiusImage() {
		return this.radiusImg;
	}

	/**
	 * Return operator mode.
	 * @return	If true, enhancement is applied component-wise.
	 */
	public boolean isAppliedComponentwise() {
		return this.applyComponentWise;
	}
	
	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// reset some variables
		this.radiusImg = null;
		this.resultImg = null;
		
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();
		
		if (!this.applyComponentWise) {
			double StDI = calStDI(this.inImg);
			double TstD = calcTStD(StDI, this.stdDevRatio);
			MTBImage[] resultImgs =	calcEnhancedImage(this.inImg, TstD, 
					this.maxradius,this.calcRadiusImage);
			this.resultImg = resultImgs[0];
			this.radiusImg = resultImgs[1];
		}
		else {
			// extract list of components
			MTBRegion2DSet components = this.extractLocalComponents();
	
			// individually process each component
			this.resultImg = MTBImage.createMTBImage(width, height,
							1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
			this.resultImg.fillBlack();
			this.radiusImg = null;
			MTBImage subImage = null;
			for (int i = 0; i < components.size(); ++i) {
				MTBRegion2D reg = components.get(i);
				double[] coords = reg.getBoundingBox();
				int xmin = (int)(coords[0]);
				int ymin = (int)(coords[1]);
				int xmax = (int)(coords[2]+0.5);
				int ymax = (int)(coords[3]+0.5);
				
				int left_x = ((xmin - 10) > 0 ? (int)(xmin - 10) : 0);
				int top_y = ((ymin - 10) > 0 ? (int)(ymin - 10) : 0);
				int right_x = ((xmax + 10) < this.inImg.getSizeX() ? 
						(int)(xmax + 10) : this.inImg.getSizeX() - 1);
				int bottom_y = ((ymax + 10) < this.inImg.getSizeY() ? 
						(int)(ymax + 10) : this.inImg.getSizeY() - 1);
				
				// extract sub-image
				subImage = this.inImg.getImagePart(left_x, top_y, 0, 0, 0, 
						right_x - left_x + 1, bottom_y - top_y + 1, 1, 1, 1);
				
				// apply contrast enhancement to sub-image
				double StDI = calStDI(subImage);
				double TstD = calcTStD(StDI, this.stdDevRatio);
				MTBImage[] resultImgs = calcEnhancedImage(subImage, TstD, 
						this.maxradius, this.calcRadiusImage);
				
				MTBImage subResult = resultImgs[0];
				// threshold the image
				CalcGlobalThreshOtsu otsuOp = new CalcGlobalThreshOtsu(subResult);
				otsuOp.runOp(false);
				double threshold= (otsuOp.getOtsuThreshold()).getValue().doubleValue();
				ImgThresh threshOp = new ImgThresh(subResult, threshold);
				threshOp.runOp(false);
				MTBImage binImage = threshOp.getResultImage();
				
				// paste sub-image back into result image
				for (int y = top_y; y<=bottom_y; ++y) {
					for (int x = left_x; x<=right_x; ++x) {
						// do not overwrite pixels that already belong to the foreground
						if (this.resultImg.getValueInt(x, y) == 0) {
							this.resultImg.putValueInt(x, y, 
									binImage.getValueInt(x - left_x, y - top_y));
						}
					}
				}
			}
		}
	}

	/**
	 * Extracts list of connected components from Otsu thresholded input image.
	 * <p>
	 * The method first applies Otsu thresholding to the given image and 
	 * subsequently applies a dilation with a 3x3 structuring element. Then
	 * connected components are extracted, and finally all components with a 
	 * size larger than 50 pixels are returned as result. 
	 * 
	 * @return	List of connected components larger than 50 pixels.
	 */
	private MTBRegion2DSet extractLocalComponents() 
			throws ALDOperatorException, ALDProcessingDAGException {
		// threshold the image
		CalcGlobalThreshOtsu otsuOp = new CalcGlobalThreshOtsu(this.inImg);
		otsuOp.runOp(false);
		double threshold = (otsuOp.getOtsuThreshold()).getValue().doubleValue();
		ImgThresh threshOp = new ImgThresh(this.inImg, threshold);
		threshOp.runOp(false);
		MTBImage binImage = threshOp.getResultImage();

		ImgDilate dilOp = new ImgDilate(binImage, 3);
		dilOp.runOp(false);
		binImage = dilOp.getResultImage();
		
		// component labeling
		LabelComponentsSequential labelOp = 
				new LabelComponentsSequential(binImage,true);
		labelOp.runOp(false);
		MTBRegion2DSet components = labelOp.getResultingRegions();
		
		// eliminate regions smaller than 50 pixels
		return components.selectLargeRegions(50);
	}
	
	/**
	 * Calculates the standard deviation of the given image.
	 * 
	 * @param image	Image to process.
	 * @return	Calculated standard deviation.
	 */
	private static double calStDI(MTBImage image) {

		float variance = 0;
		float pixelvalSum = 0;
		int pixelSum = image.getSizeX() * image.getSizeY();

		for (int y = 0; y < image.getSizeY(); y++) {
			for (int x = 0; x < image.getSizeX(); x++) {
				float pixelval = image.getValueInt(x, y);
				pixelvalSum += pixelval;
			}
		}

		float pixelaverage = pixelvalSum / pixelSum;

		for (int y = 0; y < image.getSizeY(); y++) {
			for (int x = 0; x < image.getSizeX(); x++) {
				float pixelval = image.getValueInt(x, y);
				variance += (pixelval - pixelaverage)
						* (pixelval - pixelaverage);
			}
		}
		variance = variance / (pixelSum - 1);
		double StDI = Math.sqrt(variance);

		return StDI;
	}

	/**
	 * Calculates the threshold for the local standard daviation.
	 * <p>
	 * Equation:
	 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}} 
	 *              \\begin{equation*} 
	 *              thresh =  percentage \\cdot StDI
	 *              \\end{equation*}}
	 * 
	 * @param StDI	Standard deviation.
	 * @param percentage	Specified percentage.
	 * @return	Calculated threshold.
	 */
	private static double calcTStD(double StDI, double percentage) {
		return StDI * percentage;
	}

	/**
	 * Calculates the new image intensity values of the image.
	 * 
	 * @param img	Image to process.
	 * @param TstD	Standard deviation threshold to apply.
	 * @param maxradius	Maximal local region radius.
	 * @param calcRadiusImage	Flag to enable calculation of radius image.
	 * @return	Array with enhanced image and (optionally) radius image.
	 */
	private static MTBImage[] calcEnhancedImage(MTBImage img, double TstD,
			int maxradius, boolean calcRadiusImage) {

		int width = img.getSizeX();
		int height = img.getSizeY();

		// result Image
		MTBImageDouble resultImg = 
				(MTBImageDouble)MTBImage.createMTBImage(width, height,
						1, 1, 1, MTBImage.MTBImageType.MTB_DOUBLE);

		// radius Image
		MTBImageInt radiusImg = null;
		if (calcRadiusImage) {
			radiusImg =	(MTBImageInt)MTBImage.createMTBImage(width, height,
					1, 1, 1, MTBImage.MTBImageType.MTB_INT);
		}

		// Images for intermediate results
		MTBImage newImg = MTBImage.createMTBImage(width, height,
				1, 1, 1, MTBImage.MTBImageType.MTB_DOUBLE);

		MTBImage testImg = MTBImage.createMTBImage(width, height,
				1, 1, 1, MTBImage.MTBImageType.MTB_DOUBLE);

		
		float regvariance = 0;
		float pixelvalvar = 0;
		float pixelvalvarold = 0;
		float pixelvalvarnew = 0;

		double IStD = 0;
		float regpixelvalsum = 0;
		float regpixelvalsumnew = 0;
		int regpixelsum = 0;
		double newvalue = 0;
		double minimum = Double.MAX_VALUE;
		float oldmean = 0;
		float newmean = 0;
		int pixelsumold = 0;
		float pixelval = 0;
		float pixelvalnew = 0;
		int regpixelsumnew = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				pixelvalvar = 0;
				regpixelsum = 0;
				regpixelvalsum = 0;

				int radius = 0;
				for (radius = 1; radius <= maxradius; radius++) {

					if (radius == 1) {

						for (int a = y - radius; a <= y + radius; a++) {

							for (int b = x - radius; b <= x + radius; b++) {

								if ((a >= 0 && a < height)
										&& (b >= 0 && b < width)) {

									regpixelsum++;
									pixelval = img.getValueInt(b, a);
									pixelvalvar += pixelval * pixelval;
									regpixelvalsum += pixelval;

								}

							}
						}

						float regpixelmean = regpixelvalsum
								/ regpixelsum ;
						oldmean = regpixelmean;
						regvariance = (pixelvalvar / (regpixelsum))
								- (regpixelmean * regpixelmean);
						double StDreg = Math.sqrt(regvariance);
						pixelsumold = regpixelsum ;
						pixelvalvarold = pixelvalvar;

						if (StDreg >= TstD) {
							IStD = StDreg;
							newvalue = ((img.getValueInt(x, y) - regpixelmean) / IStD);
							if (newvalue < minimum) {
								minimum = newvalue;
							}
							newImg.putValueDouble(x, y, 0, 0, 0, newvalue);
							regpixelsum  = 0;
							regpixelvalsum = 0;
							pixelvalvar = 0;

							break;
						}
					}

					else if (radius > 1) {

						for (int a = y - radius + 1; a <= y + radius - 1; a++) {

							int b = x + radius;

							if ((a >= 0 && a < height)
									&& (b >= 0 && b < width)) {
								pixelvalnew = img.getValueInt(b, a);
								pixelvalvarnew += pixelvalnew * pixelvalnew;
								regpixelvalsumnew += pixelvalnew;
								regpixelsumnew++;
							}

							int d = x - radius;

							if ((a >= 0 && a < height)
									&& (d >= 0 && d < width)) {
								pixelvalnew = img.getValueInt(d, a);
								pixelvalvarnew += pixelvalnew * pixelvalnew;
								regpixelvalsumnew += pixelvalnew;
								regpixelsumnew++;
							}

						}

						for (int e = x - radius; e <= x + radius; e++) {

							int f = y - radius;

							if ((e >= 0 && e < width)
									&& (f >= 0 && f < height)) {
								pixelvalnew = img.getValueInt(e, f);
								pixelvalvarnew += pixelvalnew * pixelvalnew;
								regpixelvalsumnew += pixelvalnew;
								regpixelsumnew++;
							}

							int h = y + radius;

							if ((h >= 0 && h < height)
									&& (e >= 0 && e < width)) {
								pixelvalnew = img.getValueInt(e, h);
								pixelvalvarnew += pixelvalnew * pixelvalnew;
								regpixelvalsumnew += pixelvalnew;
								regpixelsumnew++;
							}

						}

						newmean = regpixelvalsumnew / regpixelsumnew;
						
						float quotient1 = (float) pixelsumold
								/ (regpixelsumnew + pixelsumold);
						float quotient2 = (float) regpixelsumnew
								/ (regpixelsumnew + pixelsumold);

						float regpixelmeannew = quotient1 * oldmean
								+ quotient2 * newmean;

						oldmean = regpixelmeannew;
						pixelsumold += regpixelsumnew;

						double regvariancenew = ((pixelvalvarnew + pixelvalvarold) / pixelsumold)
								- (regpixelmeannew * regpixelmeannew);
						
						double StDreg = Math.sqrt(regvariancenew);

						pixelvalvarold += pixelvalvarnew;
						pixelvalvarnew = 0;

						if (StDreg >= TstD) {
							IStD = StDreg;
							newvalue = ((img.getValueInt(x, y) - regpixelmeannew) / IStD);

							if (newvalue < minimum) {
								minimum = newvalue;
							}

							int newradius = radius;
							if (radiusImg != null)
								radiusImg.putValueInt(x, y, 0, 0, 0, newradius);

							newImg.putValueDouble(x, y, 0, 0, 0, newvalue);
							regpixelsumnew = 0;
							regpixelvalsumnew = 0;
							regvariancenew = 0;
							pixelvalvarnew = 0;
							pixelvalvarold = 0;
							break;
						}

						else if ((radius == maxradius) && (StDreg < TstD)) {
							IStD = TstD;
							newvalue = ((img.getValueInt(x, y) - regpixelmeannew) / IStD);

							if (newvalue < minimum) {
								minimum = newvalue;
							}

							int newradius = radius;
							if (radiusImg != null)
								radiusImg.putValueInt(x, y, 0, 0, 0, newradius);
							newImg.putValueDouble(x, y, 0, 0, 0, newvalue);
							regpixelsumnew = 0;
							regpixelvalsumnew = 0;
							regvariancenew = 0;
							pixelvalvarnew = 0;
							pixelvalvarold = 0;
						}

						regpixelvalsumnew = 0;
						regpixelsumnew = 0;

					} // end of else-branch: radius > 1

					regpixelsum = 0;
					regpixelvalsum = 0;
					pixelvalvar = 0;

				} // end of for-loop over all radii

			} // end of for-loop over all x

		} // end of for-loop over all y
			
		double min = minimum * (-1);

		for (int y = 0; y < newImg.getSizeY(); y++) {
			for (int x = 0; x < newImg.getSizeX(); x++) {
				
				double resultvalue = (newImg.getValueDouble(x, y) + min);
				
				testImg.putValueDouble(x, y, 0, 0, 0, resultvalue);

			}
		}
		
		double[] bildmax2 = testImg.getMinMaxDouble();
		
		for (int y = 0; y < testImg.getSizeY(); y++) {
			for (int x = 0; x < testImg.getSizeX(); x++) {
				
				double resultvalue = testImg.getValueDouble(x, y);

				double newresultvalue = (resultvalue / bildmax2[1]);

				resultImg.putValueDouble(x, y, 0, 0, 0, newresultvalue);

			}
		}

		MTBImage[] resultImages = new MTBImage[2];
		resultImages[0] = newImg; //resultImg;
		if (calcRadiusImage)
			resultImages[1] = radiusImg;
		else
			resultImages[1] = null;
		return resultImages;
	}

}
