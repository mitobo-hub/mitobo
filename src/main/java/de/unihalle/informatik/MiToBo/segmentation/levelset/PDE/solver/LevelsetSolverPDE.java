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

package de.unihalle.informatik.MiToBo.segmentation.levelset.PDE.solver;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBLevelsetException;
import de.unihalle.informatik.MiToBo.segmentation.levelset.PDE.datatypes.MTBLevelsetFunctionPDE;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.LevelsetSolverDerivatives;

import java.util.Iterator;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN4;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN8;

/**
 * Solver for level set segmentation problems based on PDEs.
 * 
 * @author Martin Scharm
 * @author Michael Schneider
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
	level = ALDAOperator.Level.APPLICATION)
public class LevelsetSolverPDE extends LevelsetSolverDerivatives {

	/**
	 * Available modes for expanding zero level velocities.
	 */
	public static enum VelocityExpansionMode {
		/**
		 * Velocity extrapolation is done by diffusing the Heaviside function.
		 */
		HEAVISIDE_APPROXIMATION,
		/**
		 * Each pixel is assigned the velocity of the closest zero-level pixel.
		 */
		ZERO_LEVEL_EXTRAPOLATION
	}
	
	/*
	 * Required parameters.
	 */

	@Parameter( label = "Velocity Expansion Mode", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 19,
			description = "Mode for expansion of contour pixel velocities.")
		protected VelocityExpansionMode vExpandMode = 
			VelocityExpansionMode.HEAVISIDE_APPROXIMATION;

	/*
	 * Optional parameters.
	 */

	@Parameter( label = "Max. Iterations", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 1,
		description = "Max. number of iterations in iterative optimization.")
	protected int maxIterations = 100;

	@Parameter( label = "Narrow Band Width", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 2,
		description = "Width of narrow band, if zero, all pixels are considered.")
	protected double narrowBandWidth = 0.0;

	@Parameter(label = "Preserve Topology", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 3,
		description = "Enables/disables topology preservation.")
	protected boolean topologyPreservation = false;

	@Parameter(label = "Regions to mask (only 2D)", required = false, 
		direction = Parameter.Direction.IN, dataIOOrder = 4, 
		description = "Optional polygon set to set parts of the image invisible.")
	protected MTBPolygon2DSet invisibleRegionSet = null;

	/*
	 * Supplemental parameters.
	 */
	
	@Parameter(label = "Show Intermediate Results", required = false, 
		supplemental = true, dataIOOrder = -99, direction = Parameter.Direction.IN,
		description = "Displays additional, intermediate segmentation results.")
	protected boolean showIntermediateResults = false;

	@Parameter(label = "Save Intermediate Segmentations, Rate = ", 
		required = false, supplemental = true, dataIOOrder = -98, 
		direction = Parameter.Direction.IN,
		description = "Sampling rate for intermediate results,\n " + 
			"if zero no stack with intermediate results is generated.")
	protected int intermediateResultSamplingRate = 0;
	
	@Parameter(label = "Result Image", direction = Parameter.Direction.OUT,
		dataIOOrder = 0, description = "Overlay of contour on input image.")
	protected transient MTBImageRGB resultImage = null;

	@Parameter(label = "Result Mask", direction = Parameter.Direction.OUT,
		dataIOOrder = 1, description = "Binary segmentation mask.")
	protected transient MTBImageByte resultMask = null;

	@Parameter(label = "Intermediate Results Stack", required = false, 
		supplemental = true, dataIOOrder = 2, direction = Parameter.Direction.OUT,
		description = "Stack with intermediate results.")
	protected transient MTBImageByte intermediateResultStack = null;

