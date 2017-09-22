# Change Log
All notable changes to this project will be documented in this file.

The format of the file is based on a template from [Keep a Changelog](http://keepachangelog.com/).

## [Unreleased]
### Added
- PaCeQuant operator for pavement cell shape analysis,  
  Birgit Möller, Yvonne Poeschl, Romina Plötner, Katharina Bürstenbinder,
  "PaCeQuant: A Tool for High-Throughput Quantification of Pavement Cell Shape Characteristics",  
  Plant Physiology, Vol. 175, Issue 1, Sep 2017. DOI: https://doi.org/10.1104/pp.17.00961
- new operator for drawing text strings into an image
- MTBDataIOFile: support for reading ImageJ ROI archive files in command line mode

### Changed

### Deprecated

### Removed

### Fixed

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





