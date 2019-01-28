package org.stowers.microscopy.ij1plugins;

import ij.IJ;

import ij.ImagePlus;

import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;

import ij.process.StackConverter;

import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, menuPath = "Plugins>Stowers>Chris>RegionGrowFromFindMax_Plugin")
public class RegionGrowFromFindMax_Plugin implements Command, Previewable {

    @Parameter
    ImagePlus oimp;  //original image

    ImagePlus imp;

    @Override
    public void run() {

        int current = oimp.getCurrentSlice();
        processSlice(current);

    }

    public void processSlice(int slice) {

        oimp.setSlice(slice);
        ImageProcessor ip = oimp.getProcessor().convertToFloat();
        ImageStatistics stats = ip.getStatistics();
        System.out.println(stats.dmode + " " + stats.mode + " " +   stats.mean + " " +  stats.stdDev);

    }

    public void findmaxima() {


    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}

