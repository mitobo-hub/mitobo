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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.*;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentation2D;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentation3D;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

import java.util.*;

/**
 * Operator for initializing objects of type {@link MTBSegmentationInterface}.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL,level=Level.STANDARD)
public class SegmentationInitializer extends MTBOperator {

	/**
	 * Available dimensions for segmentations.
	 */
	public static enum SegmentationDimension {
		/**
		 * Initialize an object of type {@link MTBSegmentation2D}.
		 */
		DIM_2,
		/**
		 * Initialize an object of type {@link MTBSegmentation3D}.
		 */
		DIM_3,
		/**
		 * The type of the initialized objects depends on dimensionality of data.
		 */
		DIM_DATA_DEPENDENT
	}
	
	/**
	 * Available shape types.
	 */
	public static enum ShapeType {
		/**
		 * Elliptical shape.
		 */
		ELLIPSOID,
		/**
		 * Cuboid shape.
		 */
		CUBOID
	}
	
	/**
	 * Available data sources.
	 */
	public static enum InputMode {
		/**
		 * A label image is used for initialization.
		 * <p>
		 * For each label a class is initialized, pixels with a label of zero
		 * are assigned to the background class.
		 */
		MODE_LABEL_IMAGE,
		/**
		 * Two labels are initialized. i.e. a foreground and a background class.
		 * <p>
		 * Pixels with intensity values larger than zero are assigned to the 
		 * foreground class, the others to the background.
		 */
		MODE_BINARY_IMAGE,
		/**
		 * Given a region set a label image is generated which is then used 
		 * for initialization (see MODE_LABEL_IMAGE).
		 */
		MODE_REGION_SET,
		/**
		 * An input image is expected which is thresholded with the given value.
		 * <p>
		 * From the resulting binary image subsequently two classes are 
		 * initialized (see MODE_BINARY_IMAGE).
		 */
		MODE_THRESHOLD,
		/**
		 * A binary mask is generated according to the shape specification.
		 * <p>
		 * The interior of the shape is defined to be foreground, the exterior 
		 * to be background.
		 */
		MODE_SHAPE
	}

  @Parameter(label = "Input Mode", required = true, dataIOOrder = 0, 
  		direction = Parameter.Direction.IN, 
  		description = "Source from where to read the data.")
	protected InputMode inMode = InputMode.MODE_BINARY_IMAGE;
	
  @Parameter(label = "Dimension", required = true, dataIOOrder = 1, 
  		direction = Parameter.Direction.IN, 
  		description = "Dimension of generated segmentation object.")
	protected SegmentationDimension segDim = SegmentationDimension.DIM_2;

  @Parameter(label = "Binary Mode Image", required = false, dataIOOrder = 10, 
  		direction = Parameter.Direction.IN, 
  		description = "Image for two-class segmentation.")
	protected transient MTBImage binaryImage = null;
	
  @Parameter(label = "Label Mode Image", required = false, dataIOOrder = 11, 
  		direction = Parameter.Direction.IN, 
  		description = "Label image for n-class segmentation.")
	protected transient MTBImage labelImage = null;
	
  @Parameter(label = "Region Set", required = false, dataIOOrder = 12, 
  		direction = Parameter.Direction.IN, 
  		description = "Region set for for two- or n-class segmentation.")
	protected MTBRegion2DSet regionSet = null;
	
  @Parameter(label = "Region Mode", required = false, dataIOOrder = 13, 
  		direction = Parameter.Direction.IN, 
  		description = "Assign all regions to the common foreground.")
	protected boolean regionBinaryMode = false;
	
  @Parameter(label = "Threshold Image", required = false, dataIOOrder = 14, 
  		direction = Parameter.Direction.IN, 
  		description = "Input image for thresholding.")
	protected transient MTBImage thresholdImage = null;
		
  @Parameter(label = "Threshold", required = false, dataIOOrder = 15, 
  		direction = Parameter.Direction.IN, 
  		description = "Threshold to be applied to threshold image.")
	protected double threshold;
	
  @Parameter(label = "Shape", required = false, dataIOOrder = 16, 
  		direction = Parameter.Direction.IN, 
  		description = "Type of sythetic shape.")
	protected ShapeType shape = ShapeType.CUBOID;
	
  @Parameter(label = "Shape Mask Size X", required = false, dataIOOrder = 17, 
  		direction = Parameter.Direction.IN, 
  		description = "Size of mask in x dimension.")
	protected int shapeMaskSizeX;
	
  @Parameter(label = "Shape Mask Size Y", required = false, dataIOOrder = 18, 
  		direction = Parameter.Direction.IN, 
  		description = "Size of mask in y dimension.")
	protected int shapeMaskSizeY;

  @Parameter(label = "Shape Mask Size Z", required = false, dataIOOrder = 19, 
  		direction = Parameter.Direction.IN, 
  		description = "Size of mask in z dimension.")
	protected int shapeMaskSizeZ;
	
  @Parameter(label = "Shape position X", required = false, dataIOOrder = 20, 
  		direction = Parameter.Direction.IN, 
  		description = "Shape position in x dimension.")
	protected double shapePosX;
	
  @Parameter(label = "Shape position Y", required = false, dataIOOrder = 21, 
  		direction = Parameter.Direction.IN, 
  		description = "Shape position in y dimension.")
	protected double shapePosY;

  @Parameter(label = "Shape position Z", required = false, dataIOOrder = 22, 
  		direction = Parameter.Direction.IN, 
  		description = "Shape position in z dimension.")
	protected double shapePosZ;

  @Parameter(label = "Shape dimension X", required = false, dataIOOrder = 23, 
  		direction = Parameter.Direction.IN, 
  		description = "Extent of shape in x dimension.")
	protected double shapeDimX;
	
  @Parameter(label = "Shape dimension Y", required = false, dataIOOrder = 24, 
  		direction = Parameter.Direction.IN, 
  		description = "Extent of shape in y dimension.")
	protected double shapeDimY;

  @Parameter(label = "Shape dimension Z", required = false, dataIOOrder = 25, 
  		direction = Parameter.Direction.IN, 
  		description = "Extent of shape in z dimension.")
	protected double shapeDimZ;

  @Parameter(label = "Generated segmentation", 
  		direction = Parameter.Direction.OUT, description = "Segmentation.")
	protected transient MTBSegmentationInterface segObject;

  /**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public SegmentationInitializer() throws ALDOperatorException {
	  super();
  }

	@SuppressWarnings("unused")
  @Override
  protected void operate() 
  	throws ALDOperatorException, ALDProcessingDAGException {
		this.generateSegmentation();	  
  }
	
	@Override
  public void validateCustom() throws ALDOperatorException {
		boolean failure = false;
		switch (this.inMode)
		{
		case MODE_LABEL_IMAGE:
			if (this.labelImage == null)
				failure= true;
			break;
		case MODE_BINARY_IMAGE:
			if (this.binaryImage == null)
				failure= true;
			break;
		case MODE_REGION_SET:
			if (this.regionSet == null)
				failure= true;
			break;
		case MODE_THRESHOLD:
			if (this.thresholdImage == null)
				failure= true;
			break;
		case MODE_SHAPE:
			break;
		}
		if (failure)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[SegmentationInitializer] not all data available for selected mode " + 
					this.inMode + ", check your parameters!");
	}

	/**
	 * Get target dimension.
	 * @return	 	Target dimension.
	 */
	public SegmentationDimension getSegDim() {
		return this.segDim;
	}

	/**
	 * Set target dimension.
	 * @param _segDim 	Target dimension to be used.
	 */
	public void setSegDim(SegmentationDimension _segDim) {
		this.segDim = _segDim;
	}

	/**
	 * Get input mode.
	 * @return 	Input mode.
	 */
	public InputMode getInputMode() {
		return this.inMode;
	}

	/**
	 * Set input mode.
	 * @param _mode 	Input mode to be used.
	 */
	public void setInputMode(InputMode _mode) {
		this.inMode = _mode;
	}

	/**
	 * Get the image for binary mode.
	 * @return Binary mode image.
	 */
	public MTBImage getBinaryImage() {
		return this.binaryImage;
	}

	/**
	 * Set the binary image.
	 * @param _binaryImage 	Binary image for mask initialization.
	 */
	public void setBinaryImage(MTBImage _binaryImage) {
		this.binaryImage = _binaryImage;
	}

	/**
	 * Get the label image.
	 * @return Label image.
	 */
	public MTBImage getLabelImage() {
		return this.labelImage;
	}

	/**
	 * Set the label image.
	 * @param _labelImage Label image for n-class initialization.
	 */
	public void setLabelImage(MTBImage _labelImage) {
		this.labelImage = _labelImage;
	}

	/**
	 * Get the region set.
	 * @return Region set.
	 */
	public MTBRegion2DSet getRegionSet() {
		return this.regionSet;
	}

	/**
	 * Set the region set.
	 * @param _regionSet 	Set for 2-/n-class initialization.
	 */
	public void setRegionSet(MTBRegion2DSet _regionSet) {
		this.regionSet = _regionSet;
	}

	/**
	 * Check for region input mode.
	 * @return True if binary mode is selected.
	 */
	public boolean isRegionBinaryMode() {
		return this.regionBinaryMode;
	}

	/**
	 * Specify region input mode.
	 * @param _regionBinaryMode Flag for region input mode.
	 */
	public void setRegionBinaryMode(boolean _regionBinaryMode) {
		this.regionBinaryMode = _regionBinaryMode;
	}

	/**
	 * Get image for thresholding
	 * @return Image to threshold.
	 */
	public MTBImage getThresholdImage() {
		return this.thresholdImage;
	}

	/**
	 * Set image for thresholding.
	 * @param _thresholdImage 	Image for thresholding.
	 */
	public void setThresholdImage(MTBImage _thresholdImage) {
		this.thresholdImage = _thresholdImage;
	}

	/**
	 * Get threshold to be applied to image.
	 * @return Threshold.
	 */
	public double getThreshold() {
		return this.threshold;
	}

	/**
	 * Set threshold for threshold mode.
	 * @param _threshold 	Threshold to be applied to threshold image.
	 */
	public void setThreshold(double _threshold) {
		this.threshold = _threshold;
	}

	/**
	 * Get the selected shape.
	 * @return Selected shape.
	 */
	public ShapeType getShape() {
		return this.shape;
	}

	/**
	 * Specify shape for synthetic initialization.
	 * @param _shape		Shape type to be used.
	 */
	public void setShape(ShapeType _shape) {
		this.shape = _shape;
	}

	/**
	 * Get size of shape mask in x dimension.
	 * @return Size of shape mask in x.
	 */
	public int getShapeMaskSizeX() {
		return this.shapeMaskSizeX;
	}

	/**
	 * Set size of shape mask in x dimension.
	 * @param _shapeMaskSizeX 	Size to be used.
	 */
	public void setShapeMaskSizeX(int _shapeMaskSizeX) {
		this.shapeMaskSizeX = _shapeMaskSizeX;
	}

	/**
	 * Get size of shape mask in y dimension.
	 * @return Size of shape mask in y.
	 */
	public int getShapeMaskSizeY() {
		return this.shapeMaskSizeY;
	}

	/**
	 * Set size of shape mask in y dimension.
	 * @param _shapeMaskSizeY 	Size to be used.
	 */
	public void setShapeMaskSizeY(int _shapeMaskSizeY) {
		this.shapeMaskSizeY = _shapeMaskSizeY;
	}

	/**
	 * Get size of shape mask in z dimension.
	 * @return Size of shape mask in z.
	 */
	public int getShapeMaskSizeZ() {
		return this.shapeMaskSizeZ;
	}

	/**
	 * Set size of shape mask in z dimension.
	 * @param _shapeMaskSizeZ 		Size to be used.
	 */
	public void setShapeMaskSizeZ(int _shapeMaskSizeZ) {
		this.shapeMaskSizeZ = _shapeMaskSizeZ;
	}

	/**
	 * Get position of shape in x dimension.
	 * @return Position in x.
	 */
	public double getShapePosX() {
		return this.shapePosX;
	}

	/**
	 * Set position of shape in x dimension.
	 * @param _shapePosX 	Position in x.
	 */
	public void setShapePosX(double _shapePosX) {
		this.shapePosX = _shapePosX;
	}

	/**
	 * Get position of shape in y dimension.
	 * @return Position in y.
	 */
	public double getShapePosY() {
		return this.shapePosY;
	}

	/**
	 * Set position of shape in y dimension.
	 * @param _shapePosY 	Position in y.
	 */
	public void setShapePosY(double _shapePosY) {
		this.shapePosY = _shapePosY;
	}

	/**
	 * Get position of shape in z dimension.
	 * @return Position in z.
	 */
	public double getShapePosZ() {
		return this.shapePosZ;
	}

	/**
	 * Set position of shape in z dimension.
	 * @param _shapePosZ 	Position in z.
	 */
	public void setShapePosZ(double _shapePosZ) {
		this.shapePosZ = _shapePosZ;
	}

	/**
	 * Get extent of shape in x dimension.
	 * @return Extent in x.
	 */
	public double getShapeDimX() {
		return this.shapeDimX;
	}

	/**
	 * Set extent of shape in x dimension.
	 * @param _shapeDimX 	Extent to be used.
	 */
	public void setShapeDimX(double _shapeDimX) {
		this.shapeDimX = _shapeDimX;
	}

	/**
	 * Get extent of shape in y dimension.
	 * @return Extent in y.
	 */
	public double getShapeDimY() {
		return this.shapeDimY;
	}

	/**
	 * Set extent of shape in y dimension.
	 * @param _shapeDimY	Extent to be used.
	 */
	public void setShapeDimY(double _shapeDimY) {
		this.shapeDimY = _shapeDimY;
	}

	/**
	 * Get extent of shape in z dimension.
	 * @return Extent in z.
	 */
	public double getShapeDimZ() {
		return this.shapeDimZ;
	}

	/**
	 * Set extent of shape in z dimension.
	 * @param _shapeDimZ	Extent to be used.
	 */
	public void setShapeDimZ(double _shapeDimZ) {
		this.shapeDimZ = _shapeDimZ;
	}
	
	/**
	 * Get the generated segmentation object.
	 * @return	Segmentation.
	 */
	public MTBSegmentationInterface getSegmentation() {
		return this.segObject;
	}

	/**
	 * Generates segmentation objects given specified data.
	 * @throws ALDOperatorException 
	 */
	protected void generateSegmentation() throws ALDOperatorException { 
		
		// process data according to selected mode
		switch (this.inMode)
		{
		case MODE_LABEL_IMAGE:
			if (this.segDim == SegmentationDimension.DIM_2) {
				this.segObject = this.get2DInterface(this.labelImage, false);
			}
			else if (this.segDim == SegmentationDimension.DIM_3) {
				this.segObject = this.get3DInterface(this.labelImage, false);
			}
			// general MTBSegmentation interface requested
			else {
				int imgDim = this.labelImage.getSizeZ();
				if (imgDim == 1) {
					this.segObject = this.get2DInterface(this.labelImage, false);
				}
				else {
					this.segObject = this.get3DInterface(this.labelImage, false);
				}
			}
			break;
		case MODE_BINARY_IMAGE:
			if (this.segDim == SegmentationDimension.DIM_2) {
				this.segObject = this.get2DInterface(this.binaryImage, true);
			}
			else if (this.segDim == SegmentationDimension.DIM_3) {
				this.segObject = this.get3DInterface(this.binaryImage, true);
			}
			// general MTBSegmentation interface requested
			else {
				int imgDim = this.binaryImage.getSizeZ();
				if (imgDim == 1) {
					this.segObject = this.get2DInterface(this.binaryImage, true);
				}
				else {
					this.segObject = this.get3DInterface(this.binaryImage, true);
				}
			}
			break;
		case MODE_REGION_SET:
			MTBSegmentationInterface seg = null;
			try {
				DrawRegion2DSet drawOp = 
						new DrawRegion2DSet(DrawType.LABEL_IMAGE, this.regionSet);
				drawOp.runOp(HidingMode.HIDDEN);
				if (   this.segDim == SegmentationDimension.DIM_DATA_DEPENDENT
						|| this.segDim == SegmentationDimension.DIM_2)
					seg = 
						this.get2DInterface(drawOp.getResultImage(),this.regionBinaryMode);
				else
					seg = 
						this.get3DInterface(drawOp.getResultImage(),this.regionBinaryMode);
			} catch (ALDException ex) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
						"[SegmentationInitializer] region mode: " + 
								"drawing regions failed!\n" + ex.getCommentString());
			}
			this.segObject = seg;
			break;
		case MODE_THRESHOLD:
			try {
				ImgThresh tOp = new ImgThresh(this.thresholdImage, this.threshold);
				tOp.runOp(HidingMode.HIDDEN);
				MTBImage tImg = tOp.getResultImage();
				if (this.segDim == SegmentationDimension.DIM_2)
					this.segObject = this.get2DInterface(tImg, true);
				else if (this.segDim == SegmentationDimension.DIM_3)
					this.segObject = this.get3DInterface(tImg, true);
				// no specific object type requested
				else {
					if (this.thresholdImage.getSizeZ() == 1)
						this.segObject = this.get2DInterface(tImg, true);
					else 
						this.segObject = this.get3DInterface(tImg, true);
				}
			} catch (ALDException e) {
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
						"[SegmentationInitializer] threshold mode: " + 
								"thresholding image failed!\n" + e.getCommentString());
			}
			break;
		case MODE_SHAPE:
			this.segObject = this.getSynthMask(this.segDim);
			break;
		default: 
			this.segObject = null;
			break;
		}
	}

	/**
	 * Generates 2D segmentation.
	 * @param labelImg			Label image to process.
	 * @param binaryMode		If true, two-class segmentation is generated.
	 * @return	Generated 2D segmentation object.
	 */
	protected MTBSegmentation2D get2DInterface(MTBImage labelImg, 
			boolean binaryMode) {
		int w = labelImg.getSizeX();
		int h = labelImg.getSizeY();

		int numClasses;
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		if (!binaryMode) {
			TreeSet<Integer> labels = new TreeSet<Integer>();
			for (int y=0;y<h;++y) { 
				for (int x=0;x<w;++x) {  
					labels.add(new Integer(labelImg.getValueInt(x, y)));
				}
			}
			numClasses = labels.size();
			Iterator<Integer> it = labels.descendingIterator();
			int id = numClasses-1;
			while (it.hasNext()) {
				Integer label = it.next();
				map.put(label, new Integer(id));
				--id;
			}
		}
		else {
			numClasses = 2;
		}
		int[][] cmap = new int[h][w];
		boolean[][] vmap = new boolean[h][w];
		double[][] wmap = new double[h][w];
		for (int y=0;y<h;++y) { 
			for (int x=0;x<w;++x) {
				if (!binaryMode)
					cmap[y][x] = 
					map.get(new Integer(labelImg.getValueInt(x, y))).intValue();
				else
					if (labelImg.getValueInt(x, y) > 0)
						cmap[y][x] = 1;
					else	
						cmap[y][x] = 0;
				vmap[y][x] = true;
				wmap[y][x] = 1.0;
			}
		}
		return new MTBSegmentation2D(w, h, numClasses,	cmap, vmap, wmap);
	}

	/**
	 * Generates 3D segmentation.
	 * @param labelImg			Label image to process.
	 * @param binaryMode		If true, two-class segmentation is generated.
	 * @return	Generated 3D segmentation object.
	 */
	protected MTBSegmentation3D get3DInterface(MTBImage labelImg, 
			boolean binaryMode) {
		int w = labelImg.getSizeX();
		int h = labelImg.getSizeY();
		int d = labelImg.getSizeZ();

		int numClasses;
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

		if (!binaryMode) {
			TreeSet<Integer> labels = new TreeSet<Integer>();
			for (int z=0;z<d;++z) {
				for (int y=0;y<h;++y) { 
					for (int x=0;x<w;++x) {  
						labels.add(new Integer(labelImg.getValueInt(x, y, z)));
					}
				}
			}
			numClasses = labels.size();
			Iterator<Integer> it = labels.descendingIterator();
			int id = numClasses-1;
			while (it.hasNext()) {
				Integer label = it.next();
				map.put(label, new Integer(id));
				--id;
			}
		}
		else {
			numClasses = 2;
		}
		int[][][] cmap = new int[d][h][w];
		boolean[][][] vmap = new boolean[d][h][w];
		double[][][] wmap = new double[d][h][w];
		for (int z=0;z<d;++z) { 
			for (int y=0;y<h;++y) { 
				for (int x=0;x<w;++x) {
					if (!binaryMode)
						cmap[z][y][x] = 
						map.get(new Integer(labelImg.getValueInt(x,y,z))).intValue();
					else
						if (labelImg.getValueInt(x, y, z) > 0)
							cmap[z][y][x] = 1;
						else	
							cmap[z][y][x] = 0;
					vmap[z][y][x] = true;
					wmap[z][y][x] = 1.0;
				}
			}
		}
		return new MTBSegmentation3D(w, h, d, numClasses, cmap, vmap, wmap);				
	}
	
  /**
   * Generate synthetic mask.
   * @return Generated segmentation mask.
   */
  protected MTBSegmentationInterface getSynthMask(SegmentationDimension sDim) {
  	
  	switch(sDim)
  	{
  	case DIM_2:
  		return this.get2DMask();
  	case DIM_3:
  		return this.get3DMask();
  	case DIM_DATA_DEPENDENT:
  		if (this.shapeMaskSizeZ == 0)
  			return this.get2DMask();
  		return this.get3DMask();
  	}
		return null;
  }
  	
  /**
   * Generates a synthetic 2D mask according to given specification.
   * @return	2D segmentation mask.
   */
  protected MTBSegmentation2D get2DMask() {
  	int width = this.shapeMaskSizeX;
  	int height = this.shapeMaskSizeY;
  	
  	double radiusX = this.shapeDimX/2.0;
  	double radiusY = this.shapeDimY/2.0;
  	
		int[][] cmap = new int[height][width];
		boolean[][] vmap = new boolean[height][width];
		double[][] wmap = new double[height][width];
		for (int y=0;y<height;++y) { 
			for (int x=0;x<width;++x) {
				vmap[y][x] = true;
				wmap[y][x] = 1.0;
			}
		}
				
		// ellipse
		if (this.shape == ShapeType.ELLIPSOID) {
  		for (int y=0;y<height;++y) { 
  			for (int x=0;x<width;++x) {
  				double xVal = 
  					((x - this.shapePosX)*(x - this.shapePosX)) / (radiusX * radiusX);
  				double yVal = 
        		((y - this.shapePosY)*(y - this.shapePosY)) / (radiusY * radiusY);
  				if (xVal + yVal > 1)
  					cmap[y][x] = 0;
  				else
  					cmap[y][x] = 1;
  			}
  		}  			
		}
		// cuboid
		else if (this.shape == ShapeType.CUBOID) {
  		for (int y=0;y<height;++y) { 
  			for (int x=0;x<width;++x) {
  				if (   Math.abs(x - this.shapePosX) <= radiusX
  						&& Math.abs(y - this.shapePosY) <= radiusY)
  					cmap[y][x] = 1;
  				else
  					cmap[y][x] = 0;    					
  			}
  		}
		}
		return 
 			new MTBSegmentation2D(width, height, 2,	cmap, vmap, wmap);
  }

  /**
   * Generates a synthetic 3D mask according to given specification.
   * @return	3D segmentation mask.
   */
  public MTBSegmentation3D get3DMask() {

  	int width = this.shapeMaskSizeX;
  	int height = this.shapeMaskSizeY;
  	int depth = this.shapeMaskSizeZ;
  	
  	double radiusX = this.shapeDimX/2.0;
  	double radiusY = this.shapeDimY/2.0;
  	double radiusZ = this.shapeDimZ/2.0;
  	
		int[][][] cmap = new int[depth][height][width];
		boolean[][][] vmap = new boolean[depth][height][width];
		double[][][] wmap = new double[depth][height][width];
		for (int z=0;z<depth;++z) { 
			for (int y=0;y<height;++y) { 
				for (int x=0;x<width;++x) {
					vmap[z][y][x] = true;
					wmap[z][y][x] = 1.0;
				}
			}
		}
		// ellipsoid
 		if (this.shape == ShapeType.ELLIPSOID) {
 			for (int z=0;z<depth;++z) {
 				for (int y=0;y<height;++y) { 
 					for (int x=0;x<width;++x) {
 	  				double xVal = 
 	  					((x - this.shapePosX)*(x - this.shapePosX)) / (radiusX*radiusX);
 	  				double yVal = 
 	        		((y - this.shapePosY)*(y - this.shapePosY)) / (radiusY*radiusY);
 	  				double zVal = 
 	 	        	((z - this.shapePosZ)*(z - this.shapePosZ)) / (radiusZ*radiusZ);
 	  				if (xVal + yVal + zVal > 1)
 	  					cmap[z][y][x] = 0;
 	  				else
 	  					cmap[z][y][x] = 1;
 					}
 				}  			
 			}
		}
 		// cuboid
		else if (this.shape == ShapeType.CUBOID) {
			for (int z=0;z<depth;++z) { 
				for (int y=0;y<height;++y) { 
					for (int x=0;x<width;++x) {
						if (   Math.abs(x - this.shapePosX) <= radiusX
								&& Math.abs(y - this.shapePosY) <= radiusY
								&& Math.abs(z - this.shapePosZ) <= radiusZ)
							cmap[z][y][x] = 1;
						else
							cmap[z][y][x] = 0;    					
					}
				}
			}
		}
		return 
 			new MTBSegmentation3D(width, height, depth, 2,	cmap, vmap, wmap);
  }
}
