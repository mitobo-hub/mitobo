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

	/**
	 * Number of detected protrusions along contour.
	 */
	int numberOfProtrusions;

	/**
	 * Average length of protrusion equators.
	 */
	double avgEquatorProtrusionLength;

	/**
	 * Average length of indentation equators.
	 */
	double avgEquatorIndentationLength;
	
	/**
	 * Average length of protrusions.
	 */
	double avgProtrusionLength;
	
	/**
	 * Average length of protrusion baselines.
	 */
	double avgBaselineProtrusionLength;
	
	/**
	 * Average of apical protrusion lengths.
	 */
	double avgApicalProtrusionLength;
	
	/**
	 * Average of basal protrusion lengths.
	 */
	double avgBasalProtrusionLength;
	
	/**
	 * Non-protrusion area in cell region.
	 */
	double nonProtrusionArea;
	
	/**
	 * Average length of indentations.
	 */
	double avgIndentationLength;

	/**
	 * Average length of indentations baselines.
	 */
	double avgBaselineIndentationLength;
	
	/**
	 * Average of apical indentations lengths.
	 */
	double avgApicalIndentationLength;

	/**
	 * Average of basal indentation lengths.
	 */
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
		
		/**
		 * Initial border points of the segment (before any shift takes place).
		 */
		public LinkedList<Point2D.Double> initialSegmentPoints;

		/**
		 * Length of segment measured as number of pixels.
		 */
		int segLength;

		/**
		 * Index of segment start pixel on contour.
		 */
		int startPosOnContour;

		/**
		 * Index of segment end pixel on contour.
		 */
		int endPosOnContour;

		/**
		 * Median point of contour.
		 */
		Point2D.Double midPoint;

		/**
		 * Index of median segment pixel on contour.
		 */
		int midPointPosOnContour;

		/**
		 * Length of the equator in pixels.
		 * <p>
		 * If length is smaller than 0 there are no inflection points along segment.
		 */
		double equatorLength;

		/**
		 * Left boundary point of segment after potential shifts.
		 */
		Point2D.Double leftBorderPoint;

		/**
		 * Index of left boundary point of segment after potential shifts.
		 */
		int leftBorderPointPosOnContour;

		/**
		 * Right boundary point of segment after potential shifts.
		 */
		Point2D.Double rightBorderPoint;

		/**
		 * Index of right boundary point of segment after potential shifts.
		 */
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
		
		/**
		 * Get equator length.
		 * @return	Length of equator.
		 */
		public double getEquatorLength() {
			return this.equatorLength;
		}

		/**
		 * Get baseline length.
		 * @return	Length of baseline.
		 */
		public double getBaselineLength() {
			return this.baselineLength;
		}

		/**
		 * Get apical length.
		 * @return	Apical length.
		 */
		public double getApicalLength() {
			return this.apicalLength;
		}

		/**
		 * Get basal length.
		 * @return	Basal length.
		 */
		public double getBasalLength() {
			return this.basalLength;
		}

		/**
		 * Get total length.
		 * @return	Total length.
		 */
		public double getTotalLength() {
			return this.totalLength;
		}
	}

	/**
	 * Class to represent inflection points.
	 */
	public class InflectionPoint extends Point2D.Double {

		/**
		 * Type of segment starting with this point.
		 */
		MorphologyAnalyzer2DInProData.SegmentType type;

		/**
		 * Default constructor.
		 * @param xx	x-coordinate of point.
		 * @param yy  y-coordinate of point.
		 * @param t		Type of segment.
		 */
		public InflectionPoint(double xx, double yy, 
				MorphologyAnalyzer2DInProData.SegmentType t) {
			super(xx,yy);
			this.type = t;
		}
	}
}