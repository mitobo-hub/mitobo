package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import ij.IJ;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

public class ManualTrackingTrajectoryExtraction extends MTBOperator
{
	@Parameter(label = "path", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "path to file containing the data from ManualTracking plugin", dataIOOrder = 0)
	private ALDFileString path = new ALDFileString(IJ.getDirectory("current"));
	
	@Parameter(label = "trajectories", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "extracted trajectories", dataIOOrder = 0)
	private Vector<Trajectory2D> trajectories = null;

	
	private int minTrackLength;
	
	protected ManualTrackingTrajectoryExtraction(String path, int minTrackLength) throws ALDOperatorException
	{
		this.path = new ALDFileString(path);
		this.minTrackLength = minTrackLength;
	}

	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		trajectories = new Vector<Trajectory2D>();
		
		Hashtable<Integer, Integer> table = new Hashtable<Integer, Integer>();	// hash table for assigning track numbers to indexes of the trajectory vector 
		
		int trackIndex = -1;
		
		try
        {
            File f = new File(path.getFileName());
            FileReader in = new FileReader(f);
            BufferedReader br = new BufferedReader(in);
            StringTokenizer tokenizer;
            String line = br.readLine();	// skip first line
            
            while((line = br.readLine()) != null)
            {
                tokenizer = new StringTokenizer(line, "\t");
                
                int lineNo = (int)Double.parseDouble((String) tokenizer.nextElement());	// line number
                int trackNo = (int)Double.parseDouble((String) tokenizer.nextElement());	// track number
                int frameNo = (int)Double.parseDouble((String) tokenizer.nextElement());	// frame number
                double x = Double.parseDouble((String) tokenizer.nextElement());	// x-coordinate of centroid
                double y = Double.parseDouble((String) tokenizer.nextElement());	// y-coordinate of centroid
                
                System.out.println("line no " + lineNo + ", track no " + trackNo + ", frame no " + frameNo + ", x = " + x + ", y = " + y);
                
                if(!table.containsKey(trackNo))	// new track
                {
                	Vector<Point2D.Double> v = new Vector<Point2D.Double>();
                	v.add(new Point2D.Double(x,y));
                	trajectories.add(new Trajectory2D(trackNo, frameNo, v));
                	trackIndex++;
                	table.put(trackNo, trackIndex);
                }
                else
                {
                	trajectories.get(table.get(trackNo)).addPoint(new Point2D.Double(x,y));
                }
            }
            br.close();
            
            
        }
        catch(IOException e)
        {
            e.printStackTrace(System.out);
        }
		
		discardShortTracks();
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
		}
		
		trajectories = temp;
	}
	
	public Vector<Trajectory2D> getTrajectories()
	{
		return this.trajectories;
	}
}
