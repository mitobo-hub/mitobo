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
 * Most recent change(s):
 * 
 * $Rev: 5378 $
 * $Date: 2012-04-10 11:30:22 +0200 (Di, 10 Apr 2012) $
 * $Author: moeller $
 * 
 */

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Operator to analyze a given binary image or a set of nuclei regions.
 * <p>
 * The data will be transformed into a distance map in which the
 * peaks are analyzed and compared to identify overlapping nuclei.
 * <p>
 * The result will be center-coordinates of all nuclei in the data.
 * Furthermore the regions may be divided or initial regions for 
 * further analysis may be delivered.
 * <p>
 * This operator has been written as part of Jochen's project in WS 2011/12.
 * 
 * @author Jochen Luechtrath
 * @author Birgit Moeller
 */

@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.NONE)
public class NucleusSeparator2DPeakSearch_RegionSeparator extends MTBOperator {

	/**
	 * Binary input image to be analyzed.
	 */
	@Parameter( label= "Input Image", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 1, 
			mode=ExpertMode.STANDARD, description = "Binary input image.")	
	private transient MTBImage inImg= null;
		
	/**
	 * Optional set of pre-segmented input regions.
	 */
	@Parameter( label= "Input Regions", required = false, 
			direction = Parameter.Direction.INOUT, dataIOOrder = 1, 
			mode=ExpertMode.ADVANCED, description = "Input nuclei regions.")
	private transient MTBRegion2DSet inputRegions=null;

	/**
	 * Threshold to suppress peak analysis close to scraggy contours.
	 */
	@Parameter( label= "Significance Threshold for Peaks", 
			required = false, direction = Parameter.Direction.IN, 
			mode=ExpertMode.ADVANCED, 
			description = "Threshold to suppress peak analysis close to " +
					"scraggy contours (should be >4).")	
	private double schwelle=7;
	
	/**
	 * Maximum peak distance factor.
	 */
	@Parameter( label= "Maximum Peak Distance Factor", 
			required = false, direction = Parameter.Direction.IN, 
			mode=ExpertMode.ADVANCED, 
			description = "Factor to adjust maximal allowed distance from " +
					"lower to upper peak.")	
	private double max_d2c=1.5;
	
	/**
	 * Maximal allowed discrepancy.
	 */
	@Parameter( label= "Maximal Allowed Discrepancy", 
			required = false, direction = Parameter.Direction.IN,
			mode=ExpertMode.ADVANCED, 
			description = "Max. allowed discrepancy of direct connection to " +
					"actual profile.")	
	private double mad=0.11;
	
	/**
	 * Metric for distance map calculation.
	 */
	@Parameter( label= "Distance Metric", 
			required = false, direction = Parameter.Direction.IN, 
			mode=ExpertMode.ADVANCED, description = "Distance metric.")	
	private DistanceMetric distmet= DistanceMetric.CHESSBOARD;
	
	/*
	 * Result data.
	 */
	
	/**
	 * Calculated distance map.
	 */
	@Parameter( label= "Distance Map", required = false, 
			direction = Parameter.Direction.OUT, description = "Distance Map.")	
	private transient MTBImage distImg=null;
		
	/**
	 * Set of detected nuclei centers.
	 */
	@Parameter( label= "Nuclei Centers", required = false, 
			direction = Parameter.Direction.OUT, 
			description = "Centers of all detected nuclei.")	
	private	 transient MTBRegion2DSet resultCenters=null;	
	
	/**
	 * Nuclei centers sorted according to former regions.
	 */
	@Parameter( label= "Nuclei Centers of former regions", required = false, 
			direction = Parameter.Direction.OUT, 
			description = "Centers of detected nuclei seperated in their " +
					"former regions")	
	private transient MTBRegion2DSet[] resultRegCenters=null;		
		
	/**
	 * Set of elliptical snake polygons.
	 */
	@Parameter( label= "Elliptic Snake Initialisations", required = false, 
			direction = Parameter.Direction.OUT, 
			description = "Set of elliptical snake polygons.")	
	private transient MTBPolygon2DSet snakePrimerEllipse=null;
	
	/**
	 * Set of snake polygons based on Voronoi tesselation.
	 */
	@Parameter( label= "Snake Initialisation by Voronoi", required = false, 
			direction = Parameter.Direction.OUT, 
			description = "Snake polygons from Voronoi-tesselated regions.")	
	private	 transient MTBPolygon2DSet snakePrimerVoronoi=null;

	/*
	 * Local variables.
	 */

	private transient int res_x=0, res_y=0; 
	private transient boolean[][] blacklist=null;
	private transient boolean[][] seen=null;
	public transient boolean report=false;
	
	private transient MTBRegion2DSet[] resultAreas=null;

	private transient MTBRegion2DSet candidates;

	/**
	 * Specify binary input image.
	 * @param inImg Binary input image.
	 */
	public void setInImg(MTBImageByte _inImg) {		
		this.inImg=_inImg;	
	}

	/**
	 * Get set of input regions.
	 * @return Set of input regions.
	 */
	public MTBRegion2DSet getInputRegs() {		
		return this.inputRegions;	
	}

	/**
	 * Specify set of input regions.
	 * @param inputRegions Set of input regions.
	 */
	public void setInputRegs(MTBRegion2DSet _inputRegions) {		
		this.inputRegions= _inputRegions;	
	}

	/**
	 * Returns calculated distance map.
	 * @return Image distance map.
	 */
	public MTBImage getDistImg() {		
		return this.distImg;	
	}

