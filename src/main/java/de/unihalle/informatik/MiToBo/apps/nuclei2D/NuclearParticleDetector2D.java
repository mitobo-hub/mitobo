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

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusDetector2D.NuclDetectMode;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.enhance.GammaCorrection2D;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter.SigmaInterpretation;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack.Mode;

/**
 * <pre>
 * 
 * This class detects nuclei and particles inside these nuclei and gives some
 * statistics about particles, like particle number per nuclei, particle size,
 * sum of intensities, etc.
 * Nuclei are detected via the NucleusDetector2D by using the Niblack
 * thresholding. As post-process the nuclei channel is gamma corrected and filter
 * with a small gaussian, due to uneven illumination of the nuclei in the images.
 * This may depends on the input image data and can be switched off separately.
 * Particles are detected via the ParticleDetectorUWT2D.
 * 
 * @see NucleusDetector2D
 * @see ParticleDetectorUWT2D
 * 
 * @author Danny Misiak
 * 
 * </pre>
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, 
	level = Level.APPLICATION, allowBatchMode = true,
	shortDescription="Detects nuclei and particles inside these nuclei.")
public class NuclearParticleDetector2D extends MTBOperator {

		// --- input parameters ---

		@Parameter(label = "Input Image", required = true, direction = Parameter.Direction.IN, description = "Input image.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImage inputImage = null;

		@Parameter(label = "Nuclei Channel", required = true, direction = Parameter.Direction.IN, description = "Channel number of nuclei stain.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private Integer nucChannel = 2;

		@Parameter(label = "Particle Channel", required = true, direction = Parameter.Direction.IN, description = "Channel number of particle stain.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private Integer partChannel = 1;

		@Parameter(label = "Use Gamma Correction", required = true, direction = Parameter.Direction.IN, description = "Flag to use gamma correction.", mode = ExpertMode.ADVANCED, callback = "useGammaCorrection", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE, dataIOOrder = 3)
		protected Boolean correctGamma = new Boolean(true);

		@Parameter(label = "Gamma Value", required = true, direction = Parameter.Direction.IN, description = "Gamma correction value.", mode = ExpertMode.ADVANCED, dataIOOrder = 4)
		protected Double gamma = new Double(0.5);

		@Parameter(label = "Use Gaussian Filter", required = true, direction = Parameter.Direction.IN, description = "Flag to use gaussian filter.", mode = ExpertMode.ADVANCED, dataIOOrder = 5)
		protected Boolean filterImage = new Boolean(true);

		@Parameter(label = "Nuclei Intensity in Particle Channel", required = true, direction = Parameter.Direction.IN, description = "Flag to use nuclei intensity in particle channel.", mode = ExpertMode.ADVANCED, dataIOOrder = 6)
		protected Boolean swapNuclei = new Boolean(false);

		@Parameter(label = "Nucleus Detector", required = true, direction = Parameter.Direction.IN, description = "Detector for nuclei.", mode = ExpertMode.ADVANCED, dataIOOrder = 7)
		private NucleusDetector2D nucDetector = new NucleusDetector2D(null,
		    NuclDetectMode.NIBLACK, new ImgThreshNiblack(null, Mode.WHOLE_IMAGE, 0.8,
		        -1, 0, -1, 0, null), true, 3, 1500, true);

		@Parameter(label = "Particle Detector", required = true, direction = Parameter.Direction.IN, description = "Detector for particles.", mode = ExpertMode.ADVANCED, dataIOOrder = 8)
		private ParticleDetectorUWT2D partDetector = new ParticleDetectorUWT2D();

		// --- supplemental parameters ---

		// --- output parameters ---

		// @Parameter(label = "Output Image", required = true, direction =
		// Parameter.Direction.OUT, description = "Analyzed image.")
		// private transient MTBImage outputImage = null;

		@Parameter(label = "Nuclei Regions", required = true, direction = Parameter.Direction.OUT, description = "Nuclei regions.")
		private transient MTBRegion2DSet nucleiRegions = null;

		@Parameter(label = "Particle Regions", required = false, direction = Parameter.Direction.OUT, description = "Particle regions.")
		private transient MTBRegion2DSet particleRegions = null;

		@Parameter(label = "Result Table", required = true, direction = Parameter.Direction.OUT, description = "Particle statistics.")
		private transient MTBTableModel resultTable = null;

		/**
		 * Image width.
		 */
		private int sizeX;

		/**
		 * Image height.
		 */
		private int sizeY;

		/**
		 * Name of input image file.
		 */
		private String fileName;

		/**
		 * Part of the input image, representing the stained nuclei.
		 */
		private transient MTBImage nucImage;

		/**
		 * Part of the input image, representing the stained particles.
		 */
		private transient MTBImage partImage;

		/**
		 * Standard constructor.
		 */
		public NuclearParticleDetector2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * 
		 * @param _inputImage
		 *          input image
		 * @param _nucChannel
		 *          channel number of nuclei stain
		 * @param _partChannel
		 *          channel number of particle stain
		 * @param _correctGamma
		 *          flag to use gamma correction
		 * @param _gamma
		 *          gamma correction value
		 * @param _filterImage
		 *          flag to use gaussian filter
		 * @param _nucDetector
		 *          detector for nuclei
		 * @param _partDetector
		 *          detector for particles
		 * @throws ALDOperatorException
		 */
		public NuclearParticleDetector2D(MTBImageByte _inputImage,
		    Integer _nucChannel, Integer _partChannel, Boolean _correctGamma,
		    Double _gamma, Boolean _filterImage, NucleusDetector2D _nucDetector,
		    ParticleDetectorUWT2D _partDetector) throws ALDOperatorException {
				this.inputImage = _inputImage;
				this.nucChannel = _nucChannel;
				this.partChannel = _partChannel;
				this.correctGamma = _correctGamma;
				this.gamma = _gamma;
				this.filterImage = _filterImage;
				this.nucDetector = _nucDetector;
				this.partDetector = _partDetector;
		}

		/**
		 * Custom validation of some input parameters.
		 */
		@Override
		public void validateCustom() throws ALDOperatorException {

				// test if nuclei channel number is in channel range of the image
				if (this.nucChannel < 1 || this.nucChannel > this.inputImage.getSizeC()) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NuclearParticleDetecxtor2D: validation failed!"
						        + "\nNuclei channel number must be in range [1, #ImageChannels].");
				}
				// test if particle channel number is in channel range of the image
				if (this.partChannel < 1
				    || this.partChannel > this.inputImage.getSizeC()) {
						throw new ALDOperatorException(
						    OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NuclearParticleDetecxtor2D: validation failed!"
						        + "\nParticle channel number must be in range [1, #ImageChannels].");
				}
				// test if nuclei channel number and particle channel number differs
				if (this.partChannel == this.nucChannel) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NuclearParticleDetecxtor2D: validation failed!"
						        + "\nParticle and nuclei channel must be different.");
				}
		}
		
		/**
		 * Callback routine to change parameters.
		 */
		@SuppressWarnings("unused")
		private void useGammaCorrection() {
				try {
						if (this.correctGamma.booleanValue() == false) {
								if (this.hasParameter("gamma")) {
										this.removeParameter("gamma");
								}
						} else {
								if (!this.hasParameter("gamma")) {
										this.addParameter("gamma");
								}
						}
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
		}

		/**
		 * Get input image.
		 */
		public MTBImage getInputImage() {
				return inputImage;
		}

		/**
		 * Set input image.
		 */
		public void setInputImage(MTBImageByte _inputImage) {
				this.inputImage = _inputImage;
		}

		/**
		 * Get nuclei channel number.
		 */
		public Integer getNucleiChannel() {
				return nucChannel;
		}

		/**
		 * Set nuclei channel number.
		 */
		public void setNucleiChannel(Integer _nucChannel) {
				this.nucChannel = _nucChannel;
		}

		/**
		 * Get particle channel number.
		 */
		public Integer getPartChannel() {
				return partChannel;
		}

		/**
		 * Set particle channel number.
		 */
		public void setPartChannel(Integer _partChannel) {
				this.partChannel = _partChannel;
		}

		/**
		 * Get flag to use gamma correction.
		 */
		public Boolean getCorrectGamma() {
				return correctGamma;
		}

		/**
		 * Set flag to use gamma correction.
		 */
		public void setCorrectGamma(Boolean _correctGamma) {
				this.correctGamma = _correctGamma;
		}

		/**
		 * Get gamma correction value.
		 */
		public Double getGamma() {
				return gamma;
		}

		/**
		 * Set gamma correction value.
		 */
		public void setGamma(Double _gamma) {
				this.gamma = _gamma;
		}

		/**
		 * Get flag to use gaussian filter.
		 */
		public Boolean getFilterImage() {
				return filterImage;
		}

		/**
		 * Set flag to use gaussian filter.
		 */
		public void setFilterImage(Boolean _filterImage) {
				this.filterImage = _filterImage;
		}

		/**
		 * Get detector for nuclei.
		 */
		public NucleusDetector2D getNucDetector() {
				return nucDetector;
		}

		/**
		 * Set detector for nuclei.
		 */
		public void setNucDetector(NucleusDetector2D _nucDetector) {
				this.nucDetector = _nucDetector;
		}

		/**
		 * Get detector for particles.
		 */
		public ParticleDetectorUWT2D getPartDetector() {
				return partDetector;
		}

		/**
		 * Set detector for particles.
		 */
		public void setPartDetector(ParticleDetectorUWT2D _partDetector) {
				this.partDetector = _partDetector;
		}

		// /**
		// * Get analyzed image.
		// */
		// public MTBImage getOutputImage() {
		// return outputImage;
		// }

		/**
		 * Get detected nuclei regions.
		 */
		public MTBRegion2DSet getNucleiRegions() {
				return nucleiRegions;
		}

		/**
		 * Get detected particle regions.
		 */
		public MTBRegion2DSet getParticleRegions() {
				return particleRegions;
		}

		/**
		 * Get particle statistics.
		 */
		public MTBTableModel getResultTable() {
				return resultTable;
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				/*
				 * Get some image properties.
				 */
				this.sizeX = this.inputImage.getSizeX();
				this.sizeY = this.inputImage.getSizeY();
				this.fileName = this.inputImage.getTitle();

				/*
				 * Process DAPI channel and detect nuclei.
				 */
				this.nucImage = this.inputImage.getImagePart(0, 0, 0, 0,
				    this.nucChannel.intValue() - 1, sizeX, sizeY, 1, 1, 1);

				MTBImage processedNucImage = this.nucImage.duplicate();

				// gamma correction
				if (this.correctGamma.booleanValue()) {
						GammaCorrection2D gammaCorrection = new GammaCorrection2D(
						    processedNucImage, this.gamma, 1);
						gammaCorrection.runOp();
						processedNucImage = gammaCorrection.getResultImage();
				}

				// gaussian filter with small sigma
				if (this.filterImage.booleanValue()) {
						GaussFilter gaussFilter = new GaussFilter(processedNucImage, 2.0, 2.0);
						gaussFilter.setSigmaInterpretation(SigmaInterpretation.PIXEL);
						gaussFilter.runOp();
						processedNucImage = gaussFilter.getResultImg();
				}

				// detect nuclei with NucleusDetector2D
				this.nucDetector.setInputImage(processedNucImage);
				this.nucDetector.runOp();
				MTBRegion2DSet tmpNucRegions = this.nucDetector.getResultData()
				    .getNucleiRegions();
				this.nucleiRegions = new MTBRegion2DSet(0, 0, sizeX - 1, sizeY - 1);

				// remove nuclei at image border
				for (int i = 0; i < tmpNucRegions.size(); i++) {
						boolean border = false;
						Vector<Point2D.Double> points = tmpNucRegions.elementAt(i).getPoints();
						for (int j = 0; j < points.size(); j++) {
								Point2D.Double p = points.elementAt(j);
								int x = (int) Math.round(p.x);
								int y = (int) Math.round(p.y);
								if (x == 0 || x == (sizeX - 1) || y == 0 || y == (sizeY - 1)) {
										border = true;
										break;
								}
						}
						if (!border) {
								this.nucleiRegions.add(tmpNucRegions.elementAt(i));
						}
				}

				/*
				 * Process particle channel and detect particles.
				 */
				this.partImage = this.inputImage.getImagePart(0, 0, 0, 0,
				    this.partChannel.intValue() - 1, this.sizeX, this.sizeY, 1, 1, 1);

				if (swapNuclei) {
						
						createSwapResultTable();
						
				} else {

						// detect particles with ParticleDetectorUWT2D
						this.partDetector.setInputImage(this.partImage);
						this.partDetector.runOp();

						// temporary detected particles
						MTBRegion2DSet tmpParticleRegions = this.partDetector.getResults();

						/*
						 * Remove particles outside nuclei regions and match particles to
						 * corresponding nuclei regions.
						 */

						// match particles to nuclei regions
						Vector<Vector<Integer>> particleMatching = matchParticles(tmpParticleRegions);

						/*
						 * Create output table with particle statistics.
						 */
						createResultTable(particleMatching);

						// /*
						// * Create output image of nuclei and particle channel. Detected nuclei
						// and
						// * particles are shown as yellow contours.
						// */
						// createOutputImage();
				}
		}

		/**
		 * Match particle regions to nuclei regions.
		 * 
		 * @return Indices of nuclei with corresponding particle indices.
		 */
		private Vector<Vector<Integer>> matchParticles(MTBRegion2DSet tmpPartRegions) {

				// create binary image of nuclei regions
				MTBImageByte nucImage = (MTBImageByte) MTBImage.createMTBImage(sizeX,
				    sizeY, 1, 1, 1, MTBImageType.MTB_BYTE);
				for (int i = 0; i < this.nucleiRegions.size(); i++) {
						MTBRegion2D reg = this.nucleiRegions.elementAt(i);
						try {
								nucImage = (MTBImageByte) reg.toMTBImage(null, nucImage);
						} catch (ALDOperatorException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
				}

				// get label image of binary nuclei regions
				LabelComponentsSequential lcs = null;
				MTBImage labeledNucleiImage = null;
				MTBRegion2DSet labeledNucleiRegions = null;
				try {
						lcs = new LabelComponentsSequential(nucImage, true);
						lcs.runOp(null);
						labeledNucleiImage = lcs.getLabelImage();
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}

				// create new empty particle region set
				this.particleRegions = new MTBRegion2DSet(0, 0, this.sizeX - 1,
				    this.sizeY - 1);

				// create empty matching vector
				Vector<Vector<Integer>> matchIndex = new Vector<Vector<Integer>>();
				for (int i = 0; i < this.nucleiRegions.size(); i++) {
						matchIndex.add(new Vector<Integer>());
				}

				// test for corresponding nuclei region for each particle
				for (int i = 0; i < tmpPartRegions.size(); i++) {
						Vector<Point2D.Double> RegionPoints = tmpPartRegions.elementAt(i)
						    .getPoints();
						boolean useRegion = true;
						int label = 0;
						for (int j = 0; j < RegionPoints.size(); j++) {
								Point2D.Double p = RegionPoints.elementAt(j);
								int x = (int) Math.round(p.getX());
								int y = (int) Math.round(p.getY());
								int tmpLabel = labeledNucleiImage.getValueInt(x, y);
								if (j == 0) {
										label = tmpLabel;
								}
								/*
								 * Test if region is inside nuclei region and is unique over whole
								 * particle region.
								 */
								if (label == 0 || label != tmpLabel) {
										useRegion = false;
								}
						}
						if (useRegion) {
								// note: region labels starts with 1
								this.particleRegions.add(tmpPartRegions.elementAt(i));
								matchIndex.elementAt(label - 1).addElement(
								    this.particleRegions.size() - 1);
						}
				}
				return matchIndex;
		}

		/**
		 * Create result table with statistics about nuclei and particles.
		 */
		private void createResultTable(Vector<Vector<Integer>> particleMatching) {

				// initialize table
				Vector<String> header = new Vector<String>();
				header.add("image");
				header.add("nuc_ID");
				header.add("nuc_size");
				header.add("nuc_int");
				header.add("part_count");
				header.add("part_ID");
				header.add("part_size");
				header.add("part_int");
				header.add("scale");
				header.add("unit");
				this.resultTable = new MTBTableModel(0, header.size(), header);

				double calibrationX = inputImage.getStepsizeX();
				calibrationX = Math.round(calibrationX / 0.001) * 0.001;
				double calibrationY = inputImage.getStepsizeY();
				calibrationY = Math.round(calibrationY / 0.001) * 0.001;
				String calibrationUnitX = inputImage.getUnitX();
				// not used, hopefully both direction are in same unit
				// String calibrationUnitY = inputImage.getUnitY();

				/*
				 * Fill result table row by row.
				 */
				int row = 0;
				for (int nucID = 0; nucID < particleMatching.size(); nucID++) {

						MTBRegion2D nucleus = this.nucleiRegions.elementAt(nucID);

						// get sum of intensities for current nucleus region
						Vector<Point2D.Double> nucPoints = nucleus.getPoints();
						int nucInt = 0;
						for (int i = 0; i < nucPoints.size(); i++) {
								Point2D.Double p = nucPoints.elementAt(i);
								int x = (int) Math.round(p.x);
								int y = (int) Math.round(p.y);
								if (swapNuclei) {
										nucInt = nucInt + this.partImage.getValueInt(x, y);
								} else {
										nucInt = nucInt + this.nucImage.getValueInt(x, y);
								}
						}

						// get list of particles corresponding to the current nucleus region
						Vector<Integer> particleLabels = particleMatching.elementAt(nucID);

						// write only nuclei information if no particles were detected inside
						if (particleLabels.size() == 0) {
								// set image name
								resultTable.setValueAt(fileName, row, 0);
								// set nucleus ID, same for all particles of current nucleus
								resultTable.setValueAt(nucID, row, 1);
								// set nucleus size, same for all particles of current nucleus
								resultTable.setValueAt(nucleus.getArea(), row, 2);
								// set nucleus sum of intensity, same for all particles of current
								// nucleus
								resultTable.setValueAt(nucInt, row, 3);
								// set number of particles in current nucleus, same for all particles of
								// current nucleus
								resultTable.setValueAt(particleLabels.size(), row, 4);
								// set particle ID
								resultTable.setValueAt(0, row, 5);
								// set particle size
								resultTable.setValueAt(0, row, 6);
								// set particle sum of intensity
								resultTable.setValueAt(0, row, 7);
								// set calibration scale
								resultTable.setValueAt(calibrationX + " x " + calibrationY, row, 8);
								// set calibration unit
								resultTable.setValueAt(calibrationUnitX, row, 9);
								row++;
						} else {

								// write statistics of nuclei and corresponding particles
								for (int partID = 0; partID < particleLabels.size(); partID++) {

										// get current processed particle region for the current nucleus
										MTBRegion2D particle = this.particleRegions.elementAt(particleLabels
										    .elementAt(partID));

										// get sum of intensities for current particle region
										Vector<Point2D.Double> partPoints = particle.getPoints();
										int partInt = 0;
										for (int i = 0; i < partPoints.size(); i++) {
												Point2D.Double p = partPoints.elementAt(i);
												int x = (int) Math.round(p.x);
												int y = (int) Math.round(p.y);
												partInt = partInt + this.partImage.getValueInt(x, y);
										}

										// set image name
										resultTable.setValueAt(fileName, row, 0);
										// set nucleus ID, same for all particles of current nucleus
										resultTable.setValueAt(nucID, row, 1);
										// set nucleus size, same for all particles of current nucleus
										resultTable.setValueAt(nucleus.getArea(), row, 2);
										// set nucleus sum of intensity, same for all particles of current
										// nucleus
										resultTable.setValueAt(nucInt, row, 3);
										// set number of particles in current nucleus, same for all particles
										// of current nucleus
										resultTable.setValueAt(particleLabels.size(), row, 4);
										// set particle ID
										resultTable.setValueAt(partID, row, 5);
										// set particle size
										resultTable.setValueAt(particle.getArea(), row, 6);
										// set particle sum of intensity
										resultTable.setValueAt(partInt, row, 7);
										// set calibration scale
										resultTable.setValueAt(calibrationX + " x " + calibrationY, row, 8);
										// set calibration unit
										resultTable.setValueAt(calibrationUnitX, row, 9);
										row++;
								}
						}
				}
		}

		/**
		 * Create result table with statistics about nuclei and particles.
		 */
		private void createSwapResultTable() {

				// initialize table
				Vector<String> header = new Vector<String>();
				header.add("image");
				header.add("nuc_ID");
				header.add("nuc_size");
				header.add("nuc_int");
				header.add("scale");
				header.add("unit");
				this.resultTable = new MTBTableModel(0, header.size(), header);

				double calibrationX = inputImage.getStepsizeX();
				calibrationX = Math.round(calibrationX / 0.001) * 0.001;
				double calibrationY = inputImage.getStepsizeY();
				calibrationY = Math.round(calibrationY / 0.001) * 0.001;
				String calibrationUnitX = inputImage.getUnitX();
				// not used, hopefully both direction are in same unit
				// String calibrationUnitY = inputImage.getUnitY();

				/*
				 * Fill result table row by row.
				 */
				int row = 0;
				for (int nucID = 0; nucID < this.nucleiRegions.size(); nucID++) {

						MTBRegion2D nucleus = this.nucleiRegions.elementAt(nucID);

						// get sum of intensities for current nucleus region
						Vector<Point2D.Double> nucPoints = nucleus.getPoints();
						int nucInt = 0;
						for (int i = 0; i < nucPoints.size(); i++) {
								Point2D.Double p = nucPoints.elementAt(i);
								int x = (int) Math.round(p.x);
								int y = (int) Math.round(p.y);
								nucInt = nucInt + this.partImage.getValueInt(x, y);
						}

						// set image name
						resultTable.setValueAt(fileName, row, 0);
						// set nucleus ID, same for all particles of current nucleus
						resultTable.setValueAt(nucID, row, 1);
						// set nucleus size, same for all particles of current nucleus
						resultTable.setValueAt(nucleus.getArea(), row, 2);
						// set nucleus sum of intensity, same for all particles of current
						// nucleus
						resultTable.setValueAt(nucInt, row, 3);
						// set calibration scale
						resultTable.setValueAt(calibrationX + " x " + calibrationY, row, 4);
						// set calibration unit
						resultTable.setValueAt(calibrationUnitX, row, 5);
						row++;
				}
		}

		// /**
		// * Create output image of nuclei and particle channel. Detected nuclei and
		// * particles are shown as yellow contours on the merged channel image of the
		// * nuclei and particle stains.
		// */
		// private void createOutputImage() {
		//
		// // create empty black image
		// MTBImageRGB background = (MTBImageRGB) MTBImage.createMTBImage(sizeX,
		// sizeY, 1, 1, 1, MTBImageType.MTB_RGB);
		// background.fillBlack();
		//
		// // merge channel information in one image
		// for (int y = 0; y < sizeY; ++y) {
		// for (int x = 0; x < sizeX; ++x) {
		// background.putValueR(x, y, this.partImage.getValueInt(x, y));
		// background.putValueG(x, y, this.nucImage.getValueInt(x, y));
		// background.putValueB(x, y, 0);
		// }
		// }
		//
		// // draw particle regions
		// for (int i = 0; i < this.particleRegions.size(); i++) {
		// MTBRegion2D reg = this.particleRegions.elementAt(i);
		// Vector<Point2D.Double> points = null;
		// try {
		// points = reg.getBorder().getPoints();
		// } catch (ALDOperatorException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (ALDProcessingDAGException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// int yellow = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);
		// int x = 0, y = 0;
		// for (int j = 0; j < points.size(); j++) {
		// x = (int) Math.round(points.get(j).x);
		// y = (int) Math.round(points.get(j).y);
		// background.drawPoint2D(x, y, 0, yellow, 0);
		// }
		// }
		//
		// // draw nuclei regions
		// for (int i = 0; i < this.nucleiRegions.size(); i++) {
		// MTBRegion2D reg = this.nucleiRegions.elementAt(i);
		// Vector<Point2D.Double> points = null;
		// try {
		// points = reg.getBorder().getPoints();
		// } catch (ALDOperatorException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (ALDProcessingDAGException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// int yellow = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);
		// int x = 0, y = 0;
		// for (int j = 0; j < points.size(); j++) {
		// x = (int) Math.round(points.get(j).x);
		// y = (int) Math.round(points.get(j).y);
		// background.drawPoint2D(x, y, 0, yellow, 0);
		// }
		// }
		//
		// // set final output image properties
		// this.outputImage = background.duplicate();
		// this.outputImage.setTitle(this.fileName +
		// "-NuclearParticleDetection-Result");
		// }

		@Override
		public String getDocumentation() {
			return " * <p>The NuclearParticleDetector2D detects nuclei and particles inside these\n" + 
					" * nuclei and gives some statistics about particles, like particle number per\n" + 
					" * nuclei, particle size, sum of intensities, etc. Nuclei are detected via the\n" + 
					" * NucleusDetector2D by using the Niblack thresholding. As post-process the\n" + 
					" * nuclei channel is gamma corrected and filter with a small gaussian, due to\n" + 
					" * uneven illumination of the nuclei in the images. This may depends on the\n" + 
					" * input image data and can be switched off separately. Particles are detected\n" + 
					" * via the ParticleDetectorUWT2D.</p> <br>\n" + 
					" * \n" + 
					" * <p>see <a\n" + 
					" * href=\"de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusDetector2D.html\"\n" + 
					" * >Nucleus Detector 2D</a></p> <p>see <a\n" + 
					" * href=\"de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D.html\"\n" + 
					" * >Particle Detector UWT 2D</a></p>\n" + 
					" * \n" + 
					" * <br>\n" + 
					" * \n" + 
					" * <p>----------------------------------------------------------------------------\n" + 
					" * ----</p> <h2>Output:</h3>\n" + 
					" * \n" + 
					" * <ul><li> <p>image with detected nuclei and particles</p> </li><li>\n" + 
					" * <p>detected nuclei regions</p> </li><li> <p>detected particle regions</p>\n" + 
					" * </li><li> <p>table with statistic results</p> <br> </li></ul>\n" + 
					" * \n" + 
					" * <p>----------------------------------------------------------------------------\n" + 
					" * ----</p> <h2>Usage (standard view)</h2> <h3>Required parameters:</h3>\n" + 
					" * \n" + 
					" * <ul><li> <p><tt><b>Nuclei Channel</b></tt> <ul><li> <p>Channel number of the\n" + 
					" * nuclei image channel</p> </li><li> <p>range [1, #ImageChannels]</p> </li><li>\n" + 
					" * <p>default: <i><b>2</b></i></p> </li></ul> </p> </li><li> <p><tt><b>Particle\n" + 
					" * Channel</b></tt> <ul><li> <p>Channel number of the particle image channel</p>\n" + 
					" * </li><li> <p>range [1, #ImageChannels]</p> </li><li> <p>default:\n" + 
					" * <i><b>1</b></i></p> </li></ul> </p> </li></ul>\n" + 
					" * \n" + 
					" * <h3>Supplemental parameters:</h3>\n" + 
					" * \n" + 
					" * <ul><li> <p><tt><b>Verbose</b></tt> <ul><li> <p>Output of additional messages\n" + 
					" * on console is disabled/enabled</p> </li><li> <p>default:\n" + 
					" * <i><b>false</b></i></p> <br> </li></ul> </p> </li></ul>\n" + 
					" * \n" + 
					" * <p>----------------------------------------------------------------------------\n" + 
					" * ----</p> <h2>Usage (advanced view):</h2> <h3>Required parameters:</h3>\n" + 
					" * \n" + 
					" * <ul><li> <p><tt><b>Nuclei Channel</b></tt> <ul><li> <p>Channel number of the\n" + 
					" * nuclei image channel</p> </li><li> <p>range [1, #ImageChannels]</p> </li><li>\n" + 
					" * <p>default: <i><b>2</b></i></p> </li></ul> </p> </li><li> <p><tt><b>Particle\n" + 
					" * Channel</b></tt> <ul><li> <p>Channel number of the particle image channel</p>\n" + 
					" * </li><li> <p>range [1, #ImageChannels]</p> </li><li> <p>default:\n" + 
					" * <i><b>1</b></i></p> </li></ul> </p> </li><li> <p><tt><b>Use Gamma\n" + 
					" * Correction</b></tt> <ul><li> <p>Flag to use gamma correction for nuclei\n" + 
					" * channel</p> </li><li> <p>default: <i><b>true</b></i></p> </li></ul> </p>\n" + 
					" * </li><li> <p><tt><b>Gamma Value</b></tt> <ul><li> <p>Gamma correction value\n" + 
					" * for nuclei channel</p> </li><li> <p>range [0, 1]</p> </li><li> <p>default:\n" + 
					" * <i><b>0.5</b></i></p> </li></ul> </p> </li><li> <p><tt><b>Use Gaussian\n" + 
					" * Filter</b></tt> <ul><li> <p>Flag to use gaussian filter for nuclei\n" + 
					" * channel</p> </li><li> <p>default: <i><b>true</b></i></p> </li></ul> </p>\n" + 
					" * </li><li> <p><tt><b>Nucleus Detector</b></tt> <ul><li> <p>Detector to use for\n" + 
					" * nuclei detection</p> </li><li> <p>Configurable via GUI (see <a\n" + 
					" * href=\"de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusDetector2D.html\"\n" + 
					" * >Nucleus Detector 2D</a>)</p> </li></ul> </p> </li><li> <p><tt><b>Particle\n" + 
					" * Detector</b></tt> <ul><li> <p>Detector to use for particle detection</p>\n" + 
					" * </li><li> <p>Configurable via GUI (see <a\n" + 
					" * href=\"de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D.html\"\n" + 
					" * >Particle Detector UWT 2D</a>)</p> </li></ul> </p> </li></ul>\n" + 
					" * \n" + 
					" * <h3>Supplemental parameters:</h3>\n" + 
					" * \n" + 
					" * <ul><li> <p><tt><b>Verbose</b></tt> <ul><li> <p>Output of additional messages\n" + 
					" * on console is disabled/enabled</p> </li><li> <p>default:\n" + 
					" * <i><b>false</b></i></p> <br> </li></ul> </p> </li></ul>\n";
		}
}
