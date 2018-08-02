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

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.operators;

import java.awt.geom.Point2D;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.apps.plantCells.plastids.PlastidDetector2DParticlesUWT;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarker;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShape;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShapeRegion;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents.BorderType;

/**
 * Cell counter detector for detecting plastids.
 *  
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE)
@ALDDerivedClass
public class CellCounterDetectOperatorParticlesUWT 
	extends CellCounterDetectOperator {
	
	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = "[Particles with UWT] ";

	/**
	 * Minimal scale to consider.
	 */
	@Parameter( label= "Jmin", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Minimum scale index.")
	private Integer Jmin = new Integer(3);

	/**
	 * Maximum scale to consider.
	 */
	@Parameter( label= "Jmax", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 3, description = "Maximum scale index.")
	private Integer Jmax = new Integer(4);

	/**
	 * Size of scale interval for calculating wavelet correlation images.
	 */
	@Parameter( label= "Scale-interval size", required = true, 
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		dataIOOrder = 4, 
		description = "Size of scale interval for correlation images.")
	private Integer scaleIntervalSize = new Integer(1);
	
	/**
	 * Threshold for correlation images.
	 */
	@Parameter( label= "Correlation threshold", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 5, 
		description = "Threshold for wavelet correlation images.")
	private Double corrThreshold = new Double(1.5);

	/**
	 * Minimal size of valid regions.
	 */
	@Parameter( label= "Minimum region size", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 6, description = "Minimum area of detected regions.")
	private int minRegionSize = 1;
	
	/**
	 * Particle detector object.
	 */
	protected ParticleDetectorUWT2D particleOp;

	/**
	 * Configuration frame for particle detector.
	 */
//	protected OperatorConfigWin particleConfigureFrame;

	/**
	 * Constructor.	
	 * @throws ALDOperatorException Thrown in case of initialization error.
	 */
	public CellCounterDetectOperatorParticlesUWT() 
			throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		// configure the particle detector, except for the input image
		// which we do not know yet
	  this.particleOp = new ParticleDetectorUWT2D();
	  this.particleOp.setJmin(3);
	  this.particleOp.setJmax(4);
	  this.particleOp.setScaleIntervalSize(1);
	  this.particleOp.setMinRegionSize(1);
	  this.particleOp.setCorrelationThreshold(1.5);
//	  this.particleConfigureFrame =	new OperatorConfigWin(this.particleOp);
	}

	@Override
  protected void operate() 
  		throws ALDOperatorException, ALDProcessingDAGException {
		
		// post ImageJ status
		String msg = opIdentifier + "running plastid detection...";	
		this.notifyListeners(new StatusEvent(msg));

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "running plastid detection...");

		PlastidDetector2DParticlesUWT pd = new PlastidDetector2DParticlesUWT();
		pd.setInputImage(this.inputImage);		
		if (this.particleOp != null) {
			this.particleOp.addStatusListener(this);
			pd.setDetector(this.particleOp);
		}
		this.particleOp.setJmin(this.Jmin.intValue());
		this.particleOp.setJmax(this.Jmax.intValue());
		this.particleOp.setScaleIntervalSize(this.scaleIntervalSize.intValue());
		this.particleOp.setMinRegionSize(this.minRegionSize);
		this.particleOp.setCorrelationThreshold(this.corrThreshold.doubleValue());
		pd.runOp();
		MTBRegion2DSet resultPlastidRegions = pd.getPlastidRegions();
		int resultPlastidCount = resultPlastidRegions.size();

		// format results
		Vector<CellCntrMarker> markers = new Vector<>();
		
		// extract borders for all regions
		BordersOnLabeledComponents blc = new BordersOnLabeledComponents(null,
			resultPlastidRegions, BorderConnectivity.CONNECTED_8, 
				BorderType.OUT_IN_BORDERS, 1);
		blc.runOp(null);				
		MTBBorder2DSet borders = blc.getResultBorders();

		for (int i=0; i<resultPlastidRegions.size(); ++i) {
			MTBRegion2D reg = resultPlastidRegions.elementAt(i);
			MTBBorder2D bor = borders.elementAt(i);

			// calculate average intensity
			double intensity = 0; 
			for (Point2D.Double p: reg.getPoints()) {
				intensity += this.inputImage.getValueDouble((int)p.x, (int)p.y,	0);
			}
			CellCntrMarkerShape s = new CellCntrMarkerShapeRegion(reg, bor);
			s.setAvgIntensity(intensity/reg.getArea());
			CellCntrMarker marker = new CellCntrMarker(
				(int)reg.getCenterOfMass_X(), (int)reg.getCenterOfMass_Y(), 
					this.detectZSlice, s);
			markers.add(marker);
		}
		this.detectResults = markers; 
				
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + 
				"\t -> Number of detected plastids: " + resultPlastidCount);

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "Operations finished!");
		
		// post ImageJ status
		msg = opIdentifier + "Operations finished!";	
		this.notifyListeners(new StatusEvent(msg));
  }

	@Override
	public String getShortName() {
		return "Particles with UWT";
	}

	@Override
	public String getUniqueClassIdentifier() {
		return "ParticlesUWT";
	}

}
