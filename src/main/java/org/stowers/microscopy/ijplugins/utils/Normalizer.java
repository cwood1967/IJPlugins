package org.stowers.microscopy.ijplugins.utils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackStatistics;

public class Normalizer {

    ImagePlus imp;
    ImageStack stack;
    ImageStack normStack= null;

    StackStatistics stats;

    int nc;
    int nz;
    int nt;

    public Normalizer(ImagePlus imp) {

        this.imp = imp;
        // even an image with a single plane has a stack
        this.stack = imp.getStack();
        checkImage();
    }

    private void checkImage() {
        nc = imp.getNChannels();
        nz = imp.getNSlices();
        nt = imp.getNFrames();
        calcStackStats();
        System.out.println(stats.min + " " + stats.mean + " " + stats.max);

    }

    private void calcStackStats() {

        stats = new StackStatistics(imp);

    }

    public ImageStack normalizeStack() {

        int size = stack.getSize();
        normStack = stack.duplicate().convertToFloat();
        for (int i = 0; i < size; i++) {

            float[] pixels = (float[])normStack.getPixels(i + 1);

            for (int ip = 0; ip < pixels.length; ip++) {
                float p = pixels[ip];
                float np = (float)((p - stats.min)/(stats.max - stats.min));
                pixels[ip] = np;
            }
        }

        return normStack;
    }


}
