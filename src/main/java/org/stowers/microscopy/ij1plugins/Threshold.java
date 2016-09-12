package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.stowers.microscopy.threshold.Adaptive;

/**
 * Created by cjw on 9/7/16.
 */



@Plugin(type = Command.class, name = "Adaptive Threshold",  menuPath="Plugins>Stowers>Chris>Adaptive Threshold")
public class Threshold implements Command, Previewable {


    @Parameter
    ImagePlus imp;

    @Parameter
    int blockSize;

    public Threshold() {

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public void run() {

        Adaptive a = new Adaptive();
        ImagePlus x = a.threshold(imp, blockSize, .00f);
        double dmin = x.getProcessor().getStatistics().min;
        double dmax = x.getProcessor().getStatistics().max;

        x.show();

    }
}

