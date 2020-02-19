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

package de.unihalle.informatik.MiToBo.apps.neurites2D;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2D;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DSet;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * This operator detects particles of a neuron image, for example from FISH
 * analysis. Particles in the complete image or inside the neurite regions of a
 * given neurite set can be detected.
 * 
 * @see ParticleDetectorUWT2D
 * 
 * @author Danny Misiak
 * 
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION, allowBatchMode = true)
public class NeuriteParticleDetector2D extends MTBOperator {

		/**
		 * Define extractor parameters.
		 */

		// --- input parameters ---

		@Parameter(label = "Particle Channel", direction = Parameter.Direction.IN, required = true, description = "Image channel including particles (1 labels first channel).", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private Integer particleChannel = 2;

		@Parameter(label = "Particle Detector", required = true, direction = Parameter.Direction.IN, description = "Particle detector.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private ParticleDetectorUWT2D particleDetector = new ParticleDetectorUWT2D();

		// --- supplemental parameters ---

		// --- output parameters ---

		@Parameter(label = "Detected Particle Regions", direction = Parameter.Direction.OUT, required = true, description = "Regions of detected particles.")
		private transient MTBRegion2DSet detectedParticles = null;

		// // --- image parameters ---
		// private transient int width;
		// private transient int height;
		// private transient int sizeC;

		/**
		 * Set of neurite regions for detecting particle only inside these regions, an
		 * be null for detection all particles in the complete image
		 */
		private transient MTBNeurite2DSet neuriteSet = null;

		/**
		 * Standardconstructor.
		 * 
		 * @throws ALDOperatorException
		 */
		public NeuriteParticleDetector2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor.
		 * 
		 * @param _particleChannel
		 *          channel number of particles in the multi-fluorescence image (1
		 *          labels first channel)
		 * @param _particleDetector
		 *          detector for particle detection
		 * @param _neuriteSet
		 *          set of neurite regions for detecting particle only inside these
		 *          regions, can be null for detection all particles in the complete
		 *          image
		 * @param _verbose
		 *          flag for standard console outputs
		 * @throws ALDOperatorException
		 */
		public NeuriteParticleDetector2D(Integer _particleChannel,
		    ParticleDetectorUWT2D _particleDetector, MTBNeurite2DSet _neuriteSet,
		    Boolean _verbose) throws ALDOperatorException {
				this.particleChannel = _particleChannel;
				this.particleDetector = _particleDetector;
				this.neuriteSet = _neuriteSet;
				this.verbose = _verbose;
		}

		/**
		 * Get image channel including particles.
		 */
		public Integer getParticleChannel() {
				return particleChannel;
		}

		/**
		 * Get particle detector.
		 */
		public ParticleDetectorUWT2D getParticleDetector() {
				return particleDetector;
		}

		/**
		 * 
		 * Set particle image for particle detector.
		 */
		public void setParticleImage(MTBImage particleImage) {
				particleDetector.setInputImage(particleImage);
		}

		/**
		 * Get the input set of neurites.
		 */
		public MTBNeurite2DSet getNeuriteSet() {
				return neuriteSet;
		}

		/**
		 * Set the input set of neurites.
		 */
		public void setNeuriteSet(MTBNeurite2DSet _neuriteSet) {
				neuriteSet = _neuriteSet;
		}

		/**
		 * Get regions of detected particles.
		 */
		public MTBRegion2DSet getDetectedParticles() {
				return detectedParticles;
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {
				// get particle detector input image
				MTBImage particleImage = particleDetector.getInputImage();
				particleImage = particleImage.getSlice(0, 0, particleChannel - 1);
				// set new input image including only particle channel
				particleDetector.setInputImage(particleImage);
				// run particle detector
				particleDetector.runOp();

				if (neuriteSet == null) {
						// get regions of detected particles in the complete image
						detectedParticles = particleDetector.getResults();
				} else {
						MTBRegion2DSet particleSet = particleDetector.getResults();
						detectedParticles = new MTBRegion2DSet(particleSet.getXmin(), particleSet
						    .getYmin(), particleSet.getXmax(), particleSet.getYmax());
						// get regions of detected particles inside the neurite regions
						for (int i = 0; i < neuriteSet.size(); i++) {
								MTBNeurite2D tmpNeurite = neuriteSet.getElementAt(i);
								MTBRegion2D tmpNeuriteRegion = tmpNeurite.getNeuriteRegion();
								Vector<Point2D.Double> tmpRegionPoints = tmpNeuriteRegion.getPoints();
								for (int j = 0; j < particleSet.size(); j++) {
										MTBRegion2D tmpParticleRegion = particleSet.elementAt(j);
										// compare if particle is inside the neurite region
										Vector<Point2D.Double> tmpParticlePoints = tmpParticleRegion
										    .getPoints();
										int counter = 0;
										for (int p = 0; p < tmpParticlePoints.size(); p++) {
												Point2D.Double tmpP = tmpParticlePoints.elementAt(p);
												if (tmpRegionPoints.contains(tmpP)) {
														counter++;
												}
										}
										double res = (((double) counter) / ((double) tmpParticleRegion
										    .getArea()));
										// TODO fixed parameter due to location shift
										if (res > 0.50) {
												detectedParticles.add(tmpParticleRegion);
										}
								}
						}
				}
		}
}
