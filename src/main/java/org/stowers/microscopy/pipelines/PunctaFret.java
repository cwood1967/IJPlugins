package org.stowers.microscopy.pipelines;

/**
 * Created by cjw on 6/13/17.
 */

/*
This is the pipeline for analyzing the fret puncta
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.MaximumFinder;
import ij.process.*;
import org.stowers.microscopy.ijplugins.utils.BinaryPatch;
import org.stowers.microscopy.ijplugins.utils.PunctaPatch;
import org.stowers.microscopy.threshold.AutoThreshold;

import inra.ijpb.binary.BinaryImages;

import java.util.ArrayList;
import java.util.List;

public class PunctaFret {

    ImagePlus imp;
    ImagePlus blank;
    ImageProcessor blankIp;
    ImagePlus blueMask;
    ImagePlus cellMask;
    ImagePlus labeledMask;

    public PunctaFret(ImagePlus imp) {
        this.imp = imp;
        int w = imp.getWidth();
        int h = imp.getHeight();

    }

    public void run() {

        String imageName = imp.getTitle();
        List<Integer> channelList = new ArrayList<>();
        channelList.add(1);
        channelList.add(2);
        channelList.add(3);
        processBlue();

        //prcessCell will segment and label the cells in the desired channel
        // cellMask is just the binary mask, labeledMask labels each segmented region with an id
        processCell(3);
        labeledMask.show();

        int nregions = (int)labeledMask.getProcessor().getStatistics().max;
        int[][] regionbounds = findRegionBounds(labeledMask.getProcessor());



        // iterate over each region found above
        // the labeled mask labelMask contains the image processor to use
        for (int i = 1; i <= nregions; i++) {
            //get a patch of the raw image
            int x0 = regionbounds[i][0];
            int w0 = regionbounds[i][1] - x0;
            int y0 = regionbounds[i][2];
            int h0 = regionbounds[i][3] - y0;

            //just skip if it is too small
            if (w0*h0 < 300) {
                continue;
            }
// Do the patch of the mask first, since it is the same for every channel
            BinaryPatch maskPatch = new BinaryPatch(labeledMask.getProcessor(), x0, y0, w0, h0, i);
            ImageProcessor patchMaskIp = maskPatch.getPatch();

            imp.setC(3);
            ImageProcessor gip = imp.getProcessor();

            PunctaPatch pp = new PunctaPatch(i, gip, x0, y0, w0, h0);
            ImageProcessor patch = pp.getPatch();

            //duplicate it so not to change anything
            ImageProcessor dpatch = patch.duplicate();
            dpatch.blurGaussian(1.);
            ArrayList<Integer> points = findMaximaFromStdDev(dpatch);

            //punctaRegions is a segmented labeled image of the puncta
            //used this way to increment pixels for every level during growMaxRegion
            ImageProcessor punctaRegions = new ShortProcessor(pp.getWidth(), pp.getHeight());
            int ri = 1;
            for (int p : points) {
                growMaxRegion(dpatch, punctaRegions, p, .33, (byte) ri);
                ri++;
            }

            for (int c : channelList) {
                imp.setC(c);
                gip = imp.getProcessor();
                PunctaPatch ppc = new PunctaPatch(i, gip, x0, y0, w0, h0);
                //blur the patch to smooth for finding maxima

                ppc.measureFromLabeledRegions(punctaRegions);
                ppc.measureFromCellMask(maskPatch.getPatch());
                System.out.println("Region(Cell): " + i + " , Channel: " + c);
                ArrayList<String> pOut = ppc.makePunctaOutput();
                String cellOut = ppc.makeCellOutput();
            }

        }
        System.out.println(imageName);
    }

    public void processBlue() {

        imp.setC(4);

        ImageProcessor blueIp = imp.getProcessor().duplicate().convertToFloatProcessor();
        BackgroundSubtracter bg = new BackgroundSubtracter();
        bg.subtractBackround(blueIp, 25);
        blueIp.blurGaussian(3.0);
        blueIp.findEdges();
        blueIp.gamma(.5);

        ImageProcessor mask = AutoThreshold.thresholdIp(blueIp, "Otsu", false, false);
        mask = mask.convertToByteProcessor();
        mask.erode();
        mask.dilate();

        ImageProcessor mask2 = mask.duplicate();
        mask2.invert();

        mask2 = BinaryImages.removeLargestRegion(mask2);
        mask2.erode();
        mask2.erode();
        mask2.dilate();

        blueMask = new ImagePlus("b -filled", mask2);

    }

    public void processCell(int channel) {

        imp.setC(channel);
        ImageProcessor greenIp = imp.getProcessor().duplicate().convertToFloatProcessor();
        BackgroundSubtracter bg = new BackgroundSubtracter();
        bg.subtractBackround(greenIp, 25);
        greenIp.blurGaussian(3.0);
        ImageProcessor mask = AutoThreshold.thresholdIp(greenIp, "Otsu", false, false);
        mask = mask.convertToByteProcessor();
        mask.erode();
        mask.dilate();
        cellMask = new ImagePlus("cell mask", mask);
        IJ.run(cellMask, "Fill Holes", "");
        EDM edm = new EDM();
        edm.toWatershed(mask);

        ImageProcessor regIp = BinaryImages.componentsLabeling(mask, 4, 16);
        labeledMask = new ImagePlus("Labeled Mask", regIp);

    }

    public int[][] findRegionBounds(ImageProcessor xip) {
        int w = xip.getWidth();
        int h = xip.getHeight();

        int nregions = (int)(xip.getStatistics().max + .001);

        int[][] regionbounds = new int[nregions + 1][4];
        for (int i = 0; i <= nregions; i++) {
            regionbounds[i][0] = w;
            regionbounds[i][2] = h;
        }
        short[] pixels = (short[])xip.getPixels();

        int m = 0;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == 0) {
                continue;
            }

            int n = pixels[i];
            int x = i % w;
            int y = i / w;
            
            if (x <  regionbounds[n][0]) regionbounds[n][0] = x;
            if (x >  regionbounds[n][1]) regionbounds[n][1] = x;
            if (y <  regionbounds[n][2]) regionbounds[n][2] = y;
            if (y >  regionbounds[n][3]) regionbounds[n][3] = y;
            m = i;

        }
//        System.out.println(m + " " +  pixels.length);

        return regionbounds;
    }



    public ArrayList<Integer> findMaximaFromStdDev(ImageProcessor xip) {

        double stdDev = xip.getStatistics().stdDev;
        MaximumFinder maxFinder = new MaximumFinder();
        ByteProcessor mfip = maxFinder.findMaxima(xip, .75*stdDev, 0, false);

        byte[] bpix = (byte[])mfip.getPixels();
        ArrayList<Integer> pointList = new ArrayList<>();
        for (int i = 0; i < bpix.length; i++) {
            if (bpix[i] != 0) {
                pointList.add(i);
            }
        }

        return pointList;
    }

    public ImageProcessor growMaxRegion(ImageProcessor xip,  ImageProcessor mask, int pixelIndex, double level,
                              byte regionValue) {

        final int w = xip.getWidth();
        final int h = xip.getHeight();

        int x = pixelIndex % w;
        int y = pixelIndex / w;

        float[] px = (float[])xip.convertToFloatProcessor().getPixels();

//        ImageProcessor mask = new ByteProcessor(w, h);
        short[] maskPix = (short[])mask.getPixels();
        boolean keepGoing = true;
        int d = 1;
        int np = 3;

        double mean = xip.getStatistics().mean;
        double max = px[pixelIndex];
        double std = xip.getStatistics().stdDev;
        double floor = mean + 1.0*std;
        double thresh = floor + level*(max - floor);
        if (thresh < floor) {
            keepGoing = false;
        }
        if ((thresh + .25*std) > max) {
            keepGoing = false;
        }
//        System.out.println(thresh + " " + max);
        ArrayList<Integer> regionPix = new ArrayList<>();

        if (keepGoing) {
            regionPix.add(pixelIndex);
            maskPix[pixelIndex] = regionValue;
        }

        while (keepGoing) {
            keepGoing = false;
            for (int id = -d; id <= d; id++) {
                int yd = y + id;
                if (yd >= h) continue;
                if (yd <= 0) continue;
                if (Math.abs(id) == d) {
                    for (int jd = -d; jd <= d; jd++) {
                        int xd = x + jd;
                        if (xd >= w) continue;
                        if (xd <= 0) continue;
                        int index = yd*w + xd;
                        if (px[index] > thresh) {
                            regionPix.add(index);
                            maskPix[index] = regionValue;
                            keepGoing = true;
                        }
                    }
                } else {
                    int xd = x - d;
                    if (xd >= w) continue;
                    if (xd <= 0) continue;
                    int index = yd*w + xd;
                    if (px[index] > thresh) {
                        regionPix.add(index);
                        maskPix[index] = regionValue;
                        keepGoing = true;
                    }
                    xd = x + d;
                    if (xd >= w) continue;
                    if (xd <= 0) continue;
                    index = yd*w + xd;
                    if (px[index] > thresh) {
                        regionPix.add(index);
                        maskPix[index] = regionValue;
                        keepGoing = true;
                    }

                }
            }
            d++;
            np += 2;
        }
        xip.setPixels(px);
        mask.setPixels(maskPix);
//        System.out.println("Num pix in region " + regionPix.size());

        return mask;
    }

    public void outputToFile() {

    }
}
