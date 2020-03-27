# Change Log
All notable changes to this project will be documented in this file.

The format of the file is based on a template from [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]
### Added

### Changed

### Deprecated

### Removed

### Fixed

## [1.8.18] - 2020-03-27
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.18

### Added
- rhizoTrak support: released drawing utils

## [1.8.17] - 2020-02-20
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.17

### Changed
- RootSegmentationOperator: changed interface to support vectors of MTBRootTree as result

## [1.8.16] - 2019-12-19
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.16

### Added
- MTBRootTree/MTBRootTreeNodeData: new data types for handling (root) treelines in MiToBo

### Changed
- MTBTree/MTBTreeNode/MTBTreeNodeData: extended interface to support cloning, added convenience algorithms

### Fixed
- PaCeQuant_FeatureColorMapper: fixed problem with path separators on Windows machines
- StegerRidgeDetection2DWrapper: segment width is only drawn now if actually estimated

## [1.8.15] - 2019-07-29
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.15

### Added
- DrawContour2DSet: new operator to draw given contours into a new/given image
- GetMTBContour2DSetFromXSVFiles: operator to read contour sets from ASCII files
- MTBTableModelDataIO: added functionality to read tab-separated tables from file
- PaCeQuant_FeatureColorMapper: new operator to map PaCeQuant cell feature values to call label images to get feature maps

### Changed
- ImageIOUtils: updated handling of physical pixel units according to new Bioformats philosophy
- PaCeQuant: checkbox for adding IDs to result images now effects all result images

### Fixed
- ImageIOUtils: corrected handling of inches on saving images
- ImageWriterMTB: fixed handling of big/little endian representation when saving images
- MTBImage and all sub-classes: calibration object is now consistently handed over between MiToBo and ImageJ data type objects

## [1.8.14] - 2019-02-11
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.14

### Added
- MTBCellCounter: stromules detection approach published at BIOIMAGING '19
- rhizoTrak: interfaces and data types for interoperability of MiToBo with rhizoTrak

## [1.8.13.1] - 2019-01-22
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.13.1

### Added
- CellBoundaryExtractor2D: a new operator to extract cell boundaries using vesselness enhancement filters
- LabelImageEditor: added new functionality, e.g., to merge regions, draw free-hand lines, fill holes or undo actions

## [1.8.13] - 2018-12-18
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.13

### Added
- CytoSkeletonAnalyzer2D: released new operator for texture analysis of cytoskeleton structural patterns
- MTBCellCounter: added line segments as new type of annotation object

### Fixed
- Grappa: fixed initialization of internal node management data structures after reloading a workflow

## [1.8.12] - 2018-10-17
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.12

### Added
- PaCeQuant: new parameter for configuration of Niblack thresholding available in GUI

## [1.8.11] - 2018-09-07
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.11

### Added
- AwtPoint2dDataIOCmdline/AwtPoint2dDataIOXmlbeans: new commandline and XML providers for Point2D.Double objects
- RoiFileToCSVConverter: operator to write ImageJ ROI data into CSV file
- DrawPolygon2DSet: new setter for input image
- DrawRegion2DSet: added some setters to specify dimensions and offsets of target image programmatically
- MTBOperatorCollection: new subclass for handling collections of MTBOperators (with MiToBo specific configuration windows)
- MTBQuadraticCurve2D: new methods to extract closest point on ellipse and to calculate tangent orientation at given position
- RootSegmentationOperator: new abstract basis class for root detection operators
- SkeletonAnalysisHelper: new class with methods useful when analyzing region skeletons

### Changed
- MTBCellCounter: detect operator handling now being based on operator collection
- LabelImageEditor: file filter argument is now optional, by default (and with an empty string) all files are processed

### Removed
- Polygon2D_Cgal wrapper class for CGAL due to outdated native bindings

### Fixed
- MTBImage, ImageDimensionReducer: preserve calibration information of original images for newly generated images
- OrientedFilter2D: explicitly shutdown ExecutorService of libimg2 in FFT mode, otherwise operator does not terminate when called from commandline
- PCA: added additional checks for singular configurations

