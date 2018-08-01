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

package de.unihalle.informatik.MiToBo.segmentation.regions.labeling;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageInt;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Sequential component labeling for binarized 2D images to find 
 * connected components. Foreground pixels are assumed to have a 
 * value > 0, all pixels with value <= 0 are assumed to be background.
 * 
 * Algorithm: <br/>
 * W. Burger and M. Burge, 
 * Digital image processing: an algorithmic introduction using Java, 
 * 2008, Springer-Verlag New York Inc
 * 
 * @author gress
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL,
	level=Level.APPLICATION)
public class LabelComponentsSequential extends MTBOperator {
	
	/**
	 * Input image to label.
	 */
	@Parameter( label= "Input image", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder=0, 
		mode=ExpertMode.STANDARD, description = "Input image")
	private transient MTBImage inputImage = null;

	/**
	 * Flag to define 4-/8-neighborhood in components.
	 */
	@Parameter( label= "Diagonal neighborhood", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder=1, 
		mode=ExpertMode.STANDARD, 
		description = "true for 8-neighborhood, false for 4-neighborhood")
	private boolean diagonalNeighbors = true;

	/**
	 * Flag to enable/disable creation of gray-scale label image.
	 */
	@Parameter( label= "Create label image", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder=2, 
		mode=ExpertMode.STANDARD, description = "Create image of regions "
			+ "with region labels as grayvalue.")
	private Boolean createLabelImage = new Boolean(true);

	/**
	 * Flag to enable/disable creation of gray-scale image with region IDs.
	 */
	@Parameter( label= "Create image with ID strings", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder=3, 
		mode=ExpertMode.STANDARD, description = "Create image of regions "
			+ "with numerical IDs written to regions.")
	private Boolean createIDImage = new Boolean(false);

	/**
	 * Flag to enable/disable creation of region image in random colors.
	 */
	@Parameter( label= "Create color image", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder=4, 
		mode=ExpertMode.STANDARD, 
		description = "Create image of regions with random colors")
	private Boolean createColorImage = new Boolean(false);

	/**
	 * Resulting set of labeled regions.
	 */
	@Parameter( label= "Resulting regions", required = true, 
		direction = Parameter.Direction.OUT, dataIOOrder=0, 
		description = "Resulting regions")
	private MTBRegion2DSet resultingRegions = null;

	/**
	 * Grayscale image of regions, gray values are identical to IDs.
	 */
	@Parameter( label= "Label image", required = false, 
		direction = Parameter.Direction.OUT, dataIOOrder=1, 
		description = "Image of regions with labels (MTBImageType.MTB_INT)")
	private transient MTBImage labelImage = null;

	/**
	 * Gray-scale image with IDs printed to regions.
	 * <p>
	 * Note that the gray values are scaled for better visibility and
	 * to not coincide with the region labels.
	 */
	@Parameter( label= "ID image", required = false, 
		direction = Parameter.Direction.OUT, dataIOOrder=2, 
		description = "Image of region IDs")
	private transient MTBImage idImage = null;

	/**
	 * Image of regions in random colors.
	 */
	@Parameter( label= "Color image", required = false, 
		direction = Parameter.Direction.OUT, dataIOOrder=3, 
		description = "Image of regions with random colors")
	private transient MTBImage colorImage = null;
	
	/* some local variables */
	
	/**
	 * Temporary label image.
	 */
	private MTBImageInt m_labelImg;

	/**
	 * Width of input image.
	 */
	private int m_width;
	/**
	 * Height of input image.
	 */
	private int m_height;
	
	/**
	 * Constructor.
	 * 
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public LabelComponentsSequential() throws ALDOperatorException {
	}

	
	/**
	 * Constructor.
	 * 
	 * @param img 	Input image.
	 * @param dn 		Set true for 8-NB or false for 4-NB.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public LabelComponentsSequential(MTBImage img, boolean dn) 
			throws ALDOperatorException {
		this.inputImage = img;
		this.diagonalNeighbors = dn;
	}


	/**
	 * Get reference to the current input image.
	 * 
	 * @return	Input image to work on.
	 */
	public MTBImage getInputImage() {
		return this.inputImage;
	}
	
