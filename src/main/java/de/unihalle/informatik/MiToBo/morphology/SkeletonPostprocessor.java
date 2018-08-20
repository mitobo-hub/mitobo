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

package de.unihalle.informatik.MiToBo.morphology;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Helper operator to post-process binary region skeletons.
 * <p>
 * This operator mainly removes spines from the skeletons.
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.STANDARD)
public class SkeletonPostprocessor extends MTBOperator {
	
	/**
	 * Identifier string for this operator class.
	 */
	private static final String opID = "\t [SkeletonPostprocessor]";

	/**
	 * Input skeleton image to process.
	 * <p>
	 * The operator expects a binary image with the skeleton pixels 
	 * having black color on a white background.
	 */
	@Parameter(label = "Input Image", required = true,
			direction = Parameter.Direction.IN, description = "Input image.",
			dataIOOrder = 0)
	private transient MTBImageByte inputImg = null;

	/**
	 * Flag to enable/disable spine removal.
	 */
	@Parameter(label = "Remove spines?", required = true,
			direction = Parameter.Direction.IN, 
			description = "If active, spines are removes.", dataIOOrder = 1)
	private boolean removeSpines = true;

	/**
	 * Maximal allowed length of a branch to be accepted as spine.
	 */
	@Parameter(label = "Maximal Length of Spines", required = true,
			direction = Parameter.Direction.IN, 
			description = "Max. spine length.", dataIOOrder = 2)
	private int maxSpineLength = 40;

	/**
	 * Postprocessed skeleton image.
	 */
	@Parameter(label = "Postprocessed Image", dataIOOrder = 0,
			direction = Parameter.Direction.OUT, 
			description = "Postprocessed skeleton image.")
	private transient MTBImageByte postprocessedImg = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown on instantiation failures.
	 */
	public SkeletonPostprocessor() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Set input image to process.
	 * <p>
	 * Expecting skeleton to be black on white background.
	 * 
	 * @param img	Input skeleton image.
	 */
	public void setInputImage(MTBImageByte img) {
		this.inputImg = img;
	}

	/**
	 * Enable/disable removal of spines.
	 * @param b	If true, spine removal is activated.
	 */
	public void doRemoveSpines(boolean b) {
		this.removeSpines = b;
	}
	
	/**
	 * Set maximal length of spines.
	 * @param maxLength	Maximal length of spines.
	 */
	public void setMaximalSpineLength(int maxLength) {
		this.maxSpineLength = maxLength;
	}

	/**
	 * Returns post-processed skeleton image.
	 * @return	Result image.
	 */
	public MTBImageByte getResultImage() {
		return this.postprocessedImg;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@SuppressWarnings("unused")
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		if (this.removeSpines) {
		
			// remove spines
			this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, opID 
							+ " removing spines..."));

			// init the result image
			this.postprocessedImg = 
					(MTBImageByte)this.inputImg.duplicate(HidingMode.HIDDEN);
			int ppW = this.postprocessedImg.getSizeX();			
			int ppH = this.postprocessedImg.getSizeY();
			// invert image, skeleton becomes white
			for (int y=0;y<ppH;++y)
				for (int x=0;x<ppW;++x)
					this.postprocessedImg.putValueInt(x, y, 
							255 - this.postprocessedImg.getValueInt(x, y));

//			// label enclosed regions, use 4er-NB since skeletons use 8er-NB
//			LabelComponentsSequential cl = 
//					new LabelComponentsSequential(this.inputImg, false);
//			cl.runOp();
//			MTBImage labelImg = cl.getLabelImage();