	/**
	 * Returns centers of result regions.
	 * @return Centers of all detected nuclei.
	 */
	public MTBRegion2DSet getResultCenters() {		
		return this.resultCenters;	
	}

	/**
	 * Returns set of detected candidate peaks. 
	 * @return Set of all possible peaks.
	 */
	public MTBRegion2DSet getCandidates()	{		
		return this.candidates;	
	}

	/**
	 * Sets factor to adjust maximal allowed distance from lower to upper peak.
	 * @param max_d2c Adjustment factor, default is 1.5.
	 */
	public void setMax_d2c(double _max_d2c)	{		
		this.max_d2c=_max_d2c;	
	}

	/**
	 * Sets maximal allowed discrepancy of direct connection to actual 
	 * profile between two peaks.
	 * @param mad Max. discrepancy, default is 0.11.
	 */
	public void setMad(double _mad)	{		
		this.mad= _mad;	
	}
	
	/**
	 * Specify the metric for the distance transformation.
	 * @param distmet Distance metric, default is Chessboard-Distance.
	 */
	public void setDistmet(DistanceMetric _distmet) {	
		this.distmet= _distmet; 
	}
	
	/**
	 * Sets threshold to suppress peak analysis close to scraggy contours.
	 * @param sup Threshold, should be >4.
	 */
	public void setSuppressor(int sup) {	
		this.schwelle= sup;	
	}
	
	/**
	 * Returns the centers of the nuclei sorted to their original regions.
	 * @return Set of nuclei centers sorted according to their original regions.
	 */
	public MTBRegion2DSet[] getRegCenters() {		
		return this.resultRegCenters;	
	}	
	
	/**
	 * Returns separated regions sorted according to original regions.
	 * @return Splitted regions of their actual centers.
	 */
	public MTBRegion2DSet[] getResultAreas(){
		if (this.resultAreas==null) 
			regionVoronoize();
		return this.resultAreas;
	}
	
	/**
	 * Returns set of snake polygons resulting from Voronoi tesselation.
	 * @return Initial snake polygons of voronoi divided regions.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	public MTBPolygon2DSet getVoronoidSnakePrimer() 
			throws ALDOperatorException, ALDProcessingDAGException {
		if (this.resultAreas==null) {
			regionVoronoize();
		}
		if (this.snakePrimerVoronoi==null) {
			voronoidSnakePrimer();
		}
		return this.snakePrimerVoronoi;
	}
	
	/**
	 * Returns set of initial snake ellipses.
	 * @return Elliptical snake polygons.
	 */
	public MTBPolygon2DSet getEllipsoidSnakePrimer() {
		if (this.snakePrimerEllipse==null) 
			ellipsoidSnakePrimer();
		return this.snakePrimerEllipse;
	}
	
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public NucleusSeparator2DPeakSearch_RegionSeparator() throws ALDOperatorException {	
		// nothing to do here
	}

	/**
	 * Constructor with input image.
	 * @param image Binary input image.
	 * @throws ALDOperatorException
	 */
	public NucleusSeparator2DPeakSearch_RegionSeparator(MTBImageByte image)
			throws ALDOperatorException {
		this.inImg=image;	
	}
	
	/**
	 * Constructor with region set.
	 * <p>
	 * <em>Note: the set needs correct referenced dimensions!</em>
	 * @param oset Set of input regions.
	 * @throws ALDOperatorException
	 */
	public NucleusSeparator2DPeakSearch_RegionSeparator(MTBRegion2DSet oset)
			throws ALDOperatorException {
		this.inputRegions=oset;
		/* Note: these settings are never used as they are immediately overwritten
		 * 			 at the beginning of operate()...
		 */
		this.res_x=(int) oset.getXmax();
		this.res_y=(int) oset.getYmax();
	}

	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		// extract image size
		this.res_x=this.inImg.getSizeX();
		this.res_y=this.inImg.getSizeY();
		
		// init some variables
		MTBRegion2DSet regset=new MTBRegion2DSet(0, 0, this.res_x, this.res_y);
		MTBRegion2DSet resultset=new MTBRegion2DSet(0, 0, this.res_x, this.res_y);
		
		this.candidates=new MTBRegion2DSet(0, 0, this.res_x, this.res_y);
		MTBRegion2DSet regionmaxima[];
		MTBRegion2DSet otsuedcenters[];		
		DistanceTransform dt;
		
		// if no input regions have been provided, segment input image
		if (this.inputRegions==null){
			LabelComponentsSequential labler= 
					new LabelComponentsSequential(this.inImg , true);
			labler.runOp(false);		
			regset=labler.getResultingRegions();
			this.inputRegions=regset;
		}
		
		int reg_num= regset.size();
		regionmaxima=new MTBRegion2DSet[reg_num];
		otsuedcenters=new MTBRegion2DSet[reg_num];
		
		for (int i=0;i<reg_num;i++){
			regionmaxima[i]=new MTBRegion2DSet(0,0,this.res_x,this.res_y);
			otsuedcenters[i]=new MTBRegion2DSet(0,0,this.res_x,this.res_y);
		}
		
