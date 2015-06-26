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
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Vector;

import org.rosuda.JRI.Rengine;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.neurites2D.NeuriteExtractor2D.NeuronColor;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2D;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DSet;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D.FieldOperation;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D.GradientMode;
import de.unihalle.informatik.MiToBo.filters.nonlinear.RankOperator;
import de.unihalle.informatik.MiToBo.filters.nonlinear.RankOperator.RankOpMode;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.io.tools.FilePathManipulator;
import de.unihalle.informatik.MiToBo.morphology.*;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess.ProcessMode;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.MTBGammaPtWiseExtEner;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination.MTBTermMotionDiff;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.MTBSnakeEnergyCD_KassLength_ParamAdaptNone;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.CalcGlobalThreshOtsu;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack;
import de.unihalle.informatik.MiToBo.tools.image.ImageDimensionReducer;

/**
 * <pre>
 * 
 * The class implements the basic algorithm for neurite detection based on a
 * 2D multichannel fluorescence image.
 * 
 * The basic steps are:
 *  1. pre-segmentation step to get a coarse contour of the neurons, yielding a
 *     initialization for the active contour models (snake)
 *  2. refinement of the initial contours by the active contour models
 *  3. identification of structural neuron parts, like soma, neurites and
 *     growth cones via a wavelet based detection
 *  
 * A lot of intermediate results are save to the special result directories.
 * After detection of the neurites a result table with all measured morphology
 * values is shown. An result image showing the detected neurites with their
 * neurite traces and special points is saved to the output directory.
 * 
 * </pre>
 * 
 * @see NeuriteExtractor2D
 * 
 * @author Danny Misiak
 * 
 */

public class NeuriteDetector2DAlgos {

		/**
		 * Label for different external energies of the snake which can be used by the
		 * detector. By default the distance map is chosen. Other energies will be
		 * available soon.
		 * 
		 * @author Danny Misiak
		 * 
		 */
		public static enum DetectorExternalEnergy {
				// external snake energy is based on a distance map
				DISTANCE_MAP
				// ,
				// // external snake energy is based on a gradient vector field (GVF)
				// GVF
		}

		/*
		 * Custom members.
		 */

		/**
		 * Image size in x-direction, given in pixel.
		 */
		private int width;

		/**
		 * Image size in y-direction, given in pixel.
		 */
		private int height;

		/**
		 * Maximum intensity projection (MIP) image of the neuron. This image gives
		 * the basis for the automatic detection.
		 */
		private MTBImage neuronMIP;

		/**
		 * Fluorescence labeled nuclei image from the multichannel microscope image.
		 */
		private MTBImage nucleus;

		/**
		 * Number of snake iterations used in current optimization process.
		 */
		private int[] snakeIterCount;

		/**
		 * Final set of detected neurites.
		 */
		private MTBNeurite2DSet detectedNeuritesSet;

		/**
		 * Result directory for pre-segmentation.
		 */
		private String result_presegmentation;

		/**
		 * Result directory for snake results.
		 */
		private String result_snakes;

		/**
		 * Result directory for neurite extraction results.
		 */
		private String result_extraction;

		/**
		 * Result directory for snake energy results.
		 */
		private String result_energy;

		/**
		 * Current image name without file extension.
		 */
		private String file_name_noExtension;

		/*
		 * Members used in constructor.
		 */

		/**
		 * Multichannel fluorescence image of the labeled neuron.
		 */
		private MTBImage inputImage;

		/**
		 * Channel number, including the nuclei, in the current multichannel
		 * fluorescence image.
		 */
		private int nucleiChannel;

		/**
		 * Minimum size of nucleus region to set region as intact nucleus.
		 */
		private int nucleusSize;
		/**
		 * Ratio of nucleus pixels which should be included in a intact neuron region
		 * to specify the neuron region as active and use for detection.
		 */
		private double nucleusRatio;

		/**
		 * Channel numbers, including the neuron stains for detection of the neurons
		 * in the current multichannel fluorescence image.
		 */
		private int[] neuronChannels;

		/**
		 * Application based constant for Niblack thresholding.
		 */
		private double niblackConstant;

		/**
		 * Maximum distance to connect a fragment to a detected neuron, given in
		 * pixel.
		 */
		private int maxFragmentDistance;

		/**
		 * File name of the current multichannel fluorescence image.
		 */
		private String fileName;

		/**
		 * Main directory for result output.
		 */
		private String resultDir;

		/**
		 * Label of available external snake energies, which are usable by the
		 * detector.
		 */
		private DetectorExternalEnergy energyLabel;

		/**
		 * Weighting factor for snake length term.
		 */
		private double alpha;
		/**
		 * Weighting factor for snake curvature term.
		 */
		private double beta;

		/**
		 * Step size value for a snake step (gamma) during optimization.
		 */
		private double stepSize;

		/**
		 * Minimum fraction of points which should have stooped to move until the
		 * snake optimization stops.
		 */
		private double motionFraction;

		/**
		 * Number of snake iterations.
		 */
		private int maxIterations;

		/**
		 * Constant for resampling the snake control points.
		 */
		private int resampleConstant;

		/**
		 * Maximum length to define a branch of a neurite as spine (filopodia-like
		 * protrusion) in pixel.
		 */
		private int maxSpineLength;

		/**
		 * Mask size of average neurite with in pixel. This mask size is used for the
		 * morphological opening to remove neurites from the soma in the neurite
		 * extraction step.
		 */
		private int neuriteMaskSize;

		/**
		 * Region color of detected neurites to view the neurites in the result image.
		 */
		private Color neuriteColor;

		/**
		 * Result table showing the the detection and morphology measurements.
		 */
		private MTBTableModel resultTable;

		/**
		 * Verbose flag for standard console outputs.
		 */
		private Boolean verbose;

		/**
		 * R engine to start R thread.
		 */
		private Rengine rEngine;

		/**
		 * Standard constructor.
		 */
		public NeuriteDetector2DAlgos() {
				// nothing to do here
		}

