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
import inra.ijpb.morphology.GeodesicReconstruction;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PunctaFret {

    ImagePlus imp;
    ImagePlus blank;
    ImageProcessor blankIp;
    ImagePlus blueMask;
    ImagePlus cellMask;
    ImagePlus labeledMask;

    StringBuffer punctaBuffer;
    StringBuffer cellBuffer;

    String punctaHeader;
    String cellHeader;

    boolean show = false;
    boolean good = true;
    public PunctaFret(ImagePlus imp) {
        this.imp = imp;
        int w = imp.getWidth();
        int h = imp.getHeight();

    }

    public boolean getGood() {
        return good;
    }
    public void setShow(boolean show) {
        this.show = show;
    }
    public String getPunctaOutput() {
        return punctaBuffer.toString();
    }

    public String getCellOutput() {
        return cellBuffer.toString();
    }

    public void run() {

        show = false;
        punctaBuffer = new StringBuffer();
        cellBuffer = new StringBuffer();
        //p.regionId, p.sum, p.mean, p.size, p.xc, p.yc);
        punctaHeader = "ImageName, Cell, Channel, Background, " +
                "PunctaId, SumIntensity, Area, XC, YX\n";

        cellHeader = "ImageName, Cell, Channel, Background, SumIntensity, MeanIntensity, Area, PunctaSum, PunctaMean, "
                + " PunctaArea, N-Puncta\n";


        String imageName = imp.getTitle();
        List<Integer> channelList = new ArrayList<>();
        channelList.add(1);
        channelList.add(2);
        channelList.add(3);
        processBlue();
//        blueMask.show();

        //prcessCell will segment and label the cells in the desired channel
        // cellMask is just the binary mask, labeledMask labels each segmented region with an id
        processCell(3);


        ImageProcessor totalmask = addMasks(blueMask.getProcessor(), cellMask.getProcessor());
        ImageProcessor regIp = BinaryImages.componentsLabeling(totalmask, 4, 16);
        labeledMask = new ImagePlus("Labeled Mask", regIp);
        if (show) {
            labeledMask.show();
        }
        cellMask.setProcessor(totalmask);
//        cellMask.show();
        float bgRed = processBackGround(cellMask, 1);
        float bgFret = processBackGround(cellMask, 2);
        float bgGreen = processBackGround(cellMask, 3);
        float[] backgrounds = new float[] { bgRed, bgFret, bgGreen};
//        System.out.println("Background " + bgRed);
//        System.out.println("Background " + bgFret);
//        System.out.println("Background " + bgGreen);
        int nregions = (int)labeledMask.getProcessor().getStatistics().max;
        int[][] regionbounds = findRegionBounds(labeledMask.getProcessor());


        // iterate over each region found above
        // the labeled mask labelMask contains the image processor to use
        if (nregions > 50) {
            good = false;
            System.out.println("To many regions " + imp.getTitle());
            return;
        }
        for (int i = 1; i <= nregions; i++) {
            //get a patch of the raw image
            int x0 = regionbounds[i][0];
            int w0 = regionbounds[i][1] - x0;
            int y0 = regionbounds[i][2];
            int h0 = regionbounds[i][3] - y0;

            //just skip if it is too small
            if (w0*h0 < 300) {
                System.out.println("To small, region " + i + " " + imp.getTitle());
                continue;
            }
            if (w0*h0 > 5000) {
                System.out.println("To big, region " + i + " " + imp.getTitle());
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
            HashMap<Integer, List<Integer>> punctaMap = new HashMap<>();
            for (int p : points) {
                ArrayList<Integer> dg = growMaxRegion(dpatch, punctaRegions, p, .5, (byte) ri);
                if (dg.size() > 0) {
                    punctaMap.put(ri, dg);
                    ri++;
                }
//                System.out.println(i + "  " + ri + " " + dg.size() + " " +  punctaRegions.getStatistics().max);
//                System.out.println(p % dpatch.getWidth() + " " + p / dpatch.getWidth());
            }

            if (show) {
                ImagePlus pImp = new ImagePlus("Labeled Puncta", punctaRegions);
                pImp.show();
            }
            for (int c : channelList) {
                imp.setC(c);
                gip = imp.getProcessor();
                PunctaPatch ppc = new PunctaPatch(i, gip, x0, y0, w0, h0);
                //blur the patch to smooth for finding maxima

                ppc.measureFromPunctaMap(punctaMap);
                //ppc.measureFromLabeledRegions(punctaRegions);
                ppc.measureFromCellMask(maskPatch.getPatch());
//                System.out.println("Region(Cell): " + i + " , Channel: " + c);
                //pOut is a list of strings with measurements for each puncta
                //prefix contains image name, regionId (same as cellId),  channel
                String bg = String.format("%10.2f", backgrounds[c - 1]);
                String prefix = String.format("%s, %4d, %4d, %s", imageName, i, c, bg);
                ArrayList<String> pOut = ppc.makePunctaOutput(prefix);
                punctaToBuffer(pOut);
                //cellOut is the string for the whole cell measurement

                String cellPrefix = String.format("%s, %4d, %4d, %s", imageName, i, c, bg);
                String cellOut = ppc.makeCellOutput(cellPrefix);
                cellBuffer.append(cellOut);
            }

        }

    }


    public ImageProcessor addMasks(ImageProcessor mask1, ImageProcessor mask2) {

        mask1.erode();
        mask1.erode();
        mask2.erode();
        mask2.erode();

        byte[] p1 = (byte[])mask1.getPixels();
        byte[] p2 = (byte[])mask2.getPixels();
        byte[] m = new byte[p1.length];

        for(int i = 0; i < p1.length; i++) {
            if (p1[i] + p2[i] != 0) {
                m[i] = (byte)255;
            }
        }

        ImageProcessor mip = new ByteProcessor(mask1.getWidth(), mask1.getHeight());
        mip.setPixels(m);
        mip.erode();
        mip.dilate();
        EDM edm = new EDM();
        edm.toWatershed(mip);


        return mip;
    }

    public String getPunctaHeader() {
        return punctaHeader;
    }

    public String getCellHeader() {
        return cellHeader;
    }

    public float processBackGround(ImagePlus mask, int channel) {

        ImageProcessor mip = mask.getProcessor().duplicate();
        mip.erode();
        mip.erode();
        mip.erode();
        mip.erode();
        byte[] bgpix = (byte[])mip.getPixels();
        imp.setC(channel);
        float[] fpix = (float[])(imp.getProcessor().convertToFloatProcessor().getPixels());

        float sum = 0;
        for (int i = 0; i < bgpix.length; i++) {
            if (bgpix[i] == 0) {
                sum += fpix[i];
            }
        }

        float bgmean = sum/bgpix.length;
        return bgmean;
    }

    public void processBlue() {

        imp.setC(4);

        ImageProcessor blueIp = imp.getProcessor().duplicate().convertToFloatProcessor();
        BackgroundSubtracter bg = new BackgroundSubtracter();
        bg.subtractBackround(blueIp, 25);
        blueIp.blurGaussian(1.0);
        blueIp.findEdges();
        blueIp.gamma(.5);


        ImageProcessor mask = AutoThreshold.thresholdIp(blueIp, "Otsu", false, false);
        mask = mask.convertToByteProcessor();
        mask.erode();  //dilate
//        mask.dilate();

        ImageProcessor mask2 = mask.duplicate();
        mask2.invert();
        mask2 = BinaryImages.removeLargestRegion(mask2);
        mask2 = GeodesicReconstruction.fillHoles(mask2);




        mask2.erode(); //dilate
        mask2.erode(); //dilate
//        mask2.dilate();

        EDM edm = new EDM();
        edm.toWatershed(mask2);
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

//        ImageProcessor regIp = BinaryImages.componentsLabeling(mask, 4, 16);
//        labeledMask = new ImagePlus("Labeled Mask", regIp);

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


    public  ArrayList<Integer> growMaxRegion(ImageProcessor xip,  ImageProcessor mask, int pixelIndex, double level,
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

        double mean = xip.getStatistics().mean;  //mean of the image processor
        double max = px[pixelIndex];  //max of the region
        double std = xip.getStatistics().stdDev;
        double floor = mean + 1.0*std;
        double thresh = floor + level*(max - floor);   //the threshold for growing
        /*
        this can decide to not even count this peak if it is not high enough above
        the mean. This is fine, but just make sure to account for it when reporting results.
         */
        if (thresh < floor) {
            keepGoing = false;
        }
        if ((thresh + .25*std) > max) {
            keepGoing = false;
        }
//        System.out.println(thresh + " " + max);
        ArrayList<Integer> regionPix = new ArrayList<>();

        int res = 0;
        if (keepGoing) {
            regionPix.add(pixelIndex);
//            maskPix[pixelIndex] = regionValue;
            res++;
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
//                            maskPix[index] = regionValue;
                            res++;
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
//                        maskPix[index] = regionValue;
                        keepGoing = true;
                        res++;
                    }
                    xd = x + d;
                    if (xd >= w) continue;
                    if (xd <= 0) continue;
                    index = yd*w + xd;
                    if (px[index] > thresh) {
                        regionPix.add(index);
//                        maskPix[index] = regionValue;
                        keepGoing = true;
                        res++;
                    }

                }
            }
            d++;
            np += 2;
        }

        //System.out.println(res + " " + regionPix.size() +  " " + "dfdfdfdfdfdf");
        if (regionPix.size() > 4) {
            for (int index : regionPix) {
                maskPix[index] = regionValue;
            }
        } else {
            res = 0;
        }
        xip.setPixels(px);
        mask.setPixels(maskPix);
//        System.out.println("Num pix in region " + regionPix.size());

        return regionPix;
    }

    public void punctaToBuffer(List<String> pOut) {

        for (String p : pOut) {
            punctaBuffer.append(p);
        }
    }

    public String outputListToString() {
        return "";
    }
}
