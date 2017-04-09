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

	public static enum SegmentType {
		INDENTATION,
		PROTRUSION		
	}
	
	MTBContour2D contour;
	int contourID;

	public LinkedList<InProContourSegment> protrusionSegs;
	public LinkedList<InProContourSegment> indentationSegs;

	LinkedList<InflectionPoint> inflections;

	int numberOfProtrusions;
	double avgEquatorProtrusionLength;
	double avgEquatorIndentationLength;
	double avgProtrusionLength;
	double avgBaselineProtrusionLength;
	double avgApicalProtrusionLength;
	double avgBasalProtrusionLength;
	double nonProtrusionArea;
	double avgDistsIndentationMidPoints;
	double minMinimalDistsIndentationMidPoints;
	double maxMinimalDistsIndentationMidPoints;

	double avgIndentationLength;
	double avgBaselineIndentationLength;
	double avgApicalIndentationLength;
	double avgBasalIndentationLength;

	public class InProContourSegment {

		MorphologyAnalyzer2DInProData.SegmentType type;

		public LinkedList<Point2D.Double> segPoints;

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