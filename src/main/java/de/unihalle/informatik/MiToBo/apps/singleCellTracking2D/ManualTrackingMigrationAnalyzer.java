package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import java.util.Vector;

import ij.IJ;
import ij.text.TextWindow;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD,
	shortDescription="Migration analyzer based on manual tracking.")
public class ManualTrackingMigrationAnalyzer extends MTBOperator
{
	@Parameter(label = "path", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "path to file containing data from the ManualTracking plugin", dataIOOrder = 0)
	private ALDFileString path = new ALDFileString(IJ.getDirectory("current"));
	
	@Parameter(label = "pixel length, x-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 1)
	private double deltaX = 1;
	
	@Parameter(label = "pixel length, y-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 2)
	private double deltaY = 1;
	
	@Parameter(label = "unit space", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit of space", dataIOOrder = 3)
	private String unitXY = "pixel";
	
	@Parameter(label = "time between frames", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "time between frames", dataIOOrder = 4)
	private double deltaT = 5;
	
	@Parameter(label = "unit time", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit of time", dataIOOrder = 5)
	private String unitT = "min";
	
	@Parameter(label = "minimum track length", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum track length to be considered", dataIOOrder = 6)
	private int minTrackLength = 24;

	public ManualTrackingMigrationAnalyzer() throws ALDOperatorException
	{
		
	}
	
	public ManualTrackingMigrationAnalyzer(String path) throws ALDOperatorException
	{
		this.path = new ALDFileString(path);
	}

	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		// extract trajectories from ManualTracking plugin result file
		ManualTrackingTrajectoryExtraction extraction = new ManualTrackingTrajectoryExtraction(path.getFileName(), minTrackLength);
		extraction.runOp();
		Vector<Trajectory2D> trajectories = extraction.getTrajectories();
		
		// analyze trajectories
		TrackAnalyzer trackAnalyzer = new TrackAnalyzer(trajectories);
		
		trackAnalyzer.setMinTrackLength(minTrackLength);
		trackAnalyzer.setDeltaX(deltaX);
		trackAnalyzer.setDeltaY(deltaY);
		trackAnalyzer.setDeltaT(deltaT);
		trackAnalyzer.setUnitSpace(unitXY);
		trackAnalyzer.setUnitTime(unitT);
		
		trackAnalyzer.runOp();
		
		String trackReport = trackAnalyzer.getReport();
		
		TextWindow tw = new TextWindow("trajectory_evaluation", trackReport, 600, 800);
		tw.setVisible(true);
	}

	public String getDocumentation() {
		return "<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p>Operator for analyzing cell trajectories obtained from the <a target=\"_blank\" href=\"http://rsb.info.nih.gov/ij/plugins/track/track.html\">ManualTracking </a> ImageJ plugin</p>\r\n" + 
				"	</li>\r\n" + 
				"</ul>\r\n" + 
				"<h2>Usage:</h2>\r\n" + 
				"<h3>required parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>path</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>path to ManualTracking results file</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"</ul>\r\n" + 
				"\r\n" + 
				"<h3>optional parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>pixel length, x-direction</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>physical length of a pixel in x-direction</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>pixel length, y-direction</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>physical length of a pixel in y-direction</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>unit space</tt> \r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>unit of measurement for pixel size</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>time between frames</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>time elapsed between the acqusition of two consecutive frames</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>unit time</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>unit of measurement for the time</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>minimum track length</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>minimum length (number of consecutive frames) of a track to be considered for analysis</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n" + 
				"	</p>\r\n" + 
				"	</li>\r\n" + 
				"</ul>\r\n" + 
				"<h3>supplemental parameters:</h3>\r\n" + 
				"\r\n" + 
				"<ul>\r\n" + 
				"	<li>\r\n" + 
				"		<p><tt>Verbose</tt>\r\n" + 
				"	<ul>\r\n" + 
				"		<li>\r\n" + 
				"			<p>output some additional information</p>\r\n" + 
				"		</li>\r\n" + 
				"	</ul>\r\n";
	}
}

