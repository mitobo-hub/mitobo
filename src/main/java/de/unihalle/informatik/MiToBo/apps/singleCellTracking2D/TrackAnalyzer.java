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

package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;


import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;

/**
 * class for analyzing movement and shape of already tracked objects in a given label image 
 * 
 * @author glass
 */
public class TrackAnalyzer extends ReportGenerator
{
	@Parameter(label = "trajectories", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "trajectories", dataIOOrder = 0)
	private Vector<Trajectory2D> trajectories = null;
	
	private Hashtable<Integer, Vector<Point2D.Double>> centroids;
	private Hashtable<Integer, Integer> offsets;
	
	private double maxDist = 0;	// test variable
	
	
	/**
	 * 
	 * @param trajectories
	 */
	public TrackAnalyzer(Vector<Trajectory2D> trajectories) throws ALDOperatorException
	{
		this.trajectories = trajectories;
	}
	
	
//	public TrackAnalyzer(MTBImage labelImg, int minTrackLength) throws ALDOperatorException
//	{
//		this.nf.setMaximumFractionDigits(4);
//		
//		this.minTrackLength = minTrackLength;
//		
//		TrajectoryExtraction2D trajectoryExtraction = new TrajectoryExtraction2D(labelImg, minTrackLength);
//		trajectoryExtraction.extractCentroids();
//		Vector<Trajectory2D> trajectories = trajectoryExtraction.getTrajectories();
//		
//		this.trajectories = trajectories;
//	}
	
	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(4);
		nf.setGroupingUsed(false);
		
		extractTrajectoriesWithOffsets();
		
		// point measures
		Hashtable<Integer, Vector<Double>> distances = getDistances();
		
		Hashtable<Integer, Vector<Double>> speeds = getSpeeds(distances);
		
		Hashtable<Integer, Vector<Double>> directions = getDirections();
		
		Hashtable<Integer, Vector<Double>> directionalDifferences = getDirectionalDifferences(directions);
		
		Hashtable<Integer, Vector<Double>> persistences = getPersistences(speeds, directionalDifferences);
		
		// single object statistics
		Hashtable<Integer, Vector<Double>> totalDistances = getTotalDistances(distances);
		
		Hashtable<Integer, Vector<Double>> netDistances = getNetDistances();
		
		Hashtable<Integer, Vector<Double>> meanSpeeds = getObjectsMeans(speeds);
		
		Hashtable<Integer, Vector<Double>> directionalities = getDirectionalities(netDistances, totalDistances);
		
		Hashtable<Integer, Vector<Double>> netDirections = getNetDirections();
		
		Hashtable<Integer, Vector<Double>> meanDirectionalDifferences = getObjectsMeans(directionalDifferences);
		
		Hashtable<Integer, Vector<Double>> meanPersistences = getObjectsMeans(persistences);
		
		Hashtable<Integer, Vector<Double>> numFrames = getNumberOfFrames();
		
		// population statistics
		double avgMeanSpeed = getAverageMean(meanSpeeds);
		double avgStdDevSpeed = getAverageStdDev(meanSpeeds);
		double avgDirectionality = getAverageMean(directionalities);
		double avgStdDevDirectionality = getAverageStdDev(directionalities);
		double avgMeanPersistence = getAverageMean(meanPersistences);
		double avgStdDevPersistence = getAverageStdDev(meanPersistences);
		
		// generate report
		// heading and parameter values
		StringBuffer buffer = new StringBuffer("Tracking Report \n\n");
		buffer.append(getSettings());
		buffer.append("\n\n");
		
		// population statistics
		buffer.append("population statistics\n\n");
		buffer.append("objects analyzed: " + distances.size() + "\n");
		buffer.append("average mean speed:\t" + numberToString(avgMeanSpeed) + "\t" + unitSpace + "/" + unitTime + "\n");
		buffer.append("average sd speed:\t" + numberToString(avgStdDevSpeed) + "\t" + unitSpace + "/" + unitTime + "\n");
		buffer.append("average directionality:\t" + numberToString(avgDirectionality) + "\n");
		buffer.append("sd directionality:\t" + numberToString(avgStdDevDirectionality) + "\n");
		buffer.append("average mean persistence:\t" + numberToString(avgMeanPersistence) + "\t" + unitSpace + "/ (" + unitTime + " * " +  unitAngle + ")" + "\n");
		buffer.append("average sd persistence:\t" + numberToString(avgStdDevPersistence) + "\n");
		
