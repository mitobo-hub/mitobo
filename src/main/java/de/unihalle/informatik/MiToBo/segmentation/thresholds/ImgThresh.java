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

package de.unihalle.informatik.MiToBo.segmentation.thresholds;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * 
 * Image thresholding operator with lower threshold (<code>threshold</code> and upper threshold
 * (<code>upperThreshold</code>.
 * <p>
 * All pixels with <code>threshold < intensity <= upperThreshold </code> are
 * defined as foreground pixels.
 * 
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,level=Level.STANDARD)
public class ImgThresh extends MTBOperator {
	
	@Parameter( label= "Lower Threshold", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 2, description = "Lower threshold, all pixels with intensity less this lower threshold are background pixels")
	private MTBDoubleData threshold = null;

	@Parameter( label= "Upper Threshold", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 2, description = "Upper threshold, all pixels with intensity greater equal this upper threshold are background pixels")
	private MTBDoubleData upperThreshold = new MTBDoubleData( Double.POSITIVE_INFINITY);

	@Parameter( label= "Current slice only", required = false, direction = Direction.IN,
				mode = ExpertMode.STANDARD, dataIOOrder = 5, description = "Threshold only the current slice")
	private boolean actualSliceOnly = false;

	@Parameter( label= "FG-value", required = true, direction = Direction.IN,
				mode = ExpertMode.STANDARD, dataIOOrder = 3, 
				description = "Gray value for foreground pixel). If value is INFINITY then the original pixel values are used.")
	private double fgValue = 255.0;

	@Parameter( label= "BG-value", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 4, 
			description = "Gray value for background pixels. If value is INFINITY then the original pixel values are used.")
	private double bgValue = 0.0;

	@Parameter( label= "Input image", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 1, 
			description = "Input image")
	private transient MTBImage inputImage = null;

	@Parameter( label= "Destination image", required = false, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 6, 
			description = "Optional destination image to draw to. If ommited a new image is created.")
	private transient MTBImage destinationImage = null;

	@Parameter( label= "Result image", required = true, direction = Direction.OUT,
			mode = ExpertMode.STANDARD, dataIOOrder = 1, 
			description = "Result image")
	private transient MTBImage resultImage = null;

	
	/**
	 * Constructor for thresholding using 255 as foreground and 0 as background value.
	 * The result image will be of type MTB_BYTE
	 * @param img input image
	 * @param thres values < threshold are set to 0, value >= threshold are set to 255
	 */
	public ImgThresh(MTBImage img, double thres) throws ALDOperatorException {
	
		this.inputImage = img;
		this.threshold = new MTBDoubleData(new Double(thres));
		this.fgValue = 255.0;
		this.bgValue = 0.0;
		this.actualSliceOnly = false;
	}

	/**
	 * 
	 * Constructor for thresholding using 'fgValue' as foreground and 'bgValue' as background value.
	 * The result image will have the same type as the input image.
	 * @param img input image
	 * @param thres values < threshold are set to 'fgValue', value >= threshold are set to 'bgValue'
	 * @param fg foreground value (POSITIVE_INFINITY is interpreted as use original value)
	 * @param bg background value (POSITIVE_INFINITY is interpreted as use original value)
	 */
	public ImgThresh(MTBImage img, double thres, double fg, double bg) throws ALDOperatorException {
		
		this.inputImage = img;
		this.threshold = new MTBDoubleData(new Double(thres));
		this.fgValue = fg;
		this.bgValue = bg;
		this.actualSliceOnly = false;
	}
	
	
	public ImgThresh() throws ALDOperatorException {
	}  
  

	@Override
	protected void operate() throws ALDOperatorException {
	
		
		MTBImage img = getInputImage();
		double thresh = getThreshold().doubleValue();
		boolean actSliceOnly = getActualSliceOnly();
		
		double fgVal, bgVal;
		
		if (isFGOriginalValue()) {
			fgVal = Double.POSITIVE_INFINITY;
		}
		else {
			fgVal = getFGValue();
		}
		
		if (isBGOriginalValue()) {
			bgVal = Double.POSITIVE_INFINITY;
		}
		else {
			bgVal = getBGValue();
		}
		
		
		MTBImage resultImg = getDestinationImage();
		
		if (resultImg == null) {
			resultImg = this.threshold(img, thresh, upperThreshold.getValue(), fgVal, bgVal, actSliceOnly);
		}
		else {
			try {
				this.threshold(img, resultImg, thresh, upperThreshold.getValue(), fgVal, bgVal, actSliceOnly);
			} catch(IllegalArgumentException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImgThresh.operate() failed: " + e.getMessage());
			}
		}
		// set title
		resultImg.setTitle("Thresholding result for image \"" 
																										+ img.getTitle() + "\"");
		setResultImage(resultImg);
	}
	
	
	/**
	 * Apply threshold to an MTBImage and create a new thresholded MTBImage. Specify values for foreground and background value.
	 * If fgValue/bgValue is Double.POSITIVE_INFINITY the original image values are used for foreground/background pixels.
	 * If fgValue == 255 and bgValue == 0 and !actualSliceOnly, the result image will be of type MTB_BYTE
	 * @param img source MTBImage
	 * @param thresh 			Lower threshold.
	 * @param upperThres Uppper threshold.
	 * @param _fgValue Foreground pixel value. If set to Double.POSITIVE_INFINITY, pixels higher than/equal to the threshold are set to their original values.
	 * @param _bgValue Background pixel value. If set to Double.POSITIVE_INFINITY, pixels lower to the threshold are set to their original values.
	 * @param _actualSliceOnly flag for only thresholding actual slice (true) or whole image (false)
	 * @return new thresholded MTBImage object
	 */
	protected MTBImage threshold(MTBImage img, double thresh, double upperThres, double _fgValue, 
			double _bgValue, boolean _actualSliceOnly) {
		int width = img.getSizeX();
		int height = img.getSizeY();
		int stacksize = img.getSizeStack();
		int actSlice = img.getCurrentSliceIndex();
			
		MTBImage threshImg;
		if (_fgValue == 255.0 && _bgValue == 0.0 && !_actualSliceOnly) {
			threshImg = MTBImage.createMTBImage(width, height, img.getSizeZ(), 
											img.getSizeT(), img.getSizeC(), MTBImageType.MTB_BYTE);
		}
		else {
			threshImg = MTBImage.createMTBImage(width, height, img.getSizeZ(), 
											img.getSizeT(), img.getSizeC(), img.getType());
		}
		
		threshImg.adoptSliceLabels(img);
		threshImg.copyPhysicalProperties(img);
			
		double val;
		int i = 0;
		
		if (_actualSliceOnly) {
			i = actSlice;
			stacksize = i+1;
		}
		
		while (i < stacksize) {
			img.setCurrentSliceIndex(i);
			threshImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					
					val = img.getValueDouble(x, y);
					
					// here the '>' is required to be compatible with our Otsu 
					// implementation...
					if (val > thresh && val <= upperThres ) {
						if (Double.isInfinite(_fgValue)) {
							threshImg.putValueDouble(x, y, val);
						}
						else {
							threshImg.putValueDouble(x, y, _fgValue);
						}
					}
					else {
						if (Double.isInfinite(_bgValue)) {
							threshImg.putValueDouble(x, y, val);
						}
						else {
							threshImg.putValueDouble(x, y, _bgValue);
						}
					}
				}
			}
			
			i++;
		}
		
		img.setCurrentSliceIndex(actSlice);
		threshImg.setCurrentSliceIndex(actSlice);
		
		return threshImg;
	}
	
	/**
	 * Apply threshold to an MTBImage and write results to a destination MTBImage. Specify values for foreground and background value.
	 * If fgValue/bgValue is Double.POSITIVE_INFINITY the original image values are used for foreground/background pixels.
	 * @param img source MTBImage
	 * @param threshImg destination image to write the thresholded result to
	 * @param thresh Threshold
	 * @param fgValue Foreground pixel value. If set to Double.POSITIVE_INFINITY, pixels higher than/equal to the threshold are set to their original values.
	 * @param bgValue Background pixel value. If set to Double.POSITIVE_INFINITY, pixels lower to the threshold are set to their original values.
	 * @param actualSliceOnly flag for only thresholding actual slice (true) or whole image (false)
	 */
	@SuppressWarnings("hiding")
  protected void threshold(MTBImage img, MTBImage threshImg, double thresh, double upperThresh, 
			double fgValue, double bgValue, boolean actualSliceOnly) 
					throws IllegalArgumentException {
		int width = img.getSizeX();
		int height = img.getSizeY();
		int stacksize = img.getSizeStack();
		int actSlice = threshImg.getCurrentSliceIndex();
		
		
		if (threshImg != null) {
			if (!img.equalSize(threshImg)) {
				throw new IllegalArgumentException("Input image and thresholded destination image must have same size.");
			}
		}
		else {
			throw new IllegalArgumentException("Thresholded destination image may not be null.");
		}
			
		double val;
		int i = 0;
		
		if (actualSliceOnly) {
			i = actSlice;
			stacksize = i+1;
		}
		
		while (i < stacksize) {
			img.setCurrentSliceIndex(i);
			threshImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					
					val = img.getValueDouble(x, y);
					
					// here the '>' is required to be compatible with our Otsu 
					// implementation...
					if (val > thresh && val <= upperThresh ) {
						if (Double.isInfinite(fgValue)) {
							threshImg.putValueDouble(x, y, val);
						}
						else {
							threshImg.putValueDouble(x, y, fgValue);
						}
					}
					else {
						if (Double.isInfinite(bgValue)) {
							threshImg.putValueDouble(x, y, val);
						}
						else {
							threshImg.putValueDouble(x, y, bgValue);
						}
					}
				}
			}
			
			i++;
		}
		
		img.setCurrentSliceIndex(actSlice);
		threshImg.setCurrentSliceIndex(actSlice);
		threshImg.updateAndRepaintWindow();
	}
	
		
	/**
	 * Get input image
	 * @return input image
	 */
	public MTBImage getInputImage() {
		return this.inputImage;
	}
	
	/**
	 * Set input image
	 */
	public void setInputImage(MTBImage img) {
		this.inputImage = img;
	}
	
	/**
	 * Get threshold. Might be null if no threshold was specified, but this will cause an exception on runtime
	 * @return threshold
	 */
	public Double getThreshold() {
		return this.threshold.getValue();
	}
	
	/**
	 * Set threshold
	 */
	public void setThreshold(double thres) {
		this.threshold = new MTBDoubleData(new Double(thres));
	}
	
	/**
	 * Get foreground value 
	 * @return foreground value
	 */
	public double getFGValue() {
		return this.fgValue;
	}
	
	/**
	 * Get background value 
	 * @return background value
	 */
	public double getBGValue() {
		return this.bgValue;
	}
	
	/**
	 * Test if foreground pixels keep their original pixel values
	 * @return true if foreground pixels keep their original pixel values
	 */
	public boolean isFGOriginalValue() {
		return getFGValue() == Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Test if background pixels keep their original pixel values
	 * @return true if background pixels keep their original pixel values
	 */
	public boolean isBGOriginalValue() {
		return getBGValue() == Double.POSITIVE_INFINITY;
	}
	
	/**
	 * Tell the thresholder to use the original pixel value for foreground pixels
	 */
	public void setFGOriginalValue() {
		this.setFGValue( Double.POSITIVE_INFINITY);
	}
	
	/**
	 * Tell the thresholder to use the original pixel value for background pixels
	 */
	public void setBGOriginalValue() {
		this.setBGValue( Double.POSITIVE_INFINITY);
	}
	
	/**
	 * Get flag for thresholding only the actual slice (true) or the whole image (false)
	 */
	public boolean getActualSliceOnly() {
		return this.actualSliceOnly;
	}
	
	/**
	 * Only actual slice is thresholded, this will force the output image type to be of the same type as the input image (or destination image type)
	 */
	public void setActualSliceOnly() {
		this.actualSliceOnly = true;
	}
	
	/**
	 * The whole image is thresholded (default)
	 */
	public void unsetActualSliceOnly() {
		this.actualSliceOnly = false;
	}
	
	/**
	 * Get the result image
	 * @return result image
	 */
	public MTBImage getResultImage() {
		return this.resultImage;
	}
	
	/**
	 * Set result image. You may specify a destination image to which the thresholding results are written. Otherwise the operator sets a new resulting image.
	 * @param img result image
	 */
	protected void setResultImage(MTBImage img) {
		this.resultImage = img;
	}

	/** Set value of Parameter argument FGValue.
	  * @param value New value for FGValue
	  */
	public void setFGValue(double value ) {
         this.fgValue = value;
	}

	/** Set value of Parameter argument BGValue.
	  * @param value New value for BGValue
	  */
	public void setBGValue(double value ) {
         this.bgValue = value;
	}
	
	/** Get value of Input argument DestinationImage.
	  * @return value of DestinationImage
	  */
	public MTBImage getDestinationImage() {
		return this.destinationImage;
	}
	
	/** Set value of Input argument DestinationImage.
	  * @param img destination image
	  */
	public void setDestinationImage(MTBImage img ) {
		this.destinationImage = img;
	}

}

