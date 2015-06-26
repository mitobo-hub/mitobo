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
 * $Rev: 5288 $
 * $Date: 2012-03-29 10:27:02 +0200 (Thu, 29 Mar 2012) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.visualization.drawing;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphNode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.DataConverter;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.PartitGraphNodeID;

/**
 * Draw trajectories of tracked observations over time.
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ExecutionMode.NONE, level=Level.STANDARD)
public class DrawTracks2D extends MTBOperator {

	@Parameter(label="Input image", direction=Direction.IN, required=true, dataIOOrder=1,
			mode=ExpertMode.STANDARD, description="Image used for drawing tracks")
	public MTBImage inputImage;

	@Parameter(label="Detected regions", direction=Direction.IN, required=false, dataIOOrder=2,
			mode=ExpertMode.STANDARD, description="Detected regions whose IDs determine target association")
	public MTBRegion2DSetBag detectedRegions = null;

	@Parameter(label="Observations", direction=Direction.IN, required=false, dataIOOrder=3,
			mode=ExpertMode.STANDARD, description="Observations whose IDs determine target association")
	public Vector<MultiState<MotionModelID>> observations;

	@Parameter(label="Track graphs", direction=Direction.IN, required=false, dataIOOrder=4,
			mode=ExpertMode.STANDARD, description="Graphs that determine target association. If specified IDs in observations or detected regions are ignored.")
	public Vector<MTBGraph> trackgraphs = null;
	
	@Parameter(label="Track colors", direction=Direction.IN, required=false, dataIOOrder=7,
			mode=ExpertMode.STANDARD, description="Color lookup table of track colors")
	public DynamicColorLUT trackcolors = null;
	
	@Parameter(label="Track image", direction=Direction.OUT, required=false, dataIOOrder=1,
			mode=ExpertMode.STANDARD, description="Image with tracks drawn")
	protected MTBImage trackImage;
	
	@Parameter(label="Draw trajectories", direction=Direction.IN, required=true, dataIOOrder=5,
			mode=ExpertMode.STANDARD, description="Flag to draw trajectories")
	public boolean drawTrajectories = true;
	
	@Parameter(label="Draw spots", direction=Direction.IN, required=true, dataIOOrder=6,
			mode=ExpertMode.STANDARD, description="Flag to draw spots (otherwise squares indicate target location)")
	public boolean drawSpots = true;
	
	/**
	 * Constructor.
	 */
	public DrawTracks2D() throws ALDOperatorException {
		super();
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.observations == null && this.detectedRegions == null)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
					"Observations and detected regions must not both be null.");
	}
	
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {

		if (this.trackcolors == null) {
			this.trackcolors = new DynamicColorLUT(new Random(0));
			this.trackcolors.setColor(0, (255 << 16));
		}
		
		if (this.detectedRegions == null) {
			this.detectedRegions = 
				DataConverter.observationsToRegions(true, 
						this.observations, 0, this.inputImage.getSizeX()-1, 0, this.inputImage.getSizeY()-1);
		}
		
		if (this.observations == null) {
			this.observations = DataConverter.regionsToObservations(true, this.detectedRegions);
		}
		
		this.trackImage = this.inputImage.convertType(MTBImageType.MTB_RGB, true);

		if (this.trackgraphs != null) {
			// sort the nodes of the track-subgraphs by partition, i.e. time index
			Vector<TreeSet<PartitGraphNodeID>> sortedNodes = new Vector<TreeSet<PartitGraphNodeID>>(this.trackgraphs.size());
			for (int s = 0; s < trackgraphs.size(); s++) {
	
				Vector<MTBGraphNode<?>> sgnodes = trackgraphs.get(s).getNodes();
				sortedNodes.add(new TreeSet<PartitGraphNodeID>());
				
				for (int n = 0; n < sgnodes.size(); n++)
					sortedNodes.get(s).add((PartitGraphNodeID) sgnodes.get(n).getData());
			}
			
			for (int s = 0; s < sortedNodes.size(); s++) {
				
				TreeSet<PartitGraphNodeID> sortedGraph = sortedNodes.get(s);
				PartitGraphNodeID currentNode = null;
				
				if (sortedGraph.size() == 1) {
					currentNode = sortedGraph.first();

					this.observations.get(currentNode.partitionID).getStateDiscrete(currentNode.nodeID).ID = (short)(s+1);
					
					if (this.drawSpots)
						this.drawSpot(this.trackImage, this.detectedRegions.get(currentNode.partitionID).get(currentNode.nodeID), 
							currentNode.partitionID, 0, this.trackcolors.getColor(s+1));

					
					
				}
				else if (sortedGraph.size() > 1) {
					
					Iterator<PartitGraphNodeID> nodeIter = sortedGraph.iterator();
					PartitGraphNodeID prevNode = null;

					while (nodeIter.hasNext()) {
						currentNode = nodeIter.next();
						
						this.observations.get(currentNode.partitionID).getStateDiscrete(currentNode.nodeID).ID = (short)(s+1);
						
						if (this.drawSpots)
							this.drawSpot(this.trackImage, detectedRegions.get(currentNode.partitionID).get(currentNode.nodeID), 
									currentNode.partitionID, 0, this.trackcolors.getColor(s+1));
						else
							this.drawSqare(this.trackImage, detectedRegions.get(currentNode.partitionID).get(currentNode.nodeID), 
									currentNode.partitionID, 0, this.trackcolors.getColor(s+1), 4);
							
						if (prevNode != null) {
							if (this.drawTrajectories) {
								this.drawTrajectories(this.observations.get(prevNode.partitionID).getStateContinuous(prevNode.nodeID), 
										this.observations.get(currentNode.partitionID).getStateContinuous(currentNode.nodeID), 
										currentNode.partitionID, sortedGraph.last().partitionID, 0, this.trackImage, this.trackcolors.getColor(s+1));
							}
						}
						prevNode = currentNode;
					}
				}
			}
		}
		else {
			HashMap<Short,Integer> tracksEndTime = new HashMap<Short,Integer>();
			
			for (int t = 0; t < this.inputImage.getSizeT(); t++) {

				short id = 0;
				for (int i = 0; i < this.observations.get(t).getNumberOfStates(); i++) {
					
					id = this.observations.get(t).getStateDiscrete(i).ID;

					if (this.drawSpots)
						this.drawSpot(this.trackImage, this.detectedRegions.get(t).get(i), t, 0, this.trackcolors.getColor(id));
					
					if (id > 0) {
						
						if (!this.drawSpots)
							this.drawSqare(this.trackImage, this.detectedRegions.get(t).get(i), t, 0, this.trackcolors.getColor(id), 4);;
						
						// find last observation associated to target 'id'
						int tt = t-1;
						Matrix lastpt = null;
						
						while (lastpt == null && tt >= 0) {
							MultiState<MotionModelID> Z = this.observations.get(tt);
							for (int j = 0; j < Z.getNumberOfStates(); j++) {
								if (Z.getStateDiscrete(j).ID == id) {
									lastpt = Z.getStateContinuous(j);
									break;
								}
							}
							if (lastpt == null)
								tt--;
						}
						
						if (lastpt != null) {
							if (this.drawTrajectories) {
								
								int t_last;
								if (tracksEndTime.containsKey(id))
									t_last = tracksEndTime.get(id);
								else {
									t_last = t;
									for (int t_ = t+1; t_ < this.observations.size(); t_++) {
										MultiState<MotionModelID> Z_ = this.observations.get(t_);
										for (int i_ = 0; i_ < Z_.getNumberOfStates(); i_++) {
											if (Z_.getStateDiscrete(i_).ID == id) {
												t_last = t_;
											}
										}
									}
									tracksEndTime.put(id, t_last);
								}
								
								this.drawTrajectories(lastpt,
									this.observations.get(t).getStateContinuous(i), t, t_last, 0, this.trackImage, this.trackcolors.getColor(id));
							}
						}
					}
				}
				
				
			}
			
		}
		
	}
	
	public MTBImage getTrackImage() {
		return this.trackImage;
	}
	
	/**
	 * Draw square to an image at time=t and channel=c with specified color and size=2*<code>radius</code>+1.
	 */
	protected void drawSqare(MTBImage img, MTBRegion2D reg, int t, int c, int color, int radius) {
		
		int mx = (int)Math.round(reg.getCenterOfMass_X());
		int my = (int)Math.round(reg.getCenterOfMass_Y());

		for (int y = my - radius; y <= my + radius; y++) {
		
			if (y == my - radius || y == my + radius) {
				for (int x = mx - radius; x <= mx + radius; x++) {
					if (y >= 0 && y < img.getSizeY() && x >= 0 && x < img.getSizeX()) {
						img.putValueInt(x, y, 0, t, c, color);
					}
				}
			}
			else {
				if (y >= 0 && y < img.getSizeY() && mx - radius >= 0 && mx - radius < img.getSizeX()) {
					img.putValueInt(mx - radius, y, 0, t, c, color);
				}
				if (y >= 0 && y < img.getSizeY() && mx + radius >= 0 && mx + radius < img.getSizeX()) {
					img.putValueInt(mx + radius, y, 0, t, c, color);
				}
			}
		}
	}	
	
	/**
	 * Draw region to an image at time=t and channel=c with specified color.
	 */
	protected void drawSpot(MTBImage img, MTBRegion2D reg, int t, int c, int color) {
		
		Vector<Point2D.Double> pts = reg.getPoints();
		for (int i = 0; i < pts.size(); i++) {
			int x = (int)Math.round(pts.get(i).getX());
			int y = (int)Math.round(pts.get(i).getY());

			if (y >= 0 && y < img.getSizeY() && x >= 0 && x < img.getSizeX()) {
				img.putValueInt(x, y, 0, t, c, color);
			}
		}
	}	

	/**
	 * Draw trajectory between two points at the given and all later timepoints until t_last
	 */
	protected void drawTrajectories(Matrix oldP, Matrix newP, int t, int t_last, int c, MTBImage img, int color) {

		int idx = img.getCurrentSliceIndex();
		
		
		for (int tt = t; tt <= t_last; tt++) {
			img.setCurrentSliceCoords(0, tt, c);

			img.drawLine2D((int)Math.round(oldP.get(0, 0)), (int)Math.round(oldP.get(1, 0)), 
					(int)Math.round(newP.get(0, 0)), (int)Math.round(newP.get(1, 0)),  color);
		}
		
		img.setCurrentSliceIndex(idx);
	}
}
