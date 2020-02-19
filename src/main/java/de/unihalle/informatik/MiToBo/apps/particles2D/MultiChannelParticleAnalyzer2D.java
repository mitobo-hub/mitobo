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

package de.unihalle.informatik.MiToBo.apps.particles2D;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_Particles;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResult_ParticlesMultiChannel;
import de.unihalle.informatik.MiToBo.apps.datatypes.cellImages.SegResultEnums.MeasureUnit;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 *  Operator for detecting sub-cellular structures in a given image.
 *  <p>
 *  This operator allows to configure a particle detector for each channel
 *  of the image separately. If a proper nucleus channel is given that one is 
 *  skipped. The first detector in the vector of detectors is applied to the 
 *  first non-nucleus channel, the second to the second, etc.
 *  It is important that for all channels to be processed detectors are 
 *  provided.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION)
public class MultiChannelParticleAnalyzer2D extends MTBOperator {

	/**
	 * Display mode of the result image.
	 * 
	 * @author moeller
	 */
	public static enum ResultImageMode {
		/**
		 * Colored particles overlayed over gray-scale input image.
		 */
		OVERLAY,
		/**
		 * Binary mask, particles in white.
		 */
		BINARY,
		/**
		 * No output image at all.
		 */
		NONE
	}
	
	/**
	 * Multi-channel input image to be processed.
	 */
	@Parameter(label= "Input image", required = true, mode= ExpertMode.STANDARD, 
			direction= Direction.IN, description = "Input image.", dataIOOrder= -20)
	private transient MTBImage inImg = null;

	/**
	 * Vector of result data objects.
	 */
	@Parameter( label= "Result statistics",	direction= Direction.OUT,
			description = "Result data.")
	private transient SegResult_ParticlesMultiChannel resultData = null;

	/*
	 * Set of individually configured particles detectors.
	 */

	@Parameter( label= "Particle detector channel 1", required = false,
			direction= Direction.IN, dataIOOrder = -15, mode= ExpertMode.STANDARD,
			description = "Particle detector operator(s) for channels.")
	private ParticleDetector particleDetector_1 = null;
	
	@Parameter( label= "Particle detector channel 2", required = false,
			direction= Direction.IN, dataIOOrder = -14, mode= ExpertMode.STANDARD,
			description = "Particle detector operator(s) for channels.")
	private ParticleDetector particleDetector_2 = null;

	@Parameter( label= "Particle detector channel 3", required = false,
			direction= Direction.IN, dataIOOrder = -13, mode= ExpertMode.STANDARD,
			description = "Particle detector operator(s) for channels.")
	private ParticleDetector particleDetector_3 = null;
	
	@Parameter( label= "Particle detector channel 4", required = false,
			direction= Direction.IN, dataIOOrder = -12, mode= ExpertMode.STANDARD,
			description = "Particle detector operator(s) for channels.")
	private ParticleDetector particleDetector_4 = null;
	
	@Parameter( label= "Particle detector channel 5", required = false,
			direction= Direction.IN, dataIOOrder = -11, mode= ExpertMode.STANDARD,
			description = "Particle detector operator(s) for channels.")
	private ParticleDetector particleDetector_5 = null;

	/**
	 * Optional mask to exclude particles in certain regions.
	 * <p>
	 * Particles in exclude regions are ignored if mask is non-null.
	 */
	@Parameter( label= "Region exclude mask", direction= Direction.IN,
			mode = ExpertMode.ADVANCED, required = false, 
			description = "Region exclude mask.")
	private transient MTBImageByte excludeMask = null;

	/**
	 * Units for measurements.
	 */
	@Parameter( label= "Measure units", required = false, dataIOOrder= 20,
			mode= ExpertMode.ADVANCED, direction= Direction.IN, 
			description = "Units for area measurements.")
  private MeasureUnit measureUnits = MeasureUnit.pixels;

	/**
	 * Stack with result overlays corresponding to channels.
	 */
	@Parameter( label= "Result image", direction= Direction.OUT,
			description = "Result image", supplemental= true)
	private transient MTBImageRGB resultImg = null;

	/**
	 * Mode of how to display result image.
	 */
	@Parameter( label= "Result image display mode", 
			required = false, mode= ExpertMode.ADVANCED, 
			direction= Direction.IN, supplemental = true,
			description = "Mode how result image is displayed.")
	private ResultImageMode resultDisplayMode = ResultImageMode.BINARY;	

	/*
	 * Some internal variables.
	 */
	
	/**
	 * Height of the processed image.
	 */
	private transient int height;

