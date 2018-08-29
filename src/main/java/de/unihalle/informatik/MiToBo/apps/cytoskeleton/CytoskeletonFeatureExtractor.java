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

package de.unihalle.informatik.MiToBo.apps.cytoskeleton;

import java.io.File;

import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.tools.image.ImageDimensionReducer;
import de.unihalle.informatik.MiToBo.tools.image.ImageDimensionReducer.ReducerMethod;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Operator for extracting features for the {@link ActinAnalyzer2D}.
 * <p>
 * The features which are to be extracted by operators extending this
 * class should be specifically dedicated to filament like structures,
 * e.g., actin fiberes or microtubuli.
 * 
 * @author moeller
 */
public abstract class CytoskeletonFeatureExtractor extends MTBOperator {

	/**
	 * Format of cell segmentation data.
	 */
	public static enum CellMaskFormat {
		/**
		 * Label image, i.e., each cell has its own gray-scale label.
		 */
		LABEL_IMAGE,
		/**
		 * Set of ImageJ 1.x ROIs.
		 */
		IJ_ROIS
	}

	/**
	 * Input image directory.
	 * <p>
	 * All files in the directory and its sub-directories are considered. 
	 * If a file cannot be opened (e.g. because it is not an image) it 
	 * is skipped. 
	 */
	@Parameter( label= "Image directory", required = true, 
		dataIOOrder = -11, direction = Direction.IN, 
		description = "Input image directory.", mode = ExpertMode.STANDARD)
	protected ALDDirectoryString imageDir = null;

	/**
	 * Channel of input image containing stained cytoskeleton.
	 */
	@Parameter(label = "Cytoskeleton Channel", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = -10, 
		description = "Cytoskeleton channel, e.g., 1, 2 and so on.")
	private int cytoChannel = 1;
	
	/**
	 * Directory with (cell) masks.
	 */
	@Parameter( label= "Mask directory", required = true, dataIOOrder = -9,
		direction = Direction.IN, description = "Cell mask directory.",
		mode = ExpertMode.STANDARD)
	protected ALDDirectoryString maskDir = null;

	/**
	 * Format of provided cell masks.
	 */
	@Parameter( label= "Mask format", required = true, dataIOOrder = -8,
		direction = Direction.IN, description = "Format of cell masks.",
		mode = ExpertMode.STANDARD)
	protected CellMaskFormat maskFormat = CellMaskFormat.LABEL_IMAGE;
	
	/**
	 * Output and working directory.
	 */
	@Parameter( label= "Output and working directory", required = true, 
		dataIOOrder = -7, direction = Direction.IN, 
		description = "Output and working directory.", 
		mode = ExpertMode.STANDARD)
	protected ALDDirectoryString outDir = null;
	
	/**
	 * Tile size in x-direction.
	 */
	@Parameter( label= "Tile size x", required = true, 
		dataIOOrder = 3, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, 
		description = "Tile size in x-direction.")
	protected int tileSizeX = 32;

	/**
	 * Tile size in y-direction.
	 */
	@Parameter( label= "Tile size y", required = true, 
		dataIOOrder = 4, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, 
		description = "Tile size in y-direction.")
	protected int tileSizeY = 32;

	/**
	 * Tile shift in x-direction.
	 */
	@Parameter( label= "Tile shift x", required = true, 
		dataIOOrder = 5, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, 
		description = "Tile shift in x-direction.")
	protected int tileShiftX = 32;

	/**
	 * Tile size in y-direction.
	 */
	@Parameter( label= "Tile shift y", required = true, 
		dataIOOrder = 6, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, 
		description = "Tile shift in y-direction.")
	protected int tileShiftY = 32;
	
	/*
	 * some helper variables
	 */
	
	/**
	 * Identifier string for the operator class.
	 */
	protected transient String operatorID;

	/**
	 * Width of the images, taking first image as reference.
	 */
	protected transient int imageWidth = -1;
	
	/**
	 * Height of the images, taking first image as reference.
	 */
	protected transient int imageHeight = -1;

	/**
	 * Operator for reading image files from disk.
	 */
	protected static ImageReaderMTB iRead = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public CytoskeletonFeatureExtractor() throws ALDOperatorException {
		// nothing to be done here
	}
	
	/**
	 * Specify input image directory.
	 * @param iDir	Directory with images.
	 */
	public void setImageDir(ALDDirectoryString iDir) {
		this.imageDir = iDir;
	}
	
	/**
	 * Specify channel with stained cytoskeleton.
	 * @param c		ID of channel with stained cytoskeleton.
	 */
	public void setCytoskeletonChannel(int c) {
		this.cytoChannel = c;
	}
	
