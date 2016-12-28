package org.stowers.microscopy.org.stowers.microscopy.utils;

import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import net.imagej.ops.Ops;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cjw on 12/12/16.
 */
public class PombeSpot {

    ImageProcessor maskIp;
    ImageProcessor blurredIp;

    ImageProcessor MaxIp = null;
    ImageProcessor SumIp = null;
    ImageProcessor smoothIp = null;

    HashMap<Integer, Float> peakMap = null;
    int x;
    int y;
    int maskArea = 0;
    int npeaks;

    int patchSize;

    double threshold;

    public PombeSpot(int x, int y, double threshold,
                     ImageProcessor iMaxIp,
                     ImageProcessor iBlurredIp,
                     int patchSize) {

        this.x = x;
        this.y = y;
        this.threshold = threshold;

        this.patchSize = patchSize;
        this.MaxIp = createPatch(iMaxIp);
        this.blurredIp = createPatch(iBlurredIp);
        maskIp = createPatchMask(this.blurredIp);

    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getNpeaks() {
        return npeaks;
    }

    public int getMaskArea() {
        return maskArea;
    }
    public ImageProcessor getMaxPatch() {
        return MaxIp;
    }

    public ImageProcessor getSumPatch() {
        return SumIp;
    }

    public ImageProcessor getMaskPatch() {
        return maskIp;
    }

    public ImageProcessor getBlurPatch() {
        return blurredIp;
    }

    public void setSumPatch(ImageProcessor sumIp) {
        this.SumIp = createPatch(sumIp);
    }

    protected ImageProcessor createPatch(ImageProcessor ip) {

        int x0 = x - patchSize/2;
        if (x0 < 0) x0 = 0;

        int y0 = y - patchSize/2;
        if (y0 < 0) y0 = 0;

        int xf = x + patchSize/2;
        if (xf >= ip.getWidth()) xf = ip.getWidth() -1;

        int yf = y + patchSize/2;
        if (yf >= ip.getHeight()) xf = ip.getHeight() - 1;

        float[] pixels = (float[])ip.getPixels();

        ip.setRoi(x0, y0, patchSize, patchSize);
        ImageProcessor patch = ip.crop();  //the FloatProcessor.crop creates a new ImageProcessor

        return patch;
    }

    protected ImageProcessor createPatchMask(ImageProcessor ip) {

        float[] pixels = (float[])ip.getPixels();
        byte[] mask = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] >= threshold) {
                mask[i] = (byte)255;
                maskArea++;
            }
            else {
                mask[i] = 0;
            }
        }

        ImageProcessor resIp = new ByteProcessor(ip.getWidth(), ip.getHeight(), mask);
        resIp.setBackgroundValue(0.);
        resIp.erode();
        resIp.erode();
        return resIp;
    }

    public ArrayList<Integer> findPeaks() {

        smoothIp = MaxIp.duplicate();
        smoothIp.smooth();
        int w = smoothIp.getWidth();
        int h = smoothIp.getHeight();
        float[] smoothPix = (float[])smoothIp.getPixels();
        MaximumFinder maxFinder = new MaximumFinder();
        ByteProcessor mfip = maxFinder.findMaxima(smoothIp, 5., 0, false);

        byte[] pixels = (byte[])mfip.getPixels();
        int np = pixels.length;

        ArrayList<Integer> resList = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < np; i++) {
            int x = i % mfip.getWidth();
            int y = i / mfip.getWidth();
            if (pixels[i] != 0) {
                count++;
                resList.add(i);
//                System.out.println(count + " " + smoothPix[i]);
            }

        }
        npeaks = count;
        peakMap = new HashMap<>();

        for (int index : resList) {
            float s = sumRegion(SumIp, index, 3);
            peakMap.put(index, s);
        }


        return resList;
//        System.out.println("Count: " + x + ", " + y + " -> " + count);
    }

    public float getPeakSum(int key) {
        return peakMap.get(key);
    }

    public float sumRegion(ImageProcessor ip, int index, int size) {

        int w = ip.getWidth();
        int h = ip.getHeight();

        float[] pixels = (float[])ip.getPixels();
        int x0 = index % w;
        int y0 = index / w;

        int xstart = x0 - size/2;
        if (xstart < 0) xstart = 0;

        int xstop = x0 + size/2;
        if (xstop >= w) xstop = w -1;

        int ystart = y0 - size/2;
        if (ystart < 0) ystart = 0;

        int ystop = y0 + size/2;
        if (ystop >= h) ystop = h - 1;

        float sum = 0;
        for (int j = ystart; j <=ystop; j++) {
            for (int i = xstart; i <= xstop; i++) {
                int ii = j*w + i;
//                System.out.println(j + " " + i + " " + xstop + " " + ystop + " " + index);
                sum += pixels[ii];
            }
        }

        return sum;
    }
    public float sumNucleus() {

        byte[] maskPix = (byte[])maskIp.getPixels();
        float[] sumPix = (float[])SumIp.getPixels();

        float sum = 0.f;

        for (int i = 0; i < maskPix.length; i++) {
            if (maskPix[i] != 0) {
                sum += sumPix[i];
            }
        }
        return sum;
    }

}