//	@Parameter( label = "initImage", required = false, direction = Parameter.Direction.IN,
//			description = "initialization, init contour")
//	private MTBImageByte initImage = null;
//	private Object visible = null;
//	@Parameter(label = "debug Bild", required = false, direction = Parameter.Direction.OUT,description = "")
//	MTBImage debugImage;
//	@Parameter(label = "File for running times", dataIOOrder = 10, required = false, direction = Parameter.Direction.IN, description = "File where runtimes are written into (appending).")
//	String timeMeasurPath;

	/*
	 * Some internally used variables.
	 */
	
	/**
	 * Height of the input image.
	 */
	protected int height;
	/**
	 * Width of the input image.
	 */
	protected int width;
	/**
	 * Depth of the input image.
	 */
	protected int depth;
	/**
	 * Iteration counter.
	 */
	protected int iteration;	
	
	/**
	 * Variable to store old level set function for comparison with new one.
	 */
	private transient MTBLevelsetFunctionPDE phi_old;

	/**
	 * Helper object used in topology preservation.
	 */
	MTBTopologicalNumber2DN4 topologicalNumber2DN4 = 
		new MTBTopologicalNumber2DN4();

	/**
	 * Helper object used in topology preservation.
	 */
	MTBTopologicalNumber2DN8 topologicalNumber2DN8 = 
		new MTBTopologicalNumber2DN8();

	/**
	 * List of intermediate result segmentations.
	 */
	protected transient Vector<MTBImageByte> intermediateResults;
	
	/**
	 * Color image to visualize intermediate segmentation results.
	 */
	protected transient MTBImageRGB intermediatePhiColorImage;
	
	/**
	 * Default constructor.
	 */
	public LevelsetSolverPDE() throws ALDOperatorException {
		super();
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		String errorMsg;
		for (int i = 0; i < this.energySet.getWeights().size(); i++) {
			if ((errorMsg = this.energySet.getEnergy(i).validate()) != null)
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
					errorMsg);
		}
		return;
	}

	/**
	 * Methode that runs the operator.
	 */
	@Override
	protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		// time measurements for everything
