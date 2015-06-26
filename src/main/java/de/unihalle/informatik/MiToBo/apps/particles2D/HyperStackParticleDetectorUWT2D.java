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
 * $Rev: 3725 $
 * $Date: 2011-04-28 11:04:48 +0200 (Do, 28 Apr 2011) $
 * $Author: moeller $
 * 
 */

package de.unihalle.informatik.MiToBo.apps.particles2D;

import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/**
 * A particle detector that runs the {@link ParticleDetectorUWT2D} given as input (with all parameters set)
 * over all slices of the input image for a given channel. The info strings of the resulting region sets
 * are set to "z=.,t=.,c=channelIdx".
 * 
 * @author Oliver Gress
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,level=Level.STANDARD)
public class HyperStackParticleDetectorUWT2D extends ALDOperator implements StatusReporter {

	@Parameter(label="Input image", required=true, direction = Parameter.Direction.IN,
			mode=ExpertMode.STANDARD, dataIOOrder=1, description="Input image")
	protected transient MTBImage inputImage;
	
	@Parameter(label="Particle detector", required=true, direction = Parameter.Direction.IN,
			mode=ExpertMode.STANDARD, dataIOOrder=2, description="Particle detector for 2D-images based on UWT")
	protected ParticleDetectorUWT2D pdetector;
	
	@Parameter(label="Channel index", required=true, direction = Parameter.Direction.IN,
			mode=ExpertMode.STANDARD, dataIOOrder=3, description="Index of the channel where particles are to be detected")
	protected int channelIdx = 0;
	
	@Parameter(label="Resulting regionsets", required=false, direction = Parameter.Direction.OUT,
			mode=ExpertMode.STANDARD, dataIOOrder=1, description="Bag of resulting region sets")
	protected MTBRegion2DSetBag resultingRegionsets = null;
	
	
	public HyperStackParticleDetectorUWT2D() throws ALDOperatorException {
		super();
		
		this.completeDAG = false;
		
		statusListeners = new Vector<StatusListener>(1);
	}

	/** vector of installed StatusListeners */
	protected transient Vector<StatusListener> statusListeners;
	
	public HyperStackParticleDetectorUWT2D(MTBImage inputImage,
											ParticleDetectorUWT2D pdetector,
											int channelIdx) throws ALDOperatorException {

		this.inputImage = inputImage;
		this.pdetector = pdetector;
		this.channelIdx = channelIdx;
		
		this.completeDAG = false;
		
		statusListeners = new Vector<StatusListener>(1);
	}

	public MTBRegion2DSetBag getResultingRegionsets() {
		return this.resultingRegionsets;
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		
		if (this.channelIdx < 0 || this.channelIdx >= this.inputImage.getSizeC())
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
					"HStackParticleDetector.validateCustom(): Channel idx must be >= 0 and < number of channels. " +
					"Specified channel index: " + this.channelIdx);
		
	}
	
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {
		int nslices = this.inputImage.getSizeT()*this.inputImage.getSizeZ();
		this.resultingRegionsets = new MTBRegion2DSetBag(nslices);
		
		// ---- detect particles for each time step
		MTBImage frameImg = null;
		MTBRegion2DSet regset = null;
		for (int t = 0; t < this.inputImage.getSizeT(); t++) {
			for (int z = 0; z < this.inputImage.getSizeZ(); z++) {
			
				this.notifyListeners(new StatusEvent(t*this.inputImage.getSizeZ() + z, nslices - 1, 
						"Detecting particles for slice z="+z+",t="+t+",c="+this.channelIdx));
				
				if (this.verbose)
					System.out.println("Detecting particles for slice z="+z+",t="+t+",c="+this.channelIdx);
				
				// get 2D-image at coordinate z and timestep t of specified channel
				frameImg = this.inputImage.getSlice(null, z, t, this.channelIdx);
				
				// set as input to detector
				this.pdetector.setInputImage(frameImg);
				
				// run detector
				this.pdetector.runOp(true);

				regset = this.pdetector.getResults();
				regset.setInfo("z="+z+",t="+t+",c="+this.channelIdx);
				
				this.resultingRegionsets.add(regset);
			}
		}

	}

	public void setInputImage(MTBImage inputimage) {
		this.inputImage = inputimage;
	}
	
	public ParticleDetectorUWT2D getParticleDetector2D() {
		return this.pdetector;
	}

	// ----- StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener statuslistener) {	
		statusListeners.add(statuslistener);	
	}

	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < statusListeners.size(); i++) {
			statusListeners.get(i).statusUpdated(e);
		}
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		statusListeners.remove(statuslistener);
	}	

}