		/**
		 * Constructor.
		 * 
		 * @param _inputImage
		 *          multichannel fluorescence image
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
		 * @param _pathToFile
		 *          path to current image file
		 * @param resultDir
		 *          directory of output results
		 * @param _energyLabel
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
		 * @param _table
		 *          result table
		 * @param _verbose
		 *          flag for standard console outputs
		 * @param _re
		 *          R engine to call R scripts from a Java thread
		 */
		public NeuriteDetector2DAlgos(MTBImage _inputImage, int _nucleiChannel,
		    int _nucleusSize, double _nucleusRation, int[] _neuronChannels,
		    double _niblackConstant, int _maxFragmentDistance, String _pathToFile,
		    String _resultDir, DetectorExternalEnergy _energyLabel, double _alpha,
		    double _beta, double _stepSize, double _motionFraction,
		    int _maxIterations, int _resampleConstant, int _maxSpineLength,
		    int _neuriteMaskSize, Color _neuriteColor, MTBTableModel _table,
		    Boolean _verbose, Rengine _re) {

				// set members
				inputImage = _inputImage;
				nucleiChannel = _nucleiChannel;
				nucleusSize = _nucleusSize;
				nucleusRatio = _nucleusRation;
				neuronChannels = _neuronChannels;
				niblackConstant = _niblackConstant;
				maxFragmentDistance = _maxFragmentDistance;
				fileName = _pathToFile;
				file_name_noExtension = FilePathManipulator.getFileName(_pathToFile);
				resultDir = _resultDir;
				energyLabel = _energyLabel;
				alpha = _alpha;
				beta = _beta;
				stepSize = _stepSize;
				motionFraction = _motionFraction;
				maxIterations = _maxIterations;
				resampleConstant = _resampleConstant;
				maxSpineLength = _maxSpineLength;
				neuriteMaskSize = _neuriteMaskSize;
				neuriteColor = _neuriteColor;
				resultTable = _table;
				verbose = _verbose;
				rEngine = _re;

				// check if nuclei channel is valid
				if (this.nucleiChannel < 1
				    || this.nucleiChannel > this.inputImage.getSizeC()) {
						try {
								throw new NeuriteDetector2DException(
								    ">>>>>>> NeuriteDetector2DAlgos: detection failed @ nuclei channel exceeds the image boundaries!");
						} catch (NeuriteDetector2DException e) {
								e.printStackTrace();
						}
				}
				// check if nucleus size is valid
				if (nucleusSize < 0) {
						try {
								throw new NeuriteDetector2DException(
								    ">>>>>>> NeuriteDetector2DAlgos: detection failed @ nucleus size must be a positive value!");
						} catch (NeuriteDetector2DException e) {
								e.printStackTrace();
						}
				}
				// check if nucleus ratio is valid
				if (nucleusRatio < 0.0 || nucleusRatio > 1.0) {
						try {
								throw new NeuriteDetector2DException(
								    ">>>>>>> NeuriteDetector2DAlgos: detection failed @ nucleus ratio must be in range [0.0, 1.0]!");
						} catch (NeuriteDetector2DException e) {
								e.printStackTrace();
						}
				}

				// check if all neuron channel numbers are valid
				boolean error = false;
				for (int i = 0; i < neuronChannels.length; i++) {
						if (neuronChannels[i] < 1
						    || neuronChannels[i] > this.inputImage.getSizeC()) {
								error = true;
						}
				}
				if (error) {
						try {
								throw new NeuriteDetector2DException(
								    ">>>>>>> NeuriteDetector2DAlgos: detection failed @ neuron channels must be in range [1, #ImageChannels]!");
						} catch (NeuriteDetector2DException e) {
								e.printStackTrace();
						}
				}

				// initialize detector
				this.init();
		}

		/**
		 * Get weighting factor for snake length term.
		 */
		public double getAlpha() {
				return alpha;
		}

		/**
		 * Get weighting factor for snake curvature term.
		 */
		public double getBeta() {
				return beta;
		}

		/**
		 * Get number snake iteration counts, used for optimization.
		 */
		public int[] getSnakeIterCount() {
				return snakeIterCount;
		}

		/**
		 * Get multichannel fluorescence input image.
		 */
		public MTBImage getInputImage() {
				return inputImage;
		}

