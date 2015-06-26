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

package de.unihalle.informatik.MiToBo.segmentation.contours.extraction;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBDatatypeException;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.FillHoles2D;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.*;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Class to extract borders from connected components.
 * <p>
 * Contrary to contours, for example extracted by the operator 
 * {@link ContourOnLabeledComponents}, the borders extracted by this operator 
 * contain <i>unordered</i> sets of pixels, i.e. the border elements are neither
 * clockwise nor counter-clockwise sorted. The main advantage of this operator
 * is that it is faster than the counterpart, and very often there is no need
 * for ordered contours, but unordered contours are sufficient for the task 
 * at hand. 
 * <p>
 * The operator allows to extract outer borders, inner borders or both. If 
 * only inner borders are to be extracted a border set is returned which 
 * contains empty outer dummy borders which contain the inner borders. This 
 * way the inner borders are grouped according to the regions they belong to.
 * But, note that this behavior is different from the 
 * {@link ContourOnLabeledComponents} operator.
 * <p>
 * This operator takes a label image as input interpreting all pixels with 
 * values larger than zero as foreground pixels. Alternatively you can supply 
 * a region set. If both are given, the operator assumes that label image and 
 * region set are consistent.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.APPLICATION)
public class BordersOnLabeledComponents extends MTBOperator {

	/**
	 * Possible types of borders to be extracted.
	 */
	public static enum BorderType {
		/**
		 * Only outer borders will be segmented.
		 */
		OUTER_BORDERS,
		/**
		 * Only inner borders will be segmented.
		 */
		INNER_BORDERS,
		/**
		 * Outer as well as inner borders will be segmented.
		 */
		OUT_IN_BORDERS,
	}

	/**
	 * Type of borders to be extracted.
	 */
	@Parameter(label = "Border Type", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Border type.")
	private BorderType borderType = BorderType.OUT_IN_BORDERS;

	/**
	 * (Optional) input image where the borders should be extracted from.
	 */
	@Parameter(label = "Input Image", required = false, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image.")
	private transient MTBImage inputImage = null;

	/**
	 * (Optional) region set where the borders should be extracted from.
	 */
	@Parameter(label = "Input Regions", required = false, dataIOOrder = 1,
			direction = Parameter.Direction.IN, description = "Input regions.")
	private transient MTBRegion2DSet inputRegions = null;

	/**
	 * Connectivity within resulting borders.
	 * <p>
	 * Default is 8-neighborhood.
	 */
	@Parameter(label = "Desired connectivity of border pixels", required = false, 
			dataIOOrder = 2, direction = Parameter.Direction.IN, 
			description = "Desired connectivity of border pixels.")
	private BorderConnectivity connectivity = BorderConnectivity.CONNECTED_8;

	/**
	 * Lower threshold for border length.
	 * <p>
	 * Allows to exclude too short borders from the result set, result only
	 * contains borders with length greater or equal threshold.
	 */
	@Parameter(label = "Minimal Length", required = false, dataIOOrder = 3,
			direction = Parameter.Direction.IN, 
			description = "Minimum length of borders.")
	private int minimalBorderLength = 0;

	/**
	 * Image of extracted borders.
	 * <p>
	 * Outer borders are labeled sequentially, inner borders are marked gray.
	 */
	@Parameter(label = "Result Image", dataIOOrder = 0, 
			direction = Parameter.Direction.OUT, 
			description = "Result image with extracted borders.")
	private transient MTBImageShort resultImage = null;

	/**
	 * Set of extracted borders.
	 * <p>
	 * A border can include additional inner borders.
	 */
	@Parameter(label = "Result Borders", dataIOOrder = 1,
			direction = Parameter.Direction.OUT, 
			description = "Resulting set of borders.")
	private transient MTBBorder2DSet resultBorders = null;

	/**
	 * Image width.
	 */
	private transient int width;
	/**
	 * Image height.
	 */
	private transient int height;

	/**
	 * Standard constructor.
	 */
	public BordersOnLabeledComponents() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor.
	 * @throws ALDOperatorException
	 */
	public BordersOnLabeledComponents(MTBImage inImg,
			MTBRegion2DSet inRegions, BorderConnectivity con, 
			BorderType type, int minLength)
					throws ALDOperatorException {
		this.inputImage = inImg;
		this.inputRegions = inRegions;
		this.connectivity = con;
		this.borderType = type;
		this.minimalBorderLength = minLength;
	}

	/**
	 * Set input image.
	 * @param Image to process.
	 */
	public void setInputImage(MTBImage inImg) {
		this.inputImage = inImg;
	}

	/**
	 * Set input regions.
	 * @param Regions to process.
	 */
	public void setInputRegions(MTBRegion2DSet inRegions) {
		this.inputRegions = inRegions;
	}

	/**
	 * Specify connectivity for extracted border pixels. 
	 * @param bc	Connectivity.
	 */
	public void setConnectivity(BorderConnectivity bc) {
		this.connectivity = bc;
	}
	
	/**
	 * Set border type to be extracted.
	 * @param Type of borders to be extracted.
	 */
	public void setBorderType(BorderType type) {
		this.borderType = type;
	}

	/**
	 * Set the minimal border length.
	 * @param length	Minimal length of borders to be extracted.
	 */
	public void setMinimalBorderLength(int length) {
		this.minimalBorderLength = length;
	}

	/**
	 * Get result image with extracted borders.
	 * @return Image with borders.
	 */
	public MTBImageShort getResultImage() {
		return this.resultImage;
	}

	/**
	 * Get result borders.
	 * @return Set of extracted borders.
	 */
	public MTBBorder2DSet getResultBorders() {
		return this.resultBorders;
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.inputImage == null && this.inputRegions == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"[BordersOnLabeledComponents] No input data given!");
	}

	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		if (this.inputImage == null) {
			this.width = (int) Math.round(this.inputRegions.getXmax()+1);
			this.height = (int) Math.round(this.inputRegions.getYmax()+1);
		} else {
			this.width = this.inputImage.getSizeX();
			this.height = this.inputImage.getSizeY();
		}

		// extract the borders
		this.extractBorders();
		String title = "input data";
		if (this.inputImage != null)
			title = this.inputImage.getTitle();
		this.drawBordersToImage();
		this.resultImage.setTitle("Borders of \"" + title + "\"");
	}

