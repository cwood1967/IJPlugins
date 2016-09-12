package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import org.stowers.microscopy.utils.HistogramPlot;
import org.stowers.microscopy.utils.Histogram;
/**
 * Created by cjw on 7/27/16.
 */



@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Image Histogram")
public class ImageHistogram implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Override
    public void run() {

        ImageProcessor ip = imp.getProcessor();

        FloatProcessor fp = ip.convertToFloatProcessor();
        double[] dpx = new double[imp.getWidth()*imp.getHeight()];
        float[] px = (float[])fp.getPixels();
        for (int i = 0; i < px.length; i++) {
            dpx[i] = (double)px[i];
        }

        Histogram h = new Histogram(dpx, 256);
        HistogramPlot p = new HistogramPlot(h);
        p.setTitle("Image Histogram");
        p.setXLabel("Intensity");
        p.setYLabel("Number");
        p.plotHist();
//        p.setLogY(true);
//        p.plotHist();
    }


    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

}