//		long startTime = 0;
//		if(this.timeMeasurPath != "")
//			startTime = System.currentTimeMillis();

		// initialize the level set function
		this.width = this.inputImg.getSizeX();
		this.height = this.inputImg.getSizeY();
		this.depth = this.inputImg.getSizeZ();
		this.initSegmentation();
		
		// Note: it might happen that the size of the initial segmentation is 
		//       different from the image size (e.g., if it is initialized from 
		//       a region set); to handle these cases the target dimension of 
		//       the level set function are passed to the constructor as well
		this.phi = 
			new MTBLevelsetFunctionPDE(this.width, this.height, this.depth,
				this.initialSegmentation, false);
		this.phi_old = 
			new MTBLevelsetFunctionPDE(this.width, this.height, this.depth,
				this.initialSegmentation, false);

		// choose appropriate step size, if no reasonable value is given
		if(this.deltaT <= 0)
			this.deltaT = this.width * this.height * 4.0;

		// init energies
		for (int i = 0; i < this.energySet.getEnergyList().size(); ++i) {
			try {
				this.energySet.getEnergyList().get(i).initEnergy(this);
				if (this.vExpandMode == VelocityExpansionMode.HEAVISIDE_APPROXIMATION)
					this.energySet.getEnergyList().get(i).useHeavideApproximation(true);
			}
			catch (MTBLevelsetException ex) {
				System.err.println("Could not initialize energy " + 
					this.energySet.getEnergyList().get(i).toString() + "\n");
				ex.printStackTrace();
			}
		}

		// optionally set pixels invisible
		if(this.invisibleRegionSet != null) {
			for (int i = 0; i < this.invisibleRegionSet.size(); ++i) {
				for (int x = 0; x < this.width; ++x) {
					for (int y = 0; y < this.height; ++y) {
						if(this.invisibleRegionSet.elementAt(i).contains(
								x, y, this.width, this.height))  {
							this.phi.setInvisible(x,y);
						}
						else {
							this.phi.setVisible(x,y);
						}
					}
				}
			}
		}

		// init some data objects for debugging and logging
		if (this.showIntermediateResults) {
			this.intermediatePhiColorImage = (MTBImageRGB)MTBImage.createMTBImage(
				this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
			this.intermediatePhiColorImage.setTitle("Development of level set " +
				"function for image <" + this.inputImg.getTitle() + ">");
			this.intermediatePhiColorImage.show();
		}
		if (this.intermediateResultSamplingRate > 0)
			this.intermediateResults = new Vector<MTBImageByte>();
		// monitor development of level set function in verbose mode
//		if (this.verbose.booleanValue()) {
//			this.intermediatePhis = new Vector<MTBImageRGB>();
//		}
		
		// solve the problem
		if (this.solve()) {
			
			// visualize the final result, contour in red on top of original image
			this.resultImage = (MTBImageRGB) MTBImage.createMTBImage(
						this.width, this.height, this.depth, 1, 1, MTBImageType.MTB_RGB);
			this.resultImage.setTitle("PDE level set result for image " + 
				"<" + this.inputImg.getTitle() + ">");
			for (int x = 0; x < this.width; ++x) {
				for (int y = 0; y < this.height; ++y) {
					for (int z = 0; z < this.depth; ++z) {
						this.resultImage.putValue(x, y, 
							this.inputImg.getValueInt(x, y, z), 
								this.inputImg.getValueInt(x, y, z), 
									this.inputImg.getValueInt(x, y, z));
						// mark the contour, i.e. near zero pixels
						if (((MTBLevelsetFunctionPDE)this.phi).nearZero(x, y, z)) {
//							if (this.phi.get(x, y, z) >= 0) {
//								resultImage.putValue(x, y, 255, 0, 0);
//							} else {
//								resultImage.putValue(x, y, 255, 0, 0);
//							}
							this.resultImage.putValue(x, y, 255, 0, 0);
						}
					}
				}
			}
			// get a binary mask of the segmentation result
			this.resultMask = ((MTBLevelsetFunctionPDE)this.phi).getBinaryMask();
			this.resultMask.setTitle("PDE level set binary result mask for image " + 
					"<" + this.inputImg.getTitle() + ">");

			// create intermediate result stack
			if (this.intermediateResultSamplingRate > 0) {
				this.intermediateResultStack = (MTBImageByte)(MTBImage.createMTBImage(
					this.width, this.height, 1, 1,	this.intermediateResults.size(), 
					MTBImage.MTBImageType.MTB_BYTE));
				for (int i=0; i<this.intermediateResults.size(); ++i) {
					this.intermediateResultStack.setImagePart(
						this.intermediateResults.get(i), 0, 0, 0, 0, i);
					this.intermediateResultStack.setSliceLabel("Iteration "+i, 0, 0, i);
				}
				this.intermediateResultStack.setTitle(
					"PDE level set intermediate results for <" + 
						this.inputImg.getTitle() + ">");
			}
			// create intermediate Phi stack
			if (this.verbose.booleanValue()) {
//				this.intermediatePhiStack = (MTBImageRGB)(MTBImage.createMTBImage(
//					this.width, this.height, 1, 1,	this.intermediatePhis.size(), 
//					MTBImage.MTBImageType.MTB_RGB));
//				for (int i=0; i<this.intermediatePhis.size(); ++i) {
//					this.intermediatePhiStack.setImagePart(
//						this.intermediatePhis.get(i), 0, 0, 0, 0, i);
//					this.intermediatePhiStack.setSliceLabel("Iteration "+i+", " + 
//							this.intermediatePhis.get(i).getTitle(), 0, 0, i);
//				}
//				this.intermediatePhiStack.setTitle(
//					"PDE level set intermediate Phis for <" + 
//						this.inputImg.getTitle() + ">");
			}
			
			// write runtime to file
//			if(this.timeMeasurPath != "")
//			{
//				long endTime = System.currentTimeMillis() - startTime;
//				try {
//					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.timeMeasurPath, true)));
//					out.println("ImageName: " + this.inputImg.getTitle() + " Date: " + (new Date()).toString() + " Time : " + endTime + " Iterations: " + this.iteration);
//					out.close();
//				} catch (IOException e) {
//					//oh noes!
//				}
//			}

			return;
		}
		throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
			"[LevelsetSolverPDE] optimization failed, couldn't solve problem!");
	}

	/**
	 * Solve this level set problem.
	 * @return True, if a solution could be found, otherwise false.
	 */
	protected boolean solve() {

		double changed = 0;
		this.iteration = 0;

		while (this.iteration < this.maxIterations) {

			// store intermediate results
			if (    this.intermediateResultSamplingRate > 0 
					&& (   this.iteration==0 
							|| this.iteration%this.intermediateResultSamplingRate==0)) {
				this.intermediateResults.add(
					((MTBLevelsetFunctionPDE)this.phi).getBinaryMask());
			}
//			if (this.verbose.booleanValue())
//				this.intermediatePhis.add(
//					((MTBLevelsetFunctionPDE)this.phi).getPhiImage());

			/**
			 * ********************
			 * debugging ********************
			 */
			++this.iteration;
			changed = step();

			if (this.verbose.booleanValue())
				System.out.println("[LevelsetSolverPDE] iteration: " + 
						this.iteration + ", changed " + changed + " pixels.");

			// abort if no pixel is moved between classes
			if (changed == 0) {
				break;
			}
		}

		// store intermediate results
		if (this.intermediateResultSamplingRate > 0) {
			this.intermediateResults.add(
				((MTBLevelsetFunctionPDE)this.phi).getBinaryMask());
		}
		return true;
	}

	/**
	 * Do one iteration, update all pixels of levelset function (in narrow band).
	 * @return Number of pixels where sign changed.
	 */
	protected int step() {

		if (this.verbose.booleanValue()) {
//			MTBImageRGB phiImage = 
//					((MTBLevelsetFunctionPDE)this.phi).getPhiColorImage2D();
//			phiImage.setTitle("... phi before update");
//			this.intermediatePhis.add(phiImage);					
		}

		int changed = 0;

		// transform levelset function into SDF
		((MTBLevelsetFunctionPDE)this.phi).signDistance(this.narrowBandWidth);

		if (this.showIntermediateResults) {
			((MTBLevelsetFunctionPDE)this.phi).getPhiColorImage2D(
				this.intermediatePhiColorImage);
			this.intermediatePhiColorImage.getImagePlus().updateAndRepaintWindow();
		}

		// update all energies
		try {
			for (int i = 0; i < this.energySet.getEnergyList().size(); i++) {
				this.energySet.getEnergyList().get(i).updateStatus(this.phi);
			}
		} catch (MTBLevelsetException e) {
			e.printStackTrace();
		}

		// save the old levelset function for later comparison
		((MTBLevelsetFunctionPDE)this.phi).copyTo(this.phi_old);

		// update all points
		int newSgn, oldSgn;
		double newValue, update;
		Iterator<MTBPoint3D> list; 
		MTBPoint3D veloSource;
		for (list = ((MTBLevelsetFunctionPDE)this.phi).getNarrowIterator(); 
						list.hasNext();) {
			MTBPoint3D p = list.next();

			// depending on selected velocity expansion mode, request precursor
			// on zero level or use directly approximate velocities
			if (this.vExpandMode == VelocityExpansionMode.ZERO_LEVEL_EXTRAPOLATION) {
				// get precursor of pixel on current contour, i.e. zero-level
				veloSource = this.phi_old.getPredecessorOnContour(
					(int)p.getX(), (int)p.getY(), (int)p.getZ());
			}
			else {
				veloSource = p; 
			}

			// energyset
			update = 0;
			for (int i = 0; i < this.energySet.getEnergyList().size(); i++) {
				update += this.energySet.getWeight(i).doubleValue() 
						* this.energySet.getEnergyList().get(i).getDerivative(
								this.phi_old, (int)veloSource.getX(), (int)veloSource.getY(), 
									(int)veloSource.getZ());
			}

			// check if update is valid
			if (!Double.isNaN(update)) {
				oldSgn = this.sgn(
					this.phi_old.get((int)p.getX(), (int)p.getY(), (int)p.getZ()));
				newValue = this.phi_old.get(
					(int)p.getX(), (int)p.getY(), (int)p.getZ()) - this.deltaT * update;
				newSgn = this.sgn(newValue);
				
				// if sign changes check topologypreservation
				double eps = 0.00001;
				if(oldSgn != newSgn)
				{
					if(this.topologyPreservation)
					{
						if(this.phi.getClass((int)p.getX(),(int)p.getY(),(int)p.getZ()) == 0 && this.topologicalNumber2DN4.topoNumberIsOne(this.phi,(int)p.getX(),(int)p.getY(),(int)p.getZ(),1))
						{
							((MTBLevelsetFunctionPDE)this.phi).set((int)p.getX(), (int)p.getY(), (int)p.getZ(), newValue);
							changed++;
						}
						else
							if(this.phi.getClass((int)p.getX(),(int)p.getY(),(int)p.getZ()) == 1 && this.topologicalNumber2DN4.topoNumberIsOne(this.phi,(int)p.getX(),(int)p.getY(),(int)p.getZ(),0))
							{
								((MTBLevelsetFunctionPDE)this.phi).set((int)p.getX(), (int)p.getY(), (int)p.getZ(), newValue);
								changed++;
							}
							else
							{
								((MTBLevelsetFunctionPDE)this.phi).set((int)p.getX(), (int)p.getY(), (int)p.getZ(), oldSgn * eps);
							}
					}
					else {
						((MTBLevelsetFunctionPDE)this.phi).set(
							(int)p.getX(), (int)p.getY(), (int)p.getZ(), newValue);
						++changed;
					}
				}
				// if sign did not change, just set value 
				else {
					((MTBLevelsetFunctionPDE)this.phi).set(
						(int)p.getX(), (int)p.getY(), (int)p.getZ(), newValue);
				}
			}
		} // end of for-loop over all pixels in narrow band
		return changed;
	}