	/**
	 * Extract requested borders for given input data.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	@SuppressWarnings("null")
	protected void extractBorders() 
			throws ALDOperatorException, ALDProcessingDAGException {

		try {
			this.resultBorders = 
					new MTBBorder2DSet(0, 0, this.width - 1, this.height - 1);

			MTBImage regImage = this.inputImage;
			
			if(this.inputImage == null) {
				DrawRegion2DSet drawRegs = new DrawRegion2DSet(DrawType.LABEL_IMAGE,
						this.inputRegions);
				drawRegs.runOp();
				regImage = drawRegs.getResultImage();
			}
			
			if (this.inputRegions == null) {
				LabelComponentsSequential labler = new LabelComponentsSequential();
				labler.setInputImage(this.inputImage);
				labler.runOp();
				this.inputRegions = labler.getResultingRegions();
				regImage = labler.getLabelImage();
			}

			MTBImageByte binImage = (MTBImageByte)MTBImage.createMTBImage(
					this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE);
			binImage.fillBlack();
			for (int y = 0; y < this.height; ++y) {
				for (int x = 0; x < this.width; ++x) {
					if (regImage.getValueInt(x, y) > 0)
						binImage.putValueInt(x, y, 255);
				}
			}
			// fill holes
			FillHoles2D filler = new FillHoles2D(binImage);
			filler.runOp();
			MTBImage filledImg = filler.getResultImage();

			HashMap<Integer, MTBBorder2D> outerBordersHash = null;
			HashMap<Integer, Vector<MTBBorder2D> > innerBordersHash = null;

			if (   this.borderType == BorderType.OUTER_BORDERS
					|| this.borderType == BorderType.OUT_IN_BORDERS) {

				outerBordersHash = new HashMap<Integer, MTBBorder2D>();
				for (int i = 0; i < this.inputRegions.size(); ++i) {
					MTBRegion2D reg = this.inputRegions.elementAt(i);
					MTBBorder2D border = new MTBBorder2D();
					border.setConnectivity(this.connectivity);

					Vector<Point2D.Double> points = reg.getPoints();
					int label = regImage.getValueInt((int)(points.elementAt(0).x+0.5),
							(int)(points.elementAt(0).y+0.5));
					for (Point2D.Double p : points) {
						int x = (int)(p.x+0.5);
						int y = (int)(p.y+0.5);
						for (int dy = -1 ; dy <= 1 ;++dy) {
							for (int dx = -1 ; dx <= 1 ;++dx) {
								// if 8-connectivity is requested, don't check diagonal 
								// neighbors; in case of 4-connectivity, check all 8 neighbors
								if (   this.connectivity == BorderConnectivity.CONNECTED_8
										&& ( dx == dy || dx == -dy || dy == -dx ) )
									continue;
								if (   x+dx < 0 || x+dx >= this.width 
										|| y+dy < 0 || y+dy >= this.height
										|| (     regImage.getValueInt(x+dx, y+dy) != label
										    && !(filledImg.getValueInt(x+dx, y+dy) > 0))) {
									border.addPixel(x, y);
									dx = 2; dy = 2;
								}
							}
						}
					}
					outerBordersHash.put(new Integer(label), border);
				}
			}

			// extract inner borders
			if (   this.borderType == BorderType.INNER_BORDERS
					|| this.borderType == BorderType.OUT_IN_BORDERS) {

				innerBordersHash = new HashMap< Integer, Vector<MTBBorder2D> >();

				for (int i = 0; i < this.inputRegions.size(); ++i) {
					MTBRegion2D reg = this.inputRegions.elementAt(i);
					MTBBorder2D border = new MTBBorder2D();

					Vector<Point2D.Double> points = reg.getPoints();
					Integer label = new Integer(regImage.getValueInt(
							(int)(points.elementAt(0).x+0.5),(int)(points.elementAt(0).y+0.5)));
					for (Point2D.Double p : points) {
						int x = (int)(p.x+0.5);
						int y = (int)(p.y+0.5);
						for (int dy = -1 ; dy <= 1 ;++dy) {
							for (int dx = -1 ; dx <= 1 ;++dx) {
								if (   x+dx >= 0 && x+dx < this.width 
										&& y+dy >= 0 && y+dy < this.height
										&& filledImg.getValueInt(x+dx, y+dy) == 255
										&& !(binImage.getValueInt(x+dx, y+dy) > 0)) {
									border.addPixel(x, y);
									dx = 2; dy = 2;
								}
							}
						}
					}
					// only add border if it is long enough
					if (border.getPointNum() >= this.minimalBorderLength) {
						if (!innerBordersHash.containsKey(label))
							innerBordersHash.put(label,new Vector<MTBBorder2D>());
						innerBordersHash.get(label).add(border);
					}
				}
			}
			// copy outer borders to result set
			if (   this.borderType == BorderType.OUT_IN_BORDERS
					|| this.borderType == BorderType.OUTER_BORDERS) {
				for (int i = 0; i < this.inputRegions.size(); ++i) {
					MTBRegion2D reg = this.inputRegions.elementAt(i);
					Vector<Point2D.Double> points = reg.getPoints();
					Integer label = new Integer(regImage.getValueInt(
						(int)(points.elementAt(0).x+0.5),(int)(points.elementAt(0).y+0.5)));
					MTBBorder2D b = outerBordersHash.get(label);
					
					// skip borders being too short
					if (b.getPointNum() < this.minimalBorderLength)
						continue;
					if (innerBordersHash != null && innerBordersHash.containsKey(label)) {
						b.setInner(innerBordersHash.get(label));
					}
					this.resultBorders.add(b);
				}
			}
			// only inner contours requested
			else {
				for (int i = 0; i < this.inputRegions.size(); ++i) {
					MTBRegion2D reg = this.inputRegions.elementAt(i);
					Vector<Point2D.Double> points = reg.getPoints();
					Integer label = new Integer(regImage.getValueInt(
						(int)(points.elementAt(0).x+0.5),(int)(points.elementAt(0).y+0.5)));

					Vector<MTBBorder2D> bs = innerBordersHash.get(label);
					MTBBorder2D b = new MTBBorder2D();
					b.setInner(bs);
					this.resultBorders.add(b);
				}
			}
		}
		catch (MTBDatatypeException ex) {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
					"[BordersOnLabeledComponents] problem with border object datatype!");
		}
	}					

	/**
	 * Draws result contours to an image.
	 */
	protected void drawBordersToImage() {
		this.resultImage = (MTBImageShort) MTBImage.createMTBImage(this.width,
				this.height, 1, 1, 1, MTBImageType.MTB_SHORT);

		for (int i = 0; i < this.resultBorders.size(); ++i) {
			MTBBorder2D border = this.resultBorders.elementAt(i);
			
			// color coincides with label
			int color = i + 1;
			
			// outer border
			Vector<Point2D.Double> borderPoints = border.getPoints();
			for (int j = 0; j < borderPoints.size(); ++j) {
				Point2D.Double p = borderPoints.elementAt(j);
				this.resultImage.putValueDouble(
						(int) Math.round(p.getX()), (int) Math.round(p.getY()), color);
			}

			// inner border
			int innerCount = border.countInner();
			for (int j = 0; j < innerCount; j++) {
				MTBBorder2D c = border.getInner(j);
				Vector<Point2D.Double> in_points = c.getPoints();
				for (int k = 0; k < in_points.size(); k++) {
					Point2D.Double p = in_points.elementAt(k);
					this.resultImage.putValueDouble(
							(int) Math.round(p.getX()), (int) Math.round(p.getY()), color);
				}
			}
		}
	}
}
