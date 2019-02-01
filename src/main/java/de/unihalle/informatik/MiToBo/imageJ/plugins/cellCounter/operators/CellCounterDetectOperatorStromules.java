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

package de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.operators;

import java.awt.geom.Point2D;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.apps.plantCells.stromules.StromulesDetector2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.filters.vesselness.StegerRidgeDetection2DWrapper;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarker;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShape;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShapePolygon;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerShapeRegion;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.datatypes.CellCntrMarkerVector;
import de.unihalle.informatik.MiToBo.imageJ.plugins.cellCounter.operators.CellCounterDetectOperator;

/**
 * Detector wrapper for detection of stromules from given plastid regions. 
 * <p>
 * This wrapper makes the stromule detector functionality of the
 * {@link StromulesDetector2D} accessible from within the 
 * <a href="http://mitobo.informatik.uni-halle.de/index.php/Applications/MTBCellCounter">
 * MTBCellCounter plugin</a> - a plugin for semi-automatic counting of small,
 * mainly spot-like structures in microscopy images.
 * <p>
 * For further details on the plugin and the methodology of the detector refer 
 * to 
 * <ul>
 * <li> B. Möller and M. Schattat, <i>Quantification of Stromule Frequencies 
 * in Microscope Images of Plastids combining Ridge Detection and Geometric 
 * Criteria</i>, <br>in Proc. of 6th Int. Conf. on Bioimaging (BIOIMAGING '19), 
 * Prague, Czech Republic, February 2019.
 * <li> L. Franke, B. Storbeck, J. L. Erickson, D. Rödel, D. Schröter, 
 * B. Möller, and M. H. Schattat, <i>The 'MTB Cell Counter' a versatile tool 
 * for the semi-automated quantification of sub-cellular phenotypes in 
 * fluorescence microscopy images. A case study on plastids, nuclei and 
 * peroxisomes</i>, in Journal of Endocytobiosis and Cell Research, 
 * 26:31-42, 2015.
 * </ul>
 *  
 * @author Birgit Moeller
 * @see StegerRidgeDetection2DWrapper
 * @see StromulesDetector2D
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.NONE)
@ALDDerivedClass
public class CellCounterDetectOperatorStromules 
		extends CellCounterDetectOperator {
	
	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = 
			"[CellCounterDetectOperatorStromules] ";

	/**
	 * Type of plastid markers.
	 */
	@Parameter(label = "Plastid Marker Type", required = true, dataIOOrder = 1,
		direction = Parameter.Direction.IN, description = "Plastid marker type.")
	private int plastidMarkerType = 1;
	
	/**
	 *  Enable/disable line multi-intersection check.
	 */
	@Parameter( label= "Apply line multi-intersection check?", 
		required = true, dataIOOrder = 11,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Checks if a potential stromuli line intersects a region " 
	  	+ "at least twice, then it might be a reflection")
	protected boolean useMultiIntersectionCheck = false;

	/**
	 *  Enable/disable ellipse distance threshold.
	 */
	@Parameter( label= "Apply ellipse distance threshold?", 
		required = true, dataIOOrder = 12,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Use Ellipse distance threshold.")
	protected boolean useEllipseDistThreshold = false;
	
	/**
	 * Maximal distance between contact points along ellipse contour.
	 */
	@Parameter(label = "Ellipse distance threshold", required = true, 
		dataIOOrder = 13, direction = Parameter.Direction.IN, 
		description = "Ellipse distance.")
	private double ellipseDistThresh = 3.0;

	/**
	 *  Enable/disable angle criterion.
	 */
	@Parameter( label= "Apply angle criterion?", 
		required = true, dataIOOrder = 15,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Apply stromuli angle criterion.")
	protected boolean useAngleCriterion = true;

	/**
	 *  Stromuli orientation angle criterion.
	 */
	@Parameter( label= "Stromuli angle threshold", required = true, 
		dataIOOrder = 16,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Stromuli-tangent angle threshold (in degrees).")
	protected double stromuliAngleThreshold = 60.0;
	
	/**
	 *  Show additional result images.
	 */
	@Parameter( label= "Show additional intermediate results?", 
		dataIOOrder = 0, supplemental = true,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Enables display of additional result images.")
	protected boolean showAdditionalResults = false;

	/**
	 * Operator for stromuli detection.
	 */
	private StromulesDetector2D stromuliOp;
	
	/**
	 * Constructor.	
	 * @throws ALDOperatorException Thrown in case of initialization error.
	 */
	public CellCounterDetectOperatorStromules() 
			throws ALDOperatorException {
		this.stromuliOp = new StromulesDetector2D();
		this.m_statusListeners = new Vector<StatusListener>(1);
	}

	@Override
  protected void operate() 
  		throws ALDOperatorException, ALDProcessingDAGException {
		
		// post ImageJ status
		String msg = opIdentifier + "running stromules detection...";	
		this.notifyListeners(new StatusEvent(msg));

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "running stromules detection...");

		int xSize = this.inputImage.getSizeX();
		int ySize = this.inputImage.getSizeY();
		int zSize = this.inputImage.getSizeZ();
		int tSize = this.inputImage.getSizeT();
		int cSize = this.inputImage.getSizeC();
		
		// get plastid regions from CellCounter...
		MTBRegion2DSet plastidRegions = new MTBRegion2DSet();
		CellCntrMarkerVector pm = 
				this.markerVects.get(new Integer(this.plastidMarkerType)); 
		
		// check if marker vector exists
		if (pm == null || pm.isEmpty()) {
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
				opIdentifier + "no markers of type " + this.plastidMarkerType 
					+ " found, cannot continue without plastid regions, exiting...");
		}
		
		for (CellCntrMarker c: pm) {
			CellCntrMarkerShape s = c.getShape();
			if (s != null && s instanceof CellCntrMarkerShapeRegion) {
				plastidRegions.add(((CellCntrMarkerShapeRegion)s).getRegion());
			}
			else if (s != null && s instanceof CellCntrMarkerShapePolygon) {
				int[][] polyMask = 
					((CellCntrMarkerShapePolygon)s).getPolygon().getBinaryMask(
							xSize, ySize, true);
				Vector<Point2D.Double> ps = new Vector<>();
				for (int y=0; y<ySize; ++y) {
					for (int x=0; x<xSize; ++x) {
						if (polyMask[y][x] > 0)
							ps.add(new Point2D.Double(x, y));
					}
				}
				plastidRegions.add(new MTBRegion2D(ps));
			}
		}
		
		// create binary mask of plastid regions
		MTBImageByte plastidMask = (MTBImageByte)MTBImage.createMTBImage(
				xSize, ySize, zSize, tSize, cSize, MTBImageType.MTB_BYTE);
		for (MTBRegion2D reg: plastidRegions) {
			for (Point2D.Double p: reg.getPoints()) {
				plastidMask.putValueInt((int)p.x, (int)p.y, 255);
			}
		}
		
		// run the stromuli detector
		this.stromuliOp.setParameter("inImg", this.inputImage);
		this.stromuliOp.setParameter("plastidMask", plastidMask);
		this.stromuliOp.setParameter("plastidRegions", plastidRegions);
		this.stromuliOp.setParameter("useMultiIntersectionCheck",
				new Boolean(this.useMultiIntersectionCheck));
		this.stromuliOp.setParameter("useEllipseDistThreshold", 
				new Boolean(this.useEllipseDistThreshold));
		this.stromuliOp.setParameter("ellipseDistThresh",
				new Double(this.ellipseDistThresh));
		this.stromuliOp.setParameter("useAngleCriterion", 
				new Boolean(this.useAngleCriterion));
		this.stromuliOp.setParameter("stromuliAngleThreshold",
				new Double(this.stromuliAngleThreshold));
		this.stromuliOp.setParameter("showAdditionalResults", 
				new Boolean(this.showAdditionalResults));
		this.stromuliOp.addStatusListener(this);
		this.stromuliOp.runOp();
		MTBRegion2DSet resultStromuliRegions = 
				(MTBRegion2DSet)this.stromuliOp.getParameter("stromuliRegions");
		int resultStromuliCount = resultStromuliRegions.size();

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + 
				"\t -> Number of detected stromuli: " + resultStromuliCount);

		// format results
		Vector<CellCntrMarker> markers = new Vector<>();
		for (MTBRegion2D reg: resultStromuliRegions) {
			CellCntrMarkerShape s = new CellCntrMarkerShapeRegion(reg);
			s.setAvgIntensity(Double.NaN);
			CellCntrMarker marker = new CellCntrMarker(
				(int)reg.getCenterOfMass_X(), (int)reg.getCenterOfMass_Y(),
					this.detectZSlice, s);
			markers.add(marker);
		}
		this.detectResults = markers;
		
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "Operations finished!");
		
		// post ImageJ status
		msg = opIdentifier + "calculations completed!";	
		this.notifyListeners(new StatusEvent(msg));
  }
	
	@Override
	public String getShortName() {
		return "Stromules (plastid regions required!)";
	}
	
	@Override
	public String getUniqueClassIdentifier() {
		return "StromulesRidgeDetect";
	}
	
}
