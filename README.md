

# ImageJ Plugins 

These are a collection of plugins for Fiji/ImageJ. They have been
written using the scijava Parameter class, so Fiji ImageJ2 or Fiji is
required. They have also been compiled using Java 1.8, so a JRE of 1.8
or greater is necessary.
 
 
* **FastFileSaver** Saves images as tiffs much faster than the
standard ImageJ SaveAs Tiff does.
* **LoG 3D** 3D Laplacian of Gaussian that is multithreaded for faster
performance.
    - About 8 times faster than the single threaded plugin.
    - 2017-08-25: Reduced the amount of memory needed by about 1/3.
* **ImageHistogram** Display a histogram of the image using JFreeChart
for the plot.
* **Column Plotter** Plot the histogram from a column in the Results
 Table
* **ClickForRoi** Set the size of a rectangle and click multiple points
for a rectangular ROI at each point. The rois are also added to the ROI
manager.
* **Adaptive Threshold** Do an adaptive threshold of an image.

