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

package de.unihalle.informatik.MiToBo.apps.plantCells.plastids;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations .ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;

import java.awt.geom.Point2D;
import java.util.Vector;

/**
 * Operator to detect plastid regions in 2D microscope images.
 * 
 * @author Julian Wendland
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class PlastidDetector2D extends MTBOperator {

	/**
	 * Gray-scale input image.
	 */
	@Parameter(label = "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image.")
	private MTBImage inImg = null;
	
	/**
	 * Particle detector object.
	 */
	@Parameter( label = "Particle detector", required = false, 
		direction = Parameter.Direction.IN,	mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Detector.")
	protected ParticleDetectorUWT2D particleOp = null;

	/**
	 * Set of detected plastid regions with stromuli.
	 */
	@Parameter(label = "Plastid Regions", dataIOOrder = 1,
			direction = Direction.OUT, 
			description = "Resulting plastid region set.")
	private MTBRegion2DSet plastidRegions = null;

	/**
	 * Label image of detected plastid regions with stromuli. 
	 */
	@Parameter(label = "Result Label Image", dataIOOrder = 2,
			direction = Parameter.Direction.OUT, 
			description = "Label image of detected plastids with stromuli.")
	private MTBImageShort resultLabelImage = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of operate failure.
	 */
	public PlastidDetector2D() throws ALDOperatorException {
	}		
	
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		int xSize = this.inImg.getSizeX();
		int ySize = this.inImg.getSizeY();
		int zSize = this.inImg.getSizeZ();
		int tSize = this.inImg.getSizeT();
		int cSize = this.inImg.getSizeC();
		
		MTBImageByte plastidImg = 
				(MTBImageByte)MTBImage.createMTBImage(xSize, ySize, zSize, 
						tSize,	cSize, MTBImageType.MTB_BYTE);
		this.resultLabelImage = (MTBImageShort)MTBImage.createMTBImage(
				xSize, ySize, zSize, tSize,	cSize, MTBImageType.MTB_SHORT);
		
		// particle detection
		MTBRegion2DSet part_results = null;
		if (this.particleOp == null) {
			this.particleOp = new ParticleDetectorUWT2D ();
			this.particleOp.setJmin(3);
			this.particleOp.setJmax(4);
			this.particleOp.setScaleIntervalSize(1);
			this.particleOp.setCorrelationThreshold(1.5);
			this.particleOp.setMinRegionSize(30);
		}
		this.particleOp.setInputImage(this.inImg);
		this.particleOp.runOp();
		part_results = this.particleOp.getResults();
		plastidImg.fillBlack();

		// do some pre-filtering: non-circular regions are removed
		MTBRegion2DSet filteredParticleRegions = new MTBRegion2DSet();
		for (MTBRegion2D r: part_results) {
			if (r.getCircularity() > 0.5)
				filteredParticleRegions.add(r);
		}
		part_results = filteredParticleRegions;

		// eliminate regions which are too dark
		filteredParticleRegions = new MTBRegion2DSet();
		Vector<Double> avgIntensities = new Vector<Double>();
		for (MTBRegion2D r: part_results) {
			int sum = 0;
			for (Point2D.Double p: r.getPoints())
				sum += this.inImg.getValueInt((int)p.x, (int)p.y);
			avgIntensities.add(new Double(sum/r.getPoints().size()));
		}
		double meanAvgIntensity = 0;
		for (Double d: avgIntensities)
			meanAvgIntensity += d.doubleValue();
		meanAvgIntensity /= avgIntensities.size();
		double varAvgIntensity = 0;
		for (Double d: avgIntensities)
			varAvgIntensity += (d.doubleValue()-meanAvgIntensity)
			*(d.doubleValue()-meanAvgIntensity);
		varAvgIntensity /= avgIntensities.size();
		double stdDevAvgIntensity = Math.sqrt(varAvgIntensity);
		int c=0;
		for (MTBRegion2D r: part_results) {
			if (   avgIntensities.get(c).doubleValue() 
					>= meanAvgIntensity-2*stdDevAvgIntensity)
				filteredParticleRegions.add(r);
			++c;
		}			
		part_results = filteredParticleRegions;			

		// visualize plastid regions in image
		for(int r = 0; r<part_results.size(); ++r) {
			for (int s= 0; s<part_results.get(r).getPoints().size(); ++s)	{
				plastidImg.putValueInt(
						(int)part_results.get(r).getPoints().get(s).getX(), 
						(int)part_results.get(r).getPoints().get(s).getY(), 255);
			}
		}
		
		// label plastids
		LabelComponentsSequential lableOp = 
				new LabelComponentsSequential(plastidImg,true);
		lableOp.runOp();
		this.plastidRegions = lableOp.getResultingRegions();
		
		// TODO für alle Regionen lokale ROI anschauen und binarisieren,
		//      um bessere Segmentierung des Plastids zu erhalten,
		//      Region dann updaten
	}
	
	/**
	 * Specify input image to process.
	 * @param image	Image to process.
	 */
	public void setInputImage(MTBImage image) {
		this.inImg = image;
	}
	 
	/**
	 * Specify particle detector to apply.
	 * @param pd	Particle detector object.
	 */
	public void setDetector(ParticleDetectorUWT2D pd) {
		this.particleOp = pd;
	}
	
	/**
	 * Access detected plastid regions.
	 * @return	Set of detected plastid regions.
	 */
	public MTBRegion2DSet getPlastidRegions() {
		return this.plastidRegions;
	}
}

