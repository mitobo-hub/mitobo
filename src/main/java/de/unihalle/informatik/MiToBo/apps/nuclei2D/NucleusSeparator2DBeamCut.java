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
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.morphology.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.*;

import java.util.*;

/**
 * Class provides routines for separating conglomerates of cell nuclei in 2D.
 * <p>
 * This operator is not supposed to be directly executed in a generic fashion.
 * Use the operator {@link NucleusSeparator2D} instead.
 * 
 * @author posch
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
@ALDDerivedClass
public class NucleusSeparator2DBeamCut extends NucleusSeparator2DAlgos {

	/**
	 * Available modes for nuclei separation in 2D images.
	 * 
	 * @author posch
	 */
	public static enum NuclSeparateMode {
		/**
		 * Standard nucleus sepration mode
		 */
		STANDARD, 
	}

	/**
	 * Operation mode of this separator.
	 */
	@Parameter( label= "Operator mode", required = true, direction= Direction.IN,
			description = "Operator separation mode.")
	private NuclSeparateMode opMode = NuclSeparateMode.STANDARD;

	@Parameter( label= "Tophat size", required = false, direction= Direction.IN,
			description = "Size of structuring element for top hat on distances.")
	private int sizeTH = 51;

	@Parameter( label= "Tophat threshold", required = false,
			direction= Direction.IN,
			description = "Threshold for result from top hat.")
	private double thresTH = 20.0;

	@Parameter( label= "Opening mask size", required = false,
			direction= Direction.IN,
			description = "Size of structuring element for opening " +
					"on cutpoint hypotheses.")
	private int sizeOpening = 10;

	@Parameter( label= "Dilation mask size", required = false,
			direction= Direction.IN, 
			description = "Size of structuring element for dilation " +
					"on cutpoint hypotheses.")
	private int sizeDilation = 5;

	@Parameter( label= "Minimum nuclei size", required = false,
			direction= Direction.IN,
			description = "Minimum size of each nucleus after separation, "+
					"otherwise undo separation")
	private int minSize = 100;

	@Parameter( label= "Minimum fraction size", required = false,
			direction= Direction.IN, 
			description = "Minimum size fraction of each nucleus after separation,"+
					" otherwise undo separation.")
	private double minSizeFraction = 0.25;

	@Parameter( label= "Do erosion", required = false, direction= Direction.IN,
			description = "Erode each nucleus resulting from a separation.")
	private boolean doErosion = true;

	@Parameter( label= "Erode mask size", required = false,
			direction= Direction.IN, 
			description = "Size of structuring element for eroding a nucleus " +
					"from a separation.")
	private int erodeSize = 2;

	@Parameter( label= "Debug mode.", required = false,
			direction= Direction.IN, supplemental= true,
			description = "Flag for debugging output.")
	private int debug = 0;
	
	// ===================
	/** temp. binary image
	 */
	transient MTBImage tmpImg;
	// ===================

	/**
	 * Default constructor
	 */
	public NucleusSeparator2DBeamCut() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor.  
	 * @throws ALDOperatorException 
	 */
	public NucleusSeparator2DBeamCut(MTBImage labelImg)
			throws ALDOperatorException {
			
		this.labelImg = labelImg;
		this.opMode = NuclSeparateMode.STANDARD;

	}

	/**
	 * Constructor.  
	 * @throws ALDOperatorException 
	 */
	public NucleusSeparator2DBeamCut(MTBImage labelImg, 
			NuclSeparateMode opMode) throws ALDOperatorException {
			
		this.labelImg = labelImg;
		this.opMode = opMode;

	}

	@Override
	protected void operate() 
  	  throws ALDOperatorException, ALDProcessingDAGException {

	  // allocate result and tmp image
      this.resultImg =  (MTBImageShort)MTBImage.createMTBImage( this.labelImg.getSizeX(), this.labelImg.getSizeY(),
                            1, 1, 1, MTBImage.MTBImageType.MTB_SHORT);
     
      this.tmpImg =  MTBImage.createMTBImage( this.labelImg.getSizeX(), this.labelImg.getSizeY(), 
							1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);

	  // lookup all labels in input lalel image
      Collection<Integer> labelVec = new Vector<Integer>();
      int maxLabel = 0;

      for (int y = 0; y < this.labelImg.getSizeY(); y++) {
          Integer l;
          for (int x = 0; x < this.labelImg.getSizeX(); x++) {
              l = this.labelImg.getValueInt( x, y);
			  this.resultImg.putValueInt( x, y, l);
              if ( ! labelVec.contains( l) )
				labelVec.add( l);
              if ( l > maxLabel ) maxLabel = l;
          }
      }

	  // recursively try to separate each component
      Collection<Integer>  newLabelVec;
      boolean done = false;
      while ( ! done ) {
          if ( this.debug > 0 ) {
              System.out.println( "NucleusSeparator2DAlgos::operate new iteration of labels");
		  }

          done = true;
          newLabelVec = new Vector<Integer>();

          for ( Integer l : labelVec ) {
			  // label == 0 is background
              if ( l != 0 ) {
                  int n = separate( l, maxLabel+1);

                  if ( this.debug > 0 ) {
                       System.out.println( "  Trying to separate: " + l + " yields " + n + " new components");
                  }
 
                  if ( n != 0 ) {
					  // all components resulting from separation need to be considered recursiveley
                      newLabelVec.add( l);
                      for ( int nn = 1 ; nn <= n ; nn++ )
                          newLabelVec.add( maxLabel+nn);

                      maxLabel += n;
                      done = false;
                  }
              }
          }

          labelVec = newLabelVec;
          
      }
  }

	
  // the following is rather ugly code, needs amending
  // ================================================================
  /**
     try to separate component with label
   * 2. Mittelpunktschaetzung: Details in Methode unten
   * 3. kreisfoermige Abtastung des Embryos mit Strahlen und Berechnung von 360 Schnittpunkten zw. Strahl und Embryorand
   * 4. euklid. Differenz zwischen benachbarten Schnittpunkten berechnen und daraus relevante Schnittpunkte als Abtrennpunkte erkennen
   * 5. Fallunterscheidung je nach Anzahl der gefundenen Trennpunkte
   */

  private int separate( int label, int nextLabel) 
	throws ALDOperatorException, ALDProcessingDAGException {

    int[] m = estimateMean( label); //2.
    double mx = m[0];
    double my = m[1];

    if ( this.debug >= 3 ) 
       System.out.println(m[0] + " " + m[1]);

    ///////////////////3.///////////////////////////////////////////////
    int feinheit = 360;
    double step = 2.0d / (double) (feinheit - 1);
    double borderPoint[][] = new double[feinheit][2]; //Matrix fuer Ablage der Schnittpunkte
    double x, y;

    int count1 = 0; //Zaehler fuer Anzahl der Schnittpunkte
    for (double i = 0; i < 2; i = i + step) {
      for (double rad = 5; ; rad++) {
        x = mx + rad * Math.cos(Math.PI * i);
        y = my + rad * Math.sin(Math.PI * i);
        if (x < 0 || (int)x >= this.resultImg.getSizeX() ||
            y < 0 || (int)y >= this.resultImg.getSizeY() ||
            this.resultImg.getValueInt( (int) x, (int) y) != label) {
          borderPoint[count1][0] = x;
          borderPoint[count1][1] = y;
          count1++;
          break;
        }
      }
    }

    /////////////////////////////////////////////////////////////////////////
    if ( this.debug >= 2 ) 
        System.out.println(borderPoint[0][0] + " " + borderPoint[0][1]);
    //System.out.println(val1 + " " + val2);

    int[] temp = new int[20]; //Position der Cutpoints in der Reihenfolge der Abtastung
    for (int i = 0; i < temp.length; i++) {
      temp[i] = -1;
    }

    ////////////////////////////4.//////////////////////////////////////////////
    int count2 = 0;

    // why only feinheit - 1 ?? (sp)
    double[][] distance = new double[feinheit - 1][2]; // distance between subsequent borderPoints

    double[] radius = new double[feinheit]; // distance of borderPoints to centroid
    double[] radiusSorted = new double[feinheit]; // sorted distance of borderPoints to centroid


    for (int i = 0; i < distance.length; i++) {
      distance[i][0] = Math.sqrt( (borderPoint[i][0] - borderPoint[i + 1][0]) *
                              (borderPoint[i][0] - borderPoint[i + 1][0]) +
                              (borderPoint[i][1] - borderPoint[i + 1][1]) *
                              (borderPoint[i][1] - borderPoint[i + 1][1]));
      distance[i][1] = i;
    }

    for (int i = 0; i < radius.length; i++) {
      radius[i] = Math.sqrt (Math.pow(borderPoint[i][0] - m[0], 2) +
                             Math.pow(borderPoint[i][1] - m[1], 2));
    }

    radiusSorted = Arrays.copyOf( radius, radius.length);
    Arrays.sort( radiusSorted);
    double medianRadius = radiusSorted[radiusSorted.length/2];
    if ( this.debug >= 3 ) {
        System.out.println( "Distances between border points; (Median = " +
                            medianRadius + ")");
        for (int i = 0; i < radius.length; i++) {
            System.out.println( "    i: " + i + "\t" + radius[i]);
        }
    }

    int[] cutPoints = new int[feinheit]; 
    boolean outside[] = new boolean[feinheit];

    double radiusTH[] = topHat1D( radius, this.sizeTH);

    if ( this.debug >= 3 ) {
        System.out.println( "Top hat transform");
        for (int i = 0; i < radius.length; i++) {
            System.out.println( "    i: " + i + "\t" + radiusTH[i]);
        }
    }
    // threshold top hat transform
    for (int i = 0; i < radius.length; i++) {
        radiusTH[i] = ( radiusTH[i] > this.thresTH ) ? 1.0 : 0.0;
    }

    // ... and open it
    radiusTH = open1D( radiusTH, this.sizeOpening);
    radiusTH = dilate1D( radiusTH, this.sizeDilation);

    for (int i = 0; i < radius.length; i++) {
        outside[i] = ( radiusTH[i]  > 0.5 ) ? true : false;
    }

    if ( this.debug >= 3 ) {
        System.out.println( "    outside");
        for (int i = 0; i < outside.length; i++) {
            System.out.println( "    i: " + i + "\t" + outside[i]);
        }
    }

    // count cutPoints
    int countCutPoints = 0;
    for (int i = 0; i < radius.length; i++) {
        boolean last = i == 0 ? outside[ outside.length-1] : outside[i-1];
        boolean next = i == outside.length-1 ? outside[ 0] : outside[i+1];

        if ( ( outside[i] == true && last == false )  ||
             ( outside[i] == true && next == false )      )  {

            cutPoints[ countCutPoints] = i;
            countCutPoints++;
        }
    }
    cutPoints = Arrays.copyOf( cutPoints, countCutPoints);

    if ( this.debug >= 2 )  {
        System.out.println( "cutPoints (# " + countCutPoints + ")");
        for (int i = 0; i < cutPoints.length; i++) {
            System.out.println( "     " + cutPoints[i]);
        }
    }


    ////////////////////////////////6.///////////////////////////////////////////
    // now finally do the separation
    int numberSeparations;
    if (cutPoints.length == 1) { //genau 1 Trennpunkt gefunden

     numberSeparations = 1;
    }
//2 Punkte--> Abtrenngerade nach 2-Punkte Gleichung
    else if (cutPoints.length == 2) { //genau 2 Trennpunkte gefunden

      double xI = borderPoint[cutPoints[0]][0];
      double yI = borderPoint[cutPoints[0]][1];
      double xII = borderPoint[cutPoints[1]][0];
      double yII = borderPoint[cutPoints[1]][1];

      if ( this.debug >= 1 ) 
          System.out.println( "Found two cutPoints: " + xI + "," + yI + " --- " + xII + "," + yII);

      double x0 = xI;
      double y0 = yI;
      double x1 = xII - xI;
      double y1 = yII - yI;
      double scaleNV = 1 / Math.sqrt(x1 * x1 + y1 * y1);

      double nX = scaleNV * y1;
      double nY = -scaleNV * x1;

      if ( (m[0] - xI) * nX + (m[1] - yI) * nY > 0) {
        nX = -scaleNV * y1;
        nY = scaleNV * x1;
      }
      ///////////////////////////////////////////////////////////////////////////

      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
        for (int samp = 0; samp < this.resultImg.getSizeX(); samp++) {

          if (this.resultImg.getValueInt(samp, line) == label) {
            double s = - ( -x1 * y0 + x0 * y1 - samp * y1 + x1 * line) /
                (nY * x1 - nX * y1); //Parameter fuer Schnittpunkt
            double xF = samp + s * nX; //Fusspunkt des Lotes auf der Trenngerade
            double yF = line + s * nY;

            if ( ( (samp - xF) * nX + (line - yF) * nY) > 0) {
              if ( this.resultImg.getValueInt(samp, line) == label ) 
                  this.resultImg.putValueInt(samp, line, nextLabel);
            }
          }
          else {
            continue;
          }
        }
      }

      // remove all but the largest connected component with nextLabel
      keepLargestCC( this.resultImg, nextLabel);

      // check size
      int size1 = getSize( label);
      int size2 = getSize( nextLabel);
      int sum = size1+size2;

      if ( (((double)size1 / sum) < this.minSizeFraction ) ||
           (((double)size2 / sum) < this.minSizeFraction )  ||
           size1 < this.minSize || size2 < this.minSize ) {

          if ( this.debug >= 1 ) System.out.println( "undo separation due to size constraint");
 

          resetLabel( nextLabel, label);
          numberSeparations = 0;
      } else {

          if ( this.doErosion ) {
              erodeComponent( label, this.erodeSize);
              keepLargestCC( this.resultImg, label);
              erodeComponent( nextLabel, this.erodeSize);
              keepLargestCC( this.resultImg, nextLabel);
          }
          numberSeparations = 1;
      }
    }

