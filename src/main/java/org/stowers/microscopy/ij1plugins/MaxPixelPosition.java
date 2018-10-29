package org.stowers.microscopy.ij1plugins;

import ij.IJ;

import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;

import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Random;

@Plugin(type = Command.class, menuPath="Plugins>Chris>MaxPosition")
public class MaxPixelPosition implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    int w;
    int h;
    int n;

    @Override
    public void run() {

        w = imp.getWidth();
        h = imp.getHeight();
        n = w*h;

        findMaxPosition();
    }

    public void findMaxPosition() {

       ImageProcessor ip = imp.getProcessor().convertToFloatProcessor();
       float[] pixels = (float[])ip.getPixels();

       float fmax = Float.MIN_VALUE;
       int mx = 0;
       int my = 0;

       for (int i = 0; i < n; i++) {
           if (pixels[i] > fmax) {
               fmax = pixels[i];
               mx = i % w;
               my = i / w;
           }
       }

       ResultsTable table = ResultsTable.getResultsTable();
       table.setDefaultHeadings();
       table.incrementCounter();
       table.addValue("max-X", mx);
       table.addValue("max-Y", my);
       table.addValue("max value", fmax);
       table.addValue("Slice", imp.getCurrentSlice());
       table.show("Results");
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}