		buffer.append("\n\n");
		
		// single object statistics
		buffer.append("single object statistics\n\n");
		buffer.append(hashtableToString(meanSpeeds, "mean speeds (" + unitSpace + "/" + unitTime + ")"));
		buffer.append(hashtableToString(directionalities, "directionalities"));
//		report.append(hashtableToString(meanSquareDisplacements, "mean square displacements"));
		buffer.append(hashtableToString(meanDirectionalDifferences, "mean directional differences (" + unitAngle + ")"));
		buffer.append(hashtableToString(meanPersistences, "mean persistences (" + unitSpace + "/ (" + unitTime + " * " + unitAngle + "))"));
		buffer.append(hashtableToString(totalDistances, "total distances (" + unitSpace + ")"));
		buffer.append(hashtableToString(netDistances, "net distances (" + unitSpace + ")"));
		buffer.append(hashtableToString(netDirections, "net directions (" + unitAngle + ")"));
		buffer.append(hashtableToString(numFrames, "number of frames observed"));
		buffer.append("\n\n");
		
		// point measures
		buffer.append("point measures\n\n");
//		report.append(pointHashtableToString(centroids, "centroids"));
		buffer.append(pointHashtableToStringX(centroids, "x coordinates of centroids"));
		buffer.append(pointHashtableToStringY(centroids, "y coordinates of centroids"));
		buffer.append(hashtableToString(distances, "distances (" + unitSpace + ")", offsets));
		buffer.append(hashtableToString(speeds, "speeds (" + unitSpace + "/" + unitTime + ")", offsets));
		buffer.append(hashtableToString(directions, "directions (" + unitAngle + ")", offsets));
		buffer.append(hashtableToString(directionalDifferences, "directional differences (" + unitAngle + ")", offsets));
		buffer.append(hashtableToString(persistences, "persistences (" + unitSpace + "/ (" + unitTime + " * " + unitAngle + "))", offsets));
		//report.append(hashtableToString(squaredDisplacements, "square displacements"));
		
		buffer.append(getExcluded());
		
		System.out.println("maximum distance: " + maxDist);
		
		this.report = buffer.toString();
	}
	
	
