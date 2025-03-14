# mitobo

![GitHub language count](https://img.shields.io/github/languages/count/mitobo-hub/mitobo?color=darkgreen)
![GitHub top language](https://img.shields.io/github/languages/top/mitobo-hub/mitobo)
![GitHub repo size](https://img.shields.io/github/repo-size/mitobo-hub/mitobo?color=red)

MiToBo - A microscope image analysis toolbox

The Microscope Image Analysis Toolbox [**MiToBo**](http://www.informatik.uni-halle.de/mitobo) 
is an extension for the widely used image processing application ImageJ and its new release ImageJ 2.0.
MiToBo ships with a set of operators ready to be used as plugins in ImageJ. They focus on the analysis of biomedical images acquired by various types of microscopes.
Some of MiToBo's operators are for example these ones:

    * ActinAnalyzer for the quantification of actin filament structures in microscopy images
    * Cell Migration Analyzer for analyzing single cell migration from time lapse image sequences
    * Neuron Analyzer for the segmentation of neurons in microscope images
    * Scratch Assay Analyzer for analyzing microscope images from collective cell migration experiments
    * Snake Optimizer for performing image segmentation based on explicit active contours 

Furthermore, MiToBo offers a user- and programmer friendly framework for developing algorithms that have properties not available in ImageJ,
however, still provides full compatibility to all ImageJ features.

MiToBo completely separates the implementation of image processing algorithms from potential user interfaces.
Moreover, MiToBo builds on top of [**Alida**](http://www.informatik.uni-halle.de/alida)
 which is a library for easing the development of data analysis algorithms and tools.
The main concept of Alida are operators as the core units for implementing data analysis algorithms.
Alida defines unified interfaces and execution procedures for operators which yield the fundament for its nice features like

    * automatic documentation of complete analysis processes, e.g., leading from an input image to analysis results, in terms of processing graphs
    * automatic generation of commandline and graphical user interfaces
    * a graphical programming editor named Grappa automatically considering all implemented operators as potential processing nodes 

MiToBo takes full advantage of Alida's features, hence, provides a framework for implementing image analysis algorithms allowing for automatic documentation and automatic user interface generation, and includes the graphical programming editor Grappa for user-friendly design of more complex processing pipelines. 

The main project homepage is located [**here**](http://www.informatik.uni-halle.de/mitobo) 
