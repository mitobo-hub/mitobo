package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelAreasToRegions;

public class TrajectoryExtraction2D
{
	private transient MTBImage labelImg;
	private Vector<Trajectory2D> trajectories;
	private int minTrackLength;
	
	int sizeX;
	int sizeY;
	int sizeT;
	
	private final int bgLabel = 0;			// label value for the background
	
	private Vector<Integer> excluded = new Vector<Integer>();
	
	
	/**
	 * 
	 * @param labelImg
	 * @param minTrackLength
	 */
	public TrajectoryExtraction2D(MTBImage labelImg, int minTrackLength)
	{
		this.labelImg = labelImg;
		this.minTrackLength = minTrackLength;
		
		this.sizeX = labelImg.getSizeX();
		this.sizeY = labelImg.getSizeY();
		this.sizeT = labelImg.getSizeT();
		
		trajectories = new Vector<Trajectory2D>();
	}
	
	
	/**
	 * extract centroids from labeled regions
	 */
	public void extractCentroids()
	{	
		for(int t = 0; t < sizeT; t++)	
		{
			MTBImage frame = labelImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);	// extract single image frame					
			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);	// extract regions of the extracted frame
			
			for(int i = 0; i < currentRegions.size(); i++)
			{
				MTBRegion2D cr = currentRegions.elementAt(i);
				int cid = cr.getID();
				double cCenterX = cr.getCenterOfMass_X();
				double cCenterY = cr.getCenterOfMass_Y();
				
				boolean found = false;
				
				for(int j = 0; j < trajectories.size(); j++)
				{
					if(trajectories.elementAt(j).getID() == cid)
					{
						found = true;
						trajectories.elementAt(j).addPoint(new Point2D.Double(cCenterX, cCenterY));
						break;
					}
				}
				
				if(!found)	// new region discovered
				{
					Vector<Point2D.Double> p = new Vector<Point2D.Double>();
					p.add(new Point2D.Double(cCenterX, cCenterY));
					trajectories.add(new Trajectory2D(cid, t, p));
					
				}
				
			}
		}
		
		discardShortTracks();
	}
	
	
	/**
	 * 
	 * @param maskImg
	 * @param include
	 */
	public void extractCentroids(MTBImage maskImg, boolean include, double factor)
	{	
		// check if mask has same dimensions like labelImg
		if(maskImg.getSizeT() == sizeT && maskImg.getSizeY() == sizeY && maskImg.getSizeX() == sizeX)
		{
			for(int t = 0; t < sizeT; t++)	
			{
				MTBImage frame = labelImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);	// extract single image frame					
				MTBImage maskFrame = maskImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);	// extract single image frame
				
				MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);	// extract regions of the extracted frame
				
				// calculate mean intensity of mask image's current frame
				double avgIntensity = 0;
				
				for(int y = 0; y < sizeY; y ++)
				{
					for(int x = 0; x < sizeX; x++)
					{
						avgIntensity += maskFrame.getValueDouble(x, y);
					}
				}
				
				avgIntensity /= (sizeX * sizeY);
				
				for(int i = 0; i < currentRegions.size(); i++)
				{
					MTBRegion2D cr = currentRegions.elementAt(i);
					int cid = cr.getID();
					double cCenterX = cr.getCenterOfMass_X();
					double cCenterY = cr.getCenterOfMass_Y();
					
					boolean found = false;
					
					for(int j = 0; j < trajectories.size(); j++)
					{
						if(trajectories.elementAt(j).getID() == cid)
						{
							found = true;
							trajectories.elementAt(j).addPoint(new Point2D.Double(cCenterX, cCenterY));
							break;
						}
					}
					
					if(!found && !excluded.contains(cid))	// new region
					{
						// check if region has sufficient signal in the mask channel
						Vector<Point2D.Double> rPoints = cr.getPoints();
						
						double avg = 0;
						
						for(int j = 0; j < rPoints.size(); j++)
						{
							Point2D.Double p = rPoints.elementAt(j);
							
							avg += maskFrame.getValueDouble((int)p.x, (int)p.y);
							
						}
						
						avg /= cr.getArea();
						
						System.out.println("region " + cid + ": avg intensity: " + avg);
						
						if((include && avg >= factor * avgIntensity) || (!include && avg < factor * avgIntensity))
						{
							Vector<Point2D.Double> points = new Vector<Point2D.Double>();
							points.add(new Point2D.Double(cCenterX, cCenterY));
							trajectories.add(new Trajectory2D(cid, t, points));
						}
						else
						{
							excluded.add(cid);
						}
						
					}
					
				}
			}
			
			discardShortTracks();
		}
		else
		{
			System.err.println(this.toString() + "::TrajectoryExtraction: dimension of mask image does not match input image's dimensions!");
		}
		
	}
	
	
	/**
	 * delete trajectories whose length is too short
	 */
	private void discardShortTracks()
	{
		Vector<Trajectory2D> temp = new Vector<Trajectory2D>();
		
		for(int i = 0; i < trajectories.size(); i++)
		{
			Trajectory2D t = trajectories.elementAt(i);
			
			if(t.getPoints().size() >= minTrackLength)
			{
				temp.add(t);
			}
			else
			{
				excluded.add(t.getID());
			}
		}
		
		trajectories = temp;
	}
	
	
	/**
	 * 
	 * @return trajectory vector
	 */
	public Vector<Trajectory2D> getTrajectories()
	{
		return trajectories;
	}
	
	
	public Vector<Integer> getExcluded()
	{
		Collections.sort(excluded);
		
		return excluded;
	}
}