// 2 Trenngeraden bilden mit den euklidisch jeweils nahestehendsten Punkten
    else if (cutPoints.length == 4) { //genau 4 Trennpunkte gefunden

      numberSeparations =  2;
      if ( this.debug >= 1 ) 
          System.out.println( "Found four cutPoints");

      double dif1 = Math.pow(borderPoint[cutPoints[0]][0] -
                             borderPoint[cutPoints[1]][0], 2) +
          Math.pow(borderPoint[cutPoints[0]][1] - borderPoint[cutPoints[1]][1], 2);
      double dif2 = Math.pow(borderPoint[cutPoints[0]][0] -
                             borderPoint[cutPoints[3]][0], 2) +
          Math.pow(borderPoint[cutPoints[0]][1] - borderPoint[cutPoints[3]][1], 2);

      if (dif1 > dif2) {
        int tempi;
        tempi = cutPoints[0];
        cutPoints[0] = cutPoints[2];
        cutPoints[2] = tempi;
      }

      // -------------------------------------
      // first separating line
      double xI = borderPoint[cutPoints[0]][0];
      double yI = borderPoint[cutPoints[0]][1];
      double xII = borderPoint[cutPoints[1]][0];
      double yII = borderPoint[cutPoints[1]][1];

      double x0 = xI;
      double y0 = yI;
      double x1 = xII - xI;
      double y1 = yII - yI;
      double scaleNV = 1 / Math.sqrt(x1 * x1 + y1 * y1);

      double nX = scaleNV * y1;
      double nY = -scaleNV * x1;

      if ( (m[0] - xI) * nX + (m[1] - yI) * nY > 0) {
        nX = -scaleNV * y1;
        nY = scaleNV * x1;
      }
      ///////////////////////////////////////////////////////////////////////////

      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
        for (int samp = 0; samp < this.resultImg.getSizeX(); samp++) {

          if (this.resultImg.getValueInt(samp, line) == label) {
            double s = - ( -x1 * y0 + x0 * y1 - samp * y1 + x1 * line) /
                (nY * x1 - nX * y1); //Parameter fuer Schnittpunkt
            double xF = samp + s * nX; //Fusspunkt des Lotes auf der Trenngerade
            double yF = line + s * nY;

            if ( ( (samp - xF) * nX + (line - yF) * nY) > 0) {
              if ( this.resultImg.getValueInt(samp, line) == label )
                  this.resultImg.putValueInt(samp, line, nextLabel);

            }
          }
          else {
            continue;
          }
        }
      }

      // remove all but the largest connected component with nextLabel
      keepLargestCC( this.resultImg, nextLabel);

      // check size
      int size1 = getSize( label);
      int size2 = getSize( nextLabel);
      int sum = size1+size2;

      if ( (((double)size1 / sum) < this.minSizeFraction ) ||
           (((double)size2 / sum) < this.minSizeFraction ) ||
           size1 < this.minSize || size2 < this.minSize) {

          if ( this.debug >= 1 ) System.out.println( "undo separation for nextLabel due to size constraint");
 

          resetLabel( nextLabel, label);
          numberSeparations--;
      } else {
          if ( this.doErosion ) {
              erodeComponent( nextLabel, this.erodeSize);
              keepLargestCC( this.resultImg, nextLabel);
          }
      }



      // -------------------------------------
      // second separating line
      xI = borderPoint[cutPoints[2]][0];
      yI = borderPoint[cutPoints[2]][1];
      xII = borderPoint[cutPoints[3]][0];
      yII = borderPoint[cutPoints[3]][1];

      x0 = xI;
      y0 = yI;
      x1 = xII - xI;
      y1 = yII - yI;
      scaleNV = 1 / Math.sqrt(x1 * x1 + y1 * y1);

      nX = scaleNV * y1;
      nY = -scaleNV * x1;

      if ( (m[0] - xI) * nX + (m[1] - yI) * nY > 0) {
        nX = -scaleNV * y1;
        nY = scaleNV * x1;
      }

      ///////////////////////////////////////////////////////////////////////////

      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
        for (int samp = 0; samp < this.resultImg.getSizeX(); samp++) {

          if (this.resultImg.getValueInt(samp, line) == label) {
            double s = - ( -x1 * y0 + x0 * y1 - samp * y1 + x1 * line) /
                (nY * x1 - nX * y1); //Parameter fuer Schnittpunkt
            double xF = samp + s * nX; //Fusspunkt des Lotes auf der Trenngerade
            double yF = line + s * nY;

            if ( ( (samp - xF) * nX + (line - yF) * nY) > 0) {
              if ( this.resultImg.getValueInt(samp, line) == label )
                  this.resultImg.putValueInt(samp, line, nextLabel+1);
            }
          } else {
            continue;
          }
        }
      }

      // remove all but the largest connected component with nextLabel
      keepLargestCC( this.resultImg, nextLabel+1);

      // check size
      size1 = getSize( label);
      size2 = getSize( nextLabel+1);
      sum = size1+size2;

      if ( (((double)size1 / sum) < this.minSizeFraction ) ||
           (((double)size2 / sum) < this.minSizeFraction ) ||
           size1 < this.minSize || size2 < this.minSize) {

          if ( this.debug >= 1 ) System.out.println( "undo separation for nextLabel+1 due to size constraint");
 

          resetLabel( nextLabel+1, label);
          numberSeparations--;
      } else {

          if ( this.doErosion ) {
              erodeComponent( nextLabel+1, this.erodeSize);
              keepLargestCC( this.resultImg, nextLabel+1);
          }

      }
    } else { 
        if ( this.debug >= 2 ) 
          System.out.println( "Found " + cutPoints.length + " cutPoints");

        numberSeparations =  0;
    }

    // initial component always remains
    if ( this.doErosion && (numberSeparations > 0) ) {
          erodeComponent( label, this.erodeSize);
          keepLargestCC( this.resultImg, label);
    }


    return numberSeparations;

  }

  /**
   * Suche nach bestem Mittelpunkt des Embryos: ueberpruefe kreisfoermige Raster (8 Punkte +Mittelpunkt des Bildes) um Bildmittelpunkt aus
   * Kriterium: max. min. Abstand zum Embryorand
   * @return int[]
   */
  private int[] estimateMean( int label) {
    double centroidRec[] = getCentroid( this.resultImg, label);

    int[] m = { (int)centroidRec[0], (int)centroidRec[1]};
    double mx = m[0];
    double my = m[1];

    double width = centroidRec[2];
    double height = centroidRec[3];
    //double r = (double) resultImg.getSizeY() * 0.15;
    double r = (double) m[1] * 0.15;

    boolean update = true;
    double dir = 0;
    if ( this.debug >= 2 ) {
        System.out.println("estimateMean: ");
	System.out.println("    Original centroid: " + m[0] + " " + m[1]);
	System.out.println("    Radius" + r);
    }



    if (update) { //berechne Abstaende fuer 9 moegliche Mittelpunkte zum Bildrand
        double[][] meanCandidates = new double[9][2];
        meanCandidates[0][0] = m[0];
        meanCandidates[0][1] = m[1];
        meanCandidates[1][0] = m[0] - (double) width * 0.25;
        meanCandidates[1][1] = m[1];
        meanCandidates[2][0] = m[0] + (double) width * 0.25;
        meanCandidates[2][1] = m[1];
        meanCandidates[3][0] = m[0];
        meanCandidates[3][1] = m[1] - (double) height * 0.25;
        meanCandidates[4][0] = m[0];
        meanCandidates[4][1] = m[1] + (double) height * 0.25;
        meanCandidates[5][0] = m[0] - (double) width * 0.25;
        meanCandidates[5][1] = m[1] - (double) height * 0.25;
        meanCandidates[6][0] = m[0] - (double) width * 0.25;
        meanCandidates[6][1] = m[1] + (double) height * 0.25;
        meanCandidates[7][0] = m[0] + (double) width * 0.25;
        meanCandidates[7][1] = m[1] + (double) height * 0.25; 
        meanCandidates[8][0] = m[0] + (double) width * 0.25;
        meanCandidates[8][1] = m[1] - (double) height * 0.25;

        double[][] borderpoint = new double[8][2];  //sp corrected to coarseness of approxmiation !!!
        double[] mindist = new double[9];
        double x, y;
        
        // for each potential centroid (meanCandidates) compute distance to
        // background (approximated!!)
        for (int j = 0; j < 9; j++) {
                //drawSquare( (int)meanCandidates[j][0], (int)meanCandidates[j][1], 2, valSquare );
                for (double i = 0; i < 2; i = i + 0.25) {
                        for (double rad = 5; ; rad++) {
                                x = meanCandidates[j][0] + rad * Math.cos(Math.PI * i);
                                y = meanCandidates[j][1] + rad * Math.sin(Math.PI * i);

                                if ( x < 0 || (int)x >= this.resultImg.getSizeX() ||
                                        y < 0 || (int)y >= this.resultImg.getSizeY() ||
                                        this.resultImg.getValueInt( (int) x, (int) y) == 0.0) {
                                    borderpoint[ (int) (i * 4.0d)][0] = x;
                                    borderpoint[ (int) (i * 4.0d)][1] = y;
                                   break;
                                }
                        }
                }

        if ( this.debug >= 2 )
            System.out.println("      potential centroid (" + j + "): " +
                                meanCandidates[j][0]+","+meanCandidates[j][1]);
        if ( this.debug >= 3 ) {
            System.out.print( "           Borderpoints: ");
            for (int h = 0; h < 8; h++) {
                System.out.println( "                     (" + borderpoint[h][0]+","+borderpoint[h][1]+")");
            }
            System.out.println();
        }

        if ( this.debug >= 2 ) 
            System.out.print( "           Distances; ");
        for (int k = 0; k < 8; k++) {
            double dist = euklidDist(meanCandidates[j][0], meanCandidates[j][1],
            borderpoint[k][0], borderpoint[k][1]);
            if ( this.debug >= 2 ) 
                System.out.print( "  " +dist);

            if (dist < mindist[j] || mindist[j] == 0) {
                mindist[j] = dist;
            }
        }

        if ( this.debug >= 2 ) 
            System.out.println( );
        }
    
        int maxpos = 0;
        double max = 0;

        if ( this.debug >= 2 )
            System.out.println( "           Max Distance");

        for (int i = 0; i < 9; i++) {
            if ( this.debug >= 2 )
                System.out.println( "               " + i + ": " + mindist[i]);
    
            if (mindist[i] > max) {
                max = mindist[i];
                maxpos = i;
            }
        }

        m[0] = (int) meanCandidates[maxpos][0];
        m[1] = (int) meanCandidates[maxpos][1];
    
        //??drawSquare( m[0], m[1], 4, valSquare );
        if ( this.debug >= 2 )
            System.out.println( "           Best centroid " +maxpos +
                                ": (" + m[0] + "," + m[1] + ")");

	}
	return m;
   }

   // ============================================================00
	  /**
	   * Hilfsmethode zur Bestimmung der euklidischen Distanz zw. 2 Punkten
	   * @param x1 double
	   * @param y1 double
	   * @param x2 double
	   * @param y2 double
	   * @return double
	   */
	  private double euklidDist(double x1, double y1, double x2, double y2) {
	    double dist = Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	    return dist;
	  }

     
	  /**
	   * auxiliary method to determine the centroid (x,y) and width/heigth of compoment with label
	   *      in a double array of length 4
	   *      (should go in class/record)
	   *
	   * @param img 
	   * @param label 
	   * @return double[]
	   */
	  private double[] getCentroid( MTBImage img, int label) {
	     double res[] = new double[4];
	     int count = 0;
	     int xmin = img.getSizeX();
	     int xmax = 0;
	     int ymin = img.getSizeY();
	     int ymax = 0;

	     res[0] = res[1] = 0.0;  //x,y of centroid

	     for (int line = 0; line < img.getSizeY(); line++) {
		for (int col = 0; col < img.getSizeX(); col++) {
		    if ( img.getValueInt( col, line) == label ) {
			res[0] += col;
			res[1] += line;
			count++;

			if ( col < xmin ) xmin = col;
			if ( col > xmax ) xmax = col;
			if ( line < ymin ) ymin = line;
			if ( line > ymax ) ymax = line;
		    }
		}
	     }

            res[0] /= count;
            res[1] /= count;
            res[2] = xmax - xmin + 1;
            res[3] = ymax - ymin + 1;

	    if ( this.debug >= 2 )
		System.out.println( "getCentroid: (" + res[0] + "," + res[1] + 
				    "), width: " + res[2] + " Height: " + res[3]);
            return res;
          }

  // ========================================================
  /**
  */
  private double[] erode1D( double x[], int size) {

      double res[] = new double[x.length];
      double window[] = new double[size];

      for ( int i=0 ; i < x.length ; i++ ) {
          for ( int s=0 ; s < size ; s++ ) {
              window[s] = x[ (i-size/2 + s + x.length) % x.length];
          }

          Arrays.sort( window);
          res[i] = window[0];
      }

      return res;
  }

  // ========================================================
  /**
  */
  private double[] dilate1D( double x[], int size) {

      double res[] = new double[x.length];
      double window[] = new double[size];

      for ( int i=0 ; i < x.length ; i++ ) {
          for ( int s=0 ; s < size ; s++ ) {
              window[s] = x[ (i-size/2 + s + x.length) % x.length];
          }

          Arrays.sort( window);
          res[i] = window[window.length-1];
      }

      return res;
  }

  // ========================================================
  /**
  */
  private double[] open1D( double x[], int size) {

      return dilate1D( erode1D( x, size), size);
  }

  // ========================================================
  /**
  */
  private double[] topHat1D( double x[], int size) {

      double res[] = open1D( x, size);

      for ( int i=0 ; i < x.length ; i++ ) {
          res[i] = x[i] - res[i];
      }

      return res;


  }

  // ========================================================
  /**
  */
  private int getSize( int label) {
      int size = 0;
      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
        for (int col = 0; col < this.resultImg.getSizeX(); col++) {
            if ( this.resultImg.getValueInt( col, line) == label ) size++;
        }
      }

     return size;
  }

  // ========================================================
  /**
  */
  private int resetLabel( int fromLabel, int toLabel) {
      int size = 0;
      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
        for (int col = 0; col < this.resultImg.getSizeX(); col++) {
            if ( this.resultImg.getValueInt( col, line) == fromLabel ) 
                this.resultImg.putValueInt( col, line, toLabel);
        }
      }

     return size;
  }

  // ========================================================
  /**
      remove all but the largest connected component with label <label>
      from <img>
  */
  private void keepLargestCC( MTBImage img, int label) 
	throws ALDOperatorException, ALDProcessingDAGException {

      // fill tmp binary image
      for (int line = 0; line < img.getSizeY(); line++) {
        for (int col = 0; col < img.getSizeX(); col++) {
            if ( img.getValueInt( col, line) == label )
                this.tmpImg.putValueInt( col, line, 1);
            else
                this.tmpImg.putValueInt( col, line, 0);
        }
      }


      LabelComponentsSequential regLabler=
		new LabelComponentsSequential( this.tmpImg, false);
      regLabler.runOp( true);
	  MTBImage 	newLabelImg = regLabler.getLabelImage();

      // reset pixels with value <label> if not in largest CC
      for (int line = 0; line < img.getSizeY(); line++) {
        for (int col = 0; col < img.getSizeX(); col++) {
            if ( img.getValueInt( col, line) == label &&
                 newLabelImg.getValueInt( col, line) == 0 )
                img.putValueInt( col, line, 0);
         
        }
      }
  }

  // ====== Getter/Setter

	/** Get value of thresTH.
	  * Explanation: Threshold for result from tophat.
	  * @return value of thresTH
	  */
	public double getThresTH(){
		return this.thresTH;
	}

	/** Set value of thresTH.
	  * Explanation: Threshold for result from tophat.
	  * @param value New value of thresTH
	  */
	public void setThresTH( double value){
		this.thresTH = value;
	}

	/** Get value of minSizeFraction.
	  * Explanation: ??? Minimum size of ????.
	  * @return value of minSizeFraction
	  */
	public double getMinSizeFraction(){
		return this.minSizeFraction;
	}

	/** Set value of minSizeFraction.
	  * Explanation: ??? Minimum size of ????.
	  * @param value New value of minSizeFraction
	  */
	public void setMinSizeFraction( double value){
		this.minSizeFraction = value;
	}

	/** Get value of sizeOpening.
	  * Explanation: Size of structuring element for opening.
	  * @return value of sizeOpening
	  */
	public int getSizeOpening(){
		return this.sizeOpening;
	}

	/** Set value of sizeOpening.
	  * Explanation: Size of structuring element for opening.
	  * @param value New value of sizeOpening
	  */
	public void setSizeOpening( int value){
		this.sizeOpening = value;
	}

	/** Get value of sizeTH.
	  * Explanation: Size of structuring element for tophat.
	  * @return value of sizeTH
	  */
	public int getSizeTH(){
		return this.sizeTH;
	}

	/** Set value of sizeTH.
	  * Explanation: Size of structuring element for tophat.
	  * @param value New value of sizeTH
	  */
	public void setSizeTH( int value){
		this.sizeTH = value;
	}

	/** Get value of minSize.
	  * Explanation: Minimum size of ????.
	  * @return value of minSize
	  */
	public int getMinSize(){
		return this.minSize;
	}

	/** Set value of minSize.
	  * Explanation: Minimum size of ????.
	  * @param value New value of minSize
	  */
	public void setMinSize( int value){
		this.minSize = value;
	}

	/** Get value of sizeDilation.
	  * Explanation: Size of structuring element for dilation.
	  * @return value of sizeDilation
	  */
	public int getSizeDilation(){
		return this.sizeDilation;
	}

	/** Set value of sizeDilation.
	  * Explanation: Size of structuring element for dilation.
	  * @param value New value of sizeDilation
	  */
	public void setSizeDilation( int value){
		this.sizeDilation = value;
	}

	/** Get value of opMode.
	  * Explanation: Operator sepration mode.
	  * @return value of opMode
	  */
	public NucleusSeparator2DBeamCut.NuclSeparateMode getOpMode(){
		return this.opMode;
	}

	/** Set value of opMode.
	  * Explanation: Operator sepration mode.
	  * @param value New value of opMode
	  */
	public void setOpMode( NucleusSeparator2DBeamCut.NuclSeparateMode value){
		this.opMode = value;
	}

	/** Get value of labelImg.
	  * Explanation: Label input image of nuclei.
	  * @return value of labelImg
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getLabelImg(){
		return this.labelImg;
	}

	/** Set value of labelImg.
	  * Explanation: Label input image of nuclei.
	  * @param value New value of labelImg
	  */
	public void setLabelImg( de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage value){
		this.labelImg = value;
	}

	/** Get value of debug.
	  * Explanation: Flag for debugging output..
	  * @return value of debug
	  */
	public int getDebug(){
		return this.debug;
	}

	/** Set value of debug.
	  * Explanation: Flag for debugging output..
	  * @param value New value of debug
	  */
	public void setDebug( int value){
		this.debug = value;
	}

	/** Get value of erodeSize.
	  * Explanation: Size of structuring element for eroding a nucleus from a separation.
	  * @return value of erodeSize
	  */
	public int getErodeSize(){
		return this.erodeSize;
	}

	/** Set value of erodeSize.
	  * Explanation: Size of structuring element for eroding a nucleus from a separation.
	  * @param value New value of erodeSize
	  */
	public void setErodeSize( int value){
		this.erodeSize = value;
	}

	/** Get value of doErosion.
	  * Explanation: Erode each nucleus resulting from a separation.
	  * @return value of doErosion
	  */
	public boolean getDoErosion(){
		return this.doErosion;
	}

	/** Set value of doErosion.
	  * Explanation: Erode each nucleus resulting from a separation.
	  * @param value New value of doErosion
	  */
	public void setDoErosion( boolean value){
		this.doErosion = value;
	}

  // ================================================================
  /**
    * erode one component with given label
    */

    private void erodeComponent( int label, int size) 
		throws ALDOperatorException, ALDProcessingDAGException {
      // fill temp binary image
      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
          for (int col = 0; col < this.resultImg.getSizeX(); col++) {
              if ( this.resultImg.getValueInt( col, line) == label )
                  this.tmpImg.putValueInt( col, line, 1);
              else
                  this.tmpImg.putValueInt( col, line, 0);
          }
      }

	  ImgErode erodeOp = new ImgErode( this.tmpImg, this.erodeSize);
	  erodeOp.runOp( true);
      MTBImage 	erodedImg = erodeOp.getResultImage();

      for (int line = 0; line < this.resultImg.getSizeY(); line++) {
          for (int col = 0; col < this.resultImg.getSizeX(); col++) {
              if ( this.resultImg.getValueInt( col, line) == label ) {
                  if ( erodedImg.getValueInt( col, line) == 1 )
                      this.resultImg.putValueInt( col, line, label);
                  else
                      this.resultImg.putValueInt( col, line, 0);
              }
          }
      }

    }

}
