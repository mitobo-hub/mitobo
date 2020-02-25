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

package de.unihalle.informatik.MiToBo.segmentation.regions.filling;

import java.awt.geom.Point2D;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * Operator to fill holes in connected components of binary or label images.
 * <p>
 * If there is only a single grey value besides the background (of zero) the
 * input image is assumed to be binary, otherwise a label image.
 * <p>
 * If the input image is a label image each label is assumed to constitute
 * one connected component, i.e. one region. 
 * Otherwise the result of the operator is undefined.
 * <p>
 * If the input image is binary conncected components are detected an filled.
 * 
 * @author gress, posch
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD,
	shortDescription="Fills holes in connected components of binary or label images.")
public class FillHoles2D extends MTBOperator implements StatusReporter {
	
	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = "[FillHoles2D] ";

	/**
	 * Input image.
	 */
	@Parameter(label= "Input image", required = true, direction = Direction.IN,
		mode = ExpertMode.STANDARD, dataIOOrder = 1, 
		description = "Input image, binary or label image")
	private transient MTBImage inImg = null;

	/**
	 * Result image.
	 */
	@Parameter(label= "Result image", required = true, direction = Direction.OUT,
		mode = ExpertMode.STANDARD, dataIOOrder = 2, description = "Result image")
	private transient MTBImage resultImg = null;
	
	/** vector of installed StatusListeners */
	private transient Vector<StatusListener> m_statusListeners;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public FillHoles2D() throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>();
	}
	
	/**
	 * Constructor with input image.
	 * @param img		Image to process.
	 * @throws IllegalArgumentException		Thrown in case of failure.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 */
	public FillHoles2D(MTBImage img) 
		throws IllegalArgumentException, ALDOperatorException {
		this.setInputImage(img);
		this.validateCustom();
		this.m_statusListeners = new Vector<StatusListener>();
	}
	
	@Override
	public 	void validateCustom() throws ALDOperatorException {
		if (   this.inImg.getSizeC() > 1 || this.inImg.getSizeT() > 1 
				|| this.inImg.getSizeZ() > 1) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
		    "\n>>>>>>> FillHoles2D: validation failed!\nOnly 2D images allowed.");

		}

		if (this.inImg.getType() != MTBImageType.MTB_BYTE && 
				this.inImg.getType() != MTBImageType.MTB_SHORT &&
				this.inImg.getType() != MTBImageType.MTB_INT ) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
		    "\n>>>>>>> FillHoles2D: validation failed!\nOnly BYTE an SHORT and INT images allowed.");

		}
	}
	
	/**
	 * Init function for deserialized objects.
	 * <p>
	 * This function is called on an instance of this class being deserialized
	 * from file, prior to handing the instance over to the user. It takes care
	 * of a proper initialization of transient member variables as they are not
	 * initialized to the default values during deserialization.
	 *  
	 * @return	Initialized object.
	 */
	@Override
  protected Object readResolve() {
		super.readResolve();
		this.m_statusListeners = new Vector<StatusListener>();
		return this;
	}

	/**
	 * Get reference to the current input image.
	 * @return Current input image.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}
	
	/**
	 * Set input image.
	 * @param inputImage Input image to process.
	 */
	public void setInputImage(MTBImage inputImage) {
		this.inImg = inputImage;
	}
	
	/**
	 * Get the resulting image.
	 * @return Result image with holes filled.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
	
	/**
	 * Set the result image.
	 * @param resultImage Set result image variable.
	 */
	protected void setResultImage(MTBImage resultImage) {
		this.resultImg = resultImage;
	}
	
	@Override
	protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException  {
		MTBRegion2DSet regs;
		boolean isBinary = true;
		int value = 0;
		
		// post ImageJ status
		String msg = opIdentifier + "checking if image is binary...";	
		this.notifyListeners(new StatusEvent(msg));

		// decide if inImg is binary
		// find first non null grey value
		int x, y;
		for ( y = 0 ; y < this.inImg.getSizeY() ; y++) {
			for ( x = 0 ; x < this.inImg.getSizeX() ; x++) {
				if ( this.inImg.getValueInt(x, y) != 0) {
					value = this.inImg.getValueInt(x, y);
					// break both loops
					x = this.inImg.getSizeX();
					y = this.inImg.getSizeY();
				}
			}
		}
		
		// check remaining pixels
		for ( y = 0 ; y < this.inImg.getSizeY() && isBinary ; y++) {
			for ( x = 0 ; x < this.inImg.getSizeX() && isBinary ; x++) {
				if (   this.inImg.getValueInt(x, y) != 0 
						&& this.inImg.getValueInt(x, y) != value) {
					isBinary = false;
				}
			}
		}
		
		msg = opIdentifier + "extracting regions...";	
		this.notifyListeners(new StatusEvent(msg));

		if ( isBinary ) {
			LabelComponentsSequential lcs = 
					new LabelComponentsSequential(this.inImg,true);
			lcs.runOp(HidingMode.HIDE_CHILDREN);
			regs = lcs.getResultingRegions();
		} else {
			regs = new MTBRegion2DSet(this.inImg);
		}

		msg = opIdentifier + "filling holes...";	
		this.notifyListeners(new StatusEvent(msg));

		MTBImage outImg = MTBImage.createMTBImage(this.inImg.getSizeX(), 
												  this.inImg.getSizeY(), 
												  this.inImg.getSizeZ(), 
												  this.inImg.getSizeT(), 
												  this.inImg.getSizeC(), 
												  this.inImg.getType());
		
		// in case of a binary image all regions can be handled at once since 
		// they all have the same label; all (background) regions not touching
		// the image border are holes
		if (isBinary) {
			
			// draw hole regions to output image
			this.paintFilledRegionsBinary(this.inImg, outImg, value);
			
			// add original regions 
			Point2D.Double pt;
			Vector<Point2D.Double> pts;
			for (int i = 0; i < regs.size(); i++) {
				pts = regs.get(i).getPoints();
				for (int j = 0; j < pts.size(); j++) {
					pt = pts.get(j);
					outImg.putValueInt((int)pt.getX(), (int)pt.getY(), value);
				}
			}
		}
		else {
			for (int i = 0; i < regs.size(); i++) {

				msg = opIdentifier + "... region: " + i;	
				this.notifyListeners(new StatusEvent(msg));

				this.paintFilledRegion(regs.elementAt(i), this.inImg, outImg);
			}
		}
		this.setResultImage(outImg);
	}

	/**
	 * Close holes in binary image.
	 * <p>
	 * In a binary image the foreground regions are in white, while background
	 * and holes are black. The difference between holes and background is given
	 * by the fact that holes have to be completely enclosed by a region, thus,
	 * never touch the image boundary.
	 * <p>
	 * The method first inverts the image and then labels the background and
	 * hole components. Afterwards all regions are checked for contact with the
	 * image boundary, and if there is no contact the region is identified as
	 * a hole region and drawn to the output image. 
	 * 
	 * @param img			Binary region image with holes (in black).
	 * @param outImg	Binary region image without holes (regions in white).
	 * @param v				Value of foreground in input/result image.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	private void paintFilledRegionsBinary(MTBImage img, MTBImage outImg, int v) 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		int w = img.getSizeX();
		int h = img.getSizeY();
		
		// invert image so that background and holes are white
		MTBImage invInImg = img.duplicate();
		for (int y=0; y<h; ++y) 
			for (int x=0; x<w; ++x)
				if (img.getValueInt(x, y) > 0)
					invInImg.putValueInt(x, y, 0);
				else
					invInImg.putValueInt(x, y, 255);

		LabelComponentsSequential lcs = 
			new LabelComponentsSequential(invInImg, false);
		lcs.runOp(HidingMode.HIDDEN);
		
		MTBRegion2DSet holes = lcs.getResultingRegions();
		
		boolean bgRegion;
		Vector<Point2D.Double> pts;
		Point2D.Double pt;
		int x,y;
		for (int i = 0; i < holes.size(); i++) {
			
			pts = holes.get(i).getPoints();
			
			bgRegion = false;
			
			for (int j=0; j<pts.size() && !bgRegion; j++) {
				pt = pts.get(j);
				x = (int)pt.getX();
				y = (int)pt.getY();
				
				if (x == 0 || x == w-1 || y == 0 || y == h-1)
					bgRegion = true;
			}
			
			if (!bgRegion) {
				
				for (int j = 0; j < pts.size(); j++) {
					pt = pts.get(j);
					outImg.putValueInt((int)pt.getX(), (int)pt.getY(), v);
				}
			}
		}
	}

	/**
	 * Draw a region without holes to an image.
	 * @param reg			Region to draw.
	 * @param img			Original input image.
	 * @param outImg	Image where to draw it.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	private void paintFilledRegion(MTBRegion2D reg, MTBImage img, MTBImage outImg) 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		Vector<Point2D.Double> pts = reg.getPoints();
		Point2D pt;
		int x, y, xmin, xmax, ymin, ymax;
		
		if ( pts.size() == 0) {
			// empty region
			return;
		}
		
		xmin = xmax = (int)pts.get(0).getX();
		ymin = ymax = (int)pts.get(0).getY();

		int value = img.getValueInt( xmin, ymin);
		outImg.putValueInt(xmin, ymax, value);		

		for (int i = 1; i < pts.size(); i++) {
			x = (int)pts.get(i).getX();
			y = (int)pts.get(i).getY();
			
			if (x < xmin) xmin = x;
			else if (x > xmax) xmax = x;
			if (y < ymin) ymin = y;
			else if (y > ymax) ymax = y;
			
			outImg.putValueInt(x, y, value);		
		}
		
		int wWidth = xmax-xmin+1;
		int wHeight = ymax-ymin+1;
		
		MTBImage iw = 
			MTBImage.createMTBImage(wWidth, wHeight, 1, 1, 1, MTBImageType.MTB_BYTE);
		
		for (int yw = 0; yw < wHeight; yw++) {
			for (int xw = 0; xw < wWidth; xw++) {
				iw.putValueInt(xw, yw, 255);
			}
		}
		
		pts = reg.getPoints();						
		
		for (int j = 0; j < pts.size(); j++) {
			pt = pts.get(j);
			iw.putValueInt((int)pt.getX()-xmin, (int)pt.getY()-ymin, 0);
		}
		
		LabelComponentsSequential lcs = new LabelComponentsSequential(iw, false);
		lcs.runOp(HidingMode.HIDDEN);
		
		MTBRegion2DSet regs = lcs.getResultingRegions();
		
		boolean bgRegion;
		for (int i = 0; i < regs.size(); i++) {
			
			pts = regs.get(i).getPoints();
			
			bgRegion = false;
			
			for (int j=0; j<pts.size() && !bgRegion; j++) {
				pt = pts.get(j);
				x = (int)pt.getX();
				y = (int)pt.getY();
				
				if (x == 0 || x == wWidth-1 || y == 0 || y == wHeight-1)
					bgRegion = true;
			}
			
			if (!bgRegion) {
				
				for (int j = 0; j < pts.size(); j++) {
					pt = pts.get(j);
					
					outImg.putValueInt((int)pt.getX()+xmin, (int)pt.getY()+ymin, value);
				}
			}
		}
	}


	// ----- StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener statuslistener) {	
		this.m_statusListeners.add(statuslistener);	
	}

	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			this.m_statusListeners.get(i).statusUpdated(e);
		}
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.m_statusListeners.remove(statuslistener);
	}
	
	@Override
	public String getDocumentation() {
		return "<p>Operator to fill holes in connected components (2D). Background is assumed to have pixel value 0.</p>\n" + 
				"<h3>Required input:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Input image</b>:</p>\n" + 
				"\n" + 
				"<p>Input image with connected components.</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Output:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><b>Result image</b></p>\n" + 
				"\n" + 
				"<p>Output image where holes in connected components are filled.</p>\n" + 
				"</li></ul>";
	}
}