//	/**
//	 * 
//	 * @return report (String) that contains information about the movement of the labeled objects from the input image
//	 */
//	public String makeReport()
//	{
//		extractTrajectoriesWithOffsets();
//		
//		// point measures
//		Hashtable<Integer, Vector<Double>> distances = getDistances();
//		
//		Hashtable<Integer, Vector<Double>> speeds = getSpeeds(distances);
//		
//		Hashtable<Integer, Vector<Double>> directions = getDirections();
//		
//		Hashtable<Integer, Vector<Double>> directionalDifferences = getDirectionalDifferences(directions);
//		
//		Hashtable<Integer, Vector<Double>> persistences = getPersistences(speeds, directionalDifferences);
//		
//		// single object statistics
//		Hashtable<Integer, Vector<Double>> totalDistances = getTotalDistances(distances);
//		
//		Hashtable<Integer, Vector<Double>> netDistances = getNetDistances();
//		
//		Hashtable<Integer, Vector<Double>> meanSpeeds = getObjectsMeans(speeds);
//		
//		Hashtable<Integer, Vector<Double>> directionalities = getDirectionalities(netDistances, totalDistances);
//		
//		Hashtable<Integer, Vector<Double>> netDirections = getNetDirections();
//		
//		Hashtable<Integer, Vector<Double>> meanDirectionalDifferences = getObjectsMeans(directionalDifferences);
//		
//		Hashtable<Integer, Vector<Double>> meanPersistences = getObjectsMeans(persistences);
//		
//		Hashtable<Integer, Vector<Double>> numFrames = getNumberOfFrames();
//		
//		// population statistics
//		double avgMeanSpeed = getAverageMean(meanSpeeds);
//		double avgStdDevSpeed = getAverageStdDev(meanSpeeds);
//		double avgDirectionality = getAverageMean(directionalities);
//		double avgStdDevDirectionality = getAverageStdDev(directionalities);
//		double avgMeanPersistence = getAverageMean(meanPersistences);
//		double avgStdDevPersistence = getAverageStdDev(meanPersistences);
//		
//		// generate report
//		// heading and parameter values
//		StringBuffer report = new StringBuffer("Tracking Report \n\n");
//		report.append(getSettings());
//		report.append("\n\n");
//		
//		// population statistics
//		report.append("population statistics\n\n");
//		report.append("objects analyzed: " + distances.size() + "\n");
//		report.append("average mean speed:\t" + numberToString(avgMeanSpeed) + "\t" + unitXY + "/" + unitT + "\n");
//		report.append("average sd speed:\t" + numberToString(avgStdDevSpeed) + "\t" + unitXY + "/" + unitT + "\n");
//		report.append("average directionality:\t" + numberToString(avgDirectionality) + "\n");
//		report.append("sd directionality:\t" + numberToString(avgStdDevDirectionality) + "\n");
//		report.append("average mean persistence:\t" + numberToString(avgMeanPersistence) + "\t" + unitXY + "/ (" + unitT + " * " +  unitAngle + ")" + "\n");
//		report.append("average sd persistence:\t" + numberToString(avgStdDevPersistence) + "\n");
//		
//		report.append("\n\n");
//		
//		// single object statistics
//		report.append("single object statistics\n\n");
//		report.append(hashtableToString(meanSpeeds, "mean speeds (" + unitXY + "/" + unitT + ")"));
//		report.append(hashtableToString(directionalities, "directionalities"));
////		report.append(hashtableToString(meanSquareDisplacements, "mean square displacements"));
//		report.append(hashtableToString(meanDirectionalDifferences, "mean directional differences (" + unitAngle + ")"));
//		report.append(hashtableToString(meanPersistences, "mean persistences (" + unitXY + "/ (" + unitT + " * " + unitAngle + "))"));
//		report.append(hashtableToString(totalDistances, "total distances (" + unitXY + ")"));
//		report.append(hashtableToString(netDistances, "net distances (" + unitXY + ")"));
//		report.append(hashtableToString(netDirections, "net directions (" + unitAngle + ")"));
//		report.append(hashtableToString(numFrames, "number of frames observed"));
//		report.append("\n\n");
//		
//		// point measures
//		report.append("point measures\n\n");
////		report.append(pointHashtableToString(centroids, "centroids"));
//		report.append(pointHashtableToStringX(centroids, "x coordinates of centroids"));
//		report.append(pointHashtableToStringY(centroids, "y coordinates of centroids"));
//		report.append(hashtableToString(distances, "distances (" + unitXY + ")", offsets));
//		report.append(hashtableToString(speeds, "speeds (" + unitXY + "/" + unitT + ")", offsets));
//		report.append(hashtableToString(directions, "directions (" + unitAngle + ")", offsets));
//		report.append(hashtableToString(directionalDifferences, "directional differences (" + unitAngle + ")", offsets));
//		report.append(hashtableToString(persistences, "persistences (" + unitXY + "/ (" + unitT + " * " + unitAngle + "))", offsets));
//		//report.append(hashtableToString(squaredDisplacements, "square displacements"));
//		
//		report.append(getExcluded());
//		
//		System.out.println("maximum distance: " + maxDist);
//		
//		return report.toString();
//	}
		
	
	/**
	 * extract Hashtables containing the centroid locations of each trajectory <br/>
	 * and the offsets for the starting time of each trajectory
	 */
	private void extractTrajectoriesWithOffsets()
	{
		centroids = new Hashtable<Integer, Vector<Point2D.Double>>();
		offsets = new Hashtable<Integer, Integer>();
		
		for(int i = 0; i < trajectories.size(); i++)
		{
			Trajectory2D t = trajectories.elementAt(i);
			int id = t.getID();
			Vector<Point2D.Double> points = t.getPoints();
			centroids.put(id, points);
			offsets.put(id, t.getStartFrame());
		}
		
	}
	
	
	/**
	 * 
	 * @return distances that the objects moved from one frame to another <br/>
	 * key: object-id, value: list of distances
	 */
	private Hashtable<Integer, Vector<Double>> getDistances()
	{
		Hashtable<Integer, Vector<Double>> distances = new Hashtable<Integer, Vector<Double>>();
		
		Enumeration<Integer> keys = centroids.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			
			Vector<Point2D.Double> v = centroids.get(k);
			
			Vector<Double> d = new Vector<Double>();
			
			for(int i = 1; i < v.size(); i++)
			{
				Point2D.Double p = v.elementAt(i-1);
				Point2D.Double q = v.elementAt(i);
				
				d.add(distance(p, q, deltaX, deltaY));
				
				// temporarily, only for testing purposes
				if(distance(p, q, deltaX, deltaY) > maxDist)
				{
					maxDist = distance(p, q, deltaX, deltaY);
				}
			}
			
			distances.put(k, d);
		}
		
		
		return distances;
	}
	
		
	/**
	 * 
	 * @param distances distances the objects moved
	 * @return key: speeds of the objects between consecutive frames <br/>
	 * key: object-id, value: list of speeds
	 */
	private Hashtable<Integer, Vector<Double>> getSpeeds(Hashtable<Integer, Vector<Double>> distances)
	{
		Hashtable<Integer, Vector<Double>> speeds = new Hashtable<Integer,Vector<Double>>();
		
		Enumeration<Integer> keys = distances.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> d = distances.get(k);
			Vector<Double> s = new Vector<Double>();
			
			for(int i = 0; i < d.size(); i++)
			{
				s.add(d.elementAt(i) / deltaT);
			}
			
			speeds.put(k, s);
		}
		
		
		return speeds;
	}
	
	
	/**
	 * 
	 * @return number of frames each object is available
	 */
	private Hashtable<Integer, Vector<Double>> getNumberOfFrames()
	{
		Hashtable<Integer, Vector<Double>> numFrames = new Hashtable<Integer, Vector<Double>>();
		
		Enumeration<Integer> keys = centroids.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> n = new Vector<Double>();
			n.add((double)centroids.get(k).size());
			numFrames.put(k, n);
		}
		
		return numFrames;
	}
	
	
	/**
	 * 
	 * @return directions of movement (degree [0, 359]) of each object from one frame to another <br/>
	 * key: object-id, value: list of directions
	 */
		private Hashtable<Integer, Vector<Double>> getDirections()
		{
			Hashtable<Integer, Vector<Double>> directions = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = centroids.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				
				Vector<Point2D.Double> c = centroids.get(k);
				
				Vector<Double> d = new Vector<Double>();
				
				for(int i = 1; i < c.size(); i++)
				{
					Point2D.Double p = c.elementAt(i-1);
					Point2D.Double q = c.elementAt(i);
					
					double angle = angle(p, q);
					
					d.add(angle);
					
				}
				
				directions.put(k, d);
			}
			
			return directions;
		}
		
		
		/**
		 * 
		 * @param directions
		 * @return differences in directions of movement (degree [0, 180]) of each object from one frame to another <br/>
		 * key: object-id, value: list of directions
		 */
		private Hashtable<Integer, Vector<Double>> getDirectionalDifferences(Hashtable<Integer, Vector<Double>> directions)
		{
			Hashtable<Integer, Vector<Double>> dirDiffs = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = directions.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				
				Vector<Double> dir = directions.get(k);
				
				Vector<Double> diff = new Vector<Double>();
				
				for(int i = 1; i < dir.size(); i++)
				{
					double val = Math.abs(dir.elementAt(i) - dir.elementAt(i-1));
					
					if(val > 180)
					{
						val = 360 - val;
					}
					
					diff.add(val);
				}
				
				dirDiffs.put(k, diff);
			}
			
			return dirDiffs;
		}
		
		
		/**
		 * 
		 * @param speeds
		 * @param dirDiffs
		 * @return persistence values of each object in every frame calculated as speed / (1 + (100 / 360) * directional change)<br\>
		 * key: object-id, value: list of persistences
		 */
		private Hashtable<Integer, Vector<Double>> getPersistences(Hashtable<Integer, Vector<Double>> speeds, Hashtable<Integer, Vector<Double>> dirDiffs)
		{
			double factor = 0.27778;
			
			Hashtable<Integer, Vector<Double>> persistences = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = speeds.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				
				Vector<Double> s = speeds.get(k);
				Vector<Double> d = dirDiffs.get(k);
				
				Vector<Double> p = new Vector<Double>();
				
				for(int i = 0; i < d.size(); i++)
				{
					p.add(s.elementAt(i+1) / (1 + factor * d.elementAt(i)));
				}
				
				persistences.put(k, p);
			}
			
			return persistences;
		}
		

		/**
		 * @param distances distances that the objects moved from one frame to another
		 * @return total (euclidean) distances the objects moved <br/>
		 * key: object-id, value: total distance
		 */
		private Hashtable<Integer, Vector<Double>> getTotalDistances(Hashtable<Integer, Vector<Double>> distances)
		{
			Hashtable<Integer, Vector<Double>> totalDistances = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = distances.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				Vector<Double> d = distances.get(k);
				Vector<Double> td = new Vector<Double>();
				double sum = 0;
				
				for(int i = 0; i < d.size(); i++)
				{
					sum += d.elementAt(i);
				}
				
				td.add(sum);
				
				totalDistances.put(k, td);
			}
			
			return totalDistances;
		}
	
	
		/**
		 * 
		 * @return net (euclidean) distances the objects moved <br/>
		 * key: object-id, value: net distance
		 */
		private Hashtable<Integer, Vector<Double>> getNetDistances()
		{
			Hashtable<Integer, Vector<Double>> netDistances = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = centroids.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				Vector<Point2D.Double> c = centroids.get(k);
				Vector<Double> nd = new Vector<Double>();
				
				double dist = distance(c.elementAt(0), c.elementAt(c.size() - 1), deltaX, deltaY);
				
				nd.add(dist);
				
				netDistances.put(k, nd);
			}
			
			//System.out.println(hashtableToString(netDistances, "net distances"));
			
			return netDistances;
		}
	
	
		/**
		 * 
		 * @param netDistances net distance the objects moved
		 * @param totalDistances total distance the objects moved
		 * @return directionalities of the distinct objects <br/>
		 * key: object-id, value: directionality
		 */
		private Hashtable<Integer, Vector<Double>> getDirectionalities(Hashtable<Integer, Vector<Double>> netDistances, Hashtable<Integer, Vector<Double>> totalDistances)
		{
			Hashtable<Integer, Vector<Double>> directionalities = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = netDistances.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				double nd = netDistances.get(k).elementAt(0);
				double td = totalDistances.get(k).elementAt(0);
				Vector<Double> d = new Vector<Double>();
				
				double dir = 0;
				
				if(td != 0)
				{
					dir = nd / td;
				}
				
				d.add(dir);
				
				directionalities.put(k, d);
			}
			
			return directionalities;
		}
	
		
		/**
		 * 
		 * @return net directions (degree [0, 359]) of the single objects, <br/>
		 * i.e. the angle between the vector formed by the first and the last position of an object and the x-axis<br/>
		 * key: object-id, value: net direction
		 */
		private Hashtable<Integer, Vector<Double>> getNetDirections()
		{
			Hashtable<Integer, Vector<Double>> netDirections = new Hashtable<Integer, Vector<Double>>();
			
			Enumeration<Integer> keys = centroids.keys();
			
			while(keys.hasMoreElements())
			{
				int k = keys.nextElement();
				Vector<Point2D.Double> c = centroids.get(k);
				Vector<Double> nd = new Vector<Double>();
				
				double dir = angle(c.elementAt(0), c.elementAt(c.size() - 1));
				
				nd.add(dir);
				
				netDirections.put(k, nd);
			}
			
			return netDirections;
		}
	
	
	/**
	 * 
	 * @param p
	 * @param q
	 * @param deltaX
	 * @param deltaY
	 * @return euclidean distance between p an q scaled by parameters deltaX and deltaY
	 */
	private double distance(Point2D.Double p, Point2D.Double q, double deltaX, double deltaY)
	{
		return(Math.sqrt((p.x - q.x) * deltaX * (p.x - q.x) * deltaX + (p.y - q.y) * deltaY * (p.y - q.y) * deltaY));
	}
	
	
	/**
	 * 
	 * @param p
	 * @param q
	 * @return angle (degree [0, 359]) between the vector formed by the points p and q and the x-axis (vector (1,0))
	 */
	private double angle(Point2D.Double p, Point2D.Double q)
	{
		double angle = 0;
		
		double xDiff = q.x - p.x;
		double yDiff = q.y - p.y;
		
		double scalarProd = xDiff;
		double magProd = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		
		if(magProd != 0)
		{
			double a = Math.acos(scalarProd / magProd);
			
			if(yDiff > 0)
			{
				angle = 360 - Math.toDegrees(a);
			}
			else
			{
				angle =  Math.toDegrees(a);
			}
		}
		
		return angle;
	}
	
}