	/**
	 * Specify input mask directory.
	 * @param mDir	Directory with masks.
	 */
	public void setMaskDir(ALDDirectoryString mDir) {
		this.maskDir = mDir;
	}
	
	/**
	 * Specify input mask format.
	 * @param mFormat	Format of mask files, i.e. label images or 
	 * 								ImageJ ROI files.
	 */
	public void setMaskFormat(CellMaskFormat mFormat) {
		this.maskFormat = mFormat;
	}
	
	/**
	 * Specify output directory.
	 * @param oDir	Output directory for feature files.
	 */
	public void setOutputDir(ALDDirectoryString oDir) {
		this.outDir = oDir;
	}
	
	/**
	 * Specify size of tiles in x-direction.
	 * @param tSizeX	Tile size in x.
	 */
	public void setTileSizeX(int tSizeX) {
		this.tileSizeX = tSizeX;
	}

	/**
	 * Specify size of tiles in y-direction.
	 * @param tSizeY	Tile size in y.
	 */
	public void setTileSizeY(int tSizeY) {
		this.tileSizeY = tSizeY;
	}

	/**
	 * Specify shift of tiles in x-direction.
	 * @param tShiftX	Tile shift in x.
	 */
	public void setTileShiftX(int tShiftX) {
		this.tileShiftX = tShiftX;
	}

	/**
	 * Specify shift of tiles in y-direction.
	 * @param tShiftY	Tile shift in y.
	 */
	public void setTileShiftY(int tShiftY) {
		this.tileShiftY = tShiftY;
	}
	
	@Override
  protected void operate() 
  		throws ALDOperatorException, ALDProcessingDAGException {
		
		int histConstructionMode = ALDOperator.getConstructionMode();
		ALDOperator.setConstructionMode(1);
		this.calculateFeatures();
		ALDOperator.setConstructionMode(histConstructionMode);
	}
	
	/**
	 * Performs the feature calculation.
	 * <p>
	 * The features are saved to files in the given feature directory.
	 * If the directory is null, the output directory is to be used. 
	 * Note that both directories can be the same.<br>
	 * For each image four files are to be saved:
	 * <ul>
	 * <li> *-features.tif: 
	 * 		stack with visualizations of individual features 
	 * <li> *-features.ald: 
	 * 		history file corresponding to feature stack
	 * <li> *-features.txt: 
	 * 		features, each row refers to a specific tile, 
	 * 		each column to an individual feature
	 * <li> *-features-config.ald: 
	 * 		history/configuration of the feature calculator                     
	 * </ul> 
	 * 
	 * @throws ALDOperatorException Thrown in case of failure.
	 * @throws ALDProcessingDAGException Thrown in case of failure.
	 */
	protected abstract void calculateFeatures() 
		throws ALDOperatorException, ALDProcessingDAGException;
	
