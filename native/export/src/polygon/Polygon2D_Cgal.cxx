/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

#include <jni.h>
#include <stdio.h>

#include <iostream>

#include "Polygon2D_Cgal.h"

#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>
#include <CGAL/enum.h>

#include <CGAL/Cartesian.h>
#include <CGAL/MP_Float.h>
#include <CGAL/Quotient.h>
#include <CGAL/Arrangement_2.h>
#include <CGAL/Arr_segment_traits_2.h>
#include <CGAL/Polygon_2.h>

typedef CGAL::Exact_predicates_inexact_constructions_kernel K;
typedef K::Point_2                              Point;
typedef CGAL::Polygon_2<K>                      Polygon_2;

typedef CGAL::Quotient<CGAL::MP_Float>          Number_type;
typedef CGAL::Cartesian<Number_type>            Kernel;
typedef CGAL::Arr_segment_traits_2<Kernel>      Traits_2;
typedef CGAL::Arrangement_2<Traits_2>           Arrangement_2;
typedef Traits_2::Point_2                       Point_2;
typedef Traits_2::X_monotone_curve_2            Segment_2;

using std::cout;
using std::endl;

/*****************************************************************************/

static Polygon_2* pointListToPolygon(int num, jdouble *xs, jdouble *ys);

static void print_ccb (Arrangement_2::Ccb_halfedge_const_circulator circ)
{
  Arrangement_2::Ccb_halfedge_const_circulator curr = circ;
  std::cout << "[" << curr->source()->point() << "]" << std::endl;
  do {
    std::cout << "\t--> [" << curr->target()->point() << "]"
              << std::endl;
  } while (++curr != circ);
  std::cout << std::endl;
}

/*****************************************************************************/

Polygon_2* pointListToPolygon(int num, jdouble *xs, jdouble *ys) 
{
  Polygon_2 *poly= new Polygon_2();
  for (int i=0;i<num;++i) {
    Point* p= new Point(xs[i],ys[i]);
    poly->push_back(*p);
    delete p;
  }
  return poly;
}