//	public MTBImage getImg() {
//		return ((MTBLevelsetFunctionPDE)this.phi).getBinaryMask();
//	}

	/**
	 * Returns the sign of a value.
	 * @param ka		Value.
	 * @return	1 or -1, according to the sign, 0 if value is identical to zero.
	 */
	protected int sgn(double ka) {
		if (ka > 0) {
			return 1;
		}
		if (ka < 0) {
			return -1;
		}
		return 0;
	}

	/**
	 * Get width of narrow band. 
	 * @return Width of narrow band.
	 */
	public double getNarrowBandWidth() {
		return this.narrowBandWidth;
	}

	/**
	 * Set width of narrow band.
	 * @param w 	New width of narrow band.
	 */
	public void setNarrowBandWidth(double w) {
		this.narrowBandWidth = w;
	}

	/**
	 * Get width of level set function domain.
	 * @return Size of level set function in x.
	 */
	public int getWidth() {
		return this.width;
	}

	/**
	 * Get maximal number of iterations.
	 * @return Maximal number of iterations in optimization.
	 */
	public int getMaxIterations() {
		return this.maxIterations;
	}

	/**
	 * Get height of level set function.
	 * @return Size of level set function domain in y.
	 */
	public int getHeight() {
		return this.height;
	}

	/**
	 * Set input image.
	 * @param img		Image to segment.
	 */
	public void setInputImg(MTBImage img) {
		this.inputImg = img;
	}

	/**
	 * Get binary mask of segmentation result.
	 * @return Binary segmentation result.
	 */
	public MTBImageByte getResultMask() {
		return this.resultMask;
	}

	/**
	 * Get image with segmentation result overlay.
	 * @return	Result image with contour overlay.
	 */
	public MTBImageRGB getResultImage() {
		return this.resultImage;
	}
}