			// iteratively search for end-points and length of check branches
			boolean changes = true;
			int neighborCount, nx, ny;
			while (changes) {
				changes = false;
				for (int y=0; y<ppH; ++y) {
					for (int x=0; x<ppW; ++x) {
						if (this.postprocessedImg.getValueInt(x, y) == 0)
							continue;
						// foreground pixel, check if it is an end-point
						neighborCount=0;
						for (int dx=-1; dx<=1; ++dx) {
							for (int dy=-1; dy<=1; ++dy) {
								if (dx==0 && dy==0)
									continue;
								nx = x+dx;
								ny = y+dy;
								if (   nx<0 || nx>=ppW
										|| ny<0 || ny>=ppH)
									continue;
								if (this.postprocessedImg.getValueInt(nx, ny) > 0)
									++neighborCount;
							}
						}
						if (neighborCount == 1) {
							Vector<Point2D.Double> br = SkeletonAnalysisHelper.traceBranch(
									this.postprocessedImg, x, y);
							if (br.size() - 1 < this.maxSpineLength) {
								for (Point2D.Double p: br) 
									this.postprocessedImg.putValueInt((int)p.x, (int)p.y, 0);
								
								// check if last point needs to be re-added: this check is
								// necessary to ensure that the skeleton is not split-up on 
								// removing splines; the check is done by checking for each
								// skeleton pixel in the neighborhood of the considered pixel
								// (x,y) if there remains at least one further neighbor if 
								// the spine branch pixel is deleted
								x = (int)br.lastElement().x; 
								y = (int)br.lastElement().y;

								// check corner pixels first
								boolean pixelNeeded = false;
								boolean nFound = false;
								
								// corner (-1,-1)
								nx = x-1; ny = y-1;
								if (   nx >= 0 && nx < ppW
										&& ny >= 0 && ny < ppH
										&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
									if (this.postprocessedImg.getValueInt(nx, y) == 255)
										nFound = true;
									if (   !nFound 
											&& this.postprocessedImg.getValueInt(x, ny) == 255)
										nFound = true;
									if (!nFound)
										pixelNeeded = true; 
								}
								// corner ( 1,-1)
								if (!pixelNeeded) {
									nFound = false;
									nx = x+1; ny = y-1;
									if (   nx >= 0 && nx < ppW
											&& ny >= 0 && ny < ppH
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (this.postprocessedImg.getValueInt(nx, y) == 255)
											nFound = true;
										if (   !nFound 
												&& this.postprocessedImg.getValueInt(x, ny) == 255)
											nFound = true;
										if (!nFound)
											pixelNeeded = true; 
									}
								}
								// corner ( 1, 1)
								if (!pixelNeeded) {
									nFound = false;
									nx = x+1; ny = y+1;
									if (   nx >= 0 && nx < ppW
											&& ny >= 0 && ny < ppH
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (this.postprocessedImg.getValueInt(nx, y) == 255)
											nFound = true;
										if (   !nFound 
												&& this.postprocessedImg.getValueInt(x, ny) == 255)
											nFound = true;
										if (!nFound)
											pixelNeeded = true; 
									}
								}
								// corner ( -1, 1)
								if (!pixelNeeded) {
									nFound = false;
									nx = x-1; ny = y+1;
									if (   nx >= 0 && nx < ppW
											&& ny >= 0 && ny < ppH
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (this.postprocessedImg.getValueInt(nx, y) == 255)
											nFound = true;
										if (   !nFound 
												&& this.postprocessedImg.getValueInt(x, ny) == 255)
											nFound = true;
										if (!nFound)
											pixelNeeded = true; 
									}
								}

								// check 4er-NB pixel
								
								// pixel (-1, 0)
								if (!pixelNeeded) {
									nFound = false;
									nx = x-1; ny = y;
									if (   nx >= 0 && nx < ppW
											&& ny >= 0 && ny < ppH
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (   y-1 >= 0 
												&& this.postprocessedImg.getValueInt(x-1, y-1) == 255)
											nFound = true;	
										if (   !nFound && y-1 >= 0 
												&& this.postprocessedImg.getValueInt( x, y-1) == 255)
											nFound = true;	
										if (   !nFound && y+1 < ppH 
												&& this.postprocessedImg.getValueInt( x, y+1) == 255)
											nFound = true;	
										if (   !nFound && y+1 < ppH 
												&& this.postprocessedImg.getValueInt(x-1, y+1) == 255)
											nFound = true;	
										if (!nFound)
											pixelNeeded = true; 
									}
								}
								// pixel (0, -1)
								if (!pixelNeeded) {
									nFound = false;
									nx = x; ny = y-1;
									if (   nx >= 0 && nx < ppW
											&& ny >= 0 && ny < ppH
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (   x-1 >= 0 
												&& this.postprocessedImg.getValueInt(x-1, y) == 255)
											nFound = true;	
										if (   !nFound && x-1 >= 0  
												&& this.postprocessedImg.getValueInt(x-1, y-1) == 255)
											nFound = true;	
										if (   !nFound && x+1 < ppW 
												&& this.postprocessedImg.getValueInt( x+1, y-1) == 255)
											nFound = true;	
										if (   !nFound && x+1 < ppW 
												&& this.postprocessedImg.getValueInt(x+1, y) == 255)
											nFound = true;	
										if (!nFound)
											pixelNeeded = true; 
									}
								}
								// pixel ( 1, 0)
								if (!pixelNeeded) {
									nFound = false;
									nx = x+1; ny = y;
									if (   nx >= 0 && nx < this.postprocessedImg.getSizeX()
											&& ny >= 0 && ny < this.postprocessedImg.getSizeY()
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (   y-1 >= 0 
												&& this.postprocessedImg.getValueInt(x, y-1) == 255)
											nFound = true;
										if (   !nFound && y-1 >= 0  
												&& this.postprocessedImg.getValueInt(x+1, y-1) == 255)
											nFound = true;	
										if (   !nFound && y+1 < ppH 
												&& this.postprocessedImg.getValueInt( x+1, y+1) == 255)
											nFound = true;	
										if (   !nFound && y+1 < ppH 
												&& this.postprocessedImg.getValueInt(x, y+1) == 255)
											nFound = true;	
										if (!nFound)
											pixelNeeded = true; 
									}
								}
								// pixel ( 0, 1)
								if (!pixelNeeded) {
									nFound = false;
									nx = x; ny = y+1;
									if (   nx >= 0 && nx < this.postprocessedImg.getSizeX()
											&& ny >= 0 && ny < this.postprocessedImg.getSizeY()
											&& this.postprocessedImg.getValueInt(nx, ny) == 255) {
										if (   x+1 < ppW 
												&& this.postprocessedImg.getValueInt(x+1, y) == 255)
											nFound = true;
										if (   !nFound && x+1 < ppW
												&& this.postprocessedImg.getValueInt(x+1, y+1) == 255)
											nFound = true;	
										if (   !nFound && x-1 >= 0 
												&& this.postprocessedImg.getValueInt( x-1, y+1) == 255)
											nFound = true;	
										if (   !nFound && x-1 >= 0 
												&& this.postprocessedImg.getValueInt(x-1, y) == 255)
											nFound = true;	
										if (!nFound)
											pixelNeeded = true; 
									}
								}

								if (pixelNeeded) {
									this.postprocessedImg.putValueInt(x, y, 255);
								}
								changes = true;
							}
						}
					}
				}
			}
			// invert image again, skeleton becomes black as initially
			for (int y=0;y<ppH;++y)
				for (int x=0;x<ppW;++x)
					this.postprocessedImg.putValueInt(x, y, 
							255 - this.postprocessedImg.getValueInt(x, y));
		}
	}
}