	/**
	 * Width of the processed image.
	 */
	private transient int width;
	
	/**
	 * Number of channels in image.
	 */
	private transient int stackSize;

	/**
	 * Empty constructor.
	 * @throws ALDOperatorException
	 */
	public MultiChannelParticleAnalyzer2D() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Default constructor.
	 * 
	 * @param image							Image to be processed.
	 * @param pOps							List of detectors.
	 * @throws ALDOperatorException
	 */
	public MultiChannelParticleAnalyzer2D(MTBImage image,
																					Vector<ParticleDetector> pOps) 
		throws ALDOperatorException {
		this.inImg = image;
		this.particleDetector_1 = pOps.get(0);
		this.particleDetector_2 = pOps.get(1);
		this.particleDetector_3 = pOps.get(2);
		this.particleDetector_4 = pOps.get(3);
		this.particleDetector_5 = pOps.get(4);
	}

	/**
	 * Specify input image.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Specify nuclei mask.
	 */
	public void setNucleiMask(MTBImageByte mask) {
		this.excludeMask = mask;
	}

	/**
	 * Specify units in which to measure areas.
	 */
	public void setMeasureUnits(MeasureUnit mu) {
		this.measureUnits = mu;
	}

	/**
	 * Returns extracted result data.
	 */
	public SegResult_ParticlesMultiChannel getResultDataArray() {
		return this.resultData;
	}

	/**
	 * Get a reference to configured detectors' vector.
	 * @return	Reference to vector of detectors.
	 */
	public Vector<ParticleDetector> getDetectors() {
		Vector<ParticleDetector> particleDetectors= new Vector<ParticleDetector>();
		particleDetectors.add(this.particleDetector_1);
		particleDetectors.add(this.particleDetector_2);
		particleDetectors.add(this.particleDetector_3);
		particleDetectors.add(this.particleDetector_4);
		particleDetectors.add(this.particleDetector_5);
		return particleDetectors;
	}
	
	/**
	 * Returns result image, i.e. the segmentation mask.
	 */
	public MTBImageRGB getResultImage() {
		return this.resultImg;
	}

	@Override
	protected void operate() throws ALDOperatorException,
	ALDProcessingDAGException {

		Vector<ParticleDetector> particleDetectors= this.getDetectors();
		
		// reset detector in case it is run multiple times
		this.resultData = null;
		this.resultImg = null;

		// stack/image info
		this.width = this.inImg.getSizeX();
		this.height = this.inImg.getSizeY();
  	this.stackSize = this.inImg.getSizeC();
  	
  	// result data object
  	this.resultData = 
  		new SegResult_ParticlesMultiChannel(this.inImg.getTitle());

		// now detect granules/p-bodies/etc. in all available channels
		MTBImageShort channelImg = null;
		for (int channel = 1; channel <= this.stackSize; ++channel) {

			// extract the image of the current channel
			channelImg = (MTBImageShort) MTBImage.createMTBImage(this.width,
					this.height, 1, 1, 1, MTBImage.MTBImageType.MTB_SHORT);
			for (int y = 0; y < this.height; ++y)
				for (int x = 0; x < this.width; ++x)
					channelImg.putValueInt(x,y,this.inImg.getValueInt(x,y,0,0,channel-1));

			ParticleDetector pOp = particleDetectors.get(channel-1);
			if (pOp instanceof ParticleDetectorUWT2D) {
				ParticleDetectorUWT2D dwtDetector = (ParticleDetectorUWT2D)pOp;
				dwtDetector.setInputImage(channelImg);
				if (this.excludeMask != null)
					dwtDetector.setExcludeMask(this.excludeMask);
				dwtDetector.runOp(false);
				// prepare result data, i.e. analyze and transform region set
				MTBRegion2DSet regions= dwtDetector.getResults();
				MTBImageByte pbodyMask= 
					(MTBImageByte)MTBImage.createMTBImage(channelImg.getSizeX(), 
							channelImg.getSizeY(),1, 1, 1, MTBImageType.MTB_BYTE);
				for (int y=0; y<channelImg.getSizeY();++y)
					for (int x=0;x<channelImg.getSizeX();++x)
						pbodyMask.putValueInt(x, y, 0);
				for (int i= 0; i<regions.size(); ++i) {
					MTBRegion2D r= regions.elementAt(i);
					for (Point2D.Double p: r.getPoints()) {
						pbodyMask.putValueInt((int)p.x, (int)p.y, 255);
					}
				}
				// do global statistics
				int particleCount = regions.size();
				double avgSize = regions.calcAverageSize();
				
				// extract image name
				String iname = this.inImg.getLocation();
				if (iname == null || iname.isEmpty())
					iname = this.inImg.getTitle();
				// provide a nice title for the mask
				pbodyMask.setTitle("Result for " + iname + " , channel " + channel);
				this.resultData.addSegmentationResult( 
						new SegResult_Particles(iname, 
								channel, regions, pbodyMask, particleCount, avgSize));
			}
			else if (pOp == null) {
				System.err.println("--> No detector, skipping channel " + channel);
			}
			else {
				System.err.println("Unknown particle detector!!!");
				System.out.println(pOp.toString());
			}
//			++opCounter;
		}
		// prepare output image
		if (this.resultDisplayMode != ResultImageMode.NONE)
			this.prepareResultImage();
	}
	