		/**
		 * Get channel number, including the labeled nuclei.
		 */
		public int getNucleiChannel() {
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
		public int[] getNeuronChannles() {
				return neuronChannels;
		}

		/**
		 * Get Niblack thresholding constant.
		 */
		public double getNiblackConstant() {
				return niblackConstant;
		}

		/**
		 * Get maximum fragment distance , in pixel.
		 */
		public int getMaxFragmentDistance() {
				return maxFragmentDistance;
		}

		/**
		 * Get energy label for external energy, used for optimization.
		 */
		public DetectorExternalEnergy getEnergyLabel() {
				return energyLabel;
		}

		/**
		 *Get step size of snake point movement.
		 */
		public double getStepSize() {
				return stepSize;
		}

		/**
		 * Get minimum motion fraction of snake control points, to finish snake
		 * optimization.
		 */
		public double getMotionFraction() {
				return motionFraction;
		}

		/**
		 * Get maximum number of iterations to finish snake optimization,
		 */
		public int getMaxIterations() {
				return maxIterations;
		}

		/**
		 * Get constant for snake point resampling.
		 */
		public int getResampleConstant() {
				return resampleConstant;
		}

		/**
		 * Get maximum length of a spine, in pixel.
		 */
		public int getMaxSpineLength() {
				return maxSpineLength;
		}

		/**
		 * Get maximum neurite mask size, in pixel. Defines the average width of a
		 * neurite.
		 */
		public int getNeuriteMaskSize() {
				return neuriteMaskSize;
		}

		/**
		 * Get neurite region color (RGB) for result image.
		 */
		public Color getNeuriteColor() {
				return neuriteColor;
		}

		/**
		 * 
		 * Get directory of intermediate and final results.
		 */
		public String getResultDir() {
				return resultDir;
		}

		/**
		 * Get final result table of morphology measurements.
		 */
		public MTBTableModel getResultTable() {
				return resultTable;
		}

		/**
		 * Get set of detected neurites.
		 */
		public MTBNeurite2DSet getDetectedNeuritesSet() {
				return detectedNeuritesSet;
		}

		/**
		 * Initialize detector.
		 */
		private void init() {

				// create some image specific values
				width = inputImage.getSizeX();
				height = inputImage.getSizeY();

				// create the result directories for all (intermediate) results
				result_presegmentation = resultDir + File.separator + file_name_noExtension
				    + File.separator + "preseg_results";
				result_snakes = resultDir + File.separator + file_name_noExtension
				    + File.separator + "snake_results";
				result_extraction = resultDir + File.separator + file_name_noExtension
				    + File.separator + "extraction_results";
				result_energy = resultDir + File.separator + file_name_noExtension
				    + File.separator + "energy_results";

				// set the result directories
				setResultDirs();

				/*
				 * Maximum intensity projection (MIP) of the labeled neuron. The MIP is used
				 * for detection of the neuron. Only the neuron channel numbers are used for
				 * the MIP.
				 */
				neuronMIP = MTBImage.createMTBImage(inputImage.getSizeX(), inputImage
				    .getSizeY(), inputImage.getSizeZ(), inputImage.getSizeT(),
				    neuronChannels.length, inputImage.getType());
				// collect given neuron channel images
				for (int i = 0; i < neuronChannels.length; i++) {
						MTBImage tmpImage = inputImage.getSlice(0, 0, neuronChannels[i] - 1);
						neuronMIP.setImagePart(tmpImage, 0, 0, 0, 0, i);
				}

				// do maximum intensity projection
				try {
						ImageDimensionReducer IDR = new ImageDimensionReducer(neuronMIP, false,
						    false, false, false, true, ImageDimensionReducer.ReducerMethod.MAX);
						IDR.runOp(null);
						neuronMIP = IDR.getResultImg();
						neuronMIP.setTitle(file_name_noExtension + "-maxProjection.tif");
				} catch (ALDOperatorException e1) {
						System.out.println(">>>>>>> detection failed @ maximum projection");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out.println(">>>>>>> detection failed @ maximum projection");
						e.printStackTrace();
				}

				// save MIP image of the neuron
				ImageWriterMTB IW;
				try {
						IW = new ImageWriterMTB(neuronMIP, result_presegmentation
						    + File.separator + file_name_noExtension + "-maxProjection.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e1) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save maximum projection!");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save maximum projection!");
						e.printStackTrace();
				}

				/*
				 * Nuclei image from the multichannel fluorescence image. This image is used
				 * to detect the nuclei and distinguish between intact and dead cells in a
				 * later step.
				 */
				nucleus = inputImage.getSlice(0, 0, nucleiChannel - 1);
				try {
						RankOperator median = new RankOperator(nucleus, RankOpMode.MEDIAN, 2, 2,
						    0, 0, 0);
						median.runOp(null);
						nucleus = median.getResultImg();
						nucleus.setTitle(file_name_noExtension + "-nucleusStain.tif");
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ median filter at nucleus!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ median filter at nucleus!");
						e.printStackTrace();
				}
		}

		/**
		 * Create all output directories for the single detection steps.
		 */
		private void setResultDirs() {
				File f = new File(result_presegmentation);
				f.mkdirs();
				f = new File(result_snakes);
				f.mkdirs();
				f = new File(result_extraction);
				f.mkdirs();
				f = new File(result_energy);
				f.mkdirs();
				f = new File(resultDir + File.separator + "profileData");
				f.mkdirs();
		}

		/**
		 * Main method for 2D neurite detection. Using the initial contours of the
		 * pre-segmented neurons as input for the active contour models to calculate
		 * an exact contour of each neuron.
		 * 
		 * @param initContourSet
		 *          set of initial contours for snakes
		 * 
		 */
		public void neuriteDetection(MTBContour2DSet initContourSet)
		    throws NeuriteDetector2DException {

				// check pre-segmented contours
				if (initContourSet == null) {
						throw new NeuriteDetector2DException(
						    ">>>>>>> NeuriteDetector2DAlgos: detection failed @ no initial contour found!");
				}

				// generate empty snake set
				MTBPolygon2DSet neuronSnakeSet = new MTBPolygon2DSet(0.0, 0.0, width - 1,
				    height - 1);

				// define some output images
				MTBImage initContourBin = MTBImage.createMTBImage(width, height, 1, 1, 1,
				    MTBImageType.MTB_BYTE);
				initContourBin.fillBlack();
				MTBImage initContourGray = neuronMIP.duplicate();

				// create the different snakes for each coarse neuron contour
				for (int i = 0; i < initContourSet.size(); i++) {

						// get initial snake control points from the initial contour
						MTBContour2D tmpInitContour = initContourSet.elementAt(i);
						Vector<MTBSnakePoint2D> controlPoints = getSnakeControlPoints(tmpInitContour);

						// create main snake
						MTBSnake tmpMainSnake = new MTBSnake(controlPoints, true);

						// add main snake to snake set
						neuronSnakeSet.add(tmpMainSnake);

						// add main snake to output images
						try {
								initContourBin = tmpMainSnake.toMTBImage(null, initContourBin);
								initContourGray = tmpMainSnake.toMTBImage(null, initContourGray);
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ initialize snakes!");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ initialize snakes!");
								e.printStackTrace();
						}

						/*
						 * Search for inner contours inside the main snake and add these to the
						 * snake set and output images.
						 */
						for (int j = 0; j < tmpInitContour.countInner(); j++) {
								MTBContour2D tmpInnerCont = tmpInitContour.getInner(j);
								Vector<MTBSnakePoint2D> tmpInnerSnakePoints = getSnakeControlPoints(tmpInnerCont);
								MTBSnake tmpInnerSnake = new MTBSnake(tmpInnerSnakePoints, true);

								// add inner contours to snake set
								neuronSnakeSet.add(tmpInnerSnake);

								// add inner contour to output images
								try {
										initContourBin = tmpInnerSnake.toMTBImage(null, initContourBin);
										initContourGray = tmpInnerSnake.toMTBImage(null, initContourGray);
								} catch (ALDOperatorException e) {
										System.out
										    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ initialize snake!");
										e.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										System.out
										    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ initialize snake!");
										e.printStackTrace();
								}
						}
				}
				// save output images
				ImageWriterMTB IW;
				try {
						IW = new ImageWriterMTB(initContourBin, result_snakes + File.separator
						    + file_name_noExtension + "-snake-init-contour_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
						IW = new ImageWriterMTB(initContourGray, result_snakes + File.separator
						    + file_name_noExtension + "-snake-init-contour.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e1) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save initialized snake!");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save initialized snake!");
						e.printStackTrace();
				}

				/*
				 * Use pre-segmented contours to initialize the snakes and start snake
				 * optimization.
				 */
				neuronSnakeSet = optimizeSnakes(neuronSnakeSet);

				// create output images for snake results
				MTBImage contourFinalBin = null;
				MTBImage contourFinalGrayNeuron = null;
				try {

						// add snakes to output images, including main and inner snakes
						contourFinalBin = ((MTBSnake) neuronSnakeSet.elementAt(0))
						    .toMTBImageByte(null, width, height);
						contourFinalGrayNeuron = ((MTBSnake) neuronSnakeSet.elementAt(0))
						    .toMTBImage(null, neuronMIP);
						for (int i = 1; i < neuronSnakeSet.size(); i++) {
								MTBSnake tmpSnake = (MTBSnake) neuronSnakeSet.elementAt(i);
								contourFinalBin = tmpSnake.toMTBImage(null, contourFinalBin);
								contourFinalGrayNeuron = tmpSnake.toMTBImage(null,
								    contourFinalGrayNeuron);
						}

						// save output images of snake results
						IW = new ImageWriterMTB(contourFinalBin, result_snakes + File.separator
						    + file_name_noExtension + "-contour-final_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
						IW = new ImageWriterMTB(contourFinalGrayNeuron, result_snakes
						    + File.separator + file_name_noExtension + "-contour-final.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e1) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save final snake!");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save final snake!");
						e.printStackTrace();
				}

				// get 2D region included in the snake
				MTBRegion2DSet snakeRegionSet = getSnakeRegion(neuronSnakeSet);

				/*
				 * Extract the neurite regions. Plot neurite width of calculated 1D neurite
				 * width profile and the detected between the structural neuron components
				 * (soma, neurites, growth cones), using the centWave algorithm for high
				 * resolution LC/MS data.
				 * 
				 * See NeuriteExtractor2D for more details.
				 */
				if (verbose.booleanValue()) {
						System.out.println("  --> " + "starting NeuriteExtractor2D()...");
				}

				// generate a new neurite extractor object
				NeuriteExtractor2D neuriteExtract = null;

				/*
				 * Generate IDs for neurites and branches.
				 * 
				 * Depending on the number of branches, the IDs of neurites and neurons are
				 * repeated.
				 */
				// IDs of the single neurons detected in the image
				Vector<Integer> neuronIDs = new Vector<Integer>();
				// IDs of the single neurites detected in each neuron
				Vector<Integer> neuriteIDs = new Vector<Integer>();
				// IDs of the single branches detected in each neurite
				Vector<Integer> branchIDs = new Vector<Integer>();

				/*
				 * Process neurite extraction for each neuron region.
				 */
				for (int i = 0; i < snakeRegionSet.size(); i++) {
						MTBRegion2D tmpSnakeRegion = snakeRegionSet.elementAt(i);
						MTBImageByte singleSnakeRegionImg = null;
						try {
								singleSnakeRegionImg = (MTBImageByte) tmpSnakeRegion.toMTBImageByte(
								    null, width, height);
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ draw single snake region!");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ draw single snake region!");
								e.printStackTrace();
						}
						singleSnakeRegionImg.setTitle(file_name_noExtension + "-" + i);
						MTBNeurite2DSet tmpNeuriteSet = null;
						try {
								neuriteExtract = new NeuriteExtractor2D(singleSnakeRegionImg,
								    NeuronColor.WHITE, neuriteMaskSize, maxSpineLength, rEngine,
								    result_extraction);
								neuriteExtract.runOp();
						} catch (ALDOperatorException e1) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ neurite feature extraction!");
								e1.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ neurite feature extraction!");
								e.printStackTrace();
						}
						if (i == 0) {
								detectedNeuritesSet = neuriteExtract.getExtractedNeurites();
								for (int j = 0; j < detectedNeuritesSet.size(); j++) {
										int numberBranches = detectedNeuritesSet.getElementAt(j)
										    .getBranchLengths().length;
										for (int k = 0; k < numberBranches; k++) {
												neuronIDs.add(new Integer(i));
												neuriteIDs.add(new Integer(j));
												branchIDs.add(new Integer(k));
										}
								}
						} else {
								tmpNeuriteSet = neuriteExtract.getExtractedNeurites();
								for (int j = 0; j < tmpNeuriteSet.size(); j++) {
										detectedNeuritesSet.add(tmpNeuriteSet.getElementAt(j));
										int numberBranches = tmpNeuriteSet.getElementAt(j).getBranchLengths().length;
										for (int k = 0; k < numberBranches; k++) {
												neuronIDs.add(new Integer(i));
												neuriteIDs.add(new Integer(j));
												branchIDs.add(new Integer(k));
										}
								}
						}
				}

				/*
				 * Save extracted neurite regions with full skeleton graph and neurite
				 * region.
				 */
				try {
						IW = new ImageWriterMTB(
						    detectedNeuritesSet.getNeuriteImage(neuriteColor), resultDir
						        + File.separator + file_name_noExtension + File.separator
						        + file_name_noExtension + "-detectedNeurites.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save detected neurites image!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save detected neurites image!");
						e.printStackTrace();
				}

				// System.out.println("##### " + neuronIDs.toString());
				// System.out.println("###### " + neuriteIDs.toString());
				// System.out.println("####### " + branchIDs.toString());

				/*
				 * Fill result table.
				 */
				double calibrationX = inputImage.getStepsizeX();
				calibrationX = Math.round(calibrationX / 0.001) * 0.001;
				double calibrationY = inputImage.getStepsizeY();
				calibrationY = Math.round(calibrationY / 0.001) * 0.001;
				String calibrationUnitX = inputImage.getUnitX();
				String calibrationUnitY = inputImage.getUnitY();

				// check calibration unit in x-and y-direction
				if (calibrationUnitX != calibrationUnitY) {
						calibrationUnitX = "NA";
				}

				if (detectedNeuritesSet.size() < 1) {
						// set name of current image
						resultTable.setValueAt(fileName, resultTable.getRowCount() - 1, 0);
				} else {

						// fill table for each neurite and its branches
						for (int index = 0; index < branchIDs.size(); index++) {
								MTBNeurite2D tmpNeurite = detectedNeuritesSet.getElementAt(neuronIDs
								    .elementAt(index)
								    + neuriteIDs.elementAt(index));
								int[] tmpNLength = tmpNeurite.getBranchLengths();
								int[] tmpSLength = tmpNeurite.getShaftLengths();
								int[] tmpCLength = tmpNeurite.getConeLengths();
								int[] tmpCArea = tmpNeurite.getConeAreas();
								int[] tmpCSpine = tmpNeurite.getConeSpineCount();
								double[] tmpNWidth = tmpNeurite.getAvgNeuriteWidths();
								double[] tmpSWidth = tmpNeurite.getAvgShaftWidths();
								double[] tmpCWidth = tmpNeurite.getAvgConeWidths();

								// Note: soma area are is not useful, due to neurites with no features.
								// somaArea += tmpNeurite.getNeuriteRegion().getArea();

								if (index == 0) {
										// set name of current image
										resultTable.setValueAt(fileName, resultTable.getRowCount() - 1, 0);
										// set neuron ID, same for all neurites of current image
										resultTable.setValueAt(neuronIDs.elementAt(index), resultTable
										    .getRowCount() - 1, 1);
								} else {
										// set image name for following neurites of the current image
										resultTable.setValueAt(fileName, resultTable.getRowCount(), 0);
										// set neuron ID, same for all neurites of current image
										resultTable.setValueAt(neuronIDs.elementAt(index), resultTable
										    .getRowCount() - 1, 1);
								}
								// set neuron area, including shaft and growth cone areas
								resultTable.setValueAt(snakeRegionSet.elementAt(
								    neuronIDs.elementAt(index)).getArea(),
								    resultTable.getRowCount() - 1, 2);
								// set neurite ID
								resultTable.setValueAt(neuriteIDs.elementAt(index), resultTable
								    .getRowCount() - 1, 3);
								// set neurite area, including shaft and growth cones
								resultTable.setValueAt(tmpNeurite.getNeuriteRegion().getArea(),
								    resultTable.getRowCount() - 1, 4);
								// set number of neurite branches
								resultTable.setValueAt(tmpNeurite.getBrancheCount(), resultTable
								    .getRowCount() - 1, 5);
								// set number of neurite spines
								resultTable.setValueAt(tmpNeurite.getSpineCount(), resultTable
								    .getRowCount() - 1, 6);
								// set number of neurite end points
								resultTable.setValueAt(tmpNeurite.getEndCount(), resultTable
								    .getRowCount() - 1, 7);
								// set neurite branch ID
								resultTable.setValueAt(branchIDs.elementAt(index), resultTable
								    .getRowCount() - 1, 8);
								// set neurite branch length
								resultTable.setValueAt(tmpNLength[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 9);
								// set average width of neurite branch
								resultTable.setValueAt(tmpNWidth[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 10);
								// set neurite shaft length of branch, without growth cone area
								resultTable.setValueAt(tmpSLength[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 11);
								// set average neurite shaft width of branch, without growth cone area
								resultTable.setValueAt(tmpSWidth[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 12);
								// set neurite shaft area of branch, without growth cone area
								resultTable.setValueAt(tmpNeurite.getNeuriteShaftRegion().getArea(),
								    resultTable.getRowCount() - 1, 13);
								// set growth cone length of branch
								resultTable.setValueAt(tmpCLength[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 14);
								// set average growth cone width of branch
								resultTable.setValueAt(tmpCWidth[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 15);
								// set growth cone area of branch
								resultTable.setValueAt(tmpCArea[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 16);
								// set number of spines in growth cone of branch
								resultTable.setValueAt(tmpCSpine[branchIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 17);
								// set calibration scale
								resultTable.setValueAt(calibrationX + " x " + calibrationY, resultTable
								    .getRowCount() - 1, 18);
								// set calibration unit
								resultTable.setValueAt(calibrationUnitX, resultTable.getRowCount() - 1,
								    19);
								// set snake iteration count
								resultTable.setValueAt(this.snakeIterCount[neuronIDs.elementAt(index)],
								    resultTable.getRowCount() - 1, 20);
						}

				}
		}

		/**
		 * Method for pre-segmentation. Binarization, regions- and contour labeling
		 * take place. As result the rough contours of the neurons are returned to be
		 * used as initial contours for active contour models in the
		 * `neuriteDetection` method.
		 * 
		 * @return Set of 2D neuron contours.
		 * @throws NeuriteDetector2DException
		 */
		public MTBContour2DSet preSegmentation() throws NeuriteDetector2DException {
				/*
				 * Thresholding
				 */
				ImgThreshNiblack niblack;
				MTBImageByte binCellsImage = null;
				ImageWriterMTB IW;
				try {
						niblack = new ImgThreshNiblack(neuronMIP,
						    ImgThreshNiblack.Mode.WHOLE_IMAGE, niblackConstant, -1, 0, -1, 0,
						    null);
						niblack.runOp(null);
						binCellsImage = niblack.getResultImage();
						// save the binary neuron cells image
						IW = new ImageWriterMTB(binCellsImage, result_presegmentation
						    + File.separator + file_name_noExtension + "-neurons_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ Niblack thresholding!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ Niblack thresholding!");
						e.printStackTrace();
				}

				// binary image of the nuclei
				MTBImage binCoreRegionImg = null;
				try {
						CalcGlobalThreshOtsu otsu = new CalcGlobalThreshOtsu(nucleus);
						otsu.runOp(null);
						double mythresh = otsu.getOtsuThreshold().getValue();
						ImgThresh IT = new ImgThresh(nucleus, mythresh);
						IT.runOp(null);
						binCoreRegionImg = IT.getResultImage();
						// save the binary core image
						IW = new ImageWriterMTB(binCoreRegionImg, result_presegmentation
						    + File.separator + file_name_noExtension + "-nuclei_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ Otsu thresholding!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ Otsu thresholding!");
						e.printStackTrace();
				}

				/*
				 * Searching for the regions in the neuron image, that have one region in
				 * the nucleus image. Most death neurons implicit removed, because of having
				 * no nucleus region in the neuron image or have a smaller region in the
				 * neuron image as in the nucleus image.
				 * 
				 * Small regions are uninteresting. The cells must have more than 1000
				 * pixels in her region, the nucleus must have more than 100 pixels. If a
				 * nucleus region should belong to a neuron region, >90% of the nucleus
				 * pixels should exist in the associated neuron region.
				 */

				// label core regions
				MTBRegion2DSet coreRegionSet = null;
				try {
						LabelComponentsSequential LCS = new LabelComponentsSequential(
						    binCoreRegionImg, true);
						LCS.runOp(null);
						coreRegionSet = LCS.getResultingRegions();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label nuclei!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label nuclei!");
						e.printStackTrace();
				}
				// TODO fixed parameter
				if (coreRegionSet == null || coreRegionSet.size() == 0
				    || coreRegionSet.size() > 1000) {
						throw new NeuriteDetector2DException(
						    ">>>>>>> NeuriteDetector2DAlgos: detection failed @ nuclei selection!");
				}
				// label cell regions with core and special size
				MTBRegion2DSet allRegionSet = null;
				try {
						LabelComponentsSequential LCS = new LabelComponentsSequential(
						    binCellsImage, true);
						LCS.runOp(null);
						allRegionSet = LCS.getResultingRegions();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label neurons!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label neurons!");
						e.printStackTrace();
				}
				if (allRegionSet == null || allRegionSet.size() == 0) {
						throw new NeuriteDetector2DException(
						    ">>>>>>> NeuriteDetector2DAlgos: presegmentation failed @ neuron selection!");
				}

				// search for regions with exact one nucleus region
				Vector<Integer> indices = new Vector<Integer>();
				for (int i = 0; i < allRegionSet.size(); i++) {
						int core_count = 0;
						MTBRegion2D ce = allRegionSet.elementAt(i);
						if (ce.getArea() > 1000) {
								for (int j = 0; j < coreRegionSet.size(); j++) {
										MTBRegion2D co = coreRegionSet.elementAt(j);
										if (co.getArea() > nucleusSize) {
												if (testRegions(ce, co) == true) {
														core_count = core_count + 1;
												}
										}
								}
								if (core_count == 1) {
										indices.addElement(new Integer(i));
								}
						}
				}
				if (indices.size() <= 0) {
						return null;
				}

				// create set of all regions with exact one nucleus
				MTBRegion2DSet cellRegions = new MTBRegion2DSet(0, 0, width - 1, height - 1);
				for (int i = 0; i < indices.size(); i++) {
						cellRegions.add(allRegionSet.elementAt(indices.elementAt(i)));
				}

				// save all binary cell regions to a binary image
				MTBImageByte binCellRegionImg = null;
				try {
						binCellRegionImg = cellRegions.elementAt(0).toMTBImageByte(null, width,
						    height);
						for (int i = 1; i < cellRegions.size(); i++) {
								binCellRegionImg = (MTBImageByte) cellRegions.elementAt(i).toMTBImage(
								    null, binCellRegionImg).convertType(MTBImageType.MTB_BYTE, true);
						}
						IW = new ImageWriterMTB(binCellRegionImg, result_presegmentation
						    + File.separator + file_name_noExtension + "-neuron-region_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e2) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save binary neurons!");
						e2.printStackTrace();
				} catch (ALDProcessingDAGException e2) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save binary neurons!");
						e2.printStackTrace();
				}

				/*
				 * Binary regions with the selected ones and those who have no nucleus and
				 * are above a given roundness threshold.
				 */
				MTBImageByte binFragmentImg = (MTBImageByte) MTBImage.createMTBImage(width,
				    height, 1, 1, 1, MTBImageType.MTB_BYTE);

				// remove all other regions with a core
				for (int i = 0; i < allRegionSet.size(); i++) {
						MTBRegion2D ce = allRegionSet.elementAt(i);
						for (int j = 0; j < coreRegionSet.size(); j++) {
								MTBRegion2D co = coreRegionSet.elementAt(j);
								// check for core inclusion
								if (testRegions2(ce, co) == true) {
										allRegionSet.removeElementAt(i);
										if (i > 0)
												i--;
										else
												i = 0;
								}
						}
				}

				// add all regions without a core to the current neuron regions
				for (int i = 0; i < allRegionSet.size(); i++) {
						MTBRegion2D ce = allRegionSet.elementAt(i);
						// check for roundness
						if ((ce.getEccentricity() * ((double) ce.getArea())) >= 50.0) {
								Vector<Point2D.Double> RV = ce.getPoints();
								for (int j = 0; j < RV.size(); j++) {
										Point2D.Double RV_P = RV.get(j);
										int x = (int) Math.round(RV_P.getX());
										int y = (int) Math.round(RV_P.getY());
										binFragmentImg
										    .putValueDouble(x, y, (int) binFragmentImg.getTypeMax());
								}
						}
				}

				/*
				 * Draw selected region with all possible fragments in one temporary image
				 * and try to link some fragments to the neuron.
				 */
				MTBRegion2DSet linkedCellRegionSet = new MTBRegion2DSet(0, 0, width - 1,
				    height - 1);
				for (int i = 0; i < cellRegions.size(); i++) {
						// temporary image with current neuron region and possible fragments
						MTBImageByte tmpLinkingImg = (MTBImageByte) binFragmentImg.duplicate()
						    .convertType(MTBImageType.MTB_BYTE, true);
						try {
								tmpLinkingImg = (MTBImageByte) cellRegions.elementAt(i).toMTBImage(
								    null, tmpLinkingImg);
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ create temporary fragment image!");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ create temporary fragment image!");
								e.printStackTrace();
						}

						/*
						 * Closing to improve the pre segmented region. Using a 3x3 mask.
						 */
						try {
								// dilation
								ImgDilate imDil = new ImgDilate(tmpLinkingImg, 3);
								imDil.runOp(null);
								// erosion
								ImgErode imEro = new ImgErode(imDil.getResultImage(), 3);
								imEro.runOp(null);
								tmpLinkingImg = (MTBImageByte) imEro.getResultImage();
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ closing at temporary fragment image!");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ closing at temporary fragment image!");
								e.printStackTrace();
						}

						/*
						 * Link all candidate regions.
						 */
						ComponentPostprocess linking = null;
						try {
								linking = new ComponentPostprocess(tmpLinkingImg,
								    ProcessMode.LINK_ADJ_COMPS);
								linking.setMaximalComponentDistance((int) Math.pow(maxFragmentDistance,
								    2.0));
								linking.setDiagonalNeighbors(true);
								linking.runOp();
						} catch (ALDOperatorException e2) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ link temporary fragment candidates!");
								e2.printStackTrace();
						} catch (ALDProcessingDAGException e2) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ link temporary fragment candidates!");
								e2.printStackTrace();
						}

						tmpLinkingImg = (MTBImageByte)linking.getResultImage();

						/*
						 * Get only the binary and linked region from the selected neuron.
						 */
						try {
								LabelComponentsSequential LCS = new LabelComponentsSequential(
								    tmpLinkingImg, true);
								LCS.runOp(null);
								MTBRegion2DSet tmpLinkedRegions = LCS.getResultingRegions();
								for (int j = 0; j < tmpLinkedRegions.size(); j++) {
										Vector<Point2D.Double> regPoints = cellRegions.elementAt(i)
										    .getPoints();
										Vector<Point2D.Double> linkedRegPoints = tmpLinkedRegions
										    .elementAt(j).getPoints();
										if (linkedRegPoints.containsAll(regPoints)) {
												linkedCellRegionSet.add(tmpLinkedRegions.elementAt(j));
												j = tmpLinkedRegions.size();
										}
								}

						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label temporary linked neuron!");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label temporary linked neuron!");
								e.printStackTrace();
						}
				}

				/*
				 * Create image with all detected neuron regions, linked with possible
				 * neuron fragments.
				 */
				MTBImageByte linkedCellImg = null;
				try {
						linkedCellImg = linkedCellRegionSet.elementAt(0).toMTBImageByte(null,
						    width, height);
						for (int i = 1; i < linkedCellRegionSet.size(); i++) {
								linkedCellImg = (MTBImageByte) linkedCellRegionSet.elementAt(i)
								    .toMTBImage(null, linkedCellImg).convertType(MTBImageType.MTB_BYTE,
								        true);
						}
						IW = new ImageWriterMTB(linkedCellImg, result_presegmentation
						    + File.separator + file_name_noExtension + "-neuron-linked_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save binary linked neuron!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save binary linked neuron!");
						e.printStackTrace();
				}

				// label the neuron regions
				LabelComponentsSequential LCS;
				try {
						LCS = new LabelComponentsSequential(linkedCellImg, true);
						LCS.runOp(null);
						linkedCellRegionSet = LCS.getResultingRegions();
				} catch (ALDOperatorException e1) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label binary linked neuron!");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ label binary linked neuron!");
						e.printStackTrace();
				}

				// get detected neuron regions
				MTBRegion2DSet initialCellRegionSet = new MTBRegion2DSet(0, 0, width - 1,
				    height - 1);
				if (linkedCellRegionSet.size() > 0) {
						for (int i = 0; i < linkedCellRegionSet.size(); i++) {
								MTBRegion2D selCell = linkedCellRegionSet.elementAt(i);
								int coreCount = 0;
								for (int j = 0; j < coreRegionSet.size(); j++) {
										MTBRegion2D selCore = coreRegionSet.elementAt(j);
										if (selCore.getArea() > nucleusSize) {
												if (testRegions(selCell, selCore)) {
														coreCount++;
												}
										}
								}
								if (coreCount == 1) {
										initialCellRegionSet.add(selCell);
								}
						}
				} else {
						throw new NeuriteDetector2DException(
						    ">>>>>>> NeuriteDetector2DAlgos: presegmentation failed @ empty set of linked cell regions!");
				}

				// save image with detected and linked neuron regions
				MTBImageByte initialCellRegionImg = null;
				try {
						initialCellRegionImg = initialCellRegionSet.elementAt(0).toMTBImageByte(
						    null, width, height);
						for (int i = 1; i < initialCellRegionSet.size(); i++) {
								initialCellRegionImg = (MTBImageByte) initialCellRegionSet.elementAt(i)
								    .toMTBImage(null, initialCellRegionImg).convertType(
								        MTBImageType.MTB_BYTE, true);
						}
						IW = new ImageWriterMTB(initialCellRegionImg, result_presegmentation
						    + File.separator + file_name_noExtension + "-neuron-initial_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e2) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save binary selected and linked neuron!");
						e2.printStackTrace();
				} catch (ALDProcessingDAGException e2) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save binary selected and linked neuron!");

						e2.printStackTrace();
				}