		//create distance map
		if (this.distImg==null) {
			if(this.inImg!=null) {
				dt= new DistanceTransform((MTBImageByte)this.inImg, 
						this.distmet,DistanceTransform.ForegroundColor.FG_BLACK);
				dt.runOp(false);
				this.distImg=dt.getDistanceImage();
			}
			// first create binary image, then calculate distance map
			else if(this.inputRegions!=null){
				MTBImage tempic = MTBImage.createMTBImage(
						(int)this.inputRegions.getXmax()+1,
						(int)this.inputRegions.getYmax()+1, 
						1, 1, 1, MTBImageType.MTB_BYTE);				
				DrawRegion2DSet drawer=
					new DrawRegion2DSet(DrawType.MASK_IMAGE, this.inputRegions, 
							tempic, true);
				drawer.runOp(false);
				if (this.inImg==null) 
					this.inImg=tempic;
				dt= new DistanceTransform((MTBImageByte) tempic, 
						this.distmet,DistanceTransform.ForegroundColor.FG_BLACK);
				dt.runOp(false);
				this.distImg=dt.getDistanceImage();
			}
			// error, no input data available
			else { 
				ALDOperatorException ape= new ALDOperatorException(
					ALDOperatorException.OperatorExceptionType.OPERATE_FAILED,
					"[NucleusRegionSeparator] No input data given...");
				throw ape;
			}
		}
		
		this.res_x= this.distImg.getSizeX(); 
		this.res_y= this.distImg.getSizeY();
		this.blacklist=new boolean[this.res_x][this.res_y];
		this.seen=new boolean[this.res_x][this.res_y];

