package de.unihalle.informatik.MiToBo.apps.minirhizotron.utils;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRootImageAnnotationType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRootSegmentStatusType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRootSegmentType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRootType;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Font;
import java.util.EnumSet;

/**
 * Created by Andreas Lange on 8/30/17.
 */
public class Drawing
{
    public enum DrawType
    {
        CENTERLINE,
        CONTOURS,
        BOTH,
        BINARY,
        NONE
    }

    public enum ROOTSEGMENT_STATUS
    {
        LIVING,
        DEAD,
        DECAYED,
        GAP
    }

    /**
     * @param rootImageAnnotationType
     * @param image
     * @param drawType
     * @param rootSegmentStatusToDraw_Set
     * @throws ALDOperatorException
     * @throws ALDProcessingDAGException
     */
    public static MTBImage draw(MTBXMLRootImageAnnotationType rootImageAnnotationType, MTBImage image,
                                DrawType drawType,
                                EnumSet<ROOTSEGMENT_STATUS> rootSegmentStatusToDraw_Set)
            throws ALDOperatorException, ALDProcessingDAGException
    {
        MTBImage resultImage = null;

        if (drawType == DrawType.BINARY)
        {
            resultImage = MTBImage.createMTBImage(image.getSizeX(), image.getSizeY(), image.getSizeZ(), image.getSizeT(), image.getSizeC(), MTBImage.MTBImageType.MTB_BYTE);
        }
        else
        {
            resultImage = image.duplicate(ALDOperator.HidingMode.HIDDEN);
        }

        final int deadColor = 0xff0000; // red
        final int livingColor = 0x00ff00; // green
        final int decayedColor = 0x0000ff; // blue
        final int greyColor = 0x999999;
        final int gapColor = 0xffff00; // yellow
        final int blackColor = 0x000000;
        final int whiteColor = 0x000001;

        MTBXMLRootType[] roots = rootImageAnnotationType.getRootsArray();

        for (int i = 0; i < roots.length; i++)
        {
            MTBXMLRootType currentRoot = roots[i];
            MTBXMLRootSegmentType[] segments = currentRoot.getRootSegmentsArray();

            for (int j = 0; j < segments.length; j++)
            {
                MTBXMLRootSegmentType currentSegment = segments[j];

                if (matchesRootSegmentStatusToDraw(rootSegmentStatusToDraw_Set, currentSegment.getType()))
                {
                    int color = greyColor; // grey = if no status can be assigned

                    if (drawType == DrawType.CENTERLINE ||
                            drawType == DrawType.CONTOURS ||
                            drawType == DrawType.BOTH)
                    {
                        if (currentSegment.getType() == MTBXMLRootSegmentStatusType.LIVING) color = livingColor;
                        else if (currentSegment.getType() == MTBXMLRootSegmentStatusType.DEAD) color = deadColor;
                        else if (currentSegment.getType() == MTBXMLRootSegmentStatusType.DECAYED) color = decayedColor;
                        else if (currentSegment.getType() == MTBXMLRootSegmentStatusType.GAP) color = gapColor;

                        int cx1 = (int) currentSegment.getStartPoint().getX();
                        int cy1 = (int) currentSegment.getStartPoint().getY();

                        int cx2 = (int) currentSegment.getEndPoint().getX();
                        int cy2 = (int) currentSegment.getEndPoint().getY();

                        resultImage.drawPoint2D(cx1, cy1, 0, color, 0);
                        resultImage.drawCircle2D(cx1, cy1, 0, (int) currentSegment.getStartRadius(), color);

                        resultImage.drawPoint2D(cx2, cy2, 0, color, 0);
                        resultImage.drawCircle2D(cx2, cy2, 0, (int) currentSegment.getEndRadius(), color);

                        if (drawType == DrawType.BOTH || drawType == DrawType.CENTERLINE)
                        {
                            resultImage.drawLine2D(cx1, cy1, cx2, cy2, color);
                        }

                        if (drawType == DrawType.BOTH || drawType == DrawType.CONTOURS)
                        {
                            int[] a = getContourPoints(cx1, cy1, cx2, cy2, (int) currentSegment.getStartRadius());
                            int[] b = getContourPoints(cx2, cy2, cx1, cy1, (int) currentSegment.getEndRadius());

                            resultImage.drawLine2D(a[0], a[1], b[2], b[3], color);
                            resultImage.drawLine2D(a[2], a[3], b[0], b[1], color);
                        }
                    }

                    if (drawType == DrawType.BINARY)
                    {
                        ImagePlus imp = resultImage.getImagePlus();

                        int cx1 = (int) currentSegment.getStartPoint().getX();
                        int cy1 = (int) currentSegment.getStartPoint().getY();

                        int cx2 = (int) currentSegment.getEndPoint().getX();
                        int cy2 = (int) currentSegment.getEndPoint().getY();

                        int startRadius = (int) currentSegment.getStartRadius();
                        int endRadius = (int) currentSegment.getEndRadius();

                        drawSegment(imp, cx1, cy1, cx2, cy2, startRadius, endRadius, whiteColor);
                        drawFilledCircle(imp, cx1, cy1, startRadius, whiteColor);
                        drawFilledCircle(imp, cx2, cy2, endRadius, whiteColor);

                        imp.updateAndDraw();
                    }
                }
            }
        }

        resultImage.setTitle(image.getTitle());
        return resultImage;
    }


