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

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Nuclei;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBStructuringElement;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.enhance.LocallyAdaptiveContrastEnhancement;
import de.unihalle.informatik.MiToBo.morphology.*;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess.ProcessMode;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.*;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.*;

/**
 * Operator for segmenting cell nuclei in 2D images.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class NucleusDetector2D extends MTBOperator {

	/**
	 * Available modes for nuclei segmentation in 2D images.
	 * 
	 * @author moeller
	 */
	public static enum NuclDetectMode {
		/**
		 * Nucleus detection by Otsu thresholding and opening/closing.
		 */
		OTSU_OPENCLOSE, 
		/**
		 * Nucleus detection by Otsu thresholding and erosion/dilation.
		 */
		OTSU_ERODEDILATE, 
		/**
		 * Nucleus detection by Niblack thresholding.
		 */
		NIBLACK,
		/**
		 * Detection based on prior local contrast enhancement.
		 */
		CONTRAST_ANALYSIS
	}

	/**
	 * Input image to process.
	 */
	@Parameter( label= "Input image", required = true, direction= Direction.IN, 
			description = "Input image.", dataIOOrder= -10)
	private transient MTBImage inImgOrig = null;

	/**
	 * Detection mode.
	 */
	@Parameter( label= "Operator mode", required = true, dataIOOrder= -9,
			direction= Direction.IN, description = "Operator detection mode.")
  private NuclDetectMode opMode = NuclDetectMode.OTSU_OPENCLOSE;

	/**
	 * Niblack operator, required if mode is 'NIBLACK'.
	 */
	@Parameter( label= "Niblack operator", required = false, dataIOOrder = -1,
			direction= Direction.IN, description = "Niblack thresholder.")
	private ImgThreshNiblack niblackOperator = null;
	
	/**
	 * Flag to enable/disable morphological pre-/postprocessing.
	 * <p>
	 * Flag is only active if mode is not 'NIBLACK'.
	 */
	@Parameter( label= "Apply morphological operations", required = false,
			direction= Direction.IN, description = "Apply morphological operations.",
			dataIOOrder = -9)
  private boolean doMorphOps = true; 

	/**
	 * Mask size to be used in morphological pre-/postprocessing.
	 */
	@Parameter( label= "Masksize", required = false, dataIOOrder = -8,
			direction= Direction.IN, description = "Structuring element size.")
	private int morphMaskSize = 3;

	/**
	 * Minimal size of valid nuclei regions.
	 */
	@Parameter( label= "Min. nuclei size", required = false, dataIOOrder = -11,
			direction= Direction.IN, description = "Minimum size of valid nuclei.")
	private int minNucleusSize = 500;

	/**
	 * Flag to enable hole filling.
	 */
	@Parameter( label= "Fill holes", required = false, dataIOOrder = -6,
			direction= Direction.IN, description = "Fill holes in regions.")
	private boolean doFillHoles = true;

	@Parameter( label= "Operator for local contrast improvement", 
			required = false,	dataIOOrder = 1, direction= Direction.IN, 
			description = "Applied in mode CONTRAST_ANALYSIS.")
	private LocallyAdaptiveContrastEnhancement contrastEnhancer = null;
	
	/**
	 * Flag to enable separation of merged nuclei regions.
	 */
	@Parameter( label= "Try Nuclei Separation", required = false, 
			dataIOOrder = 2, direction= Direction.IN, 
			description = "Apply nuclei separator to split merged regions.")
	private boolean doNucleiSeparation = false;

	/**
	 * Operator to be applied for nuclei separation.
	 */
	@Parameter( label= "Nuclei Separator", required = false, 
			dataIOOrder = 4, direction= Direction.IN, 
			description = "Operator for nuclei region splitting.")
	private NucleusSeparator2D nucleusSepOp =	new NucleusSeparator2D();

	/**
	 * Units to be used for measurements.
	 */
	@Parameter( label= "Units", required = false, mode=ExpertMode.ADVANCED,
			direction= Direction.IN, dataIOOrder = 100,
			description = "Units for area measurements.")
  private MeasureUnit measureUnits = MeasureUnit.pixels;
	
	/**
	 * Result data object containing quantitative data and label images.
	 */
	@Parameter( label= "Result statistics",
			direction= Direction.OUT, description = "Quantitative result data.")
  private transient SegResult_Nuclei resultData = null;

	/**
	 * Reference to original image, for internal use only
	 */
	private transient MTBImageShort inImg = null;
	
	/**
	 * Default constructor.
	 */
	public NucleusDetector2D() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor.  
	 * @param img					Image to be processed.
	 * @param opmode			Detection mode.
	 * @param niblackOp		Optional Niblack operator for mode 'NIBLACK'.
	 * @param doMorph			Flag to enable/disable morphological processing.
	 * @param msize				Size of structuring element in pre-/postprocessing.
	 * @param minSize			Minimum size of valid nuclei.
	 * @param fillHoles		Flag to enable/disable hole filling.
	 * @throws ALDOperatorException
	 */
	public NucleusDetector2D(MTBImage img, 
			NuclDetectMode opmode, ImgThreshNiblack niblackOp,
			boolean doMorph, int msize, int minSize, boolean fillHoles) 
					throws ALDOperatorException {
		this.inImgOrig = img;
		this.opMode = opmode;
		this.niblackOperator = niblackOp;
		this.doMorphOps = doMorph;
		this.morphMaskSize = msize;
		this.minNucleusSize = minSize;
		this.doFillHoles = fillHoles;
	}

	/**
	 * Specify input image.
	 */
	public void setInputImage(MTBImage img) {
		this.inImgOrig = img;
	}
	
	/**
	 * Specify units in which to measure areas.
	 */
	public void setMeasureUnits(MeasureUnit mu) {
		this.measureUnits = mu;
	}
	
	/**
	 * Get the result label image.
	 * @return	Reference to result label image, might be null.
	 */
	public MTBImage getResultImage() {
		if (this.resultData != null)
			return this.resultData.getLabelImage();
		return null;
	}

	/**
	 * Get the result data, i.e. mask and related quantitative data.
	 * @return	Result data object.
	 */
	public SegResult_Nuclei getResultData() {
		return this.resultData;
	}

	@Override
	protected void operate() 
  	throws ALDOperatorException, ALDProcessingDAGException {
		
		// reset operator
		this.resultData = null;
		
		// do some initializations
		this.inImg= 
			(MTBImageShort)this.inImgOrig.convertType(MTBImageType.MTB_SHORT, true);
		if (this.inImg == null) {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
					"NucleusDetector2DAlgos: " +
					"input image cannot be converted to type MTBImage.MTB_SHORT!");
		}

		// apply the detector
		MTBImageByte nucleiMask = null;
		switch(this.opMode)
		{
		case OTSU_OPENCLOSE:
			nucleiMask= this.detectWithOtsu_openclose();
			break;
		case OTSU_ERODEDILATE:
			nucleiMask= this.detectWithOtsu_erodedilate();
			break;
		case NIBLACK:
			nucleiMask= this.detectWithNiblack();
			break;
		case CONTRAST_ANALYSIS:
			nucleiMask= this.detectWithLocalContrastEnhancement();
			break;
		}

		if (nucleiMask==null)
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
					"[NucleusDetector2D] Detecting nuclei failed, null mask received...");
		
		// optional: try to separate merged nuclei regions
		MTBImage resultLabelImg = null;
		MTBRegion2DSet nucleusRegions = null;
		if (this.doNucleiSeparation) {
			// extract labels from binary mask
			LabelComponentsSequential labler = 
					new LabelComponentsSequential(nucleiMask, true);
			labler.runOp(true);
			MTBImage labelImg = labler.getLabelImage();
			
			// run the separator
			this.nucleusSepOp.setInputLabelImage(labelImg);
			this.nucleusSepOp.setInputGrayScaleImage(this.inImg);
			this.nucleusSepOp.runOp(false);
			resultLabelImg = this.nucleusSepOp.getResultImage();
			
			// get set of regions from label image
			int maxLabel = resultLabelImg.getMinMaxInt()[1];
			MTBRegion2D [] regs = new MTBRegion2D[maxLabel];
			for (int i=0;i<maxLabel;++i)
				regs[i] = new MTBRegion2D();
			int xMin = Integer.MAX_VALUE, xMax = 0,
					yMin = Integer.MAX_VALUE, yMax = 0;
			for (int y=0;y<resultLabelImg.getSizeY();++y) {
				for (int x=0;x<resultLabelImg.getSizeX();++x) {
					if (resultLabelImg.getValueInt(x, y) > 0) {
						if (x < xMin)
							xMin = x;
						if (y < yMin)
							yMin = y;
						if (x > xMax)
							xMax = x;
						if (y > yMax)
							yMax = y;
						regs[resultLabelImg.getValueInt(x, y)-1].addPixel(x, y);
					}
				}
			}
			Vector<MTBRegion2D> regVec = new Vector<MTBRegion2D>();
			for (int i=0;i<maxLabel;++i) {
				if (regs[i].getArea() > 0)
					regVec.add(regs[i]);
			}			
			nucleusRegions = new MTBRegion2DSet(regVec, xMin, yMin, xMax, yMax);
			
			// recover binary mask
			nucleiMask.fillBlack();
			for (int y=0;y<resultLabelImg.getSizeY();++y) {
				for (int x=0;x<resultLabelImg.getSizeX();++x) {
					if (resultLabelImg.getValueInt(x, y) > 0) 
						nucleiMask.putValueInt(x, y, 255);
				}
			}
		}
		else {
			// post-process binary map: connected component labeling
			LabelComponentsSequential regLabler = 
					new LabelComponentsSequential(nucleiMask,true);
			regLabler.runOp(false);
			resultLabelImg = regLabler.getLabelImage();
			nucleusRegions= regLabler.getResultingRegions();
		}
		// calc some numbers
		double avgSize = nucleusRegions.calcAverageSize();
		
		// set result data
		String iname = this.inImgOrig.getLocation();
		if (iname == null || iname.isEmpty())
			iname = this.inImgOrig.getTitle();
		// set titles of result images
		nucleiMask.setTitle("Binary detection result for " + iname);
		resultLabelImg.setTitle("Labeled segmentation result for " + iname);
		this.resultData = 
			new SegResult_Nuclei(iname, 1 /* always work on 1st channel */,
				nucleiMask, resultLabelImg, nucleusRegions, nucleusRegions.size(), 
				avgSize);
		this.resultData.setMeasurementUnit(this.measureUnits);
	}
	
	/**
	 * Detects nuclei based on Otsu thresholding and a combination of opening 
	 * and closing.
	 * 
	 * @return Label image with detected nuclei.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImageByte detectWithOtsu_openclose() 
		throws ALDOperatorException, ALDProcessingDAGException {

		// apply otsu thresholding
		CalcGlobalThreshOtsu otsuCalculator= 
			new CalcGlobalThreshOtsu(this.inImg);
		otsuCalculator.runOp(false);
		double thresh= otsuCalculator.getOtsuThreshold().getValue().doubleValue();
		
		// threshold the image
		ImgThresh thresholder= new ImgThresh(this.inImg,thresh);
		thresholder.runOp(false);
		MTBImageByte binImg= 
			(MTBImageByte)thresholder.getResultImage().convertType(this,
																							MTBImageType.MTB_BYTE, false);
		
		// apply opening to the binary image
		MTBImageByte tmpResultImg= null;
		if (this.doMorphOps) {
			ImgErode eroder= new ImgErode(binImg, this.morphMaskSize);
			eroder.runOp(false);
			MTBImageByte erodedImg= (MTBImageByte)eroder.getResultImage();
			ImgDilate dilater= new ImgDilate(erodedImg,this.morphMaskSize);
			dilater.runOp(false);
			tmpResultImg= 
				(MTBImageByte)(dilater.getResultImage().convertType(this,
																								MTBImageType.MTB_BYTE,true));
		}
		else {
			tmpResultImg= binImg; 
		}
		
		// suppress too small regions
		ComponentPostprocess postproc= 
				new ComponentPostprocess(tmpResultImg,ProcessMode.ERASE_SMALL_COMPS);
		postproc.setMinimalComponentSize(this.minNucleusSize);
		postproc.runOp(false);
		tmpResultImg= (MTBImageByte)postproc.getResultImage();
		
		// fill holes in components
		if (this.doFillHoles) {
			FillHoles2D filler= new FillHoles2D(tmpResultImg);
			filler.runOp(false);
			tmpResultImg= (MTBImageByte)filler.getResultImage();
		}
		return tmpResultImg;
	}
	
	/**
	 * Detects nuclei based on Otsu thresholding and a combination of erosion/
	 * dilation.
	 * <p>
	 * This routine has been developed by Jochen Luechtrath for 
	 * nuclei/cytoplasm segmentation.
	 * 
	 * @return Label image with detected nuclei.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImageByte detectWithOtsu_erodedilate() 
		throws ALDOperatorException, ALDProcessingDAGException {

		// apply otsu thresholding
		CalcGlobalThreshOtsu otsuCalculator = new CalcGlobalThreshOtsu(this.inImg);
		otsuCalculator.runOp(false);
		double thresh= otsuCalculator.getOtsuThreshold().getValue().doubleValue();

		// threshold the image
		ImgThresh thresholder = new ImgThresh(this.inImg,thresh);
		thresholder.runOp(false);
		MTBImageByte binImg= 
			(MTBImageByte)thresholder.getResultImage().convertType(this,
																							MTBImageType.MTB_BYTE, true);
		
		// init temporary variable
		MTBImageByte tmpResultImg = binImg;
		
		// erosion with small mask size
		if (this.doMorphOps) {
			ImgErode eroder = new ImgErode(tmpResultImg, this.morphMaskSize);
			eroder.runOp(false);
			tmpResultImg= (MTBImageByte) eroder.getResultImage();
		}
			
		// fill holes
		if (this.doFillHoles) {
			FillHoles2D filler = new FillHoles2D(tmpResultImg);
			filler.runOp(false);
			tmpResultImg = (MTBImageByte) filler.getResultImage();
		}
		
		// erode again
		if (this.doMorphOps) {
			ImgErode eroder=new ImgErode(tmpResultImg, this.morphMaskSize);
			eroder.runOp(false);
			tmpResultImg = (MTBImageByte) eroder.getResultImage();
		}
		
		// eliminate small components
		ComponentPostprocess compproc = 
				new ComponentPostprocess(tmpResultImg, ProcessMode.ERASE_SMALL_COMPS);
		compproc.setMinimalComponentSize(this.minNucleusSize);
		compproc.runOp(false);
		tmpResultImg = (MTBImageByte)compproc.getResultImage();
		
		// final dilation
		if (this.doMorphOps) {
			ImgDilate dilater = new ImgDilate(tmpResultImg, this.morphMaskSize);
			dilater.runOp(false);
			tmpResultImg = (MTBImageByte)dilater.getResultImage().convertType(this,
																								MTBImageType.MTB_BYTE, true);
		}
		
		// return result
		return tmpResultImg;
	}

	/**
	 * Detects nuclei based on Niblack thresholding.
	 * 
	 * @return Label image with detected nuclei.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImageByte detectWithNiblack() 
		throws ALDOperatorException, ALDProcessingDAGException {

		// copy parameters to parameter object
		ImgThreshNiblack niblackThresholder= this.niblackOperator;
		niblackThresholder.setInputImage(this.inImg);
		niblackThresholder.runOp(false);
		MTBImageByte tmpResultImg= niblackThresholder.getResultImage();
		
		// closing 
		if (this.doMorphOps) {
			ImgDilate dilater= new ImgDilate(tmpResultImg,this.morphMaskSize);
			dilater.runOp(false);
//			MTBImageByte dilatedImg= dilater.getResultImage();
			MTBImageByte dilatedImg= 
				(MTBImageByte)dilater.getResultImage().convertType(this,
																								MTBImageType.MTB_BYTE, true);
			ImgErode eroder= new ImgErode(dilatedImg, this.morphMaskSize);
			eroder.runOp(false);
			tmpResultImg= (MTBImageByte)eroder.getResultImage();
		}
		
		// suppress too small regions
		ComponentPostprocess postproc= 
				new ComponentPostprocess(tmpResultImg,ProcessMode.ERASE_SMALL_COMPS);
		postproc.setMinimalComponentSize(this.minNucleusSize);
		postproc.runOp(false);
		tmpResultImg= (MTBImageByte)postproc.getResultImage();
		
		// fill holes in components
		if (this.doFillHoles) {
			FillHoles2D filler= new FillHoles2D(tmpResultImg);
			filler.runOp(false);
			tmpResultImg= (MTBImageByte)(filler.getResultImage());
		}
		
		// opening 
		if (this.doMorphOps) {
			ImgErode eroder= new ImgErode(tmpResultImg,	this.morphMaskSize);
			eroder.runOp(false);
			MTBImageByte erodedImg= (MTBImageByte)eroder.getResultImage();
			ImgDilate dilater= new ImgDilate(erodedImg,this.morphMaskSize);
			dilater.runOp(false);
//			tmpResultImg= dilater.getResultImage();
			tmpResultImg= (MTBImageByte)dilater.getResultImage().convertType(this,
																								MTBImageType.MTB_BYTE, true);
		}
		
		// suppress too small regions
		postproc= 
				new ComponentPostprocess(tmpResultImg,ProcessMode.ERASE_SMALL_COMPS);
		postproc.setMinimalComponentSize(this.minNucleusSize);
		postproc.runOp(false);
		tmpResultImg= (MTBImageByte)postproc.getResultImage();

		// return result image
		return tmpResultImg;
	}
	
	/**
	 * Detect nuclei by first improving image contrast.
	 * 
	 * @return Label image with detected nuclei.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImageByte detectWithLocalContrastEnhancement() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBStructuringElement mask_3 = 
				new MTBStructuringElement(new int[][]{{1,1,1},{1,1,1},{1,1,1}});

		this.contrastEnhancer.setInputImage(this.inImg);
		this.contrastEnhancer.runOp(false);
		MTBImage result = this.contrastEnhancer.getResultImage();
		if (this.contrastEnhancer.isAppliedComponentwise()) {
			// result is already a binary mask
			MTBImageByte binResult = (MTBImageByte)result;
			
			// fill wholes
			FillHoles2D fillOp = new FillHoles2D(binResult);
			fillOp.runOp(false);
			binResult = (MTBImageByte)fillOp.getResultImage().convertType(
																								MTBImageType.MTB_BYTE, true);
			// do an opening to remove noise
			BasicMorphology morphOp = new BasicMorphology(binResult, mask_3);
			morphOp.setMode(BasicMorphology.opMode.ERODE);
			morphOp.runOp(false);
			binResult = (MTBImageByte)morphOp.getResultImage().convertType(
																								MTBImageType.MTB_BYTE, true);
			
			// suppress too small regions
			ComponentPostprocess postproc= 
					new ComponentPostprocess(binResult,ProcessMode.ERASE_SMALL_COMPS);
			postproc.setMinimalComponentSize(this.minNucleusSize);
			postproc.runOp(false);
			return (MTBImageByte)postproc.getResultImage();
		}
			
		// result is a gray-scale image which needs to be thresholded
		CalcGlobalThreshOtsu otsuOp = new CalcGlobalThreshOtsu(result);
		otsuOp.runOp(false);
		double threshold = otsuOp.getOtsuThreshold().getValue().doubleValue();
		ImgThresh threshOp = new ImgThresh(result, threshold);
		threshOp.runOp(false);
		MTBImage binResult = threshOp.getResultImage();

		// fill wholes
		FillHoles2D fillOp = new FillHoles2D(binResult);
		fillOp.runOp(false);
		binResult= fillOp.getResultImage().convertType(MTBImageType.MTB_BYTE,true);

		// suppress too small regions
		ComponentPostprocess postproc= new ComponentPostprocess(
				binResult.convertType(MTBImageType.MTB_BYTE, true),
				ProcessMode.ERASE_SMALL_COMPS);
		postproc.setMinimalComponentSize(this.minNucleusSize);
		postproc.runOp(false);

		// do an closing
		BasicMorphology morphOp = 
				new BasicMorphology(postproc.getResultImage(), mask_3);
		morphOp.setMode(BasicMorphology.opMode.CLOSE);
		morphOp.runOp(false);
		binResult = 
				morphOp.getResultImage().convertType(MTBImageType.MTB_BYTE, true);
		return (MTBImageByte)binResult.convertType(MTBImageType.MTB_BYTE, true);
	}
}