		for (int i=0; i<reg_num; i++)
		{
			//create container for candidates 	
			MTBRegion2D tempcand=new MTBRegion2D();		
			boolean greater=false, really=false;
			//check every pixel in region
			for (int j = 0; j < regset.get(i).getPoints().size(); j++) {				
				
				//create container for hits
				MTBRegion2D temphit=new MTBRegion2D();
				greater=false; really=false;
				
				int h_x=(int)regset.get(i).getPoints().get(j).x;
				int h_y=(int)regset.get(i).getPoints().get(j).y;
				double intensitxy=this.distImg.getValueDouble(h_x,h_y); 
				
				 //if any is greater => greater=false & next
				if (h_x >0  &&    										  // check west
						intensitxy<this.distImg.getValueDouble(h_x-1,h_y)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if (h_x >0  &&h_y>0 && 							 // check nwest
						intensitxy<this.distImg.getValueDouble(h_x-1, h_y-1)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if (h_x >0  && h_y<this.res_y-1 && 					// check swest
						intensitxy<this.distImg.getValueDouble(h_x-1, h_y+1)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if ( h_y< this.res_y-1 && 								// check south
						intensitxy<this.distImg.getValueDouble(h_x, h_y+1)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if ( h_y>0 && 									// check north
						intensitxy<this.distImg.getValueDouble(h_x, h_y-1)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if ( h_x< this.res_x-1 && 								// check east
						intensitxy<this.distImg.getValueDouble(h_x+1, h_y)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if ( h_x< this.res_x-1 && h_y>0	&&					// check neast
						intensitxy<this.distImg.getValueDouble(h_x+1, h_y-1)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				}else if ( h_x< this.res_x-1 && h_y< this.res_y-1 &&	// check seast
								intensitxy<this.distImg.getValueDouble(h_x+1, h_y+1)){		
					greater=false; this.blacklist[h_x][h_y]=true;
				} else greater=true;	//no one else is greater
		
				
				if (greater){ //if no one else is greater, are all less?
					if ( h_x >0  &&    										// check west
							intensitxy==this.distImg.getValueDouble(h_x-1,h_y)){		
						really=false;
					}else if ( h_x >0  &&h_y>0 && 							// check nwest
							intensitxy==this.distImg.getValueDouble(h_x-1, h_y-1)){		
						really=false;
					}else if ( h_x >0  && h_y<this.res_y-1 && 					// check swest
							intensitxy==this.distImg.getValueDouble(h_x-1, h_y+1)){		
						really=false;
					}else if ( h_y< this.res_y-1 && 								// check south
							intensitxy==this.distImg.getValueDouble(h_x, h_y+1)){		
						really=false;
					}else if ( h_y>0 && 									// check north
							intensitxy==this.distImg.getValueDouble(h_x, h_y-1)){		
						really=false;
					}else if ( h_x< this.res_x-1 && 								// check east
							intensitxy==this.distImg.getValueDouble(h_x+1, h_y)){		
						really=false;
					}else if ( h_x< this.res_x-1 && h_y>0	&&					// check neast
							intensitxy==this.distImg.getValueDouble(h_x+1, h_y-1)){		
						really=false;
					}else if ( h_x< this.res_x-1 && h_y< this.res_y-1 &&	// check seast
									intensitxy==this.distImg.getValueDouble(h_x+1, h_y+1)){		
						really=false;} else really=true;
				}
				
				if (intensitxy > this.schwelle)
				{
					if (really){
						temphit.addPixel(h_x, h_y);
						regionmaxima[i].add(temphit);
					} else if (greater) tempcand.addPixel(h_x, h_y);
				}
			}//endfor allpixel j of region
			
			// all candidates of one main region in one set
			this.candidates.add(tempcand);
		}//endfor allregions i
		
		
		//########################################################################
		//########################### check candidates ###########################
		//########################################################################
		
		for (int i=0; i<reg_num; i++)
		{
			for (int j = 0; j <this.candidates.get(i).getPoints().size(); j++) {	
				int pos_x=(int)this.candidates.get(i).getPoints().get(j).x;
				int pos_y=(int)this.candidates.get(i).getPoints().get(j).y;
				MTBRegion2D tempregloc=new MTBRegion2D();				
				if (!this.seen[pos_x][pos_y] && gratwanderung(pos_x, pos_y,tempregloc))
					regionmaxima[i].add(tempregloc);
			}//endfor candy to hit
		}//endfor all reg_num i

		// Note:
		// during gratwanderung it's possible that not all "bad" pixel were 
		// blacklisted, if it started somewhere in the middle and first followed a 
		// "good" branch.
		// if blacklist information shall be used further, branch blacklisting 
		// should be done
		
		//#########################################################################
		//#################### connect close/equal regions (peaks) ################
		//#################### save them in otsuedcenters[] #######################
		//#########################################################################
		
		double [] pdist;//distance table		
		for(int nreg=0;nreg<regionmaxima.length;nreg++)//forall mainregions
		{
			int reg_max=regionmaxima[nreg].size();
			//transfer data in local array 
			MTBRegion2D[] connection_array= new MTBRegion2D[reg_max];
			for (int i=0;i<reg_max;i++){
				connection_array[i]=new MTBRegion2D();
				connection_array[i]=regionmaxima[nreg].get(i).clone();
			}
			if (this.report) System.out.println("----["+nreg+"]----");
			for(int reg1=0;reg1<reg_max;reg1++){//compare all regions pairwise
				for(int reg2=reg1+1;reg2<reg_max;reg2++){
					int reg1_max=connection_array[reg1].getPoints().size(),
						reg2_max=connection_array[reg2].getPoints().size();
					pdist=new double[reg1_max*reg2_max];
					for (int p1=0;p1<reg1_max;p1++){//check possible routes
						for (int p2=0;p2<reg2_max;p2++){
							pdist[p1*reg2_max+p2]=Math.sqrt(
								Math.pow(
										(connection_array[reg1].getPoints().get(p1).x 
												- connection_array[reg2].getPoints().get(p2).x),2)
								+ Math.pow(
										(connection_array[reg1].getPoints().get(p1).y
												- connection_array[reg2].getPoints().get(p2).y),2) );
						}
					}
					int pmax=0;
					for(int i=0;i<pdist.length;i++){//get shortest
						if(pdist[i]<pdist[pmax])
							pmax=i;
					}
					
					if (this.report) 
						System.out.println("Testing aggregation of reg " + reg1 + 
																												" and reg " + reg2);
					
					//hypothesize connection
					int point1_x=
					 (int)connection_array[reg1].getPoints().get((pmax/reg2_max)).x,
							point1_y=
					 (int)connection_array[reg1].getPoints().get((pmax/reg2_max)).y,
							point2_x=
					 (int) connection_array[reg2].getPoints().get(pmax%reg2_max).x,
						 	point2_y=
					 (int) connection_array[reg2].getPoints().get(pmax%reg2_max).y;
					Vector<Point2D.Double> connection = new Vector<Point2D.Double>();
					makeline(point1_x, point1_y, point2_x, point2_y, connection);
					int xdist=point1_x - point2_x, ydist=point1_y - point2_y;
					
					double summe=0,pixval,oldval,lowval;
					double level1 = this.distImg.getValueDouble(point1_x, point1_y),
								 level2 = this.distImg.getValueDouble(point2_x, point2_y),
								 levelup, leveldown;
					if (level1<level2){
						levelup =  level2; 
						leveldown=level1;
					}	
					else {
						levelup =level1; 
						leveldown=level2;
					}
					
					int abstiege=0,aufstiege=0,gleich=0,abs=0,aufs=0,gleichs=-1,letztes=0;
					//letztes: last action 0-equal, 1-down, 2-up
					oldval= this.distImg.getValueDouble(
							(int)connection.get(0).getX(),(int)connection.get(0).getY());
					lowval=oldval;
					for (int line_pos=0;line_pos<connection.size();line_pos++){
						pixval= this.distImg.getValueDouble(
								(int)connection.get(line_pos).getX(),
								(int)connection.get(line_pos).getY());
						summe+=pixval;
						if(pixval<lowval)
							lowval=pixval;
						/*leveldiff1+=(levelup-pixval);
						leveldiff2+=(leveldown-pixval);*/
						//nutzbar fuer quadratische abweichung
						if (oldval<pixval){
							aufs++;
							if (this.report) 
								System.out.print("h");
							if(letztes!=2){
								aufstiege++;	
								letztes=2;
							}
						}else if (oldval==pixval){
							gleichs++;
							if (this.report) 
								System.out.print("g");
							if(letztes!=0){
								gleich++;	
								letztes=0;
							}
						}else if (oldval>pixval){
							abs++;
							if (this.report) 
								System.out.print("r");
							if(letztes!=1){
								abstiege++;	
								letztes=1;
							}
						}
						oldval=pixval;
					}					

					double abw_rel_mid, abw_total_mid, 
							abw_rel_lo, abw_total_lo, 
							abw_rel_hi, abw_total_hi;
					abw_total_lo=leveldown * connection.size() - summe;
					abw_total_hi= levelup * connection.size() - summe;
					abw_total_mid=(levelup+leveldown)/2* connection.size() - summe;
					abw_rel_lo =1- summe / (leveldown * connection.size());
					abw_rel_hi =1- summe / (levelup * connection.size());
					abw_rel_mid=  //bisher wird nur dieses genutzt
							1-summe/((levelup+leveldown) * connection.size()/2);
					
					if (this.report){
						System.out.println("\nCombination "+pmax
								+ " contains shortest distance: "+pdist[pmax]
								+ " resp. "+connection.size()
								+ " resp. dist(x/y)"+xdist+"/"+ydist
								+ "\nleveldifference: "+(levelup-leveldown)
								+"\t lowest point: "+lowval+"("+(lowval-leveldown)+")"
								+"\t with an average difference of \n"
								+abw_total_lo +" lo\t vs \t  hi "
								+abw_total_hi+" \t vs \t  mid "+abw_total_mid
								+abw_rel_lo+" lo\t vs \t  hi "
								+abw_rel_hi+" \t vs \t  mid "+abw_rel_mid
								+"Descents: "+abstiege+"("+abs+")\t Ascents: "
															+aufstiege+"("+aufs+")"
								+"\t Levels: "+gleich+"("+gleichs+")");						
					}
					if(   connection.size()<this.max_d2c*leveldown
						 && abw_rel_mid<this.mad
						 && leveldown-lowval<leveldown/4) {
						if (this.report) 	
							System.out.println("Get Connected!\t"+reg1+"/"+reg2);
						connection_array[reg2]=
								connection_array[reg2].join(connection_array[reg1]);						
						connection_array[reg1]=new MTBRegion2D();
						if (this.report) 	
							System.out.println("----c----");
						break;
					}
					if (this.report) 	
						System.out.println("--------");
				}//endfor reg2
			}//endfor reg1
			
			for (int i=0; i < connection_array.length; i++)
			{
				if (connection_array[i].getArea() > 0)
				{
					resultset.add(connection_array[i]);
					otsuedcenters[nreg].add(connection_array[i]);
					if (this.report) 
						System.out.println("convey region " + i + 
								" into resultset with a size of " + 
																		connection_array[i].getPoints().size());
				}
			}
		}//endfor regmaxima		
		
		this.resultCenters=resultset;
		this.resultRegCenters=otsuedcenters;		
	}
	
	/**
	 * Function to extract Voronoi primers.
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private void voronoidSnakePrimer() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImageByte tempic2 = 
				(MTBImageByte) this.distImg.convertType(MTBImageType.MTB_BYTE,true);
		tempic2.fillBlack();
		DrawRegion2DSet drawer;	
		MTBContour2DSet conto;
		MTBPolygon2DSet polyset = 
				new MTBPolygon2DSet(0, 0, this.res_x, this.res_y);
		
		MTBRegion2DSet lset = 
				new MTBRegion2DSet(0,0,this.res_x,this.res_y);
		for (int h=0;h<this.resultAreas.length;h++)
			for (int r=0;r<this.resultAreas[h].size();r++)
				if(this.resultAreas[h].get(r).getPoints().size()>0)
					lset.add(this.resultAreas[h].get(r));
		drawer= 
				new DrawRegion2DSet(DrawType.MASK_IMAGE, lset, tempic2, true);
		drawer.runOp(false);
		
		ContourOnLabeledComponents cont = new ContourOnLabeledComponents(
				tempic2, lset , ContourType.OUTER_CONTOUR, 20);
		cont.runOp(false);
		conto = cont.getResultContours();
		int segmentlength;
		
		for (int ci=0;ci<conto.size();ci++){
			int vecsize=conto.elementAt(ci).getPointNum();
			MTBSnake tempoly=new MTBSnake();
			if (this.report) 
				System.out.println("Kontur: "+ci+"\t enthaelt "+vecsize+" Knoten");
			segmentlength=Math.max(1,Math.min(30,(vecsize)/20));
			for (int pi=0;pi<vecsize;pi++){
				int x=(int)conto.elementAt(ci).getPointAt(pi).x,
					y=(int)conto.elementAt(ci).getPointAt(pi).y;
				//put full Contour here
				if(pi%segmentlength==0){
					tempoly.addPoint(x, y);
				}
			}
			polyset.add(tempoly);
		}
		this.snakePrimerVoronoi=polyset;
	}

	/**
	 * Function to perform Voronoi tesselation.
	 */
	private void regionVoronoize() {
		/*\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
		  *\\\\\\\\\\\\\\\\\\\\\\\\construct ellipsoid\\\\\\\\\\\\\\\\\\\\\\\\\\\
		   *\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
		    */
		
		this.resultAreas=new MTBRegion2DSet[this.resultRegCenters.length];
		for (int i=0;i<this.resultRegCenters.length;i++)
			this.resultAreas[i]=new MTBRegion2DSet(0, 0, this.res_x, this.res_y);
		
		//TODO: tocheck not used yet, 
		//      meant to look for good cut-areas between direct neighbors
		Vector<Boolean[][]> tocheck= new Vector<Boolean[][]>();		
		
		for (int oreg=0;oreg<this.resultRegCenters.length;oreg++){
			int centers=this.resultRegCenters[oreg].size();
			tocheck.add(new Boolean[centers][centers]);
			if (centers==1)
				this.resultAreas[oreg].add(this.inputRegions.get(oreg));
			else if (centers!=0){
				for (int p=0;p<this.inputRegions.get(oreg).getPoints().size();p++){
					int c1=0,c2=1;
					double x=this.inputRegions.get(oreg).getPoints().get(p).getX(),
							y=this.inputRegions.get(oreg).getPoints().get(p).getY();
					
					this.resultAreas[oreg].add(new MTBRegion2D());//reg for center0
					for (int c=1;c<centers;c++){ //reg for other centers
						this.resultAreas[oreg].add(new MTBRegion2D());
						if(Math.abs(
								this.resultRegCenters[oreg].get(c).getCenterOfMass_X()-x) +
							 Math.abs(
								this.resultRegCenters[oreg].get(c).getCenterOfMass_Y()-y)
							<
							 Math.abs(
								this.resultRegCenters[oreg].get(c1).getCenterOfMass_X()-x) +
							 Math.abs(
								this.resultRegCenters[oreg].get(c1).getCenterOfMass_Y()-y))
						{
							c2=c1;
							c1=c;
						}
						else if(Math.abs(
								     this.resultRegCenters[oreg].get(c).getCenterOfMass_X()-x) 
								  + Math.abs(
							       this.resultRegCenters[oreg].get(c).getCenterOfMass_Y()-y)
							<
								Math.abs(
										this.resultRegCenters[oreg].get(c2).getCenterOfMass_X()-x)+
							  Math.abs(
							  		this.resultRegCenters[oreg].get(c2).getCenterOfMass_Y()-y))
						{
							c2=c;
						}
					}
					//have now 2 nearest centers |p-c1|<|p-c2|
					if( (Math.abs(
								this.resultRegCenters[oreg].get(c2).getCenterOfMass_X()-x) +
						   Math.abs(
						  	this.resultRegCenters[oreg].get(c2).getCenterOfMass_Y()-y)) -
						  (Math.abs(
						  	this.resultRegCenters[oreg].get(c1).getCenterOfMass_X()-x) +
						   Math.abs(
						  	this.resultRegCenters[oreg].get(c1).getCenterOfMass_Y()-y))
						>2)
					{
						this.resultAreas[oreg].get(c1).addPixel((int)x,(int)y);
					}						
					else 
						tocheck.get(oreg)[c1][c2]= new Boolean(true);						
				}
			}
		}
	}
	
	/**
	 * Function to extract elliptical snake primers.
	 */
	private void ellipsoidSnakePrimer(){
		/*\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
		  *\\\\\\\\\\\\\\\\\\\\\\\\construct ellipsoid\\\\\\\\\\\\\\\\\\\\\\\\\\\
		   *\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
		    */
			
		double a,b,m,phi,x,y;
		int polylength=28;
		MTBPolygon2DSet poly=new MTBPolygon2DSet(0, 0, this.res_x, this.res_y);
		MTBSnake tempoly;
		for (int i=0; i <this.resultCenters.size() ; i++)
		{
			int last=this.resultCenters.get(i).getPoints().size()-1,
				x1=(int)this.resultCenters.get(i).getPoints().get(0).x,
				x2=(int)this.resultCenters.get(i).getPoints().get(last).x,
				y1=(int)this.resultCenters.get(i).getPoints().get(0).y,
				y2=(int)this.resultCenters.get(i).getPoints().get(last).y;
			double CoM_x=this.resultCenters.get(i).getCenterOfMass_X(),
					CoM_y=this.resultCenters.get(i).getCenterOfMass_Y(),
					dif1_x=Math.abs(CoM_x-x1),
					dif1_y=Math.abs(CoM_y-y1),
					dif2_x=Math.abs(CoM_x-x2),
					dif2_y=Math.abs(CoM_y-y2);
			
			b=this.distImg.getValueDouble((int)(CoM_x+0.5),(int)(CoM_y+.5));
				//(distImg.getValueDouble(x1,y1)+distImg.getValueDouble(x2,y2))/2;

			b*=.8;//0.75*AvgIntensity
			
			if (dif1_x+dif1_y>dif2_x+dif2_y){ //get farest point & let it rule
				m=Math.max(dif1_x,dif1_y);//get 
				if(dif1_x<2) 
					phi=90;
				else if (dif1_y<2) 
					phi=0;
				else 
					phi = Math.atan( (CoM_y-y1) / (CoM_x-x1)  );
			}else{
				m=Math.max(dif2_x,dif2_y);
				if(dif2_x<2) 
					phi=90;
				else if (dif2_y<2) 
					phi=0;
				else 
					phi = Math.atan( (CoM_y-y2) / (CoM_x-x2) );
			}
			//m*=.9;
			
			//@least circle (b*1.4)=sqrt(2*b*b)
			a=Math.max((b*1.414213562),Math.sqrt(b*b+m*m));		
			
			tempoly= new MTBSnake();
			for (int j = 0; j < polylength; j++) {
				double t = 2 * j * Math.PI / polylength;
				x= CoM_x + a*Math.cos(t)*Math.cos(phi)-b*Math.sin(t)*Math.sin(phi);
				y= CoM_y + a*Math.cos(t)*Math.sin(phi)+b*Math.sin(t)*Math.cos(phi);
		
				if (x < 0) {
					x = 0;
				} 
				else if (x > this.res_x)
					x = this.res_x - 1;
				
				if (y < 0) {
					y = 0;
				} 
				else if (y > this.res_y)
					y = this.res_y - 1;
				
				tempoly.addPoint(x, y);
			}
			poly.add(tempoly);
		}
		this.snakePrimerEllipse=poly;
	}

	/**
	 * Function to travers ridges.
	 * @param x	x-coordinate to check.
	 * @param y	y-coordinate to check.
	 * @param tempreg	Region to check.
	 * @return True if pixel refers to peak.
	 */
	private boolean gratwanderung(int x, int y, MTBRegion2D tempreg){
		this.seen[x][y]=true;
		byte check=0;
		double actval=this.distImg.getValueDouble(x,y);
		boolean greatest=true;			
		if ( x >0  &&      // check west
				actval==this.distImg.getValueDouble(x-1, y)) {	
			if(this.blacklist[x-1][y]) {
				this.blacklist[x][y]=true;
				greatest=false;
				check++;
			}
			else if(!this.seen[x-1][y]) { /*nuked[x][y][0]=true;*/ 
				gratwanderung(x-1,y,tempreg); 
				if(this.blacklist[x-1][y]) { 
					this.blacklist[x][y]=true;
					greatest=false;
					check++;
				}
			}
		}
		 
		if ( x >0  && y>0 && 						 // check nwest
				actval==this.distImg.getValueDouble(x-1, y-1)) {	
			if(this.blacklist[x-1][y-1]) {
				this.blacklist[x][y]=true;
				greatest=false;
				check++;
			} 
			else if(!this.seen[x-1][y-1]) { /*nuked[x][y][0]=true;*/ 
				gratwanderung(x-1, y-1,tempreg); 
				if(this.blacklist[x-1][y-1]) {
					this.blacklist[x][y]=true;
					greatest=false;
					check++;
				}
			}
		}
		if (x >0  && y<this.res_y-1 && 			// check swest
				actval==this.distImg.getValueDouble(x-1, y+1)) {	
			if(this.blacklist[x-1][y+1]) {
				this.blacklist[x][y]=true;
				greatest=false;
				check++;
			} 
			else if(!this.seen[x-1][y+1]){/*nuked[x][y][0]=true;*/ 
				gratwanderung(x-1, y+1,tempreg); 
				if(this.blacklist[x-1][y+1]){
					this.blacklist[x][y]=true;
					greatest=false;
					check++;
				}
			}
		}
		if ( y< this.res_y-1 && 					// check south
				actval==this.distImg.getValueDouble(x, y+1)) {	
			 if(this.blacklist[x][y+1]) {
				 this.blacklist[x][y]=true;
				 greatest=false;
				 check++;
			 } 
			 else if(!this.seen[x][y+1]){/*nuked[x][y][0]=true;*/ 
				 gratwanderung(x, y+1,tempreg); 
				 if(this.blacklist[x][y+1]){
					 this.blacklist[x][y]=true;
					 greatest=false;
					 check++;
				 }
			 }
		 }
		 if ( y>0 && 												// check north
				actval==this.distImg.getValueDouble(x, y-1)) {	
			 if(this.blacklist[x][y-1]) {
				 this.blacklist[x][y]=true;
				 greatest=false;
				 check++;
			 } 
			 else if(!this.seen[x][y-1]){/*nuked[x][y][0]=true;*/ 
				 gratwanderung(x, y-1,tempreg); 
				 if(this.blacklist[x][y-1]){
					 this.blacklist[x][y]=true;
					 greatest=false;
					 check++;
				 }
			 }
		 }
		 if ( x< this.res_x-1 && 							// check east
				 actval==this.distImg.getValueDouble(x+1, y)) {	
			 if(this.blacklist[x+1][y]) {
				 this.blacklist[x][y]=true;
				 greatest=false;
				 check++;
			 } 
			 else if(!this.seen[x+1][y]){/*nuked[x][y][0]=true;*/
				 gratwanderung(x+1, y,tempreg); 
				 if(this.blacklist[x+1][y]){
					 this.blacklist[x][y]=true;
					 greatest=false;
					 check++;
				 }
			 }
		 }
		 if ( x< this.res_x-1 && y>0	&&				// check neast
				 actval==this.distImg.getValueDouble(x+1, y-1)) {	
			 if(this.blacklist[x+1][y-1])	{
				 this.blacklist[x][y]=true;
				 greatest=false;
				 check++;
			 } 
			 else if(!this.seen[x+1][y-1]){/*nuked[x][y][0]=true;*/ 
				 gratwanderung(x+1, y-1,tempreg); 
				 if(this.blacklist[x+1][y-1]){
					 this.blacklist[x][y]=true;
					 greatest=false;
					 check++;
				 }
			 }
		 }
		 if ( x< this.res_x-1 && y< this.res_y-1 &&	// check seast
				 actval==this.distImg.getValueDouble(x+1, y+1)) {	
			 if(this.blacklist[x+1][y+1]) {
				 this.blacklist[x][y]=true;
				 greatest=false;
				 check++;
			 } 
			 else if(!this.seen[x+1][y+1]){/*nuked[x][y][0]=true;*/ 
				 gratwanderung(x+1, y+1,tempreg); 
				 if(this.blacklist[x+1][y+1]){
					 this.blacklist[x][y]=true;
					 greatest=false;
					 check++;
				 }
			 }
		 }
		 
		if (check==0){
			greatest=true;		
			tempreg.addPixel(x,y);
		}
		return greatest;	
	}
	
	/**
	 * Draws a line between two points.
	 * 
	 * @param xstart	Start point x-coordinate.
	 * @param ystart	Start point x-coordinate.
	 * @param xend	End point x-coordinate.
	 * @param yend	End point y-coordinate.
	 * @param line	Points in between.
	 */
	private void makeline(int xstart, int ystart, int xend, int yend, 
			Vector<Point2D.Double> line) {
		
		int x, y, t, dx, dy, incx, incy, pdx, pdy, ddx, ddy, es, el, err;

		// Entfernung in beiden Dimensionen berechnen 
		dx = xend - xstart;
		dy = yend - ystart;

		// Vorzeichen des Inkrements bestimmen 
		incx = (int) Math.signum(dx);
		incy = (int) Math.signum(dy);
		if (dx < 0)
			dx = -dx;
		if (dy < 0)
			dy = -dy;

		// feststellen, welche Entfernung groeßer ist 
		if (dx > dy) {
			// x ist schnelle Richtung 
			pdx = incx;
			pdy = 0; // pd. ist Parallelschritt 
			ddx = incx;
			ddy = incy; // dd. ist Diagonalschritt 
			es = dy;
			el = dx; // Fehlerschritte schnell, langsam 
		} else {
			// y ist schnelle Richtung 
			pdx = 0;
			pdy = incy; // pd. ist Parallelschritt 
			ddx = incx;
			ddy = incy; // dd. ist Diagonalschritt 
			es = dx;
			el = dy; // Fehlerschritte schnell, langsam 
		}

		// Initialisierungen vor Schleifenbeginn 
		x = xstart;
		y = ystart;
		err = el / 2;
		if (x >= 0 && x < this.res_x && y >= 0 && y < this.res_y)
			line.add(new Point2D.Double(x,y));

		// Pixel berechnen 
		for (t = 0; t < el; ++t) // t zaehlt die Pixel, el ist auch Anzahl 
		{
			// Aktualisierung Fehlerterm 
			err -= es;
			if (err < 0) {
				// Fehlerterm wieder positiv (>=0) machen 
				err += el;
				// Schritt in langsame Richtung, Diagonalschritt 
				x += ddx;
				y += ddy;
			} else {
				// Schritt in schnelle Richtung, Parallelschritt 
				x += pdx;
				y += pdy;
			}
			if (x >= 0 && x < this.res_x && y >= 0 && y < this.res_y)
				line.add(new Point2D.Double(x,y));
		}
	}
	
//	public static void main(String[] args) throws ALDOperatorException, ALDProcessingDAGException, FormatException, IOException, DependencyException, ServiceException{
//		configs cc=new configs(3);
//		ImageReaderMTB imageReader=null;
//		imageReader=new ImageReaderMTB(cc.picsrc);
//		imageReader.runOp();		
//		MTBImageShort sMTBI=(MTBImageShort)  imageReader.getResultMTBImage().convertType(null, MTBImageType.MTB_SHORT,true);
//		MTBImageByte tmpResultImg=null;		
//		DrawRegion2DSet drawer=null;
//			
//		NucleusDetector2D nd=new NucleusDetector2D(sMTBI,
//				NuclDetectMode.OTSU_ERODEDILATE,null,true,3,55,true);
//		nd.runOp();
//		tmpResultImg=(MTBImageByte) nd.getResultImage().convertType(MTBImageType.MTB_BYTE,true);
//
//		//%%%%%%%%%%%%%%%%%%% init and start NucleusRegionSeparator %%%%%%%%%%%%%%%%%%%%%%%
//		boolean inputtest=!true;
//		NucleusRegionSeparator nrs=new NucleusRegionSeparator();
//		if (inputtest){
//			LabelComponentsSequential labler= new LabelComponentsSequential(tmpResultImg, true);
//			labler.runOp();		
//			nrs.setInputRegs(labler.getResultingRegions());
//		}			
//		else nrs.setInImg(tmpResultImg);
//		nrs.setMax_d2c(1.5);
//		nrs.setSuppressor(5);		
//		nrs.runOp();
//		//%%%%%%%%%%%%%%%%%%%%%%%%%%%% result preparation %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//		
//		MTBRegion2DSet otsuRegs= nrs.getInputRegs();		
//		MTBRegion2DSet ergebnis= nrs.getResultCenters() ;
//		MTBRegion2DSet[]aset= 	 nrs.getResultAreas();
//		MTBPolygon2DSet sep=nrs.getEllipsoidSnakePrimer();
//		MTBPolygon2DSet svp=nrs.getVoronoidSnakePrimer();
//		
//		MTBRegion2DSet lset=new MTBRegion2DSet(0,0,0,0);
//		for (int h=0;h<aset.length;h++)
//			for (int r=0;r<aset[h].size();r++)
//				lset.add(aset[h].get(r));
//		
//		
//		MTBImage 	tmpdist= (MTBImage) nrs.getDistImg();
//		MTBImageRGB tempic = (MTBImageRGB) tmpdist.convertType(MTBImageType.MTB_RGB,true);
//		MTBImageRGB last=	 (MTBImageRGB) sMTBI.convertType(MTBImageType.MTB_RGB,true);
//		
//		
//		drawer=new DrawRegion2DSet(DrawType.TRANSPARENT_IMAGE, otsuRegs, tempic);
//		drawer.runOp();			
//		drawer=new DrawRegion2DSet(DrawType.COLOR_IMAGE, ergebnis, tempic);
//		drawer.runOp();	
//		drawer=new DrawRegion2DSet(DrawType.TRANSPARENT_IMAGE, lset, last);
//		drawer.runOp();
//		
//		for (int g=0;g<sep.size();g++){
//			for (int h=0;h<sep.elementAt(g).getPoints().size();h++){
//				int x=(int) sep.elementAt(g).getPoints().get(h).x,
//					y=(int) sep.elementAt(g).getPoints().get(h).y;
//				last.putValueInt(x, y, 255);
//				tempic.putValueInt(x, y, 255);
//			}
//		}
//		for (int g=0;g<svp.size();g++){
//			for (int h=0;h<svp.elementAt(g).getPoints().size();h++){
//				int x=(int) svp.elementAt(g).getPoints().get(h).x,
//					y=(int) svp.elementAt(g).getPoints().get(h).y;
//				last.putValueG(x, y, 222);
//				tempic.putValueG(x, y, 222);
//			}
//		}
//				
//		tmpdist.show();
//		last.show();
//		tempic.show();
//	}
}