	/**
	 * Prepare visualization of result data by overlay on input image.
	 */
	private void prepareResultImage() {
		// vector of images from which lateron stack will be generated
		Vector<MTBImage> resultImages = new Vector<MTBImage>();
		
		// iterate over all channels
		int channel = 0;
		for (SegResult_Particles res: this.resultData.getResultVec()) {

			MTBImageRGB finalResult = (MTBImageRGB)MTBImage.createMTBImage(
					this.width, this.height, 1, 1, 1, MTBImage.MTBImageType.MTB_RGB);
				finalResult.fillBlack();
			
			channel++;
			// channels without result get a black image in output
			if (res == null) {
				resultImages.add(finalResult);
				continue;
			}
			// determine a suitable range for the output image
			double colorScale = 1.0;
			switch (this.inImg.getType())
			{
				case MTB_BYTE:
					// 8-bit image
					colorScale = 1.0;
					break;
				case MTB_SHORT:
					// 16-bit image, or only 12-bit?!
					colorScale = 256.0;
					if (this.inImg.getMinMaxDouble()[1] <= 4095)
						// not more than 12-bit used...
						colorScale = 16.0;
					break;
				default:
					// unknown, keep default...
			}
			// initialize result gray-scale image if requested
			if (this.resultDisplayMode == ResultImageMode.OVERLAY) {
				for (int y = 0; y < this.height; ++y) {
					for (int x = 0; x < this.width; ++x) {
						int color= 
								(int)(this.inImg.getValueInt(x,y,0,0,channel-1)/colorScale);
						finalResult.putValueR(x, y, color);
						finalResult.putValueG(x, y, color);
						finalResult.putValueB(x, y, color);
					}
				}
			}

			// paint result data into image
			for (int y = 0; y < this.height; ++y) {
				for (int x = 0; x < this.width; ++x) {
					if (this.resultDisplayMode == ResultImageMode.BINARY) { // 8-bit
						// structures in white
						if (res.getMask().getValueInt(x, y) > 0) {
							finalResult.putValueInt(x, y,
									((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (255 & 0xff));
						}
					}
					else { // structures in yellow
						if (res.getMask().getValueInt(x, y) > 0) {
							finalResult.putValueInt(x, y, 
									((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff));							
						}
					}
				}
			}
			// put final result on stack
			resultImages.add(finalResult);
		} // end of for-loop: all channels
		this.resultImg = this.prepareResultImageStack(resultImages);
	} 

	/**
	 * Initialize result image stack.
	 * <p>
	 * Transfers result images to a multi-channel RGB image. 
	 */
	private MTBImageRGB prepareResultImageStack(Vector<MTBImage> imgvec) {

		String imageName = this.inImg.getLocation();
		if (imageName == null || imageName.isEmpty())
			imageName = this.inImg.getImagePlus().getTitle();

		// set image title to current path, but if it is too long,
		// cut the path to the last three components
		String title = new String();
		String path = imageName;
		String[] pathParts = path.split("/");
		int parts = pathParts.length;
		if (parts > 4)
			title = ".../" + pathParts[parts - 3] + "/" + pathParts[parts - 2]
					+ "/" + pathParts[parts - 1];
		else
			title = path;

		// add prefix
		title = "Detection result for: " + title;
		
		// generate the stack
		MTBImageRGB mtbStack= 
				(MTBImageRGB)(MTBImage.createMTBImage(this.width, this.height, 1, 1,
						imgvec.size(), MTBImage.MTBImageType.MTB_RGB));
		mtbStack.setTitle(title);
		for (int i=0; i<imgvec.size(); ++i)
			mtbStack.setImagePart(imgvec.get(i), 0, 0, 0, 0, i);
		return mtbStack;
	}
}
