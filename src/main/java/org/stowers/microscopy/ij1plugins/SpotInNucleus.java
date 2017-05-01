package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.plugin.ZProjector;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import inra.ijpb.binary.BinaryImages;
import org.stowers.microscopy.org.stowers.microscopy.utils.NucleusPixels;
import org.stowers.microscopy.threshold.AutoThreshold;

import java.io.IOException;
import java.util.*;

/**
 * Created by cjw on 4/25/17.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>NotReady>Nuclei Spots")
public class SpotInNucleus  implements Previewable, Command {

//    @Parameter
    ImagePlus imp;

    HashMap<Integer, List<Integer>> map = null;
    @Override
    public void run() {

        String filename = "/Users/cjw/DataTemp/Weems/20170421_153425_443/MAX_NDExp_Point0004_Seq0004.nd2.tif";

        try {
            ImagePlus[] impArray = BF.openImagePlus(filename);
            imp = impArray[0];
        }

        catch(Exception e) {
            e.printStackTrace();
            return;
        }
        imp.show();

        go();
    }

    private void go() {

        int nc = imp.getNChannels();
        int nt = imp.getNFrames();
        int nz = imp.getStackSize()/nc/nt;

        ImagePlus maxImp = null;
        if (nz > 1) {
            maxImp = zProject(imp, "MAX_METHOD");
        } else {
            maxImp = imp;
        }

        maxImp.getStack().convertToFloat();
        maxImp.show();
        maxImp.setC(4);
        ImageProcessor bip = guassianBlur(maxImp.getProcessor(), 4.0);
        ImageProcessor mip = AutoThreshold.thresholdIp(bip, "Li");
        mip.setBackgroundValue(0.);
//        mip.erode();
//        mip.erode();
        ImageProcessor rip = AutoThreshold.labelRegions(mip);
        ImagePlus rimp = new ImagePlus("Labeled Regions", rip);
        rimp.show();

        // create a map to hold list of pixels for each segmented cell
        // key is tje region number, value is the List of pixels
        map = AutoThreshold.labelsToMap(rip);

        for (int i : map.keySet()) {
            System.out.println(map.get(i).size());
        }

        maxImp.setC(3);
        ImageProcessor gfpIp = maxImp.getProcessor().convertToFloatProcessor();
        gfpIp.filter(ImageProcessor.BLUR_MORE);
        gfpIp.filter(ImageProcessor.BLUR_MORE);
//        map2Image(gfpIp);

        for (Map.Entry<Integer, List<Integer>> entry : map.entrySet()) {
            System.out.println(entry.getKey());
            NucleusPixels np = new NucleusPixels(gfpIp, entry.getValue());
        }

    }

    protected ImageProcessor guassianBlur(ImageProcessor ip, double radius) {
        ImageProcessor bip = ip.duplicate();
        bip = bip.convertToFloatProcessor();
        GaussianBlur blur =new GaussianBlur();
        blur.blurGaussian(bip, radius);
        return bip;
    }

    protected ImagePlus zProject(ImagePlus timp, String method) {

        ImagePlus projected = null;
        String[] methods = ZProjector.METHODS;
        int imethod = -1;
        for (int k = 0; k < methods.length; k++) {
            if (methods[k] == method) {
                imethod = k;
                break;
            }
        }

        int nc = timp.getNChannels();
        int nt = timp.getNFrames();
        int nz = timp.getStackSize()/nc/nt;

        ZProjector zproj = new ZProjector(timp);
        zproj.setStartSlice(1);
        zproj.setStopSlice(nz);
        zproj.setMethod(imethod);
        zproj.doHyperStackProjection(true);
        projected = zproj.getProjection();
        return projected;
    }

    /* use the instance variable map for the map
    Sort the pixels values in each region of the green channel.
    Smooth the image first (do that somewhere else) to get the intensity of the neighborhood)
    */
    protected void map2Image(ImageProcessor ip) {

        int w = ip.getWidth();
        int h = ip.getHeight();
        float[] pixels = (float[])ip.getPixels();
        for (Map.Entry<Integer, List<Integer>> s :map.entrySet()) {
            List<Integer> x =s.getValue();
            int inum = s.getKey();
            System.out.println("########### " + inum + " ##############");
            HashMap<Integer, Float> a = new HashMap();

            for (Integer i : x) {
                a.put(i, pixels[i]);
            }

            Comparator<Map.Entry<Integer, Float>> byValue = (entry1, entry2) -> entry1.getValue()
                    .compareTo(entry2.getValue());

            a.entrySet().stream()
                    .sorted(byValue.reversed())
                    .limit(10)
                    .forEach(System.out::println);
        }

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