	/**
	 * Set a new input image.
	 * @param iImage Input image to label.
	 */
	public void setInputImage(MTBImage iImage) {
		this.inputImage = iImage;
	}

	/**
	 * Get the neighborhood flag.
	 * @return true if 8-neighborhood is used, false if 4-neighborhood is used
	 */
	public boolean getDiagonalNeighborsFlag() {
		return this.diagonalNeighbors;
	}
	
	/**
	 * Set the neighborhood flag.
	 * @param diagonalNeighbors set true for 8-neighborhood and false for 4-neighborhood
	 */
	public void setDiagonalNeighborsFlag(boolean diagonalNeighbors) {
		this.diagonalNeighbors = diagonalNeighbors;
	}


	/**
	 * Get the flag that determines the creation of an image with region labels.
	 */
	public boolean getCreateLabelImageFlag() {
		return this.createLabelImage;
	}
	
	/**
	 * Set the flag that determines the creation of an image with region labels.
	 */
	public void setCreateLabelImageFlag(boolean createLabelImage) {
		this.createLabelImage = createLabelImage;
	}
	
	/**
	 * Enable/disable creation of ID image.
	 * @param b If true, ID image is created.
	 */
	public void setCreateIDImageFlag(boolean b) {
		this.createIDImage = new Boolean(b);
	}

	/**
	 * Get the flag that determines the creation of an image with randomly colored regions.
	 */
	public boolean getCreateColorImageFlag() {
		return this.createColorImage;
	}
	
	/**
	 * Set the flag that determines the creation of an image with randomly colored regions.
	 */
	public void setCreateColorImageFlag(boolean createColorImage) {
		this.createColorImage = createColorImage;
	}

	/**
	 * Attach the vector of resulting regions.
	 */
	protected void setResultingRegions( MTBRegion2DSet regions) {
		this.resultingRegions = regions;
	}

	/**
	 * Get the resulting regions.
	 */
	public MTBRegion2DSet getResultingRegions() {
		return this.resultingRegions;
	}

	/**
	 * Set image of region labels
	 */
	protected void setLabelImage(MTBImage labelImage) {
		this.labelImage = labelImage;
	}

	/**
	 * Get image of region labels (of type MTB_INT).
	 * <p>
	 * Only available if the create-label-image-flag was set to true,
	 * otherwise returns null.
	 * @return Label image.
	 */
	public MTBImage getLabelImage() {
		return this.labelImage;
	}
	
	/**
	 * Get image of region ID (of type MTB_BYTE).
	 * <p>
	 * Only available if the corresponding flag was set to true,
	 * otherwise returns null.
	 * @return ID image.
	 */
	public MTBImage getIDImage() {
		return this.labelImage;
	}

	/**
	 * Add the label image to the parameter object
	 */
	protected void setColorImage(MTBImage colorImage) {
		this.colorImage = colorImage;
	}

	/**
	 * Get image of randomly colored regions, if the create-color-image-flag was set to true. Otherwise returns null.
	 */
	public MTBImage getColorImage() {
		return this.colorImage;
	}

	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		this.setResultingRegions(
				this.labelComponents(this.inputImage, this.diagonalNeighbors));
		
		this.labelImage = null;
		this.idImage = null;
		this.colorImage = null;
		
