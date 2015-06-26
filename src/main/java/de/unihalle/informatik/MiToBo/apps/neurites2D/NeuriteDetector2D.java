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

package de.unihalle.informatik.MiToBo.apps.neurites2D;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

import org.rosuda.JRI.Rengine;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.apps.neurites2D.NeuriteDetector2DAlgos.DetectorExternalEnergy;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DSet;
import de.unihalle.informatik.MiToBo.core.helpers.MTBEnvironmentConfig;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.tools.ImageFilter;

/**
 * 
 * The Neurite Detector 2D application offers advanced functionality to detect
 * neurites in 2D fluorescence microscope images of neurons. Neuron boundaries
 * are coarsely detected and refined, using active contour models (snakes).
 * Structural neuron parts, like soma, neurites and growth cones are identified,
 * using an wavelet based detection. Morphology analysis of the detected
 * neurites is performed and an collection of all measurements is visualized in
 * the result table. Intermediate and final results are automatically saved and
 * the results are stored in a result image and presented in a final result
 * table.
 * 
 * The specific algorithms can be found in the `NeuriteDetector2DAlgos` class.
 * 
 * 
 * @see NeuriteDetector2DAlgos
 * 
 * @author Danny Misiak
 * 
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION, allowBatchMode = false)
public class NeuriteDetector2D extends MTBOperator {

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
		 * Different detection modes.
		 * 
		 * 
		 * @author Danny Misiak
		 * 
		 */
		public static enum NeuriteDetector2DMode {
				// detect neurites from scratch
				DETECTION,
				// detect neurites from stored snake results
				// RECALCULATION
				// EVALUATION
		}

		/**
		 * Define detector parameters.
		 */

		// --- input parameters ---

		@Parameter(label = "Input Directory", direction = Parameter.Direction.IN, required = true, description = "Directory of input images.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private ALDDirectoryString inputDir = currentInput;

		@Parameter(label = "Nuclei Channel", direction = Parameter.Direction.IN, required = true, description = "Image channel including nuclei (1 labels first channel).", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private Integer nucleiChannel = 4;

		@Parameter(label = "Nucleus Size", direction = Parameter.Direction.IN, required = true, description = "Minimum size of nuclei regions in pixel.", mode = ExpertMode.ADVANCED, dataIOOrder = 2)
		private Integer nucleusSize = 100;

		@Parameter(label = "Nucleus Ratio", direction = Parameter.Direction.IN, required = true, description = "Ratio of nucleus pixels which should be included in an intact neuron region in percent.", mode = ExpertMode.ADVANCED, dataIOOrder = 3)
		private Double nucleusRatio = 0.9;

		@Parameter(label = "Neuron Channels", direction = Parameter.Direction.IN, required = true, description = "Channels which stain the neurons for detection.", mode = ExpertMode.STANDARD, dataIOOrder = 4)
		private Integer[] neuronChannels = { new Integer(1), new Integer(2),
		    new Integer(3) };

		@Parameter(label = "Maximum Spine Length", required = true, direction = Parameter.Direction.IN, description = "Maximum length of a spine in pixel.", mode = ExpertMode.STANDARD, dataIOOrder = 5)
		private int maxSpineLength = 53;

		@Parameter(label = "Neurite Mask Size", required = true, direction = Parameter.Direction.IN, description = "Neurite mask size in pixel.", mode = ExpertMode.ADVANCED, dataIOOrder = 6)
		private int neuriteMaskSize = 21;

		@Parameter(label = "Maximum Fragment Distance", required = true, direction = Parameter.Direction.IN, description = "Maximum distance of neuron frgaments in pixel.", mode = ExpertMode.ADVANCED, dataIOOrder = 7)
		private int maxFragmentDistance = 50;

		@Parameter(label = "Niblack Constant", required = true, direction = Parameter.Direction.IN, description = "Niblack threshold constant.", mode = ExpertMode.ADVANCED, dataIOOrder = 8)
		private double niblackConstant = 0.8;

		@Parameter(label = "Detector Mode", required = true, direction = Parameter.Direction.IN, description = "Mode of neurite detector.", mode = ExpertMode.ADVANCED, dataIOOrder = 9)
		private NeuriteDetector2DMode detectorMode = NeuriteDetector2DMode.DETECTION;

		@Parameter(label = "External Energy", direction = Parameter.Direction.IN, required = true, description = "External energy label for detection.", mode = ExpertMode.ADVANCED, dataIOOrder = 10)
		private DetectorExternalEnergy energy = DetectorExternalEnergy.DISTANCE_MAP;

		@Parameter(label = "Alpha", required = true, direction = Parameter.Direction.IN, description = "Weighting factor for snake length term.", mode = ExpertMode.ADVANCED, dataIOOrder = 11)
		private double alpha = 0.2;
		@Parameter(label = "Beta", required = true, direction = Parameter.Direction.IN, description = "Weighting factor for snake curvature term.", mode = ExpertMode.ADVANCED, dataIOOrder = 12)
		private double beta = 0.0;

		@Parameter(label = "Step Size", required = true, direction = Parameter.Direction.IN, description = "Step size of snake movement.", mode = ExpertMode.ADVANCED, dataIOOrder = 13)
		private double stepSize = 0.05;

		@Parameter(label = "Resample Constant", required = true, direction = Parameter.Direction.IN, description = "Snake point resampling constant.", mode = ExpertMode.ADVANCED, dataIOOrder = 14)
		private int resampleConstant = 5;

		@Parameter(label = "Motion Fraction", required = true, direction = Parameter.Direction.IN, description = "Fraction of minimum point motion to stop detection in percent.", mode = ExpertMode.ADVANCED, dataIOOrder = 15)
		private double motionFraction = 0.95;

		@Parameter(label = "Maximum Iterations", required = true, direction = Parameter.Direction.IN, description = "Maximum iteration count to stop optimization.", mode = ExpertMode.ADVANCED, dataIOOrder = 16)
		private int maxIterations = 120;

		// --- supplemental parameters ---

		@Parameter(label = "Neurite Color", supplemental = true, direction = Parameter.Direction.IN, description = "Color of neurite regions in result image.")
		protected Color neuriteColor = new Color(100, 100, 100);

		// --- output parameters ---

		@Parameter(label = "Detection Result Table", direction = Parameter.Direction.OUT, required = true, description = "Table of detection results.")
		private transient MTBTableModel detectionResultTable = null;

		/**
		 * Vector with all image files included in the experiment folder for
		 * detection.
		 */
		private transient Vector<String> imageFiles;

		/**
		 * Vector with all analyzed image files.
		 */
		private transient Vector<String> analyzedImages;

		/**
		 * Directory of output results.
		 */
		private transient String outputDir;

		/**
		 * Vector of detected neurite sets for each image.
		 */
		private transient Vector<MTBNeurite2DSet> detectedNeurites;

		/**
		 * JRI R engine to run R as a single thread.
		 */
		private transient Rengine rEngine;

		/**
		 * Standard constructor.
		 * 
		 * @throws ALDOperatorException
		 */
		public NeuriteDetector2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor
		 * 
		 * @param _inputDir
		 *          input directory with multichannel fluorescence images
		 * @param _nucleiChannel
		 *          channel number including nuclei image
		 * @param _nucleusSize
		 *          minimum size of nucleus region
		 * @param _nucleusRation
		 *          ratio of nucleus pixels which should be included in an intact
		 *          neuron region
		 * @param _neuronChannels
		 *          channel numbers including neuron stains for detection
		 * @param _niblackConstant
		 *          constant for niblack thresholding
		 * @param _maxFragmentDistance
		 *          maximum distance to connect a fragment to the neuron, in pixel
		 * @param _detectorMode
		 *          mode of neurite detection
		 * @param _energy
		 *          label of available external energies for detection using snakes
		 * @param _alpha
		 *          weighting factor for snake length term
		 * @param _beta
		 *          weighting factor for snake curvature term
		 * @param _stepSize
		 *          step size gamma to move snake in a optimization step
		 * @param _motionFraction
		 *          minimum fraction of not moving points to stop optimization
		 * @param _maxIterations
		 *          maximum iteration to stop optimization
		 * @param _resampleConstant
		 *          constant for resampling snake control points
		 * @param _maxSpineLength
		 *          maximum length of a branch to define it as spine, in pixel
		 * @param _neuriteMaskSize
		 *          mask size of average neurite width, in pixel
		 * @param _neuriteColor
		 *          color for neurite regions in result image
		 * @param _verbose
		 *          flag for standard console outputs
		 * @throws ALDOperatorException
		 */
		public NeuriteDetector2D(String _inputDir, Integer _nucleiChannel,
		    int _nucleusSize, double _nucleusRation, Integer[] _neuronChannels,
		    double _niblackConstant, int _maxFragmentDistance,
		    NeuriteDetector2DMode _detectorMode, DetectorExternalEnergy _energy,
		    double _alpha, double _beta, double _stepSize, double _motionFraction,
		    int _maxIterations, int _resampleConstant, int _maxSpineLength,
		    int _neuriteMaskSize, Color _neuriteColor, Boolean _verbose)
		    throws ALDOperatorException {

				// set members
				inputDir = new ALDDirectoryString(_inputDir);
				nucleiChannel = _nucleiChannel;
				nucleusSize = _nucleusSize;
				nucleusRatio = _nucleusRation;
				neuronChannels = _neuronChannels;
				niblackConstant = _niblackConstant;
				maxFragmentDistance = _maxFragmentDistance;
				detectorMode = _detectorMode;
				energy = _energy;
				alpha = _alpha;
				beta = _beta;
				stepSize = _stepSize;
				motionFraction = _motionFraction;
				maxIterations = _maxIterations;
				resampleConstant = _resampleConstant;
				maxSpineLength = _maxSpineLength;
				neuriteMaskSize = _neuriteMaskSize;
				neuriteColor = _neuriteColor;
				verbose = _verbose;
		}

		/**
		 * Custom validation of some input parameters.
		 */
		@Override
		public void validateCustom() throws ALDOperatorException {

				if (nucleiChannel < 1) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nNuclei channel must be in range [1, #ImageChannels].");
				}

				boolean error = false;
				for (int i = 0; i < neuronChannels.length; i++) {
						if (neuronChannels[i] < 1) {
								error = true;
						}
				}
				if (error) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nNeuron channels must be in range [1, #ImageChannels].");
				}

				// check if nucleus size is valid
				if (nucleusSize < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nNucleus size must be a positive value.");
				}
				// check if nucleus ratio is valid
				if (nucleusRatio < 0.0 || nucleusRatio > 1.0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nNucleus ratio must be in range [0.0, 1.0].");
				}

				if (maxSpineLength < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nMaximum Spine Length must be a positive value.");
				}
				if (neuriteMaskSize < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nNeurite Mask Size must be a positive value.");
				}
				if (maxFragmentDistance < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nMaximum Fragment Distance must be a positive value.");
				}
				if (alpha < 0.0 || alpha > 1.0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nAlpha must be in range [0.0, 1.0].");
				}
				if (beta < 0.0 || beta > 1.0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nBeta must be in range [0.0, 1.0].");
				}
				if (stepSize < 0.0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nStep Size must be a positive value.");
				}
				if (resampleConstant < 1) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nResample Constant must be > 0.");
				}
				if (motionFraction < 0.0 || motionFraction > 1.0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nMotion Fraction must be in range [0.0, 1.0]");
				}
				if (maxIterations < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteDetector2D: validation failed!"
						        + "\nMaximum Iterations must be a positive value");
				}
		}

		/**
		 * Get directory of input images.
		 */
		public ALDDirectoryString getInputDir() {
				return inputDir;
		}

		/**
		 * Get image channel of nuclei.
		 */
		public Integer getNucleiChannel() {
				return nucleiChannel;
		}

		/**
		 * Get minimum nucleus size of nuclei regions
		 */
		public int getNucleusSize() {
				return nucleusSize;
		}

		/**
		 * Get ratio of nucleus pixels which should be included in an intact neuron
		 * region
		 */
		public double getNucleusRatio() {
				return nucleusRatio;
		}

		/**
		 * Get neuron channels, used for detection.
		 */
		public Integer[] getNeuronChannles() {
				return neuronChannels;
		}

		/**
		 * Get maximum length of a spine (filopodia-like protrusion) in pixel.
		 */
		public int getMaxSpineLength() {
				return maxSpineLength;
		}

		/**
		 * Get neurite mask size in pixel. This value defines the maximum average
		 * width of a neurite.
		 */
		public int getNeuriteMaskSize() {
				return neuriteMaskSize;
		}

		/**
		 * Get maximum neuron fragment distance in pixel.
		 */
		public int getMaxFragmentDistance() {
				return maxFragmentDistance;
		}

		/**
		 * Get niblack constant.
		 */
		public double getNiblackConstant() {
				return niblackConstant;
		}

		/**
		 * Get current detector mode.
		 */
		public NeuriteDetector2DMode getDetectorMode() {
				return detectorMode;
		}

		/**
		 * Get external snake energy label.
		 */
		public DetectorExternalEnergy getEnergy() {
				return energy;
		}

		/**
		 * Get snake length term weight alpha.
		 */
		public double getAlpha() {
				return alpha;
		}

		/**
		 * Get snake curvature term weight beta.
		 */
		public double getBeta() {
				return beta;
		}

		/**
		 * Get step size of snake movement.
		 */
		public double getStepSize() {
				return stepSize;
		}

		/**
		 * Get snake point resampling constant.
		 */
		public int getResampleConstant() {
				return resampleConstant;
		}

		/**
		 * Get minimum fraction of point motion to stop detection.
		 */
		public double getMotionFraction() {
				return motionFraction;
		}

		/**
		 * Get maximum number of iterations to stop detection.
		 */
		public int getMaxIterations() {
				return maxIterations;
		}

		/**
		 * Get neurite region color (RGB) for result image.
		 */
		public Color getNeuriteColor() {
				return neuriteColor;
		}

		/**
		 * Get vector of detected neurite sets for each image.
		 * 
		 * @return Vector of neurite sets.
		 */
		public Vector<MTBNeurite2DSet> getDetectedNeurites() {
				return detectedNeurites;
		}

		/**
		 * Get the result table of the detected neurites.
		 * 
		 * @return Result table.
		 */
		public MTBTableModel getResultTable() {
				return detectionResultTable;
		}

		/**
		 * Get vector of all image files included in the experiment folder for
		 * detection.
		 * 
		 * @return Input images.
		 */
		public Vector<String> getImageFiles() {
				return imageFiles;
		}

		/**
		 * Get vector of analyzed images.
		 * 
		 * @return Analyzed images.
		 */
		public Vector<String> getAnalyzedImages() {
				return analyzedImages;
		}

		/**
		 * Get JRI R engine to run R as a single thread.
		 */
		public Rengine getREngine() {
				return rEngine;
		}

		/**
		 * Get directory of output results.
		 */
		public String getOutputDir() {
				return outputDir;
		}

		/**
		 * Initialization method.
		 */
		private void init() {

				// remember currentDir in ImageJ preferences
				MTBEnvironmentConfig.setImageJPref("neurite_detector", "imageinput",
				    inputDir.getDirectoryName());

				// initialize some fields and make outputs
				imageFiles = new Vector<String>();
				outputDir = "";
				analyzedImages = new Vector<String>();
				detectedNeurites = new Vector<MTBNeurite2DSet>();
				System.out
				    .println("\n---------- NeuriteDetector2D ... >>>started<<< [Mode: "
				        + detectorMode.toString() + "] ----------\n");
				if (verbose.booleanValue()) {
						System.out
						    .println("  --> " + "input dir: " + inputDir.getDirectoryName());
				}
		}

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {

				// start initialization
				init();

				// start neurite detection in given mode
				boolean finished = false;
				switch (detectorMode) {
				case DETECTION:
						finished = startDetectionMode();
						break;
				// case RECALCULATION:
				// finished = startRecalculationMode();
				// break;
				}

				// finish neurite detection
				if (finished) {
						System.out
						    .println("\n---------- NeuriteDetector2D ... >>>finished<<< [Mode: "
						        + detectorMode.toString() + "] ----------\n");
				} else {
						System.out
						    .println("\n---------- NeuriteDetector2D ... >>>ERROR: NOT finished<<< [Mode: "
						        + detectorMode.toString() + "] ----------\n");
						throw new ALDOperatorException(OperatorExceptionType.UNSPECIFIED_ERROR,
						    ">>>>>>> NeuriteDetector2D: operate failed!");
				}
		}

		/**
		 * Method to start neurite detection mode with presegmentation, active
		 * contours and wavelet based neuron component detection.
		 */
		private boolean startDetectionMode() {

				File f = new File(inputDir.getDirectoryName());
				// create path to output directory
				createResultDir(f.getPath());
				if (verbose.booleanValue()) {
						System.out.println("  --> " + "output dir: " + outputDir);
				}

				// create list of input images
				ImageFilter imgFilter = new ImageFilter();
				File[] tmpList = f.listFiles();
				for (File file : tmpList) {
						if (imgFilter.accept(file) && file.isFile()) {
								imageFiles.addElement(file.getPath());
						}
				}

				// check input directory
				if (imageFiles.size() == 0) {
						try {
								throw new NeuriteDetector2DException(
								    ">>>>>>> NeuriteDetector2D: NO valid images found in: " + inputDir);
						} catch (NeuriteDetector2DException e) {
								e.printStackTrace();
						}
						return false;
				}

				// create R engine to use JRI
				rEngine = createRengine();

				// create result table.
				detectionResultTable = makeTable();

				// open all files in the specified directory
				for (int i = 0; i < imageFiles.size(); i++) {
						detectionResultTable.setValueAt(i, detectionResultTable.getRowCount(), 1);
						File currentImage = new File(imageFiles.elementAt(i));
						/*
						 * Open current image and delete non black border of the image (problem of
						 * some microscope cameras).
						 */
						MTBImage mtbImg = null;
						ImageReaderMTB io;
						try {
								io = new ImageReaderMTB(currentImage.getPath());
								io.runOp(null);
								mtbImg = io.getResultMTBImage();
								mtbImg = mtbImg.getImagePart(2, 2, 0, 0, 0, mtbImg.getSizeX() - 4,
								    mtbImg.getSizeY() - 4, mtbImg.getSizeZ(), mtbImg.getSizeT(), mtbImg
								        .getSizeC());

								// check pixel size in x- and y-direction
								if ((mtbImg.getStepsizeX()) != (mtbImg.getStepsizeY())) {
										try {
												throw new NeuriteDetector2DException(
												    "Image pixel size differs in x and y.");
										} catch (NeuriteDetector2DException e) {
												e.printStackTrace();
										}
								}
								if (verbose.booleanValue()) {
										System.out.println("  --> image to open: " + currentImage.getPath());
								}
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D detection failed @ open image: "
								        + currentImage.getPath());
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D detection failed @ open image: "
								        + currentImage.getPath());
								e.printStackTrace();
						} catch (FormatException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D detection failed @ open image: "
								        + currentImage.getPath());
								e.printStackTrace();
						} catch (IOException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D detection failed @ open image: "
								        + currentImage.getPath());
								e.printStackTrace();
						} catch (DependencyException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D detection failed @ open image: "
								        + currentImage.getPath());
								e.printStackTrace();
						} catch (ServiceException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D detection failed @ open image: "
								        + currentImage.getPath());
								e.printStackTrace();
						}

						// get neuron channels as primitive data type array
						int[] tmpNeuronChannels = new int[neuronChannels.length];
						for (int j = 0; j < tmpNeuronChannels.length; j++) {
								tmpNeuronChannels[j] = neuronChannels[j].intValue();
						}

						// generate a new neurite detector algorithm object
						NeuriteDetector2DAlgos neuriteAlgo = new NeuriteDetector2DAlgos(mtbImg,
						    nucleiChannel.intValue(), nucleusSize, nucleusRatio,
						    tmpNeuronChannels, niblackConstant, maxFragmentDistance, currentImage
						        .getPath(), outputDir, energy, alpha, beta, stepSize,
						    motionFraction, maxIterations, resampleConstant, maxSpineLength,
						    neuriteMaskSize, neuriteColor, detectionResultTable, verbose, rEngine);

						// generate a new empty initial contour
						MTBContour2DSet initContourSet = null;
						try {

								// get initial contour from pre-segmentation step
								initContourSet = neuriteAlgo.preSegmentation();

						} catch (NeuriteDetector2DException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D: detection failed @ pre-segmentation in "
								        + currentImage.getPath());
								e.printStackTrace();
						}
						try {

								// refine initial contour by active contours
								neuriteAlgo.neuriteDetection(initContourSet);
						} catch (NeuriteDetector2DException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2D: detection failed @ detection in "
								        + currentImage.getPath());
								e.printStackTrace();
						}

						// add currently detected neurite set to the final vector of neurite sets
						detectedNeurites.addElement(neuriteAlgo.getDetectedNeuritesSet());

						// add analyzed image to list
						analyzedImages.addElement(currentImage.getPath());
						if (verbose.booleanValue()) {
								System.out.println("  --> detection in file: " + currentImage.getPath()
								    + " ...done!\n");
						}
						// intermediate and final table of detection results
						File tableFile = new File(outputDir + File.separator
						    + new File(outputDir).getName() + ".txt");
						detectionResultTable.saveTable(tableFile);
				}
				return true;
		}

		// /**
		// * Method to start neurite recalculation mode using previously calculated
		// * snake results and use the wavelet based neurite component detection to
		// * calculate the neurites.
		// */
		// private boolean startRecalculationMode() {
		// try {
		// throw new NeuriteDetector2DException(
		// ">>>>>>> NeuriteDetector2D: RECALCULATION mode is net yet available!");
		// } catch (NeuriteDetector2DException e) {
		// e.printStackTrace();
		// }
		// return false;
		// }

		/**
		 * Create directory where all results will be stored. The directory is created
		 * in the directory where the image files are stored.
		 * 
		 * @param file
		 *          name of the current experiment (directory name where input images
		 *          are stored)
		 */
		private void createResultDir(String file) {
				String date = getDate();
				int dirCount = 0;
				File tmpF = new File(file);
				// if (detectorMode == NeuriteDetector2DMode.RECALCULATION) {
				// tmpF = new File(file + "_reload");
				// } else {
				tmpF = new File(file + File.separator + tmpF.getName() + "_" + date);
				// }
				while (tmpF.exists()) {
						dirCount++;
						// if (detectorMode == NeuriteDetector2DMode.RECALCULATION) {
						// tmpF = new File(file + "_reload_" + dirCount);
						// } else {
						tmpF = new File(file + File.separator + new File(file).getName() + "_"
						    + date + "_" + dirCount);
						// }
				}
				outputDir = tmpF.toString();
		}

		/**
		 * Get current date for result dirs.
		 * 
		 * @return Current date.
		 */
		private String getDate() {
				Date date = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
				return (dateFormat.format(date));
		}

		/**
		 * Method to generate a R thread inside the java application.
		 * 
		 * @return Generated R engine.
		 */
		private Rengine createRengine() {
				// ensure, that the right versions of R and Java are available
				if (!Rengine.versionCheck()) {
						System.err
						    .println("** JRI R-Engine: Version mismatch - Java files don't match library version.");
						System.exit(1);
				}
				System.out.println("\n------------------------------");
				System.out.println("Creating JRI R-Engine");
				// arguments which should be passed to R
				String[] args = new String[3];
				args[0] = "--quiet"; // Don't print startup message
				args[1] = "--no-restore"; // Don't restore anything
				args[2] = "--no-save";// Don't save workspace at the end of the session
				// generate new R engine
				Rengine re = new Rengine(args, false, null);
				System.out.println("JRI R-Engine created, waiting for R...");
				// wait until thread to create R is ready
				if (!re.waitForR()) {
						System.out.println("Cannot load R");
						return null;
				}
				// print R engine arguments
				System.out.print("JRI R-Engine call: ");
				for (int i = 0; i < args.length; i++) {
						System.out.print(args[i] + " ");
				}
				System.out.println("...done!");
				System.out.println("------------------------------\n");
				// return the R engine
				return re;
		}

		/**
		 * Create the result table and include the specific table header.
		 */
		private MTBTableModel makeTable() {
				// initialize table
				Vector<String> header = new Vector<String>();
				header.add("file");
				header.add("neuronID");
				header.add("neuronArea");
				// header.add("somaArea");
				header.add("neuriteID");
				header.add("nArea");
				header.add("nBranches");
				header.add("nSpines");
				header.add("nEnds");
				header.add("branchID");
				header.add("bLength");
				header.add("bWidth");
				header.add("bsLength");
				header.add("bsWidth");
				header.add("bsArea");
				header.add("bcLength");
				header.add("bcWidth");
				header.add("bcArea");
				header.add("bcSpines");
				header.add("scale");
				header.add("unit");
				header.add("iter");
				// 21
				MTBTableModel table = new MTBTableModel(0, header.size(), header);
				return table;
		}
}
