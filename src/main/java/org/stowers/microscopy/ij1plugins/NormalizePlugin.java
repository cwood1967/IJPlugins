package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import org.stowers.microscopy.ijplugins.utils.Normalizer;
import org.stowers.microscopy.threshold.AutoThreshold;

@Plugin(type = Command.class, menuPath="Plugins>Chris>Normalize")
public class NormalizePlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Override
    public void run() {

        Normalizer n = new Normalizer(imp);
        ImageStack nstack = n.normalizeStack();

        ImagePlus nImp = new ImagePlus("Normalized");
        nImp.setStack(nstack);

        nImp.setSlice(5);
        ImageProcessor ip = nImp.getProcessor();
        ImageProcessor mask = AutoThreshold.thresholdIp(ip, "Otsu", false);
        ImagePlus mImp = new ImagePlus("Mask");
        mImp.setTitle("Mask");
        mImp.setProcessor(mask);

        nImp.show();
        mImp.show();
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
