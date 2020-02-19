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

package de.unihalle.informatik.MiToBo.apps.neurons2D;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.apps.neurites2D.NeuriteDetector2D;
import de.unihalle.informatik.MiToBo.apps.neurites2D.NeuriteMolProfExtractor2D;
import de.unihalle.informatik.MiToBo.apps.neurites2D.NeuriteParticleDetector2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DProfileSet;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DSet;
import de.unihalle.informatik.MiToBo.core.helpers.MTBEnvironmentConfig;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.io.tools.FilePathManipulator;

/**
 * 
 * <pre>
 * 
 * The Neuron Analyzer 2D application offers advanced functionality to analyze
 * 2D fluorescence microscope images of neurons.
 *  
 * Features:
 * - neuron boundary detection based on active contours
 * - identification of structural neuron parts, like soma, neurites,
 *   growth cones
 * - morphology analysis, e.g., neurite length, average neurite width, number
 *   of branch and end points, growth cone size and shape roundness, ...
 * - extraction of molecular profiles from the given molecules, like labeled
 *   proteins, along the neurites from soma to growth cones
 * - detection of molecular particles, for example FISH data,
 *   along the neurites from soma to growth cones
 * - intermediate and final results are automatically saved
 * - tabular presentation of result data
 * 
 * </pre>
 * 
 * @see NeuriteDetector2D
 * @see NeuriteMolProfExtractor2D
 * @see NeuriteParticleDetector2D
 * 
 * @author Danny Misiak
 * 
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION, allowBatchMode = false)
public class NeuronAnalyzer2D extends MTBOperator {

		/**
		 * Get initial directory from environment configuration.
		 */
		static ALDDirectoryString currentInput;
		static {
				String file;
				file = MTBEnvironmentConfig.getImageJPropValue("neurite_detector",
				    "imageinput");
				if (file == null) {
						file = ij.Prefs.get("dir.image", null);
						if (file == null)
								currentInput = new ALDDirectoryString(System.getProperty("user.home")
								    .toString());
						else
								currentInput = new ALDDirectoryString(file);
				} else
						currentInput = new ALDDirectoryString(file);
		}

		/**
		 * Define analyzer parameters.
		 */

		// --- input parameters ---

		@Parameter(label = "Neurite Detector", direction = Parameter.Direction.IN, required = true, description = "Detector to use for neurite detection.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private NeuriteDetector2D neuriteDetector = new NeuriteDetector2D();

		@Parameter(label = "Extract Profiles", direction = Parameter.Direction.IN, required = true, description = "Flag to use molecular profile extraction.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		protected Boolean extractProfiles = new Boolean(true);

		@Parameter(label = "Define Molecules", direction = Parameter.Direction.IN, required = true, description = "Names of molecules stained in each image channel.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private String[] molecules = { new String("Tubulin"), new String("ZBP"),
		    new String("Actin"), new String("DAPI") };

		@Parameter(label = "Profile Molecule Channels", direction = Parameter.Direction.IN, required = true, description = "Channels of molecules for profile extraction.", mode = ExpertMode.STANDARD, dataIOOrder = 3)
		private Integer[] moleculeChannels = { new Integer(1), new Integer(2),
		    new Integer(3) };

		@Parameter(label = "Extract Particles", direction = Parameter.Direction.IN, required = true, description = "Flag to use molecular particle extraction.", mode = ExpertMode.STANDARD, dataIOOrder = 4)
		protected Boolean extractParticles = new Boolean(false);

		@Parameter(label = "Particle Detector", direction = Parameter.Direction.IN, required = true, description = "Detector to use for  molecular particle detection.", mode = ExpertMode.STANDARD, dataIOOrder = 5)
		private NeuriteParticleDetector2D particleDetector = new NeuriteParticleDetector2D();

		// --- supplemental parameters ---

		// --- output parameters ---

		@Parameter(label = "Detection Result Table", direction = Parameter.Direction.OUT, required = true, description = "Table of detection results.")
		private transient MTBTableModel detectionResultTable = null;

		/**
		 * JRI R engine to run R as a single thread.
		 */
		private transient Rengine rEngine;

		/**
		 * Directory of output results.
		 */
		private transient String outpath;

		/**
		 * Standard constructor.
		 * 
		 * @throws ALDOperatorException
		 */
		public NeuronAnalyzer2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor.
		 * 
		 * @param _detector
		 *          detector to use for neurite detection
		 * @param _extractProfiles
		 *          flag to use molecular profile extraction
		 * @param _molecules
		 *          names of molecules to analyze for each image channel
		 * @param _moleculeChannels
		 *          channles including molecule stains
		 * @param _extractParticles
		 *          flag to use molecular particle extraction
		 * @param _particleDetector
		 *          detector for molecular particles
		 * @param _verbose
		 *          flag for standard console outputs
		 * @throws ALDOperatorException
		 */
		public NeuronAnalyzer2D(NeuriteDetector2D _neuriteDetector,
		    Boolean _extractProfiles, String[] _molecules,
		    Integer[] _moleculeChannels, Boolean _extractParticles,
		    NeuriteParticleDetector2D _particleDetector, Boolean _verbose)
		    throws ALDOperatorException {

				this.neuriteDetector = _neuriteDetector;
				this.extractProfiles = _extractProfiles;
				this.molecules = _molecules;
				this.moleculeChannels = _moleculeChannels;
				this.extractParticles = _extractParticles;
				this.particleDetector = _particleDetector;
				this.verbose = _verbose;
		}

		/**
		 * Custom validation of some input parameters.
		 */
		@Override
		public void validateCustom() throws ALDOperatorException {
				if (molecules.length < 1) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteAnalyzer2D: validation failed!"
						        + "\nAt least one molecule name must be defined.");
				}

				boolean error = false;
				for (int i = 0; i < moleculeChannels.length; i++) {
						if (moleculeChannels[i] < 1) {
								error = true;
						}
				}
				if (error) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteAnalyzer2D: validation failed!"
						        + "\nMolecule channels must be in range [1, #ImageChannels].");
				}
		}

		/**
		 * Get names of molecules for each image channel.
		 */
		public String[] getMolecules() {
				return molecules;
		}

		/**
		 * Get molecule channels, used for molecular profile extraction.
		 */
		public Integer[] getMoleculeChannles() {
				return moleculeChannels;
		}

		/**
		 * Get the result table of the detected neurites.
		 */
		public MTBTableModel getDetectionResultTable() {
				return detectionResultTable;
		}

		/**
		 * Get current neurite detector.
		 */
		public NeuriteDetector2D getNeuriteDetector() {
				return neuriteDetector;
		}

		/**
		 * Initialization method.
		 */
		private void init() {
				System.out
				    .println("\n---------- NeuriteAnalyzer2D ... >>>started<<< ----------\n");
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				// start initialization
				init();
				// start neurite detection
				System.out.println("\n  --> " + "starting neurite detection...\n");
				neuriteDetector.runOp();
				// get detector results
				detectionResultTable = neuriteDetector.getResultTable();
				// get R engine from detector
				rEngine = neuriteDetector.getREngine();

				Vector<String> imageFiles = neuriteDetector.getAnalyzedImages();
				Vector<MTBNeurite2DSet> neurites = neuriteDetector.getDetectedNeurites();
				outpath = neuriteDetector.getOutputDir();

				// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
				/*
				 * Process all steps for each multichannel fluorescence image in the input
				 * directory.
				 */
				// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
				for (int i = 0; i < imageFiles.size(); i++) {
						// get current image path
						File currentImage = new File(imageFiles.elementAt(i));

						String currentImagePath = currentImage.getPath();
						String currentImageName = FilePathManipulator.getFileName(currentImage
						    .getName());

						/*
						 * Open current image and delete non black border of the image (problem of
						 * some microscope cameras).
						 */
						MTBImage mtbImg = null;
						ImageReaderMTB io;
						try {
								io = new ImageReaderMTB(currentImagePath);
								io.runOp(null);
								mtbImg = io.getResultMTBImage();
								mtbImg = mtbImg.getImagePart(2, 2, 0, 0, 0, mtbImg.getSizeX() - 4,
								    mtbImg.getSizeY() - 4, mtbImg.getSizeZ(), mtbImg.getSizeT(),
								    mtbImg.getSizeC());
								if (verbose.booleanValue()) {
										System.out.println("  --> image to open: " + currentImagePath);
								}
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuronAnalyzer2D extraction failed @ open image: "
								        + currentImagePath);
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuronAnalyzer2D extraction failed @ open image: "
								        + currentImagePath);
								e.printStackTrace();
						} catch (FormatException e) {
								System.out
								    .println(">>>>>>> NeuronAnalyzer2D extraction failed @ open image: "
								        + currentImagePath);
								e.printStackTrace();
						} catch (IOException e) {
								System.out
								    .println(">>>>>>> NeuronAnalyzer2D extraction failed @ open image: "
								        + currentImagePath);
								e.printStackTrace();
						} catch (DependencyException e) {
								System.out
								    .println(">>>>>>> NeuronAnalyzer2D extraction failed @ open image: "
								        + currentImagePath);
								e.printStackTrace();
						} catch (ServiceException e) {
								System.out
								    .println(">>>>>>> NeuronAnalyzer2D extraction failed @ open image: "
								        + currentImagePath);
								e.printStackTrace();
						}

						// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
						/*
						 * Start molecular profile extraction, if selected.
						 */
						// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
						if (extractProfiles) {
								if (i == 0) {
										System.out.println("\n  --> "
										    + "starting molecular profile extraction...\n");
								}
								// check number of image channels and molecules
								if (mtbImg.getSizeC() != molecules.length) {
										try {
												throw new NeuronAnalyzer2DException(
												    ">>>>>>> NeuronAnalyzer2D: #channels and #defined molecules differ in "
												        + currentImagePath + "...skipping!");
										} catch (NeuronAnalyzer2DException e) {
												e.printStackTrace();
										}
								} else {
										extractMolProf(mtbImg, neurites.elementAt(i), currentImagePath,
										    outpath);

										/*
										 * Run R scripts to normalize profile data, plot and fit profiles.
										 */
										if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
												String dirPath = new String(outpath + File.separator
												    + "profileData");

												String[] args = new String[0];

												if (rEngine != null) {
														/*
														 * Normalize molecule profile data.
														 */
														args = new String[2];
														args[0] = dirPath;
														args[1] = new String(
														    new Integer(moleculeChannels.length).toString());
														boolean rCall = callNeuriteSrcipt(rEngine,
														    "MTBProfileNormalization.R", args);
														if (!rCall) {
																try {
																		throw new NeuronAnalyzer2DException(
																		    ">>>>>>> NeuronAnalyzer2D: profile normalization failed @ executing R!");
																} catch (NeuronAnalyzer2DException e) {
																		e.printStackTrace();
																}
														}
														/*
														 * Plot the molecule profiles.
														 */
														args = new String[(moleculeChannels.length) + 1];
														args[0] = dirPath;
														// add molecule names
														for (int j = 0; j < moleculeChannels.length; j++) {
																args[j + 1] = molecules[(moleculeChannels[j] - 1)];
														}
														rCall = callNeuriteSrcipt(rEngine, "MTBProteinProfilePlot.R",
														    args);
														if (!rCall) {
																try {
																		throw new NeuronAnalyzer2DException(
																		    ">>>>>>> NeuronAnalyzer2D: profile plot failed @ executing R!");
																} catch (NeuronAnalyzer2DException e) {
																		e.printStackTrace();
																}
														}
														/*
														 * Plot fitted version of the molecule profiles.
														 */
														rCall = callNeuriteSrcipt(rEngine, "MTBProteinProfileFit.R", args);
														if (!rCall) {
																try {
																		throw new NeuronAnalyzer2DException(
																		    ">>>>>>> NeuronAnalyzer2D: profile fit failed @ executing R!");
																} catch (NeuronAnalyzer2DException e) {
																		e.printStackTrace();
																}
														}

												}

										} else if (System.getProperty("os.name").toLowerCase()
										    .indexOf("windows") > -1) {
												try {
														throw new NeuronAnalyzer2DException(
														    ">>>>>>> NeuronAnalyzer2D: failed executing R on Windows OS!");
												} catch (NeuronAnalyzer2DException e) {
														e.printStackTrace();
												}
										}
								}
						}
						// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
						/*
						 * Start molecular particle extraction, if selected.
						 */
						// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
						if (extractParticles) {
								if (i == 0) {
										System.out.println("\n  --> "
										    + "starting molecular particle extraction...\n");
								}
								MTBRegion2DSet particleSet = extractParticles(mtbImg,
								    neurites.elementAt(i));
								// visualize detected particles and save image
								MTBImage detectedParticlesImage = MTBImage.createMTBImage(
								    mtbImg.getSizeX(), mtbImg.getSizeY(), 1, 1, 1, mtbImg.getType());
								detectedParticlesImage.setImagePart(
								    mtbImg.getSlice(0, 0, particleDetector.getParticleChannel() - 1),
								    0, 0, 0, 0, 0);
								detectedParticlesImage
								    .setTitle(currentImageName + "-detectedParticles");
								for (int k = 0; k < particleSet.size(); k++) {
										detectedParticlesImage = particleSet.elementAt(k).getContour()
										    .toMTBImage(null, detectedParticlesImage);

								}
								// save detected particles image
								try {
										ImageWriterMTB IW = new ImageWriterMTB(detectedParticlesImage,
										    outpath + File.separator + currentImageName + File.separator
										        + currentImageName + "-detectedParticles.tif");
										IW.setVerbose(false);
										IW.setOverwrite(false);
										IW.runOp();
								} catch (ALDOperatorException e) {
										System.out.println(">>>>>>> NeuronAnalyzer2D failed @ write "
										    + outpath + File.separator + currentImageName + File.separator
										    + currentImageName + "-detectedParticles.tif");
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										System.out.println(">>>>>>> NeuronAnalyzer2D failed @ write "
										    + outpath + File.separator + currentImageName + File.separator
										    + currentImageName + "-detectedParticles.tif");
										e.printStackTrace();
								}
						}
				}

				// >>>>>>>>>>>>>>>>>>>>>>>>>>>>
				System.out
				    .println("\n---------- NeuriteAnalyzer2D ... >>>finished<<< ----------\n");
		}

		/**
		 * Extraction of molecular profiles along the detected neurites.
		 */
		private void extractMolProf(MTBImage moleculeImage,
		    MTBNeurite2DSet neuriteSet, String imagePath, String outPath) {

				/*
				 * Extract the molecular neurite profiles.
				 */
				NeuriteMolProfExtractor2D extractor = null;
				try {
						// Extract profile of each stained protein.
						// TODO check if molecule channel is out of range
						extractor = new NeuriteMolProfExtractor2D(moleculeImage,
						    moleculeChannels, neuriteSet, verbose);
						extractor.runOp();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuronAnalyzer2D profile extraction failed @ run extraction!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuronAnalyzer2D profile extraction failed @ run extraction!");
						e.printStackTrace();
				}

				/*
				 * Save the protein profiles of each stain.
				 */
				Vector<MTBNeurite2DProfileSet> neuriteProfiles = extractor.getProfiles();
				// get current file name without file extension
				String fileName = FilePathManipulator.getFileName(imagePath);
				// get protein stains by image channel
				for (int j = 0; j < moleculeChannels.length; j++) {
						MTBNeurite2DProfileSet tmpProfile = new MTBNeurite2DProfileSet();
						// get profiles by neurite region and add to current protein stain
						for (int k = 0; k < neuriteProfiles.size(); k++) {
								tmpProfile.add(neuriteProfiles.elementAt(k).getElementAt(j));
						}
						// save profile of current protein.
						tmpProfile.saveProfileSet(outpath + File.separator + fileName
						    + File.separator + "extraction_results" + File.separator + fileName
						    + "-neuriteProfile-prot" + (j + 1) + ".mtb");
						if (j == 0) {

								/*
								 * Save voronoi tesselation of neurites from current neuron. Saving the
								 * voronoi tesselation from first profile set is enough, because the
								 * tesseleations of the other sets are the same.
								 */
								try {
										ImageWriterMTB IW = new ImageWriterMTB(tmpProfile.getVoronoiStack(),
										    outPath + File.separator + fileName + File.separator
										        + "extraction_results" + File.separator + fileName
										        + "-neuriteVoronoi.tif");
										IW.setVerbose(false);
										IW.setOverwrite(false);
										IW.runOp();
								} catch (ALDOperatorException e) {
										System.out
										    .println(">>>>>>> NeuronAnalyzer2D profile extraction error @ save voronoi image!");
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										System.out
										    .println(">>>>>>> NeuronAnalyzer2D profile extraction error @ save voronoi image!");
										e.printStackTrace();
								}
						}
				}

				/*
				 * Use R script to normalize profile data. Set length and intensity for all
				 * neurites to 100% for every single neurite to get normalized profile data.
				 */
				if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
						// test if R engine was created successful
						if (rEngine != null) {

								/*
								 * Recalculate profile data and generate .mtb.txt files with neurite
								 * length data.
								 */
								String[] args = new String[1];
								args[0] = outPath + File.separator + fileName + File.separator
								    + "extraction_results";

								boolean rCall = callNeuriteSrcipt(rEngine, "MTBDataToNeuriteProfile.R",
								    args);
								if (!rCall) {
										try {
												throw new NeuronAnalyzer2DException(
												    ">>>>>>> NeuronAnalyzer2D: profile extraction failed @ executing R!");
										} catch (NeuronAnalyzer2DException e) {
												e.printStackTrace();
										}
								}
						}
				} else if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
						try {
								throw new NeuronAnalyzer2DException(
								    ">>>>>>> NeuronAnalyzer2D: failed executing R on Windows OS!");
						} catch (NeuronAnalyzer2DException e) {
								e.printStackTrace();
						}
				}
		}

		/**
		 * Extraction of molecular particles along the detected neurites.
		 */
		private MTBRegion2DSet extractParticles(MTBImage particleImage,
		    MTBNeurite2DSet neuriteSet) {
				MTBRegion2DSet particles = null;
				try {
						particleDetector.setParticleImage(particleImage);
						particleDetector.setNeuriteSet(neuriteSet);
						particleDetector.runOp();
						particles = particleDetector.getDetectedParticles();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuronAnalyzer2D particle extraction error @ run particle detector!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuronAnalyzer2D particle extraction error @ run particle detector!");
						e.printStackTrace();
				}
				return particles;
		}

		/**
		 * Call a specified R script for neurite calculations.
		 * 
		 * @param re
		 *          current R engine (thread)
		 * @param sourceFile
		 *          neurite calculation R script file
		 * @param scriptArg
		 *          input directory path for the R script and other arguments
		 * @return True if R call is done successfully.
		 */
		private boolean callNeuriteSrcipt(Rengine re, String sourceFile,
		    String[] scriptArgs) {
				// set working directory in R
				re.eval("setwd(Sys.getenv(\"R_SCRIPTS\"))");
				// Create global R variable for input directory named. Same like in R:
				// arg <- scriptArg
				re.assign("arg", scriptArgs);
				// Load R script file in current R engine. Same like in R:
				// source(sourceFile)
				REXP rExpression = re.eval("source(\"" + sourceFile + "\")");
				// check if source file was successfully loaded and executed
				if (rExpression == null) {
						System.out.println("** JRI R-Engine: error executing source file!");
						System.out.println("** file   : \"" + sourceFile + "\"");
						return false;
				}
				System.out.println("JRI R-Engine: executing " + sourceFile + " ...done!");
				return true;
		}
}