		if (   this.createLabelImage.booleanValue()
				|| this.createIDImage.booleanValue()) {
			
			// create label image, required in any case
			DrawRegion2DSet drawer = new DrawRegion2DSet(
					DrawType.ID_IMAGE, this.getResultingRegions());
			drawer.runOp(null);
			MTBImage lImg = drawer.getResultImage();
			lImg.setTitle(
					MTBImage.getTitleRunning(this.inputImage.getTitle()));

			// copy to output parameter if requested
			if (this.createLabelImage.booleanValue())
				this.labelImage = lImg;
			
			// draw region IDs to label ID image
			if (this.createIDImage.booleanValue()) {
				
				// do a distance transformation of the label image to determine
				// nice positions for the labels, first binarize label image...
				MTBImageByte dImg = (MTBImageByte)MTBImage.createMTBImage(
						this.inputImage.getSizeX(), this.inputImage.getSizeY(), 
						this.inputImage.getSizeZ(), this.inputImage.getSizeT(), 
						this.inputImage.getSizeC(), MTBImageType.MTB_BYTE);
				for (int y=0;y<lImg.getSizeY();++y)
					for (int x=0;x<lImg.getSizeX();++x)
						if (lImg.getValueInt(x, y) > 0)
							dImg.putValueInt(x, y, 255);
						else
							dImg.putValueInt(x, y, 0);
				// ... then calculate distances
				DistanceTransform dt = new DistanceTransform(dImg, 
						DistanceMetric.CITYBLOCK, ForegroundColor.FG_BLACK);
				dt.runOp();
				MTBImage distances = dt.getDistanceImage();
				
				// search for pixel with largest distance in each region
				HashMap<Integer, Point2D.Double> maxPoints = 
						new HashMap<Integer, Point2D.Double>();
				HashMap<Integer, Double> maxValues = 
						new HashMap<Integer, Double>();				
				int label;
				for (int y=0;y<lImg.getSizeY();++y) {
					for (int x=0;x<lImg.getSizeX();++x) {
						label = lImg.getValueInt(x, y);
						
						// only pixels with a label larger than zero are interesting
						if (label == 0)
							continue;
						
						// if we did not see a pixel of this region before
						if (maxPoints.get(new Integer(label)) == null) {
							maxPoints.put(new Integer(label), 
									new Point2D.Double(x, y));
							maxValues.put(new Integer(label), 
									new Double(distances.getValueDouble(x, y)));
						}
						// if we found a pixel already, compare distances
						else {
							if (  distances.getValueDouble(x, y) 
									> maxValues.get(new Integer(label)).doubleValue()) {
								maxPoints.put(new Integer(label), 
										new Point2D.Double(x, y));
								maxValues.put(new Integer(label), 
										new Double(distances.getValueDouble(x, y)));
							}
						}
					}
				}
				
				// draw IDs to label image
				this.idImage = this.labelImage.duplicate();
				for (MTBRegion2D r: this.getResultingRegions()) {
					// mark distance maximum of region by index
					Point2D.Double p = maxPoints.get(new Integer(r.getID()));
					int comX = (int)p.x;
					int comY = (int)p.y;
					this.idImage = drawStringToImage(this.idImage, 
							Integer.toString(r.getID()), comX-5, comY);
				}
				// make sure that the ID image is of a common type
				this.idImage = this.idImage.convertType(
						MTBImageType.MTB_BYTE, true);
			}
		}
		
