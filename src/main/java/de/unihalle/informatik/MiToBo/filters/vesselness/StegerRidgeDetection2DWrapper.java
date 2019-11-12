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
 * Note that the method drawResultsToImage(...) in this class has been adapted
 * from the original RidgeDetector plugin code, in particular from the method
 * de.biomedical_imaging.ij.steger.Lines_#displayContours(...) in that package.
 * 
 * Here is the original copyright notice for that file:
 * 
 * #%L
 * Ridge Detection plugin for ImageJ
 * %%
 * Copyright (C) 2014 - 2015 Thorsten Wagner (ImageJ java plugin), 1996-1998 Carsten Steger (original C code), 1999 R. Balasubramanian (detect lines code to incorporate within GRASP)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */ 

package de.unihalle.informatik.MiToBo.filters.vesselness;

import java.awt.Color;
import java.awt.Font;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.biomedical_imaging.ij.steger.Junctions;
import de.biomedical_imaging.ij.steger.Line;
import de.biomedical_imaging.ij.steger.LineDetector;
import de.biomedical_imaging.ij.steger.Lines;
import de.biomedical_imaging.ij.steger.OverlapOption;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.process.FloatPolygon;

/**
 * Wrapper for Ridge Detection plugin by Carsten Steger (and Thorsten Wagner).
 * <p>
 * Related publication:
 * <ul>
 * <li> C. Steger, <i>An unbiased detector of curvilinear structures</i>, in  
 * IEEE Transactions on Pattern Analysis and Machine Intelligence, 
 * 20(2), pp.113–125, 1998
 * </ul>
 * <p>
 * For details on parameters and functionality take a look at ...
 * <ul>
 * <li> the website of the corresponding ImageJ plugin: 
 * 			<a href="http://imagej.net/Ridge_Detection">
 * 			         http://imagej.net/Ridge_Detection</a>
 * <li> the Github page with the source code: 
 * 			<a href="https://github.com/thorstenwagner/ij-ridgedetection">
 *               https://github.com/thorstenwagner/ij-ridgedetection</a>
 * </ul>
 *
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, 
	level=Level.APPLICATION, allowBatchMode=false)
public class StegerRidgeDetection2DWrapper extends MTBOperator 
	implements StatusReporter {

	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = "[StegerRidgeDetector] ";

	/**
	 * Input image to be processed.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Input image.")
	protected transient MTBImageByte inImg = null;

	/**
	 * Sigma value of derivatives depending on line width.
	 */
	@Parameter( label= "Sigma", required = true, dataIOOrder = 1,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Sigma of derivatives.")
	protected double sigma = 1.08;

	/**
	 * Lower threshold for filter responses.
	 */
	@Parameter( label= "Lower Threshold", required = true, dataIOOrder = 2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Lower threshold for filter responses.")
	protected double lowerThresh = 1.36;

	/**
	 * Pixels with a response above this threshold are immidiately accepted.
	 */
	@Parameter( label= "Upper Threshold", required = true, dataIOOrder = 3,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Upper threshold for filter responses.")
	protected double upperThresh = 6.29;

	/**
	 * Minimal line length.
	 */
	@Parameter( label= "Minimum Line Length", required = true, dataIOOrder = 4,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Minimum line length.")
	protected double minLineLength = 0.0;

	/**
	 * Maximal line length.
	 */
	@Parameter( label= "Maximum Line Length", required = true, dataIOOrder = 5,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Maximum line length.")
	protected double maxLineLength = 0.0;

	/*
	 * Optional parameters.
	 */
	
	/**
	 * Line width.
	 */
	@Parameter( label= "Line Width", required = false, dataIOOrder = 1,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Line width.", callback = "callBackUpdateSigmaThresholds", 
	  paramModificationMode=ParameterModificationMode.MODIFIES_VALUES_ONLY)
	protected double lineWidth = 2.0;

	/**
	 *  Lowest grayscale value of the line.
	 */
	@Parameter( label= "Low Contrast", required = false, dataIOOrder = 2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Low contrast.", callback = "callBackUpdateSigmaThresholds", 
	  paramModificationMode=ParameterModificationMode.MODIFIES_VALUES_ONLY)
	protected double lowContrast = 20;

	/**
	 * Highest grayscale value of the line.
	 */
	@Parameter( label= "High Contrast", required = false, dataIOOrder = 3,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "High contrast.", callback = "callBackUpdateSigmaThresholds", 
	  paramModificationMode=ParameterModificationMode.MODIFIES_VALUES_ONLY)
	protected double highContrast = 90;

	/**
	 * If true, dark lines on bright background are extracted.
	 */
	@Parameter( label= "Dark Lines on Bright Background?", required = false, 
		dataIOOrder = 4, direction= Parameter.Direction.IN, 
		mode=ExpertMode.STANDARD, description = "Dark lines on bright background.")
	protected boolean darkLine = false;

	/**
	 * Correct the line position if it has different contrast on each side of it. 
	 */
	@Parameter( label= "Correct Position?", required = false, dataIOOrder = 5,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Line positions are corrected.")
	protected boolean correctPosition = false;

	/**
	 * If true the widths of the lines are estimated.
	 */
	@Parameter( label= "Estimate Width?", required = false, dataIOOrder = 6,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "If true widths of lines are estimated.")
	protected boolean estimateWidth = false;

	/**
	 * If true lines are extended.
	 */
	@Parameter( label= "Extend Lines?", required = false, dataIOOrder = 7,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "If true lines are extended.")
	protected boolean extendLine = false;

	/**
	 * Mode for how to resolve overlaps of lines.
	 */
	@Parameter( label= "Overlap Resolution Mode", required= false, dataIOOrder= 8,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Mode for resolving overlaps of lines.")
	protected OverlapOption overlapOpt = OverlapOption.NONE;

	/*
	 * Result parameters.
	 */
	
	/**
	 * Output image.
	 */
	@Parameter( label= "Result image", dataIOOrder = 3, 
		direction=Parameter.Direction.OUT, description = "Result image.")
	private transient MTBImageRGB resultImage = null;	

	/*
	 * Not automatically accessible parameters.
	 */
	
	/**
	 * Detected lines.
	 */
	private transient Lines resultLines = null;

	/**
	 * Detected junction points.
	 */
	private transient Junctions resultJunctions = null;
	
	/** 
	 * Vector of installed StatusListeners.
	 */
	protected transient Vector<StatusListener> statusListeners;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public StegerRidgeDetection2DWrapper() throws ALDOperatorException {
		super();
		this.statusListeners = new Vector<StatusListener>(1);
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#validateCustom()
	 */
	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.lowerThresh >= this.upperThresh)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				opIdentifier + "lower threshold larger than upper threshold!");
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() {
		
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();
		
		// detect ridge lines
		LineDetector detect = new LineDetector();
		this.resultLines = 
			detect.detectLines(this.inImg.getImagePlus().getProcessor(), 
				this.sigma, this.upperThresh, this.lowerThresh, this.minLineLength, 
					this.maxLineLength, this.darkLine, this.correctPosition, 
						this.estimateWidth, this.extendLine, this.overlapOpt);
		this.resultJunctions = detect.getJunctions();
		
		this.resultImage = (MTBImageRGB)MTBImage.createMTBImage(
			width, height, 1, 1, 1, MTBImageType.MTB_RGB);
		
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				this.resultImage.putValueR(x, y, this.inImg.getValueInt(x, y));
				this.resultImage.putValueG(x, y, this.inImg.getValueInt(x, y));
				this.resultImage.putValueB(x, y, this.inImg.getValueInt(x, y));
			}
		}
		
		drawResultsToImage(this.resultImage, this.resultLines, null, this.estimateWidth, false);
	}
	
	/**
	 * Specify input image.
	 * @param inputImage	Input image to process.
	 */
	public void setInputImage(MTBImageByte inputImage) {
		this.inImg = inputImage;
	}
	
	/**
	 * Specify sigma value.
	 * @param s	Sigma value.
	 */
	public void setSigma(double s) {
		this.sigma = s;
	}

	/**
	 * Specify lower threshold.
	 * @param lowerT 	Lower threshold to apply.
	 */
	public void setLowerThresh(double lowerT) {
		this.lowerThresh = lowerT;
	}

	/**
	 * Specify upper threshold.
	 * @param upperT 	Upper threshold to apply.
	 */
	public void setUpperThresh(double upperT) {
		this.upperThresh = upperT;
	}

	/**
	 * Specify minimal line length.
	 * @param minLength 	The mininmal line length to apply.
	 */
	public void setMinLineLength(double minLength) {
		this.minLineLength = minLength;
	}

	/**
	 * Specify maximal line length.
	 * @param maxLength 	The maximal line length to apply.
	 */
	public void setMaxLineLength(double maxLength) {
		this.maxLineLength = maxLength;
	}

	/**
	 * Specify line width.
	 * @param w	Line width to apply.
	 */
	public void setLineWidth(double w) {
		this.lineWidth = w;
	}

	/**
	 * Specify high contrast.
	 * @param highC	High contrast value to apply.
	 */
	public void setHighContrast(double highC) {
		this.highContrast = highC;
	}

	/**
	 * Specify low contrast.
	 * @param lowC	Low contrast value to apply.
	 */
	public void setLowContrast(double lowC) {
		this.lowContrast = lowC;
	}

	/**
	 * Specify if to detect dark lines on bright background.
	 * @param dark 	If true dark lines on bright background are assumed.
	 */
	public void setDarkLine(boolean dark) {
		this.darkLine = dark;
	}

	/**
	 * Specify if to correct line positions.
	 * @param cp 	If true positions will be corrected.
	 */
	public void setCorrectPosition(boolean cp) {
		this.correctPosition = cp;
	}

	/**
	 * Specify if to estimate width.
	 * @param ew	If true line width will be estimated.
	 */
	public void setEstimateWidth(boolean ew) {
		this.estimateWidth = ew;
	}

	/**
	 * Specify if to extend lines.
	 * @param el	If true lines will be extended.
	 */
	public void setExtendLine(boolean el) {
		this.extendLine = el;
	}

	/**
	 * Specify overlap handling mode.
	 * @param overlapOption 	The option to use for resolving overlaps.
	 */
	public void setOverlapOpt(OverlapOption overlapOption) {
		this.overlapOpt = overlapOption;
	}

	/**
	 * Get detected lines.
	 * @return	Set of detected lines.
	 */
	public Lines getResultLines() {
		return this.resultLines;
	}
	
	/**
	 * Get detected junctions.
	 * @return	Set of detected junctions.
	 */
	public Junctions getResultJunctions() {
		return this.resultJunctions;
	}
	
	/**
	 * Calculates values for mandatory parameters {@link #sigma}, 
	 * {@link #upperThresh} and {@link #lowerThresh} from optional parameters
	 * {@link #lineWidth}, {@link #highContrast} and {@link #lowContrast}
	 * in case that one of the values changed.
	 * 
	 * @throws ALDOperatorException	Thrown in case of failure. 
	 */
	@SuppressWarnings("unused")
	private void callBackUpdateSigmaThresholds() throws ALDOperatorException {
		
		// calculations are performed according to code in Lines_.java,
		// lines 802 to 827
		double estimatedSigma = this.lineWidth / (2 * Math.sqrt(3)) + 0.5;

		double clow = this.lowContrast;
		if (this.darkLine) {
			clow = 255 - this.highContrast;
		}
		double estimatedLowerThresh = Math.floor( Math.abs(
				-2 * clow * (this.lineWidth / 2.0) 
			/ (Math.sqrt(2*Math.PI) * estimatedSigma*estimatedSigma*estimatedSigma)
			* Math.exp(  -((this.lineWidth / 2.0) * (this.lineWidth / 2.0)) 
									/ (2 * estimatedSigma * estimatedSigma) )));
		estimatedLowerThresh *= 0.17;
		
		double chigh = this.highContrast;
		if (this.darkLine) {
			chigh = 255 - this.lowContrast;
		}
		double estimatedUpperThresh = Math.floor( Math.abs(
				-2 * chigh * (this.lineWidth / 2.0)
			/ (Math.sqrt(2*Math.PI) * estimatedSigma*estimatedSigma*estimatedSigma)
			* Math.exp(  -((this.lineWidth / 2.0) * (this.lineWidth / 2.0)) 
					        / (2 * estimatedSigma * estimatedSigma) )));
		estimatedUpperThresh *= 0.17;
		
		this.setParameter("sigma", new Double(estimatedSigma));
		this.setParameter("lowerThresh", new Double(estimatedLowerThresh)); 
		this.setParameter("upperThresh", new Double(estimatedUpperThresh));
	}
	
	/**
	 * Draw results to given image.
	 * <p>
	 * This code is adapted from the method 
	 * {@link de.biomedical_imaging.ij.steger.Lines_#displayContours}.
	 * 
	 * @param image				Image to which to draw the lines.
	 * @param lines				Lines to draw.
	 * @param junctions		If not null, junction points to draw
	 * @param drawWidth		If true, width of lines is also drawn.
	 * @param showIDs			If true, IDs are drawn to image.
	 */
	public static void drawResultsToImage(MTBImageRGB image, 
			Lines lines, Junctions junctions, boolean drawWidth, boolean showIDs) {
		
		ImagePlus imp = image.getImagePlus();
		
		imp.setOverlay(null);
		Overlay ovpoly = new Overlay();

		double px, py, nx, ny, px_r = 0, py_r = 0, px_l = 0, py_l = 0;
		double last_w_r, last_w_l;

		for (int i = 0; i < lines.size(); i++) {
			FloatPolygon polyMitte = new FloatPolygon();

			FloatPolygon polyR = new FloatPolygon();
			FloatPolygon polyL = new FloatPolygon();
			Line cont = lines.get(i);
			int num_points = cont.getNumber();
			last_w_r = 0;
			last_w_l = 0;

			for (int j = 0; j < num_points; j++) {

				px = cont.getXCoordinates()[j];
				py = cont.getYCoordinates()[j];
				nx = Math.sin(cont.getAngle()[j]);
				ny = Math.cos(cont.getAngle()[j]);
				if (drawWidth) {
					px_r = px + cont.getLineWidthR()[j] * nx;
					py_r = py + cont.getLineWidthR()[j] * ny;
					px_l = px - cont.getLineWidthL()[j] * nx;
					py_l = py - cont.getLineWidthL()[j] * ny;
				}

				polyMitte.addPoint((px + 0.5), (py + 0.5));
				if (drawWidth) {
					if (last_w_r > 0 && cont.getLineWidthR()[j] > 0) {
						polyR.addPoint((px_r + 0.5), (py_r + 0.5));
					}
					if (last_w_l > 0 && cont.getLineWidthL()[j] > 0) {
						polyL.addPoint((px_l + 0.5), (py_l + 0.5));
					}
				}
				if (drawWidth) {
					last_w_r = cont.getLineWidthR()[j];
					last_w_l = cont.getLineWidthL()[j];
				}
			}

			PolygonRoi polyRoiMitte = new PolygonRoi(polyMitte, Roi.POLYLINE);

			polyRoiMitte.setStrokeColor(Color.red);
			polyRoiMitte.setPosition(0);
			ovpoly.add(polyRoiMitte);

			if (drawWidth) {
				if (polyL.npoints > 1) {
					PolygonRoi polyRoiRand1 = new PolygonRoi(polyL, Roi.POLYLINE);
					polyRoiRand1.setStrokeColor(Color.green);
					polyRoiRand1.setPosition(0);
					ovpoly.add(polyRoiRand1);

					PolygonRoi polyRoiRand2 = new PolygonRoi(polyR, Roi.POLYLINE);
					polyRoiRand2.setStrokeColor(Color.green);
					polyRoiRand2.setPosition(0);
					ovpoly.add(polyRoiRand2);
				}
			}

			// Show IDs
			if (showIDs) {/*
			 * int posx = polyMitte.xpoints[0]; int posy = polyMitte.ypoints[0];
			 * if(cont.cont_class == contour_class.cont_start_junc){ posx =
			 * polyMitte.xpoints[polyMitte.npoints-1]; posy =
			 * polyMitte.ypoints[polyMitte.npoints-1]; }
			 */

				int posx = (int) polyMitte.xpoints[polyMitte.npoints / 2];
				int posy = (int) polyMitte.ypoints[polyMitte.npoints / 2];
				TextRoi tr = new TextRoi(posx, posy, "" + cont.getID());
				tr.setCurrentFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
				tr.setIgnoreClipRect(true);
				tr.setStrokeColor(Color.orange);
				tr.setPosition(0);
				ovpoly.add(tr);
			}

		}
		if (junctions != null) {

			FloatPolygon pointpoly = new FloatPolygon();
			for (int i = 0; i < junctions.size(); i++) {
				pointpoly.addPoint(junctions.get(i).getX() + 0.5, 
						junctions.get(i).getY() + 0.5);
			}

			PointRoi pointroi = new PointRoi(pointpoly);
			pointroi.setShowLabels(false);
			pointroi.setPosition(0);
			ovpoly.add(pointroi);
		}
		if (ovpoly.size() > 0) {
			imp.setOverlay(ovpoly);
		}

	}
	
	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#addStatusListener(loci.common.StatusListener)
	 */
	@Override
	public void addStatusListener(StatusListener statuslistener) {
		this.statusListeners.add(statuslistener);
	}

	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#notifyListeners(loci.common.StatusEvent)
	 */
	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.statusListeners.size(); i++) {
			this.statusListeners.get(i).statusUpdated(e);
		}
	}

	/* (non-Javadoc)
	 * @see loci.common.StatusReporter#removeStatusListener(loci.common.StatusListener)
	 */
	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.statusListeners.remove(statuslistener);
	}
}