	/**
	 * Read image from file, do optional max projection and extract 
	 * selected cytoskeleton channel.
	 * 
	 * @param f Name of image file to read.
	 * @return	Channel of provided image file containing cytoskeleton.
	 * @throws ALDOperatorException 			Thrown in case of failure.
	 * @throws ALDProcessingDAGException  Thrown in case of failure.
	 */
	protected MTBImage readInputImageMaxProjectChannel(String f) 
		throws ALDOperatorException, ALDProcessingDAGException {
		try {
			MTBImage maxProjImg;
			if (iRead == null)
				iRead = new ImageReaderMTB();
			iRead.setFileName(f);
			iRead.runOp(HidingMode.HIDDEN);
			MTBImage img = iRead.getResultMTBImage();
			maxProjImg = img;
			// check if a maximum projection is required
			if (img.getSizeZ() > 1) {
				ImageDimensionReducer reduce = new ImageDimensionReducer(img, 
						false, false, true, false, false, ReducerMethod.MAX);
				reduce.runOp();
				maxProjImg = reduce.getResultImg();
			}
			// extract cytoskeleton channel
			return maxProjImg.getSlice(0, 0, this.cytoChannel-1);
		} catch (ALDProcessingDAGException apex) {
			throw apex;
		} catch (Exception ex) {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
				this.operatorID + " Could not read file " + f + "...");
		}
	}
	
	/**
	 * Read mask data from disk if available.
	 * <p>
	 * The method reads segmentation data from file. It considers the 
	 * specified mask format, i.e., if a label image is to be read or 
	 * ImageJ 1.x ROIs. In the latter case it automatically differentiates 
	 * between files ending with '.zip', i.e., containing more than one 
	 * region, and files ending with '.roi' which contain exactly a single 
	 * region.
	 * 
	 * @param maskDir			Directory where to search for the mask images.
	 * @param basename		Basename of the corresponding image file.
	 * @param maskFormat	Format of the mask data file.
	 * @param xmin Minimum x-value of input image domain.
	 * @param ymin Minimum y-value of input image domain.
	 * @param xmax Maximum x-value of input image domain, 
	 * 							i.e. image width - 1.
	 * @param ymax Maximum y-value of input image domain, 
	 * 							i.e. image height - 1.
	 * @param verbose			If true, additional information is printed.
	 * 
	 * @return	Mask image, null if appropriate file could not be found.
	 */
	 public static MTBImage readMaskImage(String maskDir, String basename,
			 CellMaskFormat maskFormat,
				double xmin, double ymin,	double xmax, double ymax, boolean verbose) {
		MTBImage maskImage = null;
		String maskName = "";
		if (maskDir != null) {
			switch(maskFormat)
			{
			case LABEL_IMAGE:
				maskName= maskDir + File.separator + basename + "-mask.tif";
				if (verbose)
					System.out.print("\t\t - searching mask " + maskName + "...");
//				fireOperatorExecutionProgressEvent(
//						new ALDOperatorExecutionProgressEvent(this,
//								" searching mask " + maskName + "..."));

				if ((new File(maskName)).exists()) {
					try {
						if (iRead == null)
							iRead = new ImageReaderMTB();
						iRead.setFileName(maskName);
						iRead.runOp();
						// get mask, make sure to convert it to byte type
						maskImage = iRead.getResultMTBImage().convertType(
								MTBImageType.MTB_BYTE, true);
						if (verbose)
							System.out.println("found!");
//						fireOperatorExecutionProgressEvent(
//								new ALDOperatorExecutionProgressEvent(
//										this,	" ... found!"));
					} catch (Exception e) {
						if (verbose)
							System.out.println("not found!");
						System.err.println("[CytoskeletonFeatureExtractor] " + 
							" Error reading mask " + maskName + ", ignoring mask...");
//						fireOperatorExecutionProgressEvent(
//								new ALDOperatorExecutionProgressEvent(
//										this," ... not found!"));
					}
				}
				else {
					if (verbose)
						System.out.println("mask not found!");
//					fireOperatorExecutionProgressEvent(
//							new ALDOperatorExecutionProgressEvent(this,
//									" ... mask not found!"));
				}
				break;
			case IJ_ROIS:
				String maskName_A= maskDir + File.separator + basename + "-mask.zip";
				String maskName_B= maskDir + File.separator + basename + "-mask.roi";
				maskName = null;
				if ((new File(maskName_A)).exists()) 
					maskName = maskName_A;
				else 
					if ((new File(maskName_B)).exists())
						maskName = maskName_B;
				if (verbose)
					System.out.print("\t\t - searching IJ ROI file " 
							+ maskName + "...");
//				fireOperatorExecutionProgressEvent(
//						new ALDOperatorExecutionProgressEvent(this,
//								" searching IJ ROI file " + maskName + "..."));

				if (maskName != null) {
					try {
						MTBRegion2DSet regions = 
							RoiManagerAdapter.getInstance().getRegionSetFromRoiFile(
								maskName, xmin, ymin, xmax, ymax);
						if (verbose)
							System.out.println("found!");
//						fireOperatorExecutionProgressEvent(
//							new ALDOperatorExecutionProgressEvent(
//									this,	" ... found!"));
						// convert region set to label image
						DrawRegion2DSet regionDrawOp = new DrawRegion2DSet(
							DrawType.LABEL_IMAGE, regions);
						regionDrawOp.runOp(HidingMode.HIDDEN);
						maskImage = regionDrawOp.getResultImage();
						// save the label image to the output directory
						String outMaskName= maskDir 
							+ File.separator + basename + "-mask.tif";
						ImageWriterMTB imgWriter = 
							new ImageWriterMTB(maskImage, outMaskName);
						imgWriter.setOverwrite(true);
						imgWriter.runOp(HidingMode.HIDDEN);
					} catch (Exception e) {
						if (verbose)
							System.out.println("not found!");
						System.err.println("[CytoskeletonFeatureExtractor] " 
							+ "Error reading IJ ROIs " +	maskName 
								+ ", ignoring segmentation...");
//						fireOperatorExecutionProgressEvent(
//							new ALDOperatorExecutionProgressEvent(
//									this," ... not found!"));
					}
				}
				else {
					if (verbose)
						System.out.println("mask / ROIs not found!");
//					fireOperatorExecutionProgressEvent(
//							new ALDOperatorExecutionProgressEvent(this,
//									" ... mask / ROIs not found!"));
				}
				break;
			}
		}
		if (maskImage != null)
			maskImage.setProperty("Filename", maskName);
		return maskImage;
	}
}