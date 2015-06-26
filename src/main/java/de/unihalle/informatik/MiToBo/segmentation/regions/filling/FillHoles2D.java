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
 * Operator to fill holes in connected components of binary or label image.
 * If there is only a single grey value besides the background (of zero) the
 * input image is assumed to be binary, otherwise a label image.
 * <p>
 * If the input image is a label image each label is assumed to constitute
 * on connected component, i.e. one region.
 * Otherwise the result of the operator is undefined.
 * <p>
 * If the input image is binary conncected components are detected an filled.
 * 
 * @author gress, posch
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD)
public class FillHoles2D extends MTBOperator implements StatusReporter {
	
	/**
	 * Input image.
	 */
	@Parameter(label= "Input image", required = true, direction = Direction.IN,
		mode = ExpertMode.STANDARD, dataIOOrder = 1, description = "Input image, binary or label image")
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
	 * @throws ALDOperatorException
	 */
	public FillHoles2D() throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>();
	}
	
	/**
	 * Constructor with input image.
	 * @param img		Image to process.
	 * @throws IllegalArgumentException
	 * @throws ALDOperatorException
	 */
	public FillHoles2D(MTBImage img) 
		throws IllegalArgumentException, ALDOperatorException {
		this.setInputImage(img);
		this.validateCustom();
		this.m_statusListeners = new Vector<StatusListener>();
	}
	
	@Override
	public 	void validateCustom() throws ALDOperatorException {
		if (inImg.getSizeC() > 1 || inImg.getSizeT() > 1 || inImg.getSizeZ() > 1) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				    "\n>>>>>>> FillHoles2D: validation failed!\nOnly 2D images allowed.");

		}

		if (inImg.getType() != MTBImageType.MTB_BYTE && 
				inImg.getType() != MTBImageType.MTB_SHORT &&
				inImg.getType() != MTBImageType.MTB_INT ) {
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
	 * @return
	 */
	@Override
  protected Object readResolve() {
		super.readResolve();
		this.m_statusListeners = new Vector<StatusListener>();
		return this;
	}

	/**
	 * Get reference to the current input image.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}
	
	/**
	 * Set input image.
	 */
	public void setInputImage(MTBImage inputImage) {
		this.inImg = inputImage;
	}
	
	/**
	 * Get the resulting image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
	
	/**
	 * Set the result image.
	 */
	protected void setResultImage(MTBImage resultImage) {
		this.resultImg = resultImage;
	}
	
	@Override
	protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException  {
		MTBRegion2DSet regs;
		Boolean isBinary = true;
		int value = 0;
		
		// decide if inImg is binary
		// find first non null grey value
		int x, y;
		for ( y = 0 ; y < inImg.getSizeY() ; y++) {
			for ( x = 0 ; x < inImg.getSizeX() ; x++) {
				if ( inImg.getValueInt(x, y) != 0) {
					value = inImg.getValueInt(x, y);
					break;
				}
			}
		}
		
		// check remaining pixels
		for ( y = 0 ; y < inImg.getSizeY() ; y++) {
			for ( x = 0 ; x < inImg.getSizeX() ; x++) {
				if ( inImg.getValueInt(x, y) != 0 &&
						inImg.getValueInt(x, y) != value) {
					isBinary = false;
					break;
				}
			}
		}

		if ( isBinary ) {
			LabelComponentsSequential lcs = 
					new LabelComponentsSequential(this.inImg,true);
			lcs.runOp(HidingMode.HIDE_CHILDREN);
			regs = lcs.getResultingRegions();
		} else {
			regs = new MTBRegion2DSet(this.inImg);
		}

		MTBImage outImg = MTBImage.createMTBImage(this.inImg.getSizeX(), 
												  this.inImg.getSizeY(), 
												  this.inImg.getSizeZ(), 
												  this.inImg.getSizeT(), 
												  this.inImg.getSizeC(), 
												  inImg.getType());
		for (int i = 0; i < regs.size(); i++) {
			this.notifyListeners(new StatusEvent(i,regs.size(),"Filling regions..."));
			this.paintFilledRegion(regs.elementAt(i), inImg, outImg);
		}
		
		this.notifyListeners(new StatusEvent(regs.size(), regs.size(), "Filling regions done"));
		this.setResultImage(outImg);
	}

	/**
	 * Draw a region to an image.
	 * @param reg			Region to draw.
	 * @param outImg	Image where to draw it.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private void paintFilledRegion(MTBRegion2D reg, MTBImage inImg, MTBImage outImg) 
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

		int value = inImg.getValueInt( xmin, ymin);
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
			
			for (int j = 0; j < pts.size(); j++) {
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

}
