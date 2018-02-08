package org.stowers.microscopy.registration;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import jguis.TurboRegJ_;

public class RegThread {

    int targetframe;
    int sourceframe;
    int targetChannel;
    int targetSlice;
    int transformation;
    double[][] localTransform;

    ImagePlus imp;
    public RegThread(ImagePlus imp, int transformation, int sourceframe, int targetframe,
                     int targetChannel, int targetSlice) {

        this.imp = imp;
        this.targetframe = targetframe;
        this.sourceframe = sourceframe;
        this.targetChannel = targetChannel;
        this.targetSlice = targetSlice;
        this.transformation = transformation;

    }

    public void calcLocalTransform(double[][][] matrix) {

//        System.out.println("Frame " + sourceframe + " + registered to " + targetframe);
        double[][] transform = RegUtils.getLocalTransform(imp, imp.getWidth(), imp.getHeight(), transformation,
                targetChannel, targetSlice, sourceframe, targetframe);

        localTransform = transform;
        matrix[sourceframe - 1] = transform;
        IJ.showStatus("Frame " + sourceframe + " + registered to " + targetframe);
    }

}
