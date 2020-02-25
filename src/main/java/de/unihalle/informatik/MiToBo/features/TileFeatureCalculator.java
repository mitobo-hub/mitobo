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

package de.unihalle.informatik.MiToBo.features;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageTileAdapter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasuresResult;

/**
 * Calculates features on images tile-wise, based on operators extending
 * {@link FeatureCalculator}.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.APPLICATION, allowBatchMode=false,
	shortDescription="Calculates features tile-wise on images for given feature extractors.")
public class TileFeatureCalculator extends MTBOperator
{
	/**
	 * Input image to process.
	 */
	@Parameter(label = "Input image", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Input image to analyze.", dataIOOrder = -10)
	private transient MTBImage inImg = null;
	
	/**
	 * Size of tiles in x direction.
	 */
	@Parameter(label = "Tile size in x", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Size of image tiles to analyze in x.", 
		dataIOOrder = -9)
	private int tileSizeX = 16;
	
	/**
	 * Size of tiles in y direction.
	 */
	@Parameter(label = "Tile size in y", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Size of image tiles to analyze in y.", 
		dataIOOrder = -8)
	private int tileSizeY = 16;

	/**
	 * Shift of tiles in x direction.
	 */
	@Parameter(label = "Tile shift in x", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Shift of image tiles to analyze in x.", 
		dataIOOrder = -7)
	private int tileShiftX = 16;
	
	/**
	 * Shift of tiles in y direction.
	 */
	@Parameter(label = "Tile shift in y", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Shift of image tiles to analyze in y.", 
		dataIOOrder = -6)
	private int tileShiftY = 16;

	/**
	 * List of feature calculators to apply.
	 */
	@Parameter(label = "Feature calculators", required = true, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "List of feature calculators to apply.", 
		dataIOOrder = -5)
	private Vector<FeatureCalculator> featureOps = null;
	
	/**
	 * Mask to exclude image regions and tiles, respectively.
	 * <p>
	 * Tiles containing at least one masked pixel (value = 0) are ignored.
	 */
	@Parameter(label = "Exclude mask", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Mask to exclude image regions.", dataIOOrder = 0)
	private transient MTBImage mask = null;

	/**
	 * Result of the calculation.
	 */
	@Parameter(label = "Result data object", 
		direction = Parameter.Direction.OUT, 
		description = "Calculated texture measures.")
	private transient TileFeatureCalculatorResult resultData = null;
	
	/**
	 * Optional stack of result visualizations, 
	 * if result data suppports that.
	 */
	@Parameter(label = "Result image", direction = Parameter.Direction.OUT, 
			description = "Optional result image stack.")
	private transient MTBImage resultImage;

	/**
	 * Vector for collecting result images.
	 */
	private transient Vector<MTBImage> resultImages;
	
	/**
	 * Number of tiles in x dimension.
	 */
	private transient int tileNumX;
	
	/**
	 * Number of tiles in y dimension.
	 */
	private transient int tileNumY;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure. 
	 */
	public TileFeatureCalculator() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor with non-default tile sizes.
	 * @param _tileSizeX	Size of one tile in x direction.
	 * @param _tileSizeY Size of one tile in y direction.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public TileFeatureCalculator(int _tileSizeX, int _tileSizeY) 
			throws ALDOperatorException {
		this(_tileSizeX, _tileSizeY, _tileSizeX, _tileSizeY);
	}

	/**
	 * Constructor with non-default tile sizes.
	 * @param _tileSizeX	Size of one tile in x direction.
	 * @param _tileSizeY  Size of one tile in y direction.
	 * @param _tileShiftX Shift between successive tiles in x direction.
	 * @param _tileShiftY Shift between successive tiles in y direction.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public TileFeatureCalculator(int _tileSizeX, int _tileSizeY,
			int _tileShiftX, int _tileShiftY) 
			throws ALDOperatorException {
		this.tileSizeX = _tileSizeX;
		this.tileSizeY = _tileSizeY;
		this.tileShiftX = _tileShiftX;
		this.tileShiftY = _tileShiftY;
	}

	@Override
	public void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		// allocate result data object
		this.resultData = new TileFeatureCalculatorResult();
		this.resultImages = null;
		this.resultImages = new Vector<MTBImage>();

		// tile the input image
		MTBImageTileAdapter tileAdapter = 
			new MTBImageTileAdapter(this.inImg, this.tileSizeX, this.tileSizeY,
				this.tileShiftX, this.tileShiftY);
		this.tileNumX = tileAdapter.getTileCols();
		this.tileNumY = tileAdapter.getTileRows();
		int tileNum = this.tileNumX * this.tileNumY;
		MTBImageTileAdapter tileAdapterMask = null;
		if (this.mask != null) 	
			tileAdapterMask = new MTBImageTileAdapter(this.mask, 
				this.tileSizeX,this.tileSizeY,this.tileShiftX,this.tileShiftY);
		
		// iterate over all operators and tiles and collect result data
		int featureDim = -1;
		boolean[] tileValid = new boolean[tileNum];
		for (int i=0; i<tileValid.length; ++i)
			tileValid[i] = true;
		for (FeatureCalculator calcOp: this.featureOps) {
			FeatureCalculatorResult[] opData = 
					new FeatureCalculatorResult[tileNum];
			int i=0;
			int x = 0, y = 0;
			for (MTBImage imageTile: tileAdapter) {
				boolean invalid = false;
				MTBImage maskTile;
				if (tileAdapterMask != null) {
					maskTile = tileAdapterMask.getTile(x,y);
					for (int yy= 0; !invalid && yy<maskTile.getSizeY(); ++yy) {
						for (int xx= 0; !invalid && xx<maskTile.getSizeX(); ++xx) {
							if (maskTile.getValueInt(xx, yy) == 0) {
								invalid = true;
							}
						}
					}
				}
				if (!invalid) {
					calcOp.setInputImage(imageTile);
					calcOp.runOp(HidingMode.HIDDEN);
					// check if result data contains NaN values
					opData[i] = calcOp.getResultData();
					featureDim = opData[i].getDimensionality();
					for (int e = 0; e < opData[i].getDimensionality(); ++e) {
						if (Double.isNaN(opData[i].getNumericalValue(e))) {
							opData[i] = null;
							tileValid[i] = false;
							break;
						}
					}
				}
				else {
					// result is invalid
					opData[i] = null;
					tileValid[i] = false;
				}
				++i;
				++x;
				if (x == this.tileNumX) {
					++y;
					x = 0;
				}
			}
			// check which tiles were invalid and set result to NaN
			if (featureDim == -1)
				// no calculations took place, error!
				throw new ALDOperatorException(
						OperatorExceptionType.OPERATE_FAILED,
						"[TileFeatureCalculator] no valid tiles in image, " 
								+ "all masked?!");

			for (i=0; i<tileNum; ++i)
				if (opData[i] == null)
					opData[i] = calcOp.getResultDataObjectInvalid(featureDim);

			this.resultData.addResult(opData);
			if (opData[0].isConvertableToNumericalData())
				this.addResultImages(opData);
		}
		// prepare image stack
		if (this.resultImages.size() > 0) {
			this.prepareImageStack();
		}
		// count invalid tiles
		int invalidCounter = 0;
		for (int i=0; i<tileValid.length; ++i)
			if (!tileValid[i]) 
				++invalidCounter;
		this.resultData.setInvalidTilesNum(invalidCounter);
	}
	
	/**
	 * Generates a visual representation of the tile data and stores it.
	 * @param data		Result data.
	 */
	private void addResultImages(FeatureCalculatorResult[] data) {
		int featCount = data[0].getDimensionality();
		String opID = data[0].getOpIdentifier();
		for (int d=0;d<featCount; ++d) {
			MTBImageDouble tmpImage = (MTBImageDouble)MTBImage.createMTBImage(
					this.tileNumX, this.tileNumY, 1, 1, 1,MTBImageType.MTB_DOUBLE);
			String title = opID + "_" + data[0].getResultIdentifier(d);
			tmpImage.setTitle(title);
			int i=0;
			for (int y=0; y<this.tileNumY; ++y) {
				for (int x=0; x<this.tileNumX; ++x) {
					tmpImage.putValueDouble(x, y, data[i].getNumericalValue(d));
					++i;
				}
			}
		this.resultImages.add(tmpImage);
		}
	}
	
	/**
	 * Puts all result images into a stack.
	 */
	private void prepareImageStack() {
		this.resultImage = MTBImage.createMTBImage(
				this.tileNumX, this.tileNumY,1,1,this.resultImages.size(),
  			MTBImageType.MTB_DOUBLE);
		// generate stack
		int slice = 0;
  	for (MTBImage img: this.resultImages) {
  		this.resultImage.setSlice(img, 0, 0, slice);
  		this.resultImage.setSliceLabel(img.getTitle(), 0, 0, slice);
  		++slice;
  	}
	}
	
	/**
	 * Set input image.
	 * @param img		Image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Set optional exclude mask.
	 * @param exMask		Exclude mask.
	 */
	public void setMask(MTBImage exMask) {
		this.mask = exMask;
	}

	/**
	 * Set feature operators.
	 * @param ops		Feature operators to apply.
	 */
	public void setFeatureOperators(Vector<FeatureCalculator> ops) {
		this.featureOps = ops;
	}
	
	/**
	 * Get result data.
	 * @return	Calculated features.
	 */
	public TileFeatureCalculatorResult getResult() {
		return this.resultData;
	}

	/**
	 * Get image with result data.
	 * @return	Image with calculated feature data.
	 */
	public MTBImage getResultImage() {
		return this.resultImage;
	}
	
	/**
	 * Get number of tiles in x-direction.
	 * @return	Number of tiles.
	 */
	public int getTileCountX() {
		return this.tileNumX;
	}

	/**
	 * Get number of tiles in x-direction.
	 * @return	Number of tiles.
	 */
	public int getTileCountY() {
		return this.tileNumY;
	}
	
	public String getDocumentation() {
		return "This operator extracts features from the given image. The features are\r\n" + 
				"calculated tile-wise on the given image. If the specified shift between \r\n" + 
				"subsequent tiles is smaller than the given tile size in either of the two \r\n" + 
				"dimensions, the tiles are overlapping. Contrary, if the shift is larger \r\n" + 
				"than the tile size gaps result between tiles. The features to be \r\n" + 
				"calculated are specified via the set of feature calculators.\r\n" + 
				" \r\n" + 
				"<ul>\r\n" + 
				"<li><p><b>input:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Input image</i>: the (gray-scale) image to analyze</p></li>\r\n" + 
				"<li><p><i>Tile size in x/y</i>: dimensions of individual tiles</p></li>\r\n" + 
				"<li><p><i>Tile shift in x/y</i>: shift between subsequent tiles</p></li>\r\n" + 
				"<li><p><i>Feature calculators</i>: set of features to be extracted</p></li>\r\n" + 
				"<li><p><i>Exclude mask</i>: optional mask to ignore image areas</p></li>\r\n" + 
				"</ul>\r\n" + 
				"</p>\r\n" + 
				"</li>\r\n" + 
				"<li><p><b>output:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Result data object</i>: object containing result data</p></li>\r\n" + 
				"<li><p><i>Result image</i>: visualization of feature measures, if \r\n" + 
				"	supported by selected feature operators (if not, image will be null)</p></li>\r\n" + 
				"</ul>\r\n" + 
				"</p>\r\n" + 
				"</li>\r\n" + 
				"</ul>\r\n";
	}
}
