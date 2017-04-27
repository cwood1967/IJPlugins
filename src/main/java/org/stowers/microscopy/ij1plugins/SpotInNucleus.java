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

        String filename = "/Users/cjw/DataTemp/Weems/20170421_153425_443/NDExp_Point0041_Seq0041.nd2";

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
        ImagePlus maxImp = zProject(imp, "MAX_METHOD");
        maxImp.getStack().convertToFloat();
        maxImp.show();
        maxImp.setC(4);
        ImageProcessor bip = guassianBlur(maxImp.getProcessor(), 4.0);

        ImageProcessor mip = AutoThreshold.thresholdIp(bip, "Li");
        mip.setBackgroundValue(0.);
        mip.erode();
        mip.erode();
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
        map2Image(gfpIp);


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

    /* use the instance variable map for the map */
    protected void map2Image(ImageProcessor ip) {

        float[] pixels = (float[])ip.getPixels();
        List<Integer> x = map.get(2);
        HashMap<Integer, Float> a = new HashMap();

        for (Integer i : x) {
            a.put(i, pixels[i]);
        }

        Comparator<Map.Entry<Integer, Float>> byValue = (entry1, entry2) -> entry1.getValue()
                .compareTo(entry2.getValue());

        a.entrySet().stream()
                .sorted(byValue.reversed())
                .limit(20)
                .forEach(System.out::println);

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