		if (this.createColorImage) {
			DrawRegion2DSet drawer = new DrawRegion2DSet(DrawType.COLOR_IMAGE, this.getResultingRegions());
			drawer.setRandom(new Random(1));
			drawer.runOp(null);
			this.colorImage = drawer.getResultImage();
			if (this.labelImage != null)
				this.colorImage.setTitle(MTBImage.getTitleRunning(this.labelImage.getTitle()));
			else
				this.colorImage.setTitle(MTBImage.getTitleRunning(this.inputImage.getTitle()));
		}
	}
	
	/**
	 * Sequential component labeling
	 * @param img				(binary) input image
	 * @param diagonalNeighbors	set true for 8-neighborhood components, set false for 4-neighborhood
	 * @return 					returns a vector of regions	
	 */
	protected MTBRegion2DSet labelComponents(MTBImage img, boolean diagonalNeighbors) {
		
		m_width= img.getSizeX();
		m_height= img.getSizeY();
		
		Vector<MTBRegion2D> regions= new Vector<MTBRegion2D>();
		
        
        m_labelImg = (MTBImageInt)MTBImage.createMTBImage(m_width, m_height, 1, 1, 1, MTBImageType.MTB_INT);
        m_labelImg.setTitle(img.getTitle() + " labels");
        
        // western, northwestern, northern, northeastern label value of the actual pixel
        int l_w,l_nw,l_n,l_ne;
        
        // if only 4-neighborhood is regarded
        // set the diagonal neighors to zero to leave them
        // out of consideration in the following algorithm
        l_nw = 0;
        l_ne = 0;
        
        // smallest neigbor-label
        int l_min;
        
        // count of labeled neighbors
        int l_cnt;
        
        // label variable, which is incremented for new labels
		int label = 1;
		
		
		// collision sets for certain labels (vectorindex = label-1)
		Vector< LinkedHashSet<Integer> > collisions = new Vector< LinkedHashSet<Integer> >();
		
		// tmp references to collision sets
		LinkedHashSet<Integer> tmpSet1;
		LinkedHashSet<Integer> tmpSet2;
		// Iterator over the set elements
		Iterator<Integer> setIter;
		Integer labelInt;
		
		// first step: 
		// run line by line through the image
		// and label pixels		
		for (int y = 0; y < m_height; y++) {
			for (int x = 0; x < m_width; x++) {
				
				if (img.getValueDouble(x,y) > 0.0 && m_labelImg.getValueInt(x,y)==0) {
					
					l_cnt = 0;
					l_min = 0;
					
					if (diagonalNeighbors) {
						
						// get relevant neighboring labels (8-neighborhood)
						l_w = getLabel(x-1,y);
						l_nw = getLabel(x-1,y-1);
						l_n = getLabel(x,y-1);
						l_ne = getLabel(x+1,y-1);
					
						// determine smallest label and count labeled neighbors
						if (l_w > 0) {
							l_cnt++;
							l_min = l_w;
						}
						
						if (l_nw > 0) {
							l_cnt++;
							
							if (l_min == 0 || l_nw < l_min) {
								l_min = l_nw;
							}
						}
						
						if (l_n > 0) {
							l_cnt++;
							
							if (l_min == 0 || l_n < l_min) {
								l_min = l_n;
							}
						}
						
						if (l_ne > 0) {
							l_cnt++;
						
							if (l_min == 0 || l_ne < l_min) {
								l_min = l_ne;
							}
						}						
					}
					else {
						
						// get relevant neighboring labels (4-neighborhood)
						l_w = getLabel(x-1,y);
						l_n = getLabel(x,y-1);
					
						// determine smallest label and count labeled neighbors
						if (l_w > 0) {
							l_cnt++;
							l_min = l_w;
						}
						
						if (l_n > 0) {
							l_cnt++;
							
							if (l_min == 0 || l_n < l_min) {
								l_min = l_n;
							}
						}									
					}

					if (l_cnt == 0) {
					// no neighbors of interest are labeled
						
						// give pixel a new label
						m_labelImg.putValueInt(x,y,label);
						
						// create the collision set for that label
						// only containing the new label (-> no collision)
						tmpSet1 = new LinkedHashSet<Integer>();
						tmpSet1.add(label);
						
						// add the new collision set to the vector of collision sets
						collisions.add(tmpSet1);
						
						label++;
					}
					else if (l_cnt == 1) {
					// no label-collision, only one neighbor is labeled, adopt the label
						
						m_labelImg.putValueInt(x, y, l_min);
					}
					else {
					// there might be a label-collision! More than one neighbor is labeled	
						
						
						// assign lowest label to the actual pixel
						m_labelImg.putValueInt(x, y, l_min);
						
						// get the collision set of the label l_min
						tmpSet1 = collisions.get(l_min - 1);
						
						// test for label collisions
						if (l_w > 0 && l_w != l_min) {
							
							// get collision set of the colliding label
							tmpSet2 = collisions.get(l_w - 1);
							
							// if the collision sets are not already the same, join them
							if (tmpSet2 != tmpSet1) {
								setIter = tmpSet2.iterator();
								
								while (setIter.hasNext()) {
									labelInt = setIter.next();
									tmpSet1.add(labelInt);
									collisions.set(labelInt.intValue() - 1, tmpSet1);
								}
							}	
						}
								
						if (l_nw > 0 && l_nw != l_min) {
							
							// get collision set of the colliding label
							tmpSet2 = collisions.get(l_nw - 1);
							
							// if the collision sets are not already the same, join them
							if (tmpSet2 != tmpSet1) {
								setIter = tmpSet2.iterator();
								
								while (setIter.hasNext()) {
									labelInt = setIter.next();
									tmpSet1.add(labelInt);
									collisions.set(labelInt.intValue() - 1, tmpSet1);
								}
							}	
						}						
						
						if (l_n > 0 && l_n != l_min) {
							
							// get collision set of the colliding label
							tmpSet2 = collisions.get(l_n - 1);
							
							// if the collision sets are not already the same, join them
							if (tmpSet2 != tmpSet1) {
								setIter = tmpSet2.iterator();
								
								while (setIter.hasNext()) {
									labelInt = setIter.next();
									tmpSet1.add(labelInt);
									collisions.set(labelInt.intValue() - 1, tmpSet1);
								}
							}	
						}					
																						
						if (l_ne > 0 && l_ne != l_min) {
							
							// get collision set of the colliding label
							tmpSet2 = collisions.get(l_ne - 1);
							
							// if the collision sets are not already the same, join them
							if (tmpSet2 != tmpSet1) {
								setIter = tmpSet2.iterator();
								
								while (setIter.hasNext()) {
									labelInt = setIter.next();
									tmpSet1.add(labelInt);
									collisions.set(labelInt.intValue() - 1, tmpSet1);
								}
							}	
						}											
					}
				}
			}
		}
		
		// run over collision sets and build a simple label mapping array (labelMap):
		// (index of the array - 1) corresponds to the found label
		// the array value corresponds to the value to which the found label is mapped
		int[] labelMapping = new int[label - 1];
		for (int i = 0; i < labelMapping.length; i++) {
			
			// get the collision set for label (i+1)
			tmpSet1 = collisions.get(i);
	
			// set the label mapping to the first label in the set
			setIter = tmpSet1.iterator();
			labelMapping[i] = setIter.next().intValue();		
		}
		
		// clean up
		collisions.clear();
		
		
		// second step: 
		// run line by line through the image and resolve the label collisions,
		// create Region objects and compute its properties
		
		// relates a label (int) to a Region object
		HashMap<Integer,MTBRegion2D> rmap = new HashMap<Integer,MTBRegion2D>();
		Vector<Integer> real_labels = new Vector<Integer>();
		MTBRegion2D act_region;
		int real_label;
		
		for (int y = 0; y < m_height; y++) {
			for (int x = 0; x < m_width; x++) {
				
				label = m_labelImg.getValueInt(x,y);
				
				// if a labeled pixel is found
				if (label != 0) {
					
					real_label = labelMapping[label - 1];
					
					if (rmap.containsKey(real_label)) {
					// update region					
						
						act_region = rmap.get(real_label);
						act_region.addPixel(x, y);
						m_labelImg.putValueInt(x, y, real_label);
					}
					else {
					// new region
						
						act_region = new MTBRegion2D();
						act_region.addPixel(x, y);	
						rmap.put(real_label, act_region);
						
						real_labels.add(real_label);
						m_labelImg.putValueInt(x, y, real_label);
					}				
				}
			}
		}
		
		m_labelImg = null;
		
		// build vector of regions
		for (int i = 0; i < real_labels.size(); i++) {
			act_region = rmap.get(real_labels.get(i));
			act_region.setID(i+1);
			regions.add(act_region);
		}
		
		// clean up
		rmap.clear();
		real_labels.clear();
		
		return new MTBRegion2DSet( regions, 0.0, 0.0, img.getSizeX()-1.0, img.getSizeY()-1.0);
	}
	
	/** returns the value of labelImg at position (x,y).
	 * Also checks for image boundaries, returns 0 when (x,y) is outside of the image
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param labelImg actual label image
	 * @return pixel value at (x,y) if (x,y) inside the image. If outside of the image, returns 0
	 */
	private int getLabel(int x, int y) {
		
		if ((x >= 0) && (x < m_width) && (y >= 0) && (y < m_height)) {
			return m_labelImg.getValueInt(x,y);
		}
		else {
			return 0;
		}
	}
	
	/**
	 * Draws string at given position into image. 
	 * 
	 * @param labelImage2		Image where to draw the string into.
	 * @param s			String to draw.
	 * @param xPos	Position of string in x.
	 * @param yPos	Position of string in y.
	 * @return Result image.
	 */
	protected static MTBImage drawStringToImage(MTBImage labelImage2, 
			String s, int xPos, int yPos) {	
		ImageProcessor ip= labelImage2.getImagePlus().getProcessor();
		ip.moveTo(xPos, yPos);
		ip.setColor(Color.white);
		ip.drawString(s);
		ImagePlus newLabelImg = new ImagePlus("Label image", ip);
		return MTBImage.createMTBImage(newLabelImg);
	}
}
