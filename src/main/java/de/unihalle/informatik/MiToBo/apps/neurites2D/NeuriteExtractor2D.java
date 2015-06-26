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

import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Vector;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphEdge;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBNeuriteSkelGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBNeuriteSkelGraphNode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2D;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DSet;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.ImgDilate;
import de.unihalle.informatik.MiToBo.morphology.ImgErode;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.tools.image.ImageValueTools;

/**
 * The Neurite Extractor 2D application offers advanced functionality to extract
 * neurite regions in 2D binary neuron images of neurons. The neurites are
 * separated from the soma. In addition, the borderlines between the soma and
 * the neurite shafts, as well as between the neurite shaft and their growth
 * cone is extracted.
 * 
 * The exact localization of neurites without the soma (cell body) region is
 * calculated via a wavelet based detection approach from the R script
 * "MTBNeuriteFeatureDetection.R". Neurites are returned as {@link MTBNeurite2D}
 * objects.
 * 
 * Note: Don't know why operator cannot run twice without restart of MiToBo.
 * Maybe a threading problem of the R engine.
 * 
 * @see MTBNeuriteSkelGraph
 * @see MTBNeurite2DSet
 * 
 * @author Danny Misiak
 * 
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.NONE, level = Level.STANDARD, allowBatchMode = false)
public class NeuriteExtractor2D extends MTBOperator {

		/**
		 * Color of binary neuron image foreground.
		 * 
		 * @author Danny Misiak
		 */
		public static enum NeuronColor {
				BLACK, WHITE
		}

		/**
		 * Define extractor parameters.
		 */

		// --- input parameters ---

		@Parameter(label = "Binary Neuron Image", required = true, direction = Parameter.Direction.IN, description = "Binary neuron input image.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImageByte neuronImage = null;

		@Parameter(label = "Output Directory", required = true, direction = Parameter.Direction.IN, description = "Output directory for neurite extraction.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private ALDDirectoryString outputDir = null;

		@Parameter(label = "Neurite Mask Size", required = true, direction = Parameter.Direction.IN, description = "Neurite mask size in pixel.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private int neuriteMaskSize = 21;

		@Parameter(label = "Binary Neuron Color", required = true, direction = Parameter.Direction.IN, description = "Binary color of the neuron.", mode = ExpertMode.STANDARD, dataIOOrder = 3)
		private NeuronColor neuronColor = NeuronColor.WHITE;

		@Parameter(label = "Maximum Spine Length", required = true, direction = Parameter.Direction.IN, description = "Maximum length of a spine in pixel.", mode = ExpertMode.STANDARD, dataIOOrder = 4)
		private int maxSpineLength = 53;

		// --- supplemental parameters ---

		// --- output parameters ---

		@Parameter(label = "Extracted Neurites", required = true, direction = Parameter.Direction.OUT, description = "Set of extracted neurites.")
		private transient MTBNeurite2DSet extractedNeurites = null;

		/**
		 * Width of the binary neuron image.
		 */
		private transient int width;
		/**
		 * Height of the binary neuron image.
		 */
		private transient int height;
		/**
		 * R engine to call R scripts.
		 */
		private transient Rengine rEngine;
		/**
		 * 2D region of the coarse cell body region (soma).
		 */
		private transient MTBRegion2D neuronCoarseSoma;
		/**
		 * NSG of the whole neuron.
		 */
		private transient MTBNeuriteSkelGraph neuronSkelGraph;
		/**
		 * Binary skeleton image of the whole neuron.
		 */
		private transient MTBImageByte skelImage;
		/**
		 * Vector of neurite graphs, one for each localized neurite.
		 */
		private transient Vector<MTBNeuriteSkelGraph> neuriteGraphs;
		/**
		 * Vector of detected feature points for each neurite. Each extracted neurite
		 * has at least 2 features. One for the borderline between soma and neurite
		 * shaft and o0ne for the borderline between neurite shaft and growth cone.
		 */
		private transient Vector<Vector<Vector<Point2D.Double>>> featurePoints;

		/**
		 * Array of detected lengths of the neurite shafts, without growth cone
		 * regions.
		 */
		private transient int[] neuriteShaftLengths;

		/**
		 * Vector of neurite widths, containing 3 elements. 1. average width of whole
		 * neurite, 2. average width of neurite shaft, and 3. average width of growth
		 * cone.
		 */
		private transient Vector<Vector<Double>> avgNeuriteWidths;

		/**
		 * Standard constructor.
		 * 
		 * @throws ALDOperatorException
		 */
		public NeuriteExtractor2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new neurite extraction object.
		 * 
		 * @param _neuronImage
		 *          binary neuron image
		 * @param _neuronColor
		 *          binary neuron color, e.g. 0 (black) or 255 (white)
		 * @param _neuriteMaskSize
		 *          mask size of average neurite width, in pixel
		 * @param _maxSpineLength
		 *          maximum length of a branch to define it as spine, in pixel
		 * @param _rEngine
		 *          engine to call R scripts
		 * @param _outputFile
		 *          file path for feature output
		 * @throws ALDOperatorException
		 */
		public NeuriteExtractor2D(MTBImageByte _neuronImage,
		    NeuronColor _neuronColor, int _neuriteMaskSize, int _maxSpineLength,
		    Rengine _rEngine, String _outputDir) throws ALDOperatorException {
				this.neuronImage = (MTBImageByte) _neuronImage.duplicate();
				this.neuronColor = _neuronColor;
				this.neuriteMaskSize = _neuriteMaskSize;
				this.maxSpineLength = _maxSpineLength;
				this.rEngine = _rEngine;
				this.outputDir = new ALDDirectoryString(_outputDir);
		}

		/**
		 * Custom validation of some input parameters.
		 */
		@Override
		public void validateCustom() throws ALDOperatorException {

				if (maxSpineLength < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteExtractor2D: validation failed!"
						        + "\nMaximum Spine Length must be a positive value.");
				}
				if (neuriteMaskSize < 0) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteExtractor2D: validation failed!"
						        + "\nNeurite Mask Size must be a positive value.");
				}
		}

		/**
		 * Get input image of the binary neuron. Maybe the binary colors have changed.
		 * The default is black for background and white for foreground pixels.
		 */
		public MTBImageByte getNeuronImage() {
				return neuronImage;
		}

		/**
		 * Set input image of the binary neuron.
		 */
		public void setNeuronImage(MTBImageByte _neuronImage) {
				neuronImage = _neuronImage;
		}

		/**
		 * Get binary neuron color. WHITE = 255 and BLACK = 0.
		 */
		public NeuronColor getNeuronColor() {
				return neuronColor;
		}

		/**
		 * Set binary neuron color. WHITE = 255 and BLACK = 0.
		 */
		public void setNeuronColor(NeuronColor _neuronColor) {
				neuronColor = _neuronColor;
		}

		/**
		 * Get neurite mask size in pixel. This value defines the maximum average
		 * width of a neurite.
		 */
		public int getNeuriteMaskSize() {
				return neuriteMaskSize;
		}

		/**
		 * Set neurite mask size in pixel. This value defines the maximum average
		 * width of a neurite.
		 */
		public void setNuriteMaskSize(int _neuriteMaskSize) {
				neuriteMaskSize = _neuriteMaskSize;
		}

		/**
		 * Get R engine.
		 */
		public Rengine getREngine() {
				return rEngine;
		}

		/**
		 * Set R engine.
		 */
		public void setREngine(Rengine _rEngine) {
				rEngine = _rEngine;
		}

		/**
		 * Get maximum length of a spine (filopodia-like protrusion) in pixel.
		 */
		public int getMaxSpineLength() {
				return maxSpineLength;
		}

		/**
		 * Set maximum length of a spine (filopodia-like protrusion) in pixel.
		 */
		public void setMaxSpineLength(int _maxSpineLength) {
				maxSpineLength = _maxSpineLength;
		}

		/**
		 * Get extracted neurites as result of the NeuriteExtraction2D operator.
		 * 
		 * @return Set of {@link MTBNeurite2D} objects.
		 */
		public MTBNeurite2DSet getExtractedNeurites() {
				return extractedNeurites;
		}

		/**
		 * Initialization method.
		 */
		private void init() {
				if (rEngine == null) {
						rEngine = createRengine();
				}
				width = neuronImage.getSizeX();
				height = neuronImage.getSizeY();
				neuriteGraphs = new Vector<MTBNeuriteSkelGraph>();
				extractedNeurites = new MTBNeurite2DSet();
				featurePoints = new Vector<Vector<Vector<Point2D.Double>>>();
				neuriteShaftLengths = new int[0];
				avgNeuriteWidths = new Vector<Vector<Double>>();
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

		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {
				// start initialization
				init();
				// get coarse soma region
				neuronCoarseSoma = generateCoarseSoma();
				// get skeleton graph of whole neuron
				neuronSkelGraph = generateSkelGraph();
				// save skeleton graph of the whole neuron in SWC file format
				neuronSkelGraph.toSWC(outputDir.getDirectoryName() + File.separator
				    + neuronImage.getTitle() + "-neuron.swc", neuronImage);

				if (neuronCoarseSoma == null) {
						try {
								throw new NeuriteExtractor2DException(
								    ">>>>>>> NeuriteExtraction2D: WARNING - No coarse soma detected! Neuron region is skipped!");
						} catch (NeuriteExtractor2DException e) {
								// nothing to do here
						}
				} else if (neuronSkelGraph == null) {
						try {
								throw new NeuriteExtractor2DException(
								    ">>>>>>> NeuriteExtraction2D: WARNING - No neuron skeleton graph detected! Neuron region is skipped!");
						} catch (NeuriteExtractor2DException e) {
								// nothing to do here
						}
				} else {
						// get start points of skeleton for each possible neurite
						Vector<Point2D.Double> startPoints = new Vector<Point2D.Double>();
						try {
								startPoints = getStartPoints();
						} catch (NeuriteExtractor2DException e) {
								System.out.println(">>>>>>> extraction failed @ get start points");
								e.printStackTrace();
						}

						// get neurite features (borderlines between structural neuron components)
						Vector<Vector<Vector<Line2D.Double>>> features = getNeuriteFeatures(startPoints);
						if (features == null) {
								try {
										throw new NeuriteExtractor2DException(
										    ">>>>>>> NeuriteExtraction2D: WARNING - No features detected! Neuron region is skipped!");
								} catch (NeuriteExtractor2DException e) {
										// nothing to do here
								}
						} else {

								/*
								 * Add neurite region, graphs and features to resulting neurites vector,
								 * get whole neurite region from soma to growth cone. Resulting regions
								 * are sorted like the features.
								 */
								MTBRegion2DSet neuriteRegions = generateNeuriteRegions(features,
								    featurePoints);

								/*
								 * Assign each regions, features and other values to to the
								 * corresponding NSG.
								 */
								MTBNeurite2DSet tmpNeuriteSet = new MTBNeurite2DSet();
								for (int i = 0; i < neuriteGraphs.size(); i++) {
										MTBNeuriteSkelGraph tmpNSG = neuriteGraphs.elementAt(i);
										// create single neurite shaft length and widths for current neurite
										int[] singleNSL = new int[1];
										singleNSL[0] = neuriteShaftLengths[i];
										Vector<Vector<Double>> tmpWidths = new Vector<Vector<Double>>();
										tmpWidths.add(avgNeuriteWidths.elementAt(i));
										// create MTBNeurite2D object
										MTBNeurite2D tmpNeurite = new MTBNeurite2D(tmpNSG, featurePoints
										    .elementAt(i), features.elementAt(i),
										    neuriteRegions.elementAt(i), maxSpineLength, singleNSL, tmpWidths);
										// add neurite to temporary vector of neurites
										tmpNeuriteSet.add(tmpNeurite);
								}
								/*
								 * Clean up the extracted neurites, due to feature shifts or multiple
								 * features along a NSG path.
								 */
								cleanNeurites(tmpNeuriteSet);
								// TODO recalculate regions / handle 2 cut branches (e.g. d00053)

								// set final extracted neurite regions
								extractedNeurites = tmpNeuriteSet;

								// save extracted neurite skeleton graph of each neurite in SWC file
								// format
								MTBImage tmpNeuritesImg = MTBImage.createMTBImage(width, height, 1, 1,
								    1, MTBImageType.MTB_BYTE);
								tmpNeuritesImg.fillBlack();
								for (int i = 0; i < extractedNeurites.size(); i++) {
										// get current neurite
										MTBNeurite2D tmpNeurite = extractedNeurites.getElementAt(i);
										// get NSG for current neurite
										MTBNeuriteSkelGraph tmpNSG = tmpNeurite.getNeuriteGraph();
										// get region of current neurite
										MTBRegion2D tmpRegion = tmpNeurite.getNeuriteRegion();

										MTBImageByte tmpRegionImg = tmpRegion.toMTBImageByte(outputDir
										    .getDirectoryName()
										    + File.separator
										    + neuronImage.getTitle()
										    + "-neurite_"
										    + (i + 1)
										    + ".tif", width, height);
										tmpNeuritesImg = tmpRegion.toMTBImage(null, tmpNeuritesImg);

										// save NSG and the data to a SWC file
										tmpNSG.toSWC(outputDir.getDirectoryName() + File.separator
										    + neuronImage.getTitle() + "-neurite_" + (i + 1) + ".swc",
										    tmpRegionImg);
								}
								ImageWriterMTB IW;
								try {
										IW = new ImageWriterMTB(tmpNeuritesImg, outputDir.getDirectoryName()
										    + File.separator + neuronImage.getTitle() + "-neurites.tif");
										IW.setVerbose(false);
										IW.setOverwrite(false);
										IW.runOp(null);
								} catch (ALDOperatorException e1) {
										e1.printStackTrace();
								} catch (ALDProcessingDAGException e) {
										e.printStackTrace();
								}
						}
				}
		}

		/**
		 * Method to save the neurites width profile in a file.
		 * 
		 * @param neuriteWidths
		 *          width values of the neurite
		 * @param file
		 *          file name for storing the data
		 * @return Saved file true or false.
		 */
		private static boolean saveNeuriteWidthList(
		    Vector<java.lang.Double[]> neuriteWidths, String file) {
				// get length of longest profile
				int maxLength = 0;
				for (int i = 0; i < neuriteWidths.size(); ++i) {
						if (neuriteWidths.elementAt(i).length > maxLength)
								maxLength = neuriteWidths.elementAt(i).length;
				}
				try {
						// open the output stream
						PrintStream pStream = new PrintStream(file);
						// iterate over all profile entries
						for (int j = 0; j < maxLength; ++j) {
								for (int k = 0; k < neuriteWidths.size(); ++k) {
										// get current profile
										java.lang.Double[] tmpWidthData = neuriteWidths.elementAt(k);
										// if we have no profile or no data, do nothing
										if (tmpWidthData == null || tmpWidthData.length <= j) {
												pStream.print("\t");
										} else {
												// write the data
												pStream.print(tmpWidthData[j]);
												if (k != neuriteWidths.size() - 1)
														pStream.print("\t");
										}
								}
								pStream.println();
						}
						pStream.close();
				} catch (FileNotFoundException e) {
						System.err
						    .println("Error: Could not open file " + file + " for writing!");
						return false;
				}
				return true;
		}

		/**
		 * Call a specified R script for neurite calculations.
		 * 
		 * @param sourceFile
		 *          neurite calculation R script file
		 * @param scriptArg
		 *          input directory path for the R script
		 * @return True if R call is done successfully.
		 */
		private boolean callNeuriteScript(String sourceFile, String scriptArg) {
				// set working directory in R
				rEngine.eval("setwd(Sys.getenv(\"R_SCRIPTS\"))");
				// Create global R variable for input directory named. Same like in R:
				// arg <- scriptArg
				rEngine.assign("arg", scriptArg);
				// Load R script file in current R engine. Same like in R:
				// source(sourceFile)
				REXP rExpression = rEngine.eval("source(\"" + sourceFile + "\")");
				// check if source file was successfully loaded and executed
				if (rExpression == null) {
						System.out.println("** JRI R-Engine: error executing source file!");
						System.out.println("** file   : \"" + sourceFile + "\"");
						return false;
				}
				System.out.println("JRI R-Engine: executing " + sourceFile + " ...done!");
				return true;
		}

		/**
		 * Compute a coarse neuron cell body region (soma). This region is later used
		 * to compute an more exact soma region of the neuron and split off the
		 * neurites from the soma area.
		 */
		private MTBRegion2D generateCoarseSoma() {
				/*
				 * 1. Opening with a great mask should split off the neuron region into a
				 * coarse cell body (soma) region and other small regions.
				 */
				MTBImageByte tmpOpening = null;
				try {
						// erosion
						ImgErode imEro = new ImgErode(neuronImage, neuriteMaskSize);
						imEro.runOp(null);
						// temporary opening image, includes all region resulting from the opening
						tmpOpening = (MTBImageByte) imEro.getResultImage();
						// dilation
						ImgDilate imDil = new ImgDilate(tmpOpening, neuriteMaskSize);
						imDil.runOp(null);
						tmpOpening = (MTBImageByte) (imDil.getResultImage().convertType(
						    MTBImageType.MTB_BYTE, true));
				} catch (ALDOperatorException e) {
						System.out.println(">>>>>>> extraction failed @ opening");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out.println(">>>>>>> extraction failed @ opening");
						e.printStackTrace();
				}
				/*
				 * 2. Soma is defined as the greatest region from opening.
				 */
				// get single regions by component labeling
				MTBRegion2DSet regions = null;
				try {
						LabelComponentsSequential LCS = new LabelComponentsSequential(tmpOpening,
						    true);
						LCS.runOp(null);
						regions = LCS.getResultingRegions();
				} catch (ALDOperatorException e) {
						System.out.println(">>>>>>> extraction failed @ LCS in opening image");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out.println(">>>>>>> extraction failed @ LCS in opening image");
						e.printStackTrace();
				}

				// find greatest region, the assumed soma region
				int index = 0;
				int maxSize = 0;
				for (int i = 0; i < regions.size(); i++) {
						if (regions.elementAt(i).getArea() > maxSize) {
								index = i;
								maxSize = regions.elementAt(i).getArea();
						}
				}
				// return the cell body region
				if (regions == null || regions.size() < 1) {
						return null;
				} else {
						return (regions.elementAt(index));
				}
		}

		/**
		 * Compute the skeleton graph for the complete neuron region. After the
		 * skeleton was calculated, the skeleton graph is computed due to one end
		 * point of the skeleton and then build up the graph, starting at this point.
		 * 
		 * @return The computed neuron skeleton graph.
		 */
		private MTBNeuriteSkelGraph generateSkelGraph() {
				/*
				 * 1. Get skeleton of complete neuron: ImageJ skeletonize() assumes black
				 * object on white ground and uses a binary processor.
				 */
				// create byte processor as basis for new binary processor
				ByteProcessor bP = new ByteProcessor(width, height);
				// draw neuron in black on a white background
				for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
								int value = neuronImage.getValueInt(x, y);
								// if neuron color is white, change it to black
								if (neuronColor == NeuronColor.WHITE) {
										if (value == 255) {
												bP.putPixel(x, y, 0);
										} else {
												bP.putPixel(x, y, 255);
										}
								} else { // neuron color is black, so just copy the values
										if (value == 0) {
												bP.putPixel(x, y, 0);
										} else {
												bP.putPixel(x, y, 255);
										}
								}
						}
				}
				// generate new binary processor
				BinaryProcessor bbP = new BinaryProcessor(bP);
				// create the skeleton of the neuron using the ImageJ skeletonize()
				bbP.skeletonize();
				// create skeleton image of type MTBImageByte
				skelImage = (MTBImageByte) MTBImage.createMTBImage(width, height, 1, 1, 1,
				    MTBImageType.MTB_BYTE);
				// fill skeleton image with white
				try {
						ImageValueTools.fillImage(skelImage, 255.0, this);
				} catch (ALDOperatorException e1) {
						System.out.println(">>>>>>> extraction failed @ fill skeleton image");
						e1.printStackTrace();
				} catch (ALDProcessingDAGException e1) {
						System.out.println(">>>>>>> extraction failed @ fill skeleton image");
						e1.printStackTrace();
				}
				// transfer neuron skeleton from binary processor into the skeleton image
				for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
								if (bbP.getPixel(x, y) == 0) {
										skelImage.putValueInt(x, y, 0);
								}
						}
				}
				/*
				 * 2. Compute the skeleton graph from the calculated skeleton.
				 */
				// search for skeleton image for one end point
				Point2D.Double endPoint = new Point2D.Double();
				boolean found = false;
				for (int y = 0; !found && y < height; ++y) {
						for (int x = 0; !found && x < width; ++x) {
								if (skelImage.getValueInt(x, y) == 0) {
										int nCount = 0;
										for (int dx = -1; dx <= 1; dx++) {
												for (int dy = -1; dy <= 1; ++dy) {
														if (dx == 0 && dy == 0)
																continue;
														if (((x + dx) >= width) || ((x + dx) < 0))
																continue;
														if (((y + dy) >= height) || ((y + dy) < 0))
																continue;
														if (skelImage.getValueInt(x + dx, y + dy) == 0)
																nCount++;
												}
										}
										// found an neurite skeleton end point
										if (nCount == 1) {
												endPoint = new Point2D.Double(x, y);
												found = true;
										}
								}
						}
				}

				// // temporary debug to save the skeleton and the neuron region
				//
				// ImageWriterMTB IW;
				// try {
				// IW = new ImageWriterMTB(skelImage, outputDir.getDirectoryName()
				// + File.separator + neuronImage.getTitle() + "-skeleton.tif");
				// IW.setVerbose(false);
				// IW.setOverwrite(false);
				// IW.runOp(null);
				//
				// IW = new ImageWriterMTB(neuronImage, outputDir.getDirectoryName()
				// + File.separator + neuronImage.getTitle() + "-neuron.tif");
				// IW.setVerbose(false);
				// IW.setOverwrite(false);
				// IW.runOp(null);
				//
				// } catch (ALDOperatorException e1) {
				// e1.printStackTrace();
				// } catch (ALDProcessingDAGException e) {
				// e.printStackTrace();
				// }

				/*
				 * Build skeleton graphs for only one end point. Visited end points during
				 * graph creation will be removed and so the graph is not rebuild for this
				 * visited points.
				 */
				MTBNeuriteSkelGraph skelGraph = new MTBNeuriteSkelGraph(maxSpineLength);
				skelGraph.buildSkeletonGraph((int) Math.round(endPoint.x), (int) Math
				    .round(endPoint.y), width, height, skelImage, 0);
				return skelGraph;
		}

		/**
		 * Get the skeleton start points of every single neurite from the inside of
		 * the coarse cell body region. Every start point will be used to build up an
		 * MTBNeuriteSkelGraph for every neurite of the neuron.
		 * 
		 * @return Vector of starting points for every neurite region.
		 * @throws NeuriteExtractor2DException
		 */
		@SuppressWarnings("unchecked")
		private Vector<Point2D.Double> getStartPoints()
		    throws NeuriteExtractor2DException {
				/*
				 * 1. Detect contour of the coarse soma region.
				 */
				Vector<Point2D.Double> somaContourPoints = null;
				try {
						somaContourPoints = neuronCoarseSoma.getContour().getPoints();
				} catch (ALDOperatorException e) {
						System.out.println(">>>>>>> extraction failed @ get soma contour");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out.println(">>>>>>> extraction failed @ get soma contour");
						e.printStackTrace();
				}
				/*
				 * 2. Detect intersection points between soma contour and neuron skeleton.
				 */
				Vector<Point2D.Double> bodyIntersecPoints = new Vector<Point2D.Double>();
				for (int i = 0; i < somaContourPoints.size(); i++) {
						Point2D.Double bcPoint = somaContourPoints.elementAt(i);
						int value = skelImage.getValueInt((int) bcPoint.x, (int) bcPoint.y);
						if (value == 0) {
								bodyIntersecPoints.addElement(bcPoint);
						}
				}

				/*
				 * 3. Detect edges which intersects the contour and edges which are
				 * localized completely inside the soma.
				 */
				// vector of all points of the coarse soma region
				Vector<Point2D.Double> bodyPoints = neuronCoarseSoma.getPoints();
				// vector of all edges completely inside the coarse soma region
				Vector<MTBGraphEdge> insideEdges = new Vector<MTBGraphEdge>();
				// vector of all edges intersecting the contour of the soma twice
				Vector<MTBGraphEdge> intersecEdges = new Vector<MTBGraphEdge>();
				for (int i = 0; i < neuronSkelGraph.getEdgeNum(); i++) {
						// get one edge from the graph
						MTBGraphEdge tmpEdge = neuronSkelGraph.getEdges().elementAt(i);
						// test if current edge is completely inside the soma region
						if (bodyPoints.containsAll(tmpEdge.getData())) {
								insideEdges.add(tmpEdge);
						}// test if current edge intersects the soma contour
						else {
								for (int j = 0; j < bodyIntersecPoints.size(); j++) {
										if (tmpEdge.getData().contains(bodyIntersecPoints.elementAt(j))) {
												intersecEdges.add(tmpEdge);
												break;
										}
								}
						}
				}

				/*
				 * Detect starting points of neurites from intersecting edges or from edges
				 * inside the soma.
				 */
				// vector of removed points from the whole neuron skeleton
				Vector<Point2D.Double> removePoints = new Vector<Point2D.Double>();
				// vector for starting points
				Vector<Point2D.Double> startPoints = new Vector<Point2D.Double>();

				if (insideEdges.size() == 0) {
						if (intersecEdges.size() == 1) {
								/*
								 * For intersecting edges, the start points are calculated as the right
								 * and the left point inside the soma from the half path between the
								 * intersection points.
								 */
								Vector<Point2D.Double> tmpEdgeData = (Vector<Point2D.Double>) intersecEdges
								    .elementAt(0).getData();
								Vector<Point2D.Double> tempRemoves = new Vector<Point2D.Double>();
								for (int i = 0; i < tmpEdgeData.size(); i++) {
										if (bodyPoints.contains(tmpEdgeData.elementAt(i))) {
												tempRemoves.addElement(tmpEdgeData.elementAt(i));
										}
								}
								int pos = (int) Math.round((double) tempRemoves.size() / 2.0);
								startPoints.addElement(tempRemoves.elementAt(pos - 2));
								startPoints.addElement(tempRemoves.elementAt(pos + 2));

								// update vector of removed points from skeleton
								Point2D.Double p = tempRemoves.elementAt(pos - 1);
								removePoints.addElement(p);
								skelImage.putValueInt((int) p.x, (int) p.y, 255);
								p = tempRemoves.elementAt(pos);
								removePoints.addElement(p);
								skelImage.putValueInt((int) p.x, (int) p.y, 255);
								p = tempRemoves.elementAt(pos + 1);
								removePoints.addElement(p);
								skelImage.putValueInt((int) p.x, (int) p.y, 255);
								return (startPoints);

						} else if (intersecEdges.size() > 1) {
								Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> tmpBranches = neuronSkelGraph
								    .getBranchNodes();
								int index = 0;
								int count = 0;
								for (int i = 0; i < tmpBranches.size(); i++) {
										if (bodyPoints.contains(tmpBranches.elementAt(i).getData())) {
												index = i;
												count++;
										}
								}
								if (count == 0 || count > 1) {
										throw new NeuriteExtractor2DException(
										    "Something is wrong with intersection edges.");
								} else {
										/*
										 * For the branch node: get 8-way neighbors (skeleton pixels) along
										 * the input and output edges and get temporary starting points (2.
										 * pixel along the skeleton edge).
										 */
										Vector<MTBGraphEdge> tmpIn = tmpBranches.elementAt(index)
										    .getInEdges();
										Vector<MTBGraphEdge> tmpOut = tmpBranches.elementAt(index)
										    .getOutEdges();
										// visit input edges
										for (int j = 0; j < tmpIn.size(); j++) {
												Vector<Point2D.Double> tmpPoints = (Vector<Point2D.Double>) tmpIn
												    .elementAt(j).getData();
												// add last point of the input edge to the remove list
												removePoints.addElement(tmpPoints.elementAt(tmpPoints.size() - 1));
												// add next to last point of the input edge to the
												// starting point list
												startPoints.addElement(tmpPoints.elementAt(tmpPoints.size() - 2));
										}
										// add branch point to remove list
										removePoints.addElement(tmpBranches.elementAt(index).getData());
										// visit output edges
										for (int j = 0; j < tmpOut.size(); j++) {
												Vector<Point2D.Double> tmpPoints = (Vector<Point2D.Double>) tmpOut
												    .elementAt(j).getData();

												// add first point of the output edge to the remove list
												removePoints.addElement(tmpPoints.elementAt(0));
												// add second point of the output edge to the
												// starting point list
												startPoints.addElement(tmpPoints.elementAt(1));
										}

										/*
										 * Delete skeleton points inside the skeleton image, which are listed
										 * in the remove points list.
										 */
										for (int i = 0; i < removePoints.size(); i++) {
												Point2D.Double p = removePoints.elementAt(i);
												skelImage.putValueInt((int) p.x, (int) p.y, 255);
										}

								}
								return (startPoints);
						} else {
								// no inside and no intersecting edges found
								throw new NeuriteExtractor2DException(
								    "Something is wrong with intersection and inside edges.");
						}
				} else if (insideEdges.size() > 0) {
						if (intersecEdges.size() > 0) {
								/*
								 * Detect several objects inside the coarse soma region: (a) nodes of
								 * the graph, which are connected to edges inside the soma (b) pixels in
								 * 8-way neighborhood of these nodes (will be removed from skeleton) (c)
								 * detect starting points (the 2. pixel along the skeleton edges of the
								 * nodes).
								 */

								// temporary vector for starting points
								Vector<Point2D.Double> tmpStartPoints = new Vector<Point2D.Double>();
								for (int i = 0; i < insideEdges.size(); i++) {
										MTBGraphEdge tmpEdge = insideEdges.elementAt(i);
										// add edge points to remove list
										removePoints.addAll((Vector<Point2D.Double>) tmpEdge.getData());
										// get the nodes of the current edge
										MTBNeuriteSkelGraphNode<Point2D.Double> tmpSrc = (MTBNeuriteSkelGraphNode<Point2D.Double>) tmpEdge
										    .getSrcNode();
										MTBNeuriteSkelGraphNode<Point2D.Double> tmpTgt = (MTBNeuriteSkelGraphNode<Point2D.Double>) tmpEdge
										    .getTgtNode();
										// add node points to remove list
										removePoints.addElement((Point2D.Double) tmpSrc.getData());
										removePoints.addElement((Point2D.Double) tmpTgt.getData());

										/*
										 * For the source node: get 8-way neighbors (skeleton pixels) along
										 * the input and output edges and get temporary starting points (2.
										 * pixel along the skeleton edge).
										 */
										Vector<MTBGraphEdge> tmpIn = tmpSrc.getInEdges();
										Vector<MTBGraphEdge> tmpOut = tmpSrc.getOutEdges();
										// visit input edges
										for (int j = 0; j < tmpIn.size(); j++) {
												Vector<Point2D.Double> tmpPoints = (Vector<Point2D.Double>) tmpIn
												    .elementAt(j).getData();
												if (!tmpEdge.getData().equals(tmpPoints)) {
														if (tmpPoints.size() > 1) {
																// add last point of the input edge to the remove list
																removePoints.addElement(tmpPoints
																    .elementAt(tmpPoints.size() - 1));
																// add next to last point of the input edge to the temporary
																// starting point list
																tmpStartPoints.addElement(tmpPoints
																    .elementAt(tmpPoints.size() - 2));
														}
												}
										}
										// visit output edges
										for (int j = 0; j < tmpOut.size(); j++) {
												Vector<Point2D.Double> tmpPoints = (Vector<Point2D.Double>) tmpOut
												    .elementAt(j).getData();
												if (!tmpEdge.getData().equals(tmpPoints)) {
														if (tmpPoints.size() > 1) {
																// add first point of the output edge to the remove list
																removePoints.addElement(tmpPoints.elementAt(0));
																// add second point of the output edge to the temporary
																// starting point list
																tmpStartPoints.addElement(tmpPoints.elementAt(1));
														}
												}
										}

										/*
										 * For the target node: get 8-way neighbors (skeleton pixels) along
										 * the input and output edges and get temporary starting points (2.
										 * pixel along the skeleton edge).
										 */
										tmpIn = tmpTgt.getInEdges();
										tmpOut = tmpTgt.getOutEdges();
										// visit input edges
										for (int j = 0; j < tmpIn.size(); j++) {
												Vector<Point2D.Double> tmpPoints = (Vector<Point2D.Double>) tmpIn
												    .elementAt(j).getData();
												if (!tmpEdge.getData().equals(tmpPoints)) {
														if (tmpPoints.size() > 1) {
																// add last point of the input edge to the remove list
																removePoints.addElement(tmpPoints
																    .elementAt(tmpPoints.size() - 1));
																// add next to last point of the input edge to the temporary
																// starting point list
																tmpStartPoints.addElement(tmpPoints
																    .elementAt(tmpPoints.size() - 2));
														}
												}
										}
										// visit output edges
										for (int j = 0; j < tmpOut.size(); j++) {
												Vector<Point2D.Double> tmpPoints = (Vector<Point2D.Double>) tmpOut
												    .elementAt(j).getData();
												if (!tmpEdge.getData().equals(tmpPoints)) {
														if (tmpPoints.size() > 1) {
																// add first point of the output edge to the remove list
																removePoints.addElement(tmpPoints.elementAt(0));
																// add second point of the output edge to the temporary
																// starting point list
																tmpStartPoints.addElement(tmpPoints.elementAt(1));
														}
												}
										}
								}
								/*
								 * Delete skeleton points inside the skeleton image, which are listed in
								 * the remove points list.
								 */
								for (int i = 0; i < removePoints.size(); i++) {
										Point2D.Double p = removePoints.elementAt(i);
										skelImage.putValueInt((int) p.x, (int) p.y, 255);
								}
								/*
								 * Collect only the starting points, which are connected to a edge, that
								 * is not complete inside the coarse soma.
								 */
								for (int i = 0; i < tmpStartPoints.size(); i++) {
										Point2D.Double p = tmpStartPoints.elementAt(i);
										if ((skelImage.getValueInt((int) p.x, (int) p.y) == 0)
										    && (!startPoints.contains(p))) {
												startPoints.addElement(p);
										}
								}
						}
						// return final list of starting points
						return (startPoints);
				}
				return null;
		}

		/**
		 * Method to calculate the neurite features from the neurite width profile.
		 * The neurite width profile is generated from the neurite skeleton graphs of
		 * every single detected neurite. The neurite graphs built up from the given
		 * start points.
		 * 
		 * @param startPoints
		 *          points to build up the single neurite graphs
		 * @return Vector with neurite features. First element is the soma feature
		 *         position (borderline between soma region and neurite shaft), second
		 *         is the growth cone feature position (borderline between neurite
		 *         shaft and growth cone region).
		 */
		private Vector<Vector<Vector<Line2D.Double>>> getNeuriteFeatures(
		    Vector<Point2D.Double> startPoints) {

				// temporary vector of neurite graphs
				Vector<MTBNeuriteSkelGraph> tmpNeuriteGraphs = new Vector<MTBNeuriteSkelGraph>();
				/*
				 * Build up the neurite graphs along the neurite skeletons from every single
				 * start point.
				 */
				for (int i = 0; i < startPoints.size(); i++) {
						Point2D.Double tmpPoint = startPoints.elementAt(i);
						MTBNeuriteSkelGraph tmpGraph = new MTBNeuriteSkelGraph(maxSpineLength);
						boolean build = tmpGraph.buildSkeletonGraph((int) tmpPoint.x,
						    (int) tmpPoint.y, width, height, skelImage, 0);
						if (build) {
								tmpNeuriteGraphs.addElement(tmpGraph);
						}
				}
				/*
				 * Compute a distance transformation of the whole neuron to determine the
				 * neurite widths.
				 */
				DistanceTransform dt = null;
				double[][] distanceMap = new double[0][0];
				try {
						dt = new DistanceTransform(neuronImage, DistanceMetric.EUCLIDEAN,
						    ForegroundColor.FG_BLACK);
						dt.runOp();
						distanceMap = dt.getDistanceMap();
				} catch (ALDOperatorException e) {
						System.out.println(">>>>>>> extraction failed @ distance transform");
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						System.out.println(">>>>>>> extraction failed @ distance transform");
						e.printStackTrace();
				}
				// vector for neurite widths along every single neurite
				Vector<java.lang.Double[]> neuriteWidths = new Vector<java.lang.Double[]>();
				Vector<Vector<Point2D.Double>> neuritePaths = new Vector<Vector<Point2D.Double>>();
				for (int i = 0; i < tmpNeuriteGraphs.size(); i++) {
						// Get all NSGs without spines to calculate the features.
						Vector<Vector<Point2D.Double>> tmpPathList = tmpNeuriteGraphs
						    .elementAt(i).getAllPaths(false);
						for (int k = 0; k < tmpPathList.size(); k++) {
								Vector<Point2D.Double> tmpPath = tmpPathList.elementAt(k);
								java.lang.Double[] tmpWidthData = new java.lang.Double[tmpPath.size()];
								for (int j = 0; j < tmpPath.size(); j++) {
										Point2D.Double p = tmpPath.elementAt(j);
										tmpWidthData[j] = new java.lang.Double(
										    distanceMap[(int) p.y][(int) p.x]);
								}
								neuriteWidths.addElement(tmpWidthData);
								neuritePaths.add(tmpPath);
						}
				}
				String outFile = outputDir.getDirectoryName() + File.separator
				    + neuronImage.getTitle() + "-neuriteWidth.mtb";

				// store the neurite width data in a given file
				saveNeuriteWidthList(neuriteWidths, outFile);

				/*
				 * If we run the program under Linux, use R to compute the neurite features
				 * (for Windows it will implemented as soon as possible).
				 */
				if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
						// initialize the final feature vector
						Vector<Vector<Vector<Line2D.Double>>> featureLines = null;
						// test if R engine was created successful
						if (rEngine != null) {
								// call the neurite feature script
								boolean rCall = callNeuriteScript("MTBNeuriteFeatureDetection.R",
								    outFile);
								if (!rCall) {
										System.out
										    .println(" --> neurite feature extraction: abnormal termination executing R");
								}
								// get features out of the r engine
								REXP getFeatures1;
								REXP getFeatures2;
								REXP getProfileID;
								REXP getNWidth;
								REXP getSWidth;
								REXP getCWidth;
								getFeatures1 = rEngine.eval("bodyPositions");
								getFeatures2 = rEngine.eval("conePositions");
								getProfileID = rEngine.eval("profileID");
								getNWidth = rEngine.eval("neuriteWidth");
								getSWidth = rEngine.eval("shaftWidth");
								getCWidth = rEngine.eval("coneWidth");
								// parse R variables to Java
								double[] bodyPos = getFeatures1.asDoubleArray();
								double[] conePos = getFeatures2.asDoubleArray();
								double[] profId = getProfileID.asDoubleArray();
								double[] neuriteWidth = getNWidth.asDoubleArray();
								double[] shaftWidth = getSWidth.asDoubleArray();
								double[] coneWidth = getCWidth.asDoubleArray();

								// check for existing profile features
								if (bodyPos == null || conePos == null || profId == null
								    || neuriteWidth == null || shaftWidth == null || coneWidth == null) {
										return (null);
								}

								neuriteShaftLengths = new int[profId.length];

								/*
								 * Generate feature vectors.
								 */
								if (bodyPos != null && conePos != null && profId != null) {
										featureLines = new Vector<Vector<Vector<Line2D.Double>>>();
										// add features for every neurite to the final feature vector
										for (int i = 0; i < profId.length; i++) {
												// make a copy of the skeleton image to calculate each neurite NSG
												MTBImageByte tmpSkelImg = (MTBImageByte) skelImage.duplicate();
												// get all path points of the current neurite
												Vector<Point2D.Double> tmpPoints = neuritePaths
												    .elementAt((int) profId[i]);
												// get normal vector for first feature
												int posLeft = (int) bodyPos[i];
												if (posLeft > 10) {
														posLeft = posLeft - 10;
												}
												Point2D.Double NV = getNormal(tmpPoints.elementAt(posLeft),
												    tmpPoints.elementAt((int) bodyPos[i] + 10));
												// create first borderline from feature
												Line2D.Double borderLine1 = getBorderLine(tmpPoints
												    .elementAt((int) bodyPos[i]), NV);
												// get normal vector for second feature
												int posRight = (int) conePos[i];
												if ((posRight + 10) < tmpPoints.size()) {
														posRight = posRight + 10;
												}
												NV = getNormal(tmpPoints.elementAt((int) conePos[i] - 10),
												    tmpPoints.elementAt(posRight));
												// create second borderline from feature
												Line2D.Double borderLine2 = getBorderLine(tmpPoints
												    .elementAt((int) conePos[i]), NV);

												/*
												 * Temporary features, the borderline between soma and neurite shaft
												 * and the borderline between neurite shaft and growth cone
												 */
												Vector<Vector<Line2D.Double>> tmpFeatures = new Vector<Vector<Line2D.Double>>();
												Vector<Line2D.Double> tmpLines = new Vector<Line2D.Double>(2);
												// add first borderline to feature vector
												tmpLines.addElement(borderLine1);
												// add second borderline to feature vector
												tmpLines.addElement(borderLine2);
												tmpFeatures.addElement(tmpLines);
												// add feature vector
												featureLines.addElement(tmpFeatures);

												/*
												 * Remove skeleton point before feature point to build up a new
												 * graph only for the whole neurite. Position of feature changed to
												 * +2, because on region splitting the feature line is thickened to
												 * 3x3 pixel to safely split the regions.
												 */
												tmpSkelImg.putValueInt((int) Math.round(tmpPoints
												    .elementAt((int) bodyPos[i] + 1).x), (int) Math.round(tmpPoints
												    .elementAt((int) bodyPos[i] + 1).y), 255);
												// build up the neurites NSG
												MTBNeuriteSkelGraph tmpExtracted = new MTBNeuriteSkelGraph(
												    maxSpineLength);
												Point2D.Double f1 = new Point2D.Double(tmpPoints
												    .elementAt((int) bodyPos[i] + 2).x, tmpPoints
												    .elementAt((int) bodyPos[i] + 2).y);
												Point2D.Double f2 = new Point2D.Double(tmpPoints
												    .elementAt((int) conePos[i]).x, tmpPoints
												    .elementAt((int) conePos[i]).y);
												Vector<Vector<Point2D.Double>> tmpResPoints = new Vector<Vector<Point2D.Double>>();
												Vector<Point2D.Double> tmpResP = new Vector<Point2D.Double>(2);
												tmpResP.addElement(f1);
												tmpResP.addElement(f2);
												tmpResPoints.addElement(tmpResP);
												featurePoints.addElement(tmpResPoints);
												neuriteShaftLengths[i] = ((int) conePos[i] - ((int) bodyPos[i] + 2) + 1);
												// fill vector of average neurite component widths
												Vector<Double> currentWidths = new Vector<Double>(3);
												currentWidths.addElement(neuriteWidth[i]);
												currentWidths.addElement(shaftWidth[i]);
												currentWidths.addElement(coneWidth[i]);
												avgNeuriteWidths.addElement(currentWidths);
												/*
												 * Rebuild graph from changed position after feature detection.
												 */
												boolean build = tmpExtracted.buildSkeletonGraph((int) Math
												    .round(tmpPoints.elementAt((int) bodyPos[i] + 2).x), (int) Math
												    .round(tmpPoints.elementAt((int) bodyPos[i] + 2).y), width,
												    height, tmpSkelImg, 0);
												// add graph to vector of neurite NSGs
												if (build) {
														neuriteGraphs.addElement(tmpExtracted);
												}
										}
								}
						}
						// return the feature vectorgetNeuriteFeatures
						return (featureLines);
				} else {
						return (null);
				}
		}

		/**
		 * Get borderlines between soma/neurite shaft and neurite shaft/growth cone.
		 * The borderline is extracted using the feature point and the normal vector,
		 * using a point before and after the feature point.
		 * 
		 * @param p
		 *          feature point
		 * @param normalVec
		 *          normal vector for feature point
		 * @return Borderline as Line.2D.double object.
		 */
		private Line2D.Double getBorderLine(Point2D.Double p, Point2D.Double normalVec) {
				Line2D.Double borderLine = new Line2D.Double();
				int colValue = 0;
				if (neuronColor == NeuriteExtractor2D.NeuronColor.WHITE) {
						colValue = 255;
				}
				int counter = 1;
				boolean found = false;
				Point2D.Double p1 = new Point2D.Double(p.x, p.y);
				while (!found) {
						int tmpX = (int) Math.round(p.x + (normalVec.x * counter));
						int tmpY = (int) Math.round(p.y + (normalVec.y * counter));
						if (tmpX < 0 || tmpY < 0 || tmpX >= this.width || tmpY >= this.height) {
								found = true;
						} else {
								int value = neuronImage.getValueInt(tmpX, tmpY);
								if (value == colValue) {
										counter++;
										p1.setLocation(tmpX, tmpY);
								} else {
										found = true;
								}
						}
				}
				counter = 1;
				found = false;
				Point2D.Double p2 = new Point2D.Double(p.x, p.y);
				while (!found) {
						int tmpX = (int) Math.round(p.x - (normalVec.x * counter));
						int tmpY = (int) Math.round(p.y - (normalVec.y * counter));
						if (tmpX < 0 || tmpY < 0 || tmpX >= this.width || tmpY >= this.height) {
								found = true;
						} else {
								int value = neuronImage.getValueInt(tmpX, tmpY);
								if (value == colValue) {
										counter++;
										p2.setLocation(tmpX, tmpY);
								} else {
										found = true;
								}
						}
				}
				borderLine.setLine(p1, p2);
				return borderLine;
		}

		/**
		 * Get normal vector between two points.
		 * 
		 * @param p1
		 *          first point
		 * @param p2
		 *          second point
		 * @return Point of normal vector.
		 */
		private Point2D.Double getNormal(Point2D.Double p1, Point2D.Double p2) {
				// assign normal vector to the left side
				double dx = 0.0;
				double dy = 0.0;
				dx = (p1.x - p2.x);
				dy = (p1.y - p2.y);
				// assign vector to the right side
				Point2D.Double n = new Point2D.Double(dy, (-1) * dx);
				// make unit vector
				n = standardization(n);
				return n;
		}

		/**
		 * Get unit vector.
		 * 
		 * @param p
		 *          point of vector, other coordinate is (0,0)
		 * @return Unit vector.
		 */
		private Point2D.Double standardization(Point2D.Double p) {
				double abs = Math.sqrt(((Math.pow(p.getX(), 2)) + (Math.pow(p.getY(), 2))));
				if (abs < MTBConstants.epsilon) {
						return (new Point2D.Double(p.x, p.y));
				}
				double x = ((p.x) / abs);
				double y = ((p.y) / abs);
				return (new Point2D.Double(x, y));
		}

		/**
		 * Generate neurite region from calculated features. The soma is excluded, so
		 * that only the neurite regions remain.
		 * 
		 * @param lines
		 *          detected feature lines for every neurite
		 * @param points
		 *          detected feature points for every feature line
		 * @return Set of neurite regions.
		 */
		private MTBRegion2DSet generateNeuriteRegions(
		    Vector<Vector<Vector<Line2D.Double>>> lines,
		    Vector<Vector<Vector<Point2D.Double>>> points) {
				MTBRegion2DSet neuriteRegs = new MTBRegion2DSet(0, 0, width - 1, height - 1);
				for (int i = 0; i < lines.size(); i++) {
						LabelComponentsSequential LCS;
						MTBRegion2DSet tmpRegs;
						try {
								LCS = new LabelComponentsSequential(drawLine2D(lines.elementAt(i)
								    .elementAt(0).elementAt(0), points.elementAt(i).elementAt(0)
								    .elementAt(0)), true);
								LCS.runOp();
								tmpRegs = LCS.getResultingRegions();
								// LCS.getLabelImage().show();
								for (int j = 0; j < tmpRegs.size(); j++) {
										MTBRegion2D reg = tmpRegs.elementAt(j);
										if (reg.contains(points.elementAt(i).elementAt(0).elementAt(0))) {
												neuriteRegs.add(reg);
										}
								}
						} catch (ALDOperatorException e) {
								System.out.println(">>>>>>> extraction failed @ label components");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out.println(">>>>>>> extraction failed @ label components");
								e.printStackTrace();
						}
				}
				return neuriteRegs;
		}

		/**
		 * Draws a 2D line into the neuron image.
		 * <p>
		 * This function implements the Bresenham algorithm. Code was 'stolen' from
		 * Wikipedia, {@link http://de.wikipedia.org/wiki/Bresenham-Algorithmus}, and
		 * then translated into Java (German comments where kept).
		 * 
		 * @param xstart
		 *          x-coordinate of start point.
		 * @param ystart
		 *          y-coordinate of start point.
		 * @param xend
		 *          x-coordinate of end point.
		 * @param yend
		 *          y-coordinate of end point.
		 * @param value
		 *          Color/gray-scale value of the polygon.
		 */
		public MTBImageByte drawLine2D(Line2D.Double line, Point2D.Double point) {

				MTBImageByte tmpNeuronImg = (MTBImageByte) neuronImage.duplicate();

				final int[][] delta = { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 },
				    { -1, 1 }, { -1, 0 }, { -1, -1 } };

				int xstart = (int) Math.round(line.x1);
				int ystart = (int) Math.round(line.y1);
				int xend = (int) Math.round(line.x2);
				int yend = (int) Math.round(line.y2);

				int x, y, t, dx, dy, incx, incy, pdx, pdy, ddx, ddy, es, el, err;

				/* Entfernung in beiden Dimensionen berechnen */
				dx = xend - xstart;
				dy = yend - ystart;

				/* Vorzeichen des Inkrements bestimmen */
				incx = (int) Math.signum(dx);
				incy = (int) Math.signum(dy);
				if (dx < 0)
						dx = -dx;
				if (dy < 0)
						dy = -dy;

				/* feststellen, welche Entfernung größer ist */
				if (dx > dy) {
						/* x ist schnelle Richtung */
						pdx = incx;
						pdy = 0; /* pd. ist Parallelschritt */
						// int[] col = { 170, 170, 170 };
						// debugImage = tmpNeurite.toImage(debugImage, col);
						ddx = incx;
						ddy = incy; /* dd. ist Diagonalschritt */
						es = dy;
						el = dx; /* Fehlerschritte schnell, langsam */
				} else {
						/* y ist schnelle Richtung */
						pdx = 0;
						pdy = incy; /* pd. ist Parallelschritt */
						ddx = incx;
						ddy = incy; /* dd. ist Diagonalschritt */
						es = dx;
						el = dy; /* Fehlerschritte schnell, langsam */
				}

				/* Initialisierungen vor Schleifenbeginn */
				x = xstart;
				y = ystart;
				err = el / 2;
				int bgColor = 0;
				int fgColor = 255;
				if (neuronColor == NeuronColor.BLACK) {
						bgColor = 255;
						fgColor = 0;
				}
				if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
						for (int j = 0; j < 8; j++) {
								tmpNeuronImg.putValueInt(x + delta[j][0], y + delta[j][1], bgColor);
						}
						tmpNeuronImg.putValueInt(x, y, bgColor);
				}

				/* Pixel berechnen */
				for (t = 0; t < el; ++t) /* t zaehlt die Pixel, el ist auch Anzahl */
				{
						/* Aktualisierung Fehlerterm */
						err -= es;
						if (err < 0) {
								/* Fehlerterm wieder positiv (>=0) machen */
								err += el;
								/* Schritt in langsame Richtung, Diagonalschritt */
								x += ddx;
								y += ddy;
						} else {
								/* Schritt in schnelle Richtung, Parallelschritt */
								x += pdx;
								y += pdy;
						}
						if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
								for (int j = 0; j < 8; j++) {
										tmpNeuronImg.putValueInt(x + delta[j][0], y + delta[j][1], bgColor);
								}
								tmpNeuronImg.putValueInt(x, y, bgColor);
						}
				}
				tmpNeuronImg.putValueInt((int) Math.round(point.x), (int) Math
				    .round(point.y), fgColor);

				return tmpNeuronImg;
		}

		/**
		 * Method to clean up the extracted neurites, due to multiple features along a
		 * NSG path or feature shifts at the start point due to different NSG lengths.
		 * 
		 * @param tmpNeuriteSet
		 *          current temporary neurite set
		 */
		public void cleanNeurites(MTBNeurite2DSet tmpNeuriteSet) {

				// TODO add and update comments

				/*
				 * Revisit all neurites and merge neurites with same region because of
				 * including more than one large neurite branch.
				 */
				for (int i = 0; i < tmpNeuriteSet.size(); i++) {
						Vector<Integer> mergeID = new Vector<Integer>();
						MTBNeurite2D n1 = tmpNeuriteSet.getElementAt(i);
						Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> n1EndNodes = n1
						    .getNeuriteGraph().getEndNodes();
						Vector<Point2D.Double> n1EndPoints = new Vector<Point2D.Double>(
						    n1EndNodes.size());
						for (int j = 0; j < n1EndNodes.size(); j++) {
								n1EndPoints.addElement(n1EndNodes.elementAt(j).getData());
						}

						for (int j = 0; j < tmpNeuriteSet.size(); j++) {
								if (j != i) {
										MTBNeurite2D n2 = tmpNeuriteSet.getElementAt(j);
										Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> n2EndNodes = n2
										    .getNeuriteGraph().getEndNodes();
										Vector<Point2D.Double> n2EndPoints = new Vector<Point2D.Double>(
										    n2EndNodes.size());
										for (int k = 0; k < n2EndNodes.size(); k++) {
												n2EndPoints.addElement(n2EndNodes.elementAt(k).getData());
										}

										if ((n1EndPoints.containsAll(n2EndPoints))
										    && (n2EndPoints.containsAll(n1EndPoints))) {
												mergeID.addElement(new Integer(j));
										}
								}
						}
						// System.out.println("merge: ");
						// for (int j = 0; j < mergeID.size(); j++) {
						// System.out.print(mergeID.elementAt(j) + ", ");
						// }
						// System.out.println();

						if (mergeID.size() > 0) {
								mergeID.addElement(new Integer(i));

								Vector<Vector<Point2D.Double>> pAll = n1.getNeuriteGraph().getAllPaths(
								    false);
								Vector<Integer> del = new Vector<Integer>();
								Vector<Integer> add = new Vector<Integer>();

								for (int k = 0; k < pAll.size(); k++) {
										Vector<Integer> tmp = new Vector<Integer>();
										Vector<Point2D.Double> tmpPath = pAll.elementAt(k);
										for (int m = 0; m < mergeID.size(); m++) {
												// TODO handle if here are more than one feature exists
												Point2D.Double fPoint = tmpNeuriteSet.getElementAt(
												    mergeID.elementAt(m)).getsecondFeatureAt(0);

												if (tmpPath.contains(fPoint)) {
														tmp.addElement(mergeID.elementAt(m));
												}
										}

										// System.out.println("tmp: ");
										// for (int j = 0; j < tmp.size(); j++) {
										// System.out.print(tmp.elementAt(j) + ", ");
										// }
										// System.out.println();

										if (tmp.size() > 1) {
												int maxPos = 0;
												int maxID = 0;
												for (int j = 0; j < tmp.size(); j++) {
														int pos = tmpPath.indexOf(tmpNeuriteSet.getElementAt(
														    tmp.elementAt(j)).getsecondFeatureAt(0));
														if (pos > maxPos) {
																maxPos = pos;
																maxID = j;

														}
												}
												for (int j = 0; j < tmp.size(); j++) {
														if (j != maxID) {
																if (!(del.contains(tmp.elementAt(j)))) {
																		del.addElement(tmp.elementAt(j));
																		if (add.contains(tmp.elementAt(j))) {
																				add.removeElement(tmp.elementAt(j));
																		}
																}
														} else {
																if (!(add.contains(tmp.elementAt(j)))) {
																		if (!(del.contains(tmp.elementAt(j)))) {
																				add.addElement(tmp.elementAt(j));
																		}
																}
														}
												}
										}

										if (tmp.size() == 1) {
												if ((!(add.contains(tmp.elementAt(0))))
												    && (!(del.contains(tmp.elementAt(0))))) {
														add.addElement(tmp.elementAt(0));
												}
										}
								}

								// System.out.println("add1: ");
								// for (int j = 0; j < add.size(); j++) {
								// System.out.print(add.elementAt(j) + ", ");
								// }
								// System.out.println();
								// System.out.println("del: ");
								// for (int j = 0; j < del.size(); j++) {
								// System.out.print(del.elementAt(j) + ", ");
								// }
								// System.out.println();
								Integer n1Pos = new Integer(i);
								if (del.contains(new Integer(i))) {
										// System.out.println("delete current");
										n1 = tmpNeuriteSet.getElementAt(add.firstElement());
										n1Pos = add.firstElement();
										add.removeElementAt(0);
										// mergeID.removeElementAt(0);
										// mergeID.addElement(new Integer(i));
								}
								if (add.contains(new Integer(i))) {

										add.removeElement(new Integer(i));

								}

								// use first feature from longest shaft, to get right region
								// changing feature see below at adding sequence
								int max = 0;
								int maxID = 0;
								for (int j = 0; j < add.size(); j++) {
										int length = tmpNeuriteSet.getElementAt(add.elementAt(j))
										    .getShaftLengths()[0];
										if (length > max) {
												max = length;
												maxID = j;
										}
								}
								if (n1.getShaftLengths()[0] < max) {
										n1 = tmpNeuriteSet.getElementAt(add.elementAt(maxID));
										add.removeElementAt(maxID);
										add.addElement(n1Pos);
								}

								// System.out.println("add2: ");
								// for (int j = 0; j < add.size(); j++) {
								// System.out.print(add.elementAt(j) + ", ");
								// }
								// System.out.println();

								int size = add.size() + del.size();
								Vector<Integer> remove = new Vector<Integer>(size);
								for (int j = 0; j < add.size(); j++) {
										remove.addElement(add.elementAt(j));
								}
								for (int j = 0; j < del.size(); j++) {
										remove.addElement(del.elementAt(j));
								}
								Collections.sort(remove);
								// System.out.println("rem: ");
								// for (int j = 0; j < remove.size(); j++) {
								// System.out.print(remove.elementAt(j) + ", ");
								// }
								// System.out.println();
								// System.out.println("curr: " + n1.getsecondFeatureAt(0));

								for (int m = 0; m < add.size(); m++) {
										MTBNeurite2D neuriteToAdd = tmpNeuriteSet.getElementAt(add
										    .elementAt(m));

										Vector<Vector<Point2D.Double>> n1FP = n1.getFeaturePoints();
										Vector<Vector<Point2D.Double>> nAFP = neuriteToAdd.getFeaturePoints();
										Point2D.Double n1P = n1FP.elementAt(0).elementAt(0);
										Point2D.Double nAP = nAFP.elementAt(0).elementAt(0);
										Vector<Vector<Line2D.Double>> n1FL = n1.getFeatureLines();
										Vector<Vector<Line2D.Double>> nAFL = neuriteToAdd.getFeatureLines();
										Line2D.Double n1L = n1FL.elementAt(0).elementAt(0);
										Line2D.Double nAL = nAFL.elementAt(0).elementAt(0);
										// change feature points and lines only if different to n1 (current
										// neurite)

										boolean lines = false;
										if ((n1L.getP1().equals(nAL.getP1()))
										    && (n1L.getP2().equals(nAL.getP2()))) {
												lines = true;
										}

										boolean points = false;
										if (n1P.equals(nAP)) {
												points = true;
										}

										if (points && lines) {
												n1.addData(neuriteToAdd.getFeaturePoints(), neuriteToAdd
												    .getFeatureLines(), neuriteToAdd.getShaftLengths(),
												    neuriteToAdd.getNeuriteWidths());
										}

										Vector<Vector<Point2D.Double>> tmpFPoints = new Vector<Vector<Point2D.Double>>();
										Vector<Point2D.Double> tmpPoint = new Vector<Point2D.Double>(2);
										tmpPoint.addElement(n1P);
										tmpPoint.addElement(nAFP.elementAt(0).elementAt(1));
										tmpFPoints.addElement(tmpPoint);

										Vector<Vector<Line2D.Double>> tmpFLines = new Vector<Vector<Line2D.Double>>();
										Vector<Line2D.Double> tmpLine = new Vector<Line2D.Double>(2);
										tmpLine.addElement(n1L);
										tmpLine.addElement(nAFL.elementAt(0).elementAt(1));
										tmpFLines.addElement(tmpLine);

										// recalculate lengths
										int[] tmpShaftLength = new int[1];
										if (!points) {
												pAll = n1.getNeuriteGraph().getAllPaths(false);
												for (int k = 0; k < pAll.size(); k++) {
														Vector<Point2D.Double> tmpPath = pAll.elementAt(k);

														Point2D.Double fPoint = neuriteToAdd.getsecondFeatureAt(0);

														if (tmpPath.contains(fPoint)) {
																tmpShaftLength[0] = (tmpPath.indexOf(fPoint) + 1);
																k = pAll.size();
														}
												}

										}

										// reassign feature points and lines
										if (points != true && lines == true) {

												// TODO recalculate shaft length
												n1.addData(tmpFPoints, neuriteToAdd.getFeatureLines(),
												    tmpShaftLength, neuriteToAdd.getNeuriteWidths());
										}

										if (points == true && lines != true) {

												// TODO recalculate shaft length
												n1.addData(neuriteToAdd.getFeaturePoints(), tmpFLines, neuriteToAdd
												    .getShaftLengths(), neuriteToAdd.getNeuriteWidths());
										}

										if (points != true && lines != true) {
												// TODO recalculate shaft length
												n1.addData(tmpFPoints, tmpFLines, tmpShaftLength, neuriteToAdd
												    .getNeuriteWidths());

										}

										// System.out.println("n1FP: ");
										// for (int j = 0; j < n1FP.size(); j++) {
										// Vector<Point2D.Double> f = n1FP.elementAt(j);
										// for (int k = 0; k < f.size(); k++) {
										// System.out.print(f.elementAt(k) + ", ");
										// }
										// }
										// System.out.println();
										// System.out.println("nAFP: ");
										// for (int j = 0; j < nAFP.size(); j++) {
										// Vector<Point2D.Double> f = nAFP.elementAt(j);
										// for (int k = 0; k < f.size(); k++) {
										// System.out.print(f.elementAt(k) + ", ");
										// }
										// }
										// System.out.println();
										//
										// int[] n1Le = n1.getShaftLengths();
										// int[] nALe = neuriteToAdd.getShaftLengths();
										// System.out.println("n1L ");
										// for (int j = 0; j < n1Le.length; j++) {
										// System.out.print(n1Le[j] + ", ");
										// }
										// System.out.println();
										// System.out.println("nAL ");
										// for (int j = 0; j < nALe.length; j++) {
										// System.out.print(nALe[j] + ", ");
										// }
										// System.out.println();

								}
								for (int j = 0; j < remove.size(); j++) {
										tmpNeuriteSet.removeElementAt(remove.elementAt(j) - j);
								}
						}
				}
		}
}