				/*
				 * Get contours from the detected regions. This contours are used as initial
				 * contours for the snakes.
				 */
				MTBContour2DSet intialContourSet = null;
				MTBImageByte initialContourImg = null;
				try {
						ContourOnLabeledComponents clc = new ContourOnLabeledComponents(
						    initialCellRegionImg, initialCellRegionSet,
						    ContourType.OUT_IN_CONTOUR, 200);
						clc.runOp(null);
						intialContourSet = clc.getResultContours();
						initialContourImg = clc.getResultImage();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ get intial contour!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ get intial contour!");
						e.printStackTrace();
				}

				// save the initial contour for the active contour model
				try {
						IW = new ImageWriterMTB(initialContourImg, result_snakes + File.separator
						    + file_name_noExtension + "-contour-initial_bin.tif");
						IW.setVerbose(false);
						IW.setOverwrite(false);
						IW.runOp(null);
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save intial contour!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos presegmentation failed @ save intial contour!");
						e.printStackTrace();
				}
				// return the initial contours
				return intialContourSet;
		}

		/**
		 * Method to get each 7th point of the initial contour as snake control point.
		 * 
		 * @param initContour
		 *          initial contour to extract snake control points
		 * @return List of control points of the snake.
		 */
		private Vector<MTBSnakePoint2D> getSnakeControlPoints(MTBContour2D initContour) {
				Vector<MTBSnakePoint2D> controlPoints = new Vector<MTBSnakePoint2D>();
				// TODO fixed parameter
				for (int i = 0; i < initContour.getPointNum(); i += 7) {
						controlPoints.addElement(new MTBSnakePoint2D(initContour.getPointAt(i)));
				}
				return controlPoints;
		}

		/**
		 * Get the region which is included in the snake contour.
		 * 
		 * @param snake
		 *          current snake
		 * @return 2D region with all pixels included in the snake, including the
		 *         snake points.
		 */
		private MTBRegion2DSet getSnakeRegion(MTBPolygon2DSet theSnakes) {
				MTBImageByte imageToDraw = (MTBImageByte) MTBImage.createMTBImage(width,
				    height, 1, 1, 1, MTBImageType.MTB_BYTE);
				imageToDraw.fillBlack();

				// handle regions from inner contours
				for (int i = 0; i < theSnakes.size(); i++) {
						int[][] tmpMask = theSnakes.elementAt(i).getBinaryMask(width, height);

						// remove pixels from inner snake region polygons
						for (int y = 0; y < height; ++y) {
								for (int x = 0; x < width; ++x) {
										if (tmpMask[y][x] == 1 && imageToDraw.getValueInt(x, y) == 255) {
												imageToDraw.putValueDouble(x, y, 0);
										} else if (tmpMask[y][x] == 1 && imageToDraw.getValueInt(x, y) == 0) {

												imageToDraw.putValueDouble(x, y, 255);
										}
								}
						}
				}

				// draw the pixels of the snake polygon itself
				ImgDilate dil = null;
				ImgErode ero = null;
				try {

						for (int i = 0; i < theSnakes.size(); i++) {
								imageToDraw = (MTBImageByte) ((MTBSnake) theSnakes.elementAt(i))
								    .toMTBImage(null, imageToDraw);
						}
						// morphological closing to improve binary snake region
						dil = new ImgDilate(imageToDraw, 3);
						dil.runOp(null);
						ero = new ImgErode(dil.getResultImage(), 3);
						ero.runOp(null);
				} catch (ALDOperatorException e1) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ closing on snake region!");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e1) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ closing on snake region!");
						e1.printStackTrace();
				}

				// get snake region as MTBRegion2D object
				MTBRegion2DSet regionSet = null;
				try {
						LabelComponentsSequential LS = new LabelComponentsSequential(ero
						    .getResultImage(), true);
						LS.runOp(null);
						regionSet = LS.getResultingRegions();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ get snake region!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ get snake region!");
						e.printStackTrace();
				}

				// return the resulting snake region
				return (regionSet);
		}

		/**
		 * Calculate the initial image for the external energy of the snake. This
		 * image is used to generate the final external energy in the snake
		 * optimization method below.
		 */
		private MTBImage getExtEnergyInit() {

				MTBImage energyInit = null;

				switch (energyLabel) {
				// case GVF: // use the Gradient Vector Flow field as external esnake energy
				// try {
				// MTBImage ImageToGVF = MTBImage.createMTBImage(width, height,
				// 1, 1, 1, MTBImageType.MTB_DOUBLE);
				// // calculate gradient image
				//
				// GradientFieldCalculator2D IG = new GradientFieldCalculator2D(img,
				// GradientMode.PARTIAL_DIFF);
				// IG.runOp(null);
				// MTBVectorField2D gradField = IG.getVectorField();
				// // normalize gradient image values to range [0,1]
				// FieldOperations2D fieldOp = new FieldOperations2D(gradField,
				// FieldOperation.NORMEDMAG_IMAGE);
				// fieldOp.runOp(null);
				// ImageToGVF = fieldOp.getResultImage();
				// extEnergy = new MTBSnakeEnergyCDIB_GVF2D(ImageToGVF, numIterations);
				// // extEnergy.saveExtEnergy(result_gvf + File.separator
				// // + file_name_noExtension + "-gvf-" + numIterations + ".m");
				// // save the gvf field as RGB image and the values in a binary file
				// MTBVectorField2D gvfField = ((MTBSnakeEnergyCDIB_GVF2D) extEnergy)
				// .getGVF();
				// fieldOp = new FieldOperations2D(gvfField, FieldOperation.COLOR_PLOT);
				// fieldOp.runOp(null);
				// MTBImageRGB gvfImage = (MTBImageRGB) fieldOp.getResultImage();
				// gvfImage.setTitle("GVF field - " + file_name_noExtension + " | "
				// + numIterations + " iterations");
				// // save RGB image of gvf field
				// ImageWriterMTB IW;
				// IW = new ImageWriterMTB(gvfImage, result_extEnergy + File.separator
				// + file_name_noExtension + "-gvf-" + numIterations + ".tif");
				// IW.setVerbose(false);
				// IW.setOverwrite(false);
				// IW.runOp(null);
				// // save gvf field values as binary data file
				// if (gvfField != null) {
				// gvfField.saveToBinFile(result_extEnergy + File.separator
				// + file_name_noExtension + "-FieldData-" + numIterations + ".bin");
				// }
				// } catch (ALDOperatorException e) {
				// System.out.println(">>>>>>> detection failed @ GVF calculation");
				// e.printStackTrace();
				// } catch (ALDProcessingDAGException e) {
				// System.out.println(">>>>>>> detection failed @ GVF calculation");
				// e.printStackTrace();
				// }
				// break;
				case DISTANCE_MAP: // use distance map as external snake energy
						/*
						 * Calculate the gradient of the neuron image. Binaries the image and
						 * calculate the distance map on the image. Use this distance map as
						 * external energy for the snake.
						 */
						// the 2D gradient field of the neuron image
						GradientFieldCalculator2D gradField;
						// the 2D vector field operation operator
						FieldOperations2D fOp;
						// otsu threshold calculation operator
						CalcGlobalThreshOtsu otsuThresh;
						// the binarized gradient magnitude image of the gradient field
						ImgThresh imgThresh;
						// median filter
						RankOperator median;
						try {
								// use median filter with radius = 2 --> 5x5 mask size for the neuron
								// image
								median = new RankOperator(neuronMIP, RankOpMode.MEDIAN, 2, 2, 0, 0, 0);
								median.runOp(null);
								// calculate the 2D gradient field of the image
								gradField = new GradientFieldCalculator2D(median.getResultImg(),
								    GradientMode.PARTIAL_DIFF);
								gradField.runOp(null);
								// calculate the gradient magnitude image of the gradient image
								fOp = new FieldOperations2D(gradField.getVectorField(),
								    FieldOperation.MAGNITUDE_IMAGE);
								fOp.runOp(null);
								// convert to MTBImageShort for histogram use
								MTBImageShort gradMagImg = (MTBImageShort) fOp.getResultImage()
								    .convertType(MTBImageType.MTB_SHORT, false);
								// use median filter with radius = 2 --> 5x5 mask size for the
								// gradient magnitude image
								median = new RankOperator(gradMagImg, RankOpMode.MEDIAN, 2, 2, 0, 0, 0);
								median.runOp(null);
								// calculate the otsu threshold on the magnitude image
								otsuThresh = new CalcGlobalThreshOtsu(median.getResultImg());
								otsuThresh.runOp(null);
								// use the otsu threshold to binarize the gradient magnitude image
								imgThresh = new ImgThresh(gradMagImg, otsuThresh.getOtsuThreshold()
								    .getValue());
								imgThresh.runOp(null);
								// convert to MTBImageByte
								MTBImageByte binGradImg = (MTBImageByte) imgThresh.getResultImage()
								    .convertType(MTBImageType.MTB_BYTE, false);
								// do closing on binary image to improve the contour
								ImgDilate dil = new ImgDilate(binGradImg, 5);
								dil.runOp(null);
								ImgErode ero = new ImgErode(dil.getResultImage(), 5);
								ero.runOp(null);
								energyInit = ero.getResultImage();
						} catch (ALDOperatorException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ external energy initial image!");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ external energy initial image!");
								e.printStackTrace();
						}
						break;
				}
				return energyInit;
		}

		/**
		 * Optimization of the snakes (main snake and inner snakes) with its specified
		 * parameters and energies.
		 * 
		 * @param initSnakes
		 *          initial snakes for optimization start
		 * 
		 * @return optimized set of snakes
		 */
		private MTBPolygon2DSet optimizeSnakes(MTBPolygon2DSet initSnakeSet) {

				// initialize external snake energy
				MTBSnakeEnergyCDImageBased externalEnergy = null;

				// generate different external snake energies
				switch (energyLabel) {
				case DISTANCE_MAP:
					// calculate the distance map as external snake energy
					MTBImageByte distInit = (MTBImageByte) getExtEnergyInit().convertType(
						MTBImageType.MTB_BYTE, true);
					externalEnergy = new MTBSnakeEnergyCDIB_Distance(distInit,
						DistanceMetric.EUCLIDEAN, ForegroundColor.FG_WHITE);
					break;
				}

				// generate new empty set of resulting snakes
				MTBPolygon2DSet snakeResultSet = new MTBPolygon2DSet(0, 0, width - 1,
				    height - 1);

				/*
				 * Start snake optimization.
				 */
				try {
						/*
						 * Generate snake energy
						 */
						MTBSet_SnakeEnergyDerivable energies = new MTBSet_SnakeEnergyDerivable();

						/*
						 * Add external energy to snake energy.
						 */
						energies.addEnergy(externalEnergy);

						/*
						 * Generate internal snake energy, including Kass length and curvature
						 * term.
						 */
						MTBSnakeEnergyCD_KassLength lengthEnergy = new MTBSnakeEnergyCD_KassLength(
						    alpha, new MTBSnakeEnergyCD_KassLength_ParamAdaptNone());
						MTBSnakeEnergyCD_KassCurvature curvEnergy = new MTBSnakeEnergyCD_KassCurvature(
						    beta, new MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone());

						/*
						 * Add internal energy to snake energy.
						 */
						energies.addEnergy(lengthEnergy);
						energies.addEnergy(curvEnergy);

						/*
						 * Generate snake optimizer.
						 */
						SnakeOptimizerSingleVarCalc SO = new SnakeOptimizerSingleVarCalc(
						    neuronMIP, initSnakeSet, energies, new MTBGammaPtWiseExtEner(),
						    stepSize, new MTBTermMotionDiff(motionFraction, maxIterations),
						    new Boolean(true), new Double((double) resampleConstant));
						// optimize each snake with the given parameters, energies and settings
						SnakeOptimizerCoupled SOC = new SnakeOptimizerCoupled(neuronMIP,
						    initSnakeSet, SO, null);
						SOC.setVerbose(false);
						// SOC.enableSaveIntermediateResults();
						// SOC.enableShowIntermediateResults();
						// SOC.setIntermediateResultPath(result_snakes);
						SOC.runOp(null);
						// get snake optimizer iteration count for each snake
						this.snakeIterCount = SOC.getIterationsPerSnake();
						// get final snake set
						snakeResultSet = SOC.getResultSnakes();
				} catch (ALDOperatorException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ snake optimization!");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out
						    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ snake optimization!");
						e.printStackTrace();
				}

				/*
				 * Save external energy which is currently used for the whole snake
				 * optimization.
				 */
				switch (energyLabel) {
				case DISTANCE_MAP:
						/*
						 * Save distance map. Saving is done here, because snake optimizer calls
						 * initial method of external energy, which is necessary for distance map
						 * calculation.
						 */
						MTBImage distMapImg = MTBImage.createMTBImage(width, height, 1, 1, 1,
						    MTBImageType.MTB_DOUBLE);
						for (int y = 0; y < height; y++) {
								for (int x = 0; x < width; x++) {
										distMapImg.putValueDouble(x, y, externalEnergy.getValue(x, y));
								}
						}
						ImageWriterMTB IW;
						try {
								IW = new ImageWriterMTB(distMapImg, result_energy + File.separator
								    + file_name_noExtension + "-distMap.tif");
								IW.setVerbose(false);
								IW.setOverwrite(false);
								IW.runOp(null);
						} catch (ALDOperatorException e1) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save distance map!");
								e1.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out
								    .println(">>>>>>> NeuriteDetector2DAlgos detection failed @ save distance map!");
								e.printStackTrace();
						}
						break;
				}

				// return final set of optimizes snakes
				return snakeResultSet;
		}

		/**
		 * Test whether the number of pixels from a neuron region contains more than
		 * 90% of the pixels from a nucleus region. The nucleus region must be smaller
		 * than the neuron region! If yes, the neuron region contains the nucleus
		 * region and is a potential living cell to detect a new neuron region.
		 * 
		 * @param cell
		 *          region of the neuron cell
		 * @param core
		 *          region of the nucleus
		 * @return True if core region is included with at least 90% of its pixel in
		 *         the neuron region.
		 */
		private boolean testRegions(MTBRegion2D cell, MTBRegion2D core) {
				double count = 0.0;
				Vector<Point2D.Double> cell_pt = cell.getPoints();
				Vector<Point2D.Double> core_pt = core.getPoints();
				if (core.getArea() < cell.getArea()) {
						for (int i = 0; i < core.getArea(); i++) {
								if (cell_pt.contains(core_pt.elementAt(i)))
										count = count + 1.0;
						}
						double erg = count / core.getArea();
						// TODO parameter, handle if no DAPI exists
						if (erg > nucleusRatio)
								return true;
						else
								return false;
				} else {
						return false;
				}
		}

		/**
		 * Test whether a single pixel from a neuron region contains a pixel from a
		 * nucleus region. If yes, the neuron region contains parts of the nucleus
		 * region.
		 * 
		 * @param cell
		 *          region of the neuron cell
		 * @param core
		 *          region of the nucleus
		 * @return Is core included in neuron region false or true.
		 */
		private boolean testRegions2(MTBRegion2D cell, MTBRegion2D core) {
				double count = 0.0;
				Vector<Point2D.Double> cell_pt = cell.getPoints();
				Vector<Point2D.Double> core_pt = core.getPoints();
				for (int i = 0; i < core.getArea(); i++) {
						if (cell_pt.contains(core_pt.elementAt(i)))
								count = count + 1.0;
				}
				// TODO fixed parameter, handle if no DAPI exists
				if (count >= 1)
						return true;
				else
						return false;
		}
}