## [1.8.10] - 2018-05-18
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.10

### Added
- new operators for analyzing images of biofilms

### Fixed
- minor changes due to update to Alida 2.7.7

## [1.8.9] - 2018-05-04
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.9

### Added
- MTBImage: new method to draw lines to arbitrary layer

### Changed
- MTBCellCounter update: new plugin mechanism for easy integration of additional detectors / functionality for adding marker regions (not just centroids) / support for simultaneous analysis of multiple channels

## [1.8.8] - 2018-03-23
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.8

### Added
- LabelImageEditor: a small interactive tool for removing regions from label images by mouse interactions
- MTBImage: method to draw filled circles into an image

### Changed
- switched to management of Maven plugin and dependency artefact versions via parent POM file
- updated JFreeChart to version 1.5.0, required minor adaptations of function calls

### Fixed
- BinaryImageEndpointTools: added safety checks to avoid accessing pixels outside of image domain
- PaCeQuant: fixed small issue in segment length calculation for lobes, added safety checks in accessing pixels

## [1.8.7] - 2018-01-31
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.7

### Added
- new data type MTBLineSegment2DSet for sets of MTBLineSegment2D objects
- support for importing sets of MTBLineSegment2D objects from ImageJ via the ROI manager
- MTBDataIOFile: added support for loading region sets from ImageJ ROI files
- UndecimatedWaveletTransform: support for exclusion mask and an initial sigma of 0.5

### Changed
- DistanceTransform: integrated functionality to extract pre-cursor infos
- DrawRegion2DSet: added parameters to specify desired size of result image directly
- ParticleDetectorUWT2D: visibility of some internal methods updated

### Removed
- obsolete class DistanceTransformPrecursorInfos, functionality is now part of DistanceTransform

### Fixed
- RoiManagerAdapter: fixed performance issue in reading regions, avoiding iterative init of internal fields
- SkeletonExtractor: padding image to compensate for border issue in ImageJ 1.x skeleton extraction
- TestDirectoryTree: fixed file separators to properly work on Windows systems

## [1.8.6.1] - 2017-10-09
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.6.1

### Changed
- updated PaCeQuant for better handling of small, circular cells;
  lobe analysis is now only performed for cells with at least two real lobes

## [1.8.6] - 2017-09-22
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.6

### Added
- PaCeQuant operator for pavement cell shape analysis,
  Birgit Möller, Yvonne Poeschl, Romina Plötner, Katharina Bürstenbinder,
  "PaCeQuant: A Tool for High-Throughput Quantification of Pavement Cell Shape Characteristics",
  Plant Physiology, Vol. 175, Issue 1, Sep 2017. DOI: https://doi.org/10.1104/pp.17.00961
- new operator for drawing text strings into an image
- MTBDataIOFile: support for reading ImageJ ROI archive files in command line mode

## [1.8.5.1] - 2017-07-29
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.5.1

### Changed
- MTBImageDataIO: setting the filename for result images on command line to '-' now directly opens the image
- MTBPolygon2D: distinguishing between polygon point lists containing all contour pixels or only a subset in generating binary polygon masks; in the first case it is guaranteed that all contour pixels are included in the mask, in the latter case not
- ROIManagerAdapter: regions within ROI manager region sets are now sorted alpha-numerically upon import

### Removed
- MTBImageHistogramm: error-prone histogram normalization method normalizeOnly()

### Fixed
- HistogramEqualization: removed multiple calls of histogram normalization leading to wrong results
- HSIToRGBPixelConverter: I=1 now always results in white color, clipped RGB values out of range back to valid range of [0,1]

## [1.8.5] - 2017-03-27
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.5

## [1.8.4] - 2016-12-20
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.4

## [1.8.3] - 2016-11-02
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.3

## [1.8.2] - 2016-05-20
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.2

## [1.8.1] - 2016-03-20
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8.1

## [1.8] - 2016-03-15
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.8

## [1.7.1] - 2016-03-04
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.7.1

## [1.7] - 2015-09-23
Birgit Moeller - <birgit.moeller@informatik.uni-halle.de>
- Released MiToBo 1.7