    /**
     * Computes both points perpendicular to the vector x1,x2 with distance 'distance'.
     *
     * @param x1       First point, x coordinate
     * @param y1       First point, y coordinate
     * @param x2       Second point, x coordinate
     * @param y2       Second point, y coordinate
     * @param distance The distance of each point from the vector
     * @return An array containing the coordinates of both points: {p1_x, p1_y, p2_x, p2_y}
     */
    private static int[] getContourPoints(int x1, int y1, int x2, int y2, int distance)
    {
        double lx = (double) (x2 - x1);
        double ly = (double) (y2 - y1);
        double len = Math.sqrt(lx * lx + ly * ly);
        lx /= len;
        ly /= len;

        double r1 = distance % 2 == 0 ? distance * 2 + 1 : distance * 2;
        r1 /= 2;

        int a1x = x1 + (int) Math.round((ly * r1));
        int a1y = y1 + (int) Math.round((-lx * r1));
        int a2x = x1 + (int) Math.round((-ly * r1));
        int a2y = y1 + (int) Math.round((lx * r1));

        return new int[]{a1x, a1y, a2x, a2y};
    }

    /**
     * Draws a filled rectangle.
     *
     * @param imp         The image on which is drawn
     * @param x1          x-coordinate of the start point of the centerline
     * @param y1          y-coordinate of the start point of the centerline
     * @param x2          x-coordinate of the end point of the centerline
     * @param y2          y-coordinate of the end point of the centerline
     * @param startRadius Root radius of start point
     * @param endRadius   Root radius of end point
     */
    public static void drawSegment(ImagePlus imp, int x1, int y1, int x2, int y2, int startRadius, int endRadius,
                                   int color)
    {
        int[] a = getContourPoints(x1, y1, x2, y2, startRadius);
        int[] b = getContourPoints(x2, y2, x1, y1, endRadius);

        ByteProcessor ip = (ByteProcessor) imp.getProcessor();
        PolygonRoi polygonRoi = new PolygonRoi(new int[]{a[0], a[2], b[0], b[2]}, new int[]{a[1], a[3], b[1], b[3]}, 4, Roi.POLYGON);
        ip.setValue((double) color);
        ip.setMask(polygonRoi.getMask());
        ip.setRoi(polygonRoi.getBoundingRect());
        ip.fill(ip.getMask());
    }


    /**
     * Draws a filled circle at position ('x', 'y') with radius 'radius'.
     *
     * @param imp    The image on which to draw.
     * @param x      x-coordinate of the circle center.
     * @param y      y-coordinate of the circle center.
     * @param radius The radius of the circle.
     * @param color  The fill color of the circle.
     */
    public static void drawFilledCircle(ImagePlus imp, int x, int y, 
    		float radius, int color) {
    	ImageProcessor ip = imp.getProcessor();
    	int width = 
    		((int) radius) % 2 == 0 ? ((int) radius) * 2 + 1 : ((int) radius) * 2;
    	OvalRoi oval = new OvalRoi(x - radius, y - radius, width, width);
    	ip.setValue(color);
    	ip.setMask(oval.getMask());
    	ip.setRoi(oval.getBounds());
    	ip.fill(ip.getMask());
    }


    /**
     * @param rootSegmentStatusToDraw_Set
     * @param status
     * @return
     */
    private static boolean matchesRootSegmentStatusToDraw(EnumSet<ROOTSEGMENT_STATUS> rootSegmentStatusToDraw_Set,
                                                          MTBXMLRootSegmentStatusType.Enum status)
    {

        for (ROOTSEGMENT_STATUS rsStat : rootSegmentStatusToDraw_Set)
        {
            switch (rsStat)
            {
                case LIVING:
                    if (status == MTBXMLRootSegmentStatusType.LIVING) return true;
                    break;
                case DEAD:
                    if (status == MTBXMLRootSegmentStatusType.DEAD) return true;
                    break;
                case DECAYED:
                    if (status == MTBXMLRootSegmentStatusType.DECAYED) return true;
                    break;
                case GAP:
                    if (status == MTBXMLRootSegmentStatusType.GAP) return true;
                    break;
                default:
                    break;
            }
        }

        return false;
    }       
    
    public static MTBImage drawRootIDs( MTBXMLRootType[] roots, MTBImage image, Color col)  {       
        ImageProcessor ip = image.getImagePlus().getProcessor();
        
        for(int i = 0; i < roots.length; i++) {
                MTBXMLRootSegmentType[] segments = roots[i].getRootSegmentsArray();
                
                for(int j = 0; j < segments.length; j++)
                {
                        MTBXMLRootSegmentType currentSegment = segments[j];
                        float x1 = currentSegment.getStartPoint().getX();
                        float y1 = currentSegment.getStartPoint().getY();
                        float x2 = currentSegment.getEndPoint().getX();
                        float y2 = currentSegment.getEndPoint().getY();
                        
                        ip.moveTo(((int)(x1+x2)/2), ((int)(y1+y2)/2) + 2);
                        ip.setColor(col);
                        ip.setFont(new Font("Arial", Font.PLAIN, 9));
                        ip.drawString("R"+currentSegment.getRootID());
                }
        }
        
        ImagePlus res = new ImagePlus("", ip);
        return MTBImage.createMTBImage(res);
}

    
    
}
