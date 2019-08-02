package org.stowers.microscopy.segmentation;

import ij.ImageStack;
import ij.process.ImageProcessor;

public class SegUtils {

    public static double[] stackEdgeZScore(ImageStack stack) {

        int nslices = stack.getSize();
        double[] res = new double[nslices];
        for (int i = 0; i < nslices; i++) {
            ImageProcessor ip = stack.getProcessor(i + 1);
            res[i] = edgeZScore(ip);
        }

        return res;
    }

    public static double edgeZScore(ImageProcessor ip) {

        ImageProcessor dip = ip.duplicate();
        dip.findEdges();
        return dip.getStatistics().mean;

    }
}
