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

package de.unihalle.informatik.MiToBo.features;

import java.awt.geom.Point2D;
import java.util.LinkedList;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;

/**
 * Data type for protrusion/indentation analysis of region contours.
 * 
 * @author Birgit Moeller
 */
public class MorphologyAnalyzer2DInProData {

	/**
	 * Type of segment to distinguish between indentation and protrusions. 
	 */
	public static enum SegmentType {
		/**
		 * Indentation, i.e. a convex segment along a region's contour.
		 */
		INDENTATION,
		/**
		 * Protrusion, i.e. a concave segment along a region's contour.
		 */
		PROTRUSION		
	}
	
	/**
	 * Analyzed region contour.
	 */
	private MTBContour2D contour;
	
	/**
	 * ID of the contour.
	 */
	private int contourID;

	/**
	 * List of protrusion segments along the contour.
	 */
	private LinkedList<InProContourSegment> protrusionSegs;
	
	/**
	 * List of indentation segments along the contour.
	 */
	private LinkedList<InProContourSegment> indentationSegs;

	/**
	 * List of inflection points along the contour.
	 */
	private LinkedList<InflectionPoint> inflectionPoints;

	int numberOfProtrusions;
	double avgEquatorProtrusionLength;
	double avgEquatorIndentationLength;
	double avgProtrusionLength;
	double avgBaselineProtrusionLength;
	double avgApicalProtrusionLength;
	double avgBasalProtrusionLength;
	double nonProtrusionArea;
//	double avgDistsIndentationMidPoints;
//	double minMinimalDistsIndentationMidPoints;
//	double maxMinimalDistsIndentationMidPoints;

	double avgIndentationLength;
	double avgBaselineIndentationLength;
	double avgApicalIndentationLength;
	double avgBasalIndentationLength;

	/**
	 * Default constructor.
	 * @param c		Contour.
	 * @param id	ID of contour.
	 */
	public MorphologyAnalyzer2DInProData(MTBContour2D c, int id) {
		this.contour = c;
		this.contourID = id;
	}
	
	/**
	 * Add list of indentation segments to object.
	 * @param is	List of segments.
	 */
	public void addIndentationSegments(LinkedList<InProContourSegment> is) {
		this.indentationSegs = is;
	}
	
	/**
	 * Add list of protrusion segments to object.
	 * @param ps	List of segments.
	 */
	public void addProtrusionSegments(LinkedList<InProContourSegment> ps) {
		this.protrusionSegs = ps;
	}

	/**
	 * Add list of inflection points to object.
	 * @param ip	List of points.
	 */
	public void addInflectionPoints(LinkedList<InflectionPoint> ip) {
		this.inflectionPoints = ip;
	}

	/**
	 * Get a reference to the contour.
	 * @return	Reference to contour.
	 */
	public MTBContour2D getContour() {
		return this.contour;
	}
	
	/**
	 * Get the contour ID.
	 * @return	ID of contour.
	 */
	public int getContourID() {
		return this.contourID;
	}
	
	/**
	 * Get list of indentation segments.
	 * @return	List of segments.
	 */
	public LinkedList<InProContourSegment> getIndentationSegments() {
		return this.indentationSegs;
	}
	
	/**
	 * Get list of protrusion segments.
	 * @return	List of segments.
	 */
	public LinkedList<InProContourSegment> getProtrusionSegments() {
		return this.protrusionSegs;
	}

	/**
	 * Get list of inflection points.
	 * @return	List of points.
	 */
	public LinkedList<InflectionPoint> getInflectionPoints() {
		return this.inflectionPoints;
	}
	
	/**
	 * Class to represent concave/convex segments along a contour.
	 */
	public class InProContourSegment {

		/**
		 * Type of segment.
		 * <p>
		 * Intrusions refer to convexities along the contour, protrusions
		 * refer to concavities.
		 */
		MorphologyAnalyzer2DInProData.SegmentType type;

		/**
		 * Reference to the preceeding segment along the contour.
		 */
		protected InProContourSegment prevSegment; 
		
		/**
		 * Reference to the subsequent segment along the contour.
		 */
		protected InProContourSegment nextSegment;
		
		public LinkedList<Point2D.Double> initialSegmentPoints;

		int segLength;

		int startPosOnContour;

		int endPosOnContour;

		Point2D.Double midPoint;

		int midPointPosOnContour;

		// if < 0 , then there are no inflection points along segment
		double equatorLength;

		Point2D.Double leftBorderPoint;
		int leftBorderPointPosOnContour;

		Point2D.Double rightBorderPoint;
		int rightBorderPointPosOnContour;
		
		/**
		 * Length of segment baseline.
		 */
		protected double baselineLength;
		
		/**
		 * Apical length of the segment, from equator to top.
		 */
		protected double apicalLength;
		
		/**
		 * Basal length of the segment, from baseline to equator.
		 */
		protected double basalLength;

		/**
		 * Total length of indentation or protrusion, i.e., equals the sum of 
		 * {@link #basalLength} and {@link #apicalLength}.
		 */
		protected double totalLength;
//		
//		double perimeter;
						
//		void setBaselineLength(double l) {
//			this.baselineLength = l;
//		}
//
//		void setApicalLength(double l) {
//			this.apicalLength = l;
//		}
//
//		void setbasalLength(double l) {
//			this.basalLength = l;
//		}
//
//		void setTotalLength(double l) {
//			this.totalLength = l;
//		}

		/**
		 * Get preceeding segment along contour.
		 * <p>
		 * Note that the preceeding segment is always of complementary type
		 * (see {@link MorphologyAnalyzer2DInProData.SegmentType}).
		 * 
		 * @return Preceeding segment.
		 */
		public InProContourSegment getPrecursorSegment() {
			return this.prevSegment;
		}
		
		/**
		 * Get subsequent segment along contour.
		 * <p>
		 * Note that the subsequent segment is always of complementary type
		 * (see {@link MorphologyAnalyzer2DInProData.SegmentType}).
		 * 
		 * @return Subsequent segment.
		 */
		public InProContourSegment getSuccessorSegment() {
			return this.nextSegment;			
		}
		
		public double getEquatorLength() {
			return this.equatorLength;
		}

		public double getBaselineLength() {
			return this.baselineLength;
		}

		public double getApicalLength() {
			return this.apicalLength;
		}

		public double getBasalLength() {
			return this.basalLength;
		}

		public double getTotalLength() {
			return this.totalLength;
		}
	}

	public class InflectionPoint extends Point2D.Double {

		public InflectionPoint(double xx, double yy, 
				MorphologyAnalyzer2DInProData.SegmentType t) {
			super(xx,yy);
			this.type = t;
		}

		MorphologyAnalyzer2DInProData.SegmentType type;

	}
}