/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_isSimple
 * Signature: ([D[D)Z
 */
JNIEXPORT jboolean JNICALL 
Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1isSimple
  (JNIEnv *jenv, jobject jo, jdoubleArray jxs, jdoubleArray jys)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size_x= (jenv)->GetArrayLength(jxs);

  // convert point list to polygon
  Polygon_2 *pgn= pointListToPolygon(size_x, xcoords, ycoords);
  
  // check if the polygon is simple.
  jboolean isSimple= pgn->is_simple();
  
  // clean-up
  (jenv)->ReleaseDoubleArrayElements(jxs, xcoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jys, ycoords, NULL);
  delete pgn;

  // return boolean value
  return isSimple;
}

/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_isConvex
 * Signature: ([D[D)Z
 */
JNIEXPORT jboolean JNICALL 
Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1isConvex
  (JNIEnv *jenv, jobject, jdoubleArray jxs, jdoubleArray jys)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size_x= (jenv)->GetArrayLength(jxs);

  // convert point list to polygon
  Polygon_2 *pgn= pointListToPolygon(size_x, xcoords, ycoords);
  
  // check if the polygon is convex.
  jboolean isConvex= pgn->is_convex();
  
  // clean-up
  (jenv)->ReleaseDoubleArrayElements(jxs, xcoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jys, ycoords, NULL);
  delete pgn;

  // return boolean value
  return isConvex;
}

/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_orientation
 * Signature: ([D[D)I
 */
JNIEXPORT jint JNICALL Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1orientation
  (JNIEnv *jenv, jobject, jdoubleArray jxs, jdoubleArray jys, jdoubleArray jpt)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);
  jdouble *pt= (jenv)->GetDoubleArrayElements(jpt, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size_x= (jenv)->GetArrayLength(jxs);

  // convert point list to polygon
  Polygon_2 *pgn= pointListToPolygon(size_x, xcoords, ycoords);
  Point p(pt[0], pt[1]);

  // get orientation of point with regard to polygon
  CGAL::Oriented_side orient= pgn->oriented_side(p);

  // clean-up
  (jenv)->ReleaseDoubleArrayElements(jxs, xcoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jys, ycoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jpt, pt, NULL);
  delete pgn;

  // return orientation
  switch(orient)
  {
  case CGAL::ON_NEGATIVE_SIDE:
    return -1;
  case CGAL::ON_ORIENTED_BOUNDARY:
    return 0;
  case CGAL::ON_POSITIVE_SIDE:
    return 1;
  default:
    std::cerr << "Unknown orientation!!!" << std::endl;
  }
}

/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_isCounterclockwiseOriented
 * Signature: ([D[D)Z
 */
JNIEXPORT jboolean JNICALL 
Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1isCounterclockwiseOriented
  (JNIEnv *jenv, jobject, jdoubleArray jxs, jdoubleArray jys)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size_x= (jenv)->GetArrayLength(jxs);

  // convert point list to polygon
  Polygon_2 *pgn= pointListToPolygon(size_x, xcoords, ycoords);
  
  // check if the polygon is oriented counter-clockwise.
  jboolean isCounterClockwise= pgn->is_counterclockwise_oriented();
  
  // clean-up
  (jenv)->ReleaseDoubleArrayElements(jxs, xcoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jys, ycoords, NULL);
  delete pgn;

  // return boolean value
  return isCounterClockwise;

}

/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_isClockwiseOriented
 * Signature: ([D[D)Z
 */
JNIEXPORT jboolean JNICALL 
Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1isClockwiseOriented
  (JNIEnv *jenv, jobject, jdoubleArray jxs, jdoubleArray jys)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size_x= (jenv)->GetArrayLength(jxs);

  // convert point list to polygon
  Polygon_2 *pgn= pointListToPolygon(size_x, xcoords, ycoords);
  
  // check if the polygon is oriented clockwise.
  jboolean isClockwise= pgn->is_clockwise_oriented();

  // clean-up
  (jenv)->ReleaseDoubleArrayElements(jxs, xcoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jys, ycoords, NULL);
  delete pgn;

  // return boolean value
  return isClockwise;
}

/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_signedArea
 * Signature: ([D[D)D
 */
JNIEXPORT jdouble JNICALL
Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1signedArea
  (JNIEnv *jenv, jobject, jdoubleArray jxs, jdoubleArray jys)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size_x= (jenv)->GetArrayLength(jxs);

  // convert point list to polygon
  Polygon_2 *pgn= pointListToPolygon(size_x, xcoords, ycoords);

  // check if the polygon is oriented clockwise.
  jdouble sarea= pgn->area();

  // clean-up
  (jenv)->ReleaseDoubleArrayElements(jxs, xcoords, NULL);
  (jenv)->ReleaseDoubleArrayElements(jys, ycoords, NULL);
  delete pgn;

  // return boolean value
  return sarea;
}

/*
 * Class:     de_unihalle_informatik_MiToBo_datatypes_primitives_Polygon2D_Cgal
 * Method:    cgal_makePolySimple
 * Signature: ([D[D)[D
 */
JNIEXPORT jdoubleArray JNICALL
Java_de_unihalle_informatik_MiToBo_core_datatypes_Polygon2D_1Cgal_cgal_1makePolySimple
  (JNIEnv *jenv, jobject, jdoubleArray jxs, jdoubleArray jys)
{
  // get the polygon point coordinates
  jdouble *xcoords= (jenv)->GetDoubleArrayElements(jxs, NULL);
  jdouble *ycoords= (jenv)->GetDoubleArrayElements(jys, NULL);

  // get the size of the polygon, i.e. the point number
  jsize size= (jenv)->GetArrayLength(jxs);

  // create the arrangement
  Arrangement_2 arr;

  // we have 'size' points, hence 'size' segments
  Segment_2 *cv= new Segment_2[size];
  for (int i=0;i<size-1;++i) {
    Point_2 ps(xcoords[i],ycoords[i]);
    Point_2 pt(xcoords[i+1],ycoords[i+1]);
    cv[i]= Segment_2(ps,pt);
  }
  // last segment
  Point_2 ps(xcoords[size-1],ycoords[size-1]);
  Point_2 pt(xcoords[0],ycoords[0]);
  cv[size-1]= Segment_2(ps,pt);

  // create arrangement
  CGAL::insert(arr, &cv[0], &cv[size]);

  // get the unbounded face of the arrangement
  Arrangement_2::Face_const_handle outFace= arr.unbounded_face();
  Arrangement_2::Hole_const_iterator hi;

  // get the first hole... if there are more, strange things happen!
  hi = outFace->holes_begin();
  Arrangement_2::Ccb_halfedge_const_circulator curr = (*hi);

  // get number of points
  int pointcount= 0;
  do {
    pointcount++;
  } while (++curr != (*hi));

  hi = outFace->holes_begin();
  curr = (*hi);

  // first and last point are identical, but inserted twice
  int pointNum= pointcount;

  // allocate result array: [x0, x1, ... ,xN, y0, y1, yN]
  jdoubleArray simplePoly= (jenv)->NewDoubleArray(2*pointNum);
  double* points= new double[2*pointNum];

  // first point
  int index= 0;
 // points[index]= to_double(curr->source()->point().x());
 // points[index+pointNum]= to_double(curr->source()->point().y());
  //index++;
  do {
    // all other points
    points[index]= to_double(curr->target()->point().x());
    points[index+pointNum]= to_double(curr->target()->point().y());
    index++;
  } while (++curr != (*hi));
  // copy points to array and return
  (jenv)->SetDoubleArrayRegion(simplePoly, 0, 2*pointNum, points);
  return simplePoly;
}
