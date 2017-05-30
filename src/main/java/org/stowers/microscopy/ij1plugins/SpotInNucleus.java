package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.ZProjector;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import loci.common.DebugTools;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import org.stowers.microscopy.spotanalysis.Spot;

import loci.plugins.BF;

import org.stowers.microscopy.ijplugins.utils.NucleusPixels;
import org.stowers.microscopy.threshold.AutoThreshold;

import java.io.File;
import java.util.*;

/**
 * Created by cjw on 4/25/17.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>NotReady>Nuclei Spots")
public class SpotInNucleus  implements Previewable, Command {


    @Parameter(label="file numnber")
    String filenumber = "0035";
//    @Parameter
    ImagePlus imp;
    ImagePlus sumImp;
    HashMap<Integer, List<Integer>> map = null;
    @Override
    public void run() {
        DebugTools.setRootLevel("WARN");
//        String filename = "/Users/cjw/DataTemp/Weems/20170421_153425_443/MAX_NDExp_Point0004_Seq0004.nd2.tif";
        String dir = "/Volumes/projects/smc/public/JCW/20170421/20170421_153425_443/ZProjections";

        File fdir = new File(dir);
        String[] names = fdir.list();

//        java.util.Random rand = new java.util.Random();
//        int fn = (int)(names.length*rand.nextDouble());
        int fn = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i].contains(filenumber)) {
                fn = i;
                break;
            }
        }
        String base = names[fn];
        base = base.substring(3);
        String filename = dir + "/" + "MAX" + base;
        //String filename = dir + "/" + "MAX_NDExp_Point0009_Seq0009.nd2.tif";
        String sumfile = dir + "/" + "SUM" + base;
//        String sumfile = dir + "/" + "SUM_NDExp_Point0009_Seq0009.nd2.tif";
        try {
            ImagePlus[] impArray = BF.openImagePlus(filename);
            imp = impArray[0];
            ImagePlus[] sumImpArray = BF.openImagePlus(sumfile);
            sumImp = sumImpArray[0];
        }

        catch(Exception e) {
            e.printStackTrace();
            return;
        }
        imp.show();
        sumImp.show();
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
        ImageProcessor bip = guassianBlur(maxImp.getProcessor(), 8.0);


        ImageProcessor mip = AutoThreshold.thresholdIp(bip, "Li", true);
        mip.setBackgroundValue(0.);
//        mip.erode();
//        mip.erode();

        //change to channel 3
        maxImp.setC(3);
        ImageProcessor gip = guassianBlur(maxImp.getProcessor(), 8.0);
        ImageProcessor mgip = AutoThreshold.thresholdIp(gip, "Li", false);

        byte[] gp = (byte[])mgip.getPixels();
        byte[] mp = (byte[])mip.getPixels();

        //check to see if the gfp maskis zero, set mip mask to zero if it is
        for (int i = 0; i < gp.length; i++) {
            if (gp[i] == 0) {
                mp[i] = 0;
            }
        }

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

        //this blurring is ok, but maybe decide to do something else later
        //gfpIp.filter(ImageProcessor.BLUR_MORE);
        //gfpIp.filter(ImageProcessor.BLUR_MORE);
        gfpIp = guassianBlur(gfpIp, 2.);
//        map2Image(gfpIp);

        RoiManager manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }

        for (Map.Entry<Integer, List<Integer>> entry : map.entrySet()) {
            System.out.println(entry.getKey());
            if (entry.getValue().size() < 300) {
                continue;
            }

            // create a nucleus pixels, gfpIp is the image the pixels come from
            // entry.getValue is the list of pixel indicies for this cell
            NucleusPixels np = new NucleusPixels(gfpIp, entry.getValue());
            if (np.getIsGoodSpot()) {
                int rIndex = np.getMeasureRegionIndex();
                int rx = rIndex % maxImp.getWidth();
                int ry = rIndex / maxImp.getWidth();
                Roi roi = new Roi(rx - 15, ry - 15, 30, 30);
                manager.add(maxImp, roi, rx);
                measure(sumImp, rIndex, 31);

                sumImp.setC(3);
                ImageProcessor fip = sumImp.getProcessor().convertToFloatProcessor();
                Spot spot = new Spot(entry.getKey(), rx, ry, fip, 31, 1, 1);
                spot.setPixelWidth(1.);
                spot.setStack(null);
                spot.makePatch();
                double[] pres = spot.fitPatch();
                if (pres == null) continue;
                for (int p = 0; p < pres.length; p++) {
                    System.out.println("P: " + p + " - " + pres[p]);
                }
                System.out.println("------ channel 1 fit ----");
                sumImp.setC(1);
                ImageProcessor fip1 = sumImp.getProcessor().convertToFloatProcessor();
                Spot spot1 = new Spot(entry.getKey(), rx, ry, fip1, 31, 1, 1);
                spot1.setPixelWidth(1.);
                spot1.setStack(null);
                spot1.makePatch();
                double[] pres1 = spot1.fitPatch();
                if (pres1 == null) continue;
                for (int p = 0; p < pres1.length; p++) {
                    System.out.println("P: " + p + " - " + pres1[p]);
                }
            }
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


    protected void measure(ImagePlus img, int index, int size) {

        int nc = img.getNChannels();

        for (int i = 0; i < nc; i++) {
            img.setC(i + 1);
            ImageProcessor ip = img.getProcessor().convertToFloatProcessor();
            double s = measurePoint(ip, index, size);
            double g = measureOutside(ip, index, size, 2*size);
            double outarea = (2*size - size)*(2*size - size);
            System.out.println("C: " + (i + 1) + " : " + s/(size*size) + " " + g/outarea);
        }
    }

    protected double measurePoint(ImageProcessor ip, int index, int size) {

        int x = index % ip.getWidth();
        int y = index / ip.getWidth();
        int x0 = x - size/2;
        int y0 = y - size/2;
        int xf = x + size/2;
        int yf = y + size/2;

        float[] pixels = (float[])ip.getPixels();

        double sum = 0.;
        for (int j = y0; j <= yf; j++) {
            for (int i = x0; i <= xf; i++) {
                int idx = i + j*ip.getWidth();
                sum += pixels[idx];
            }
        }

        return sum;

    }

    protected double measureOutside(ImageProcessor ip, int index, int size, int outsize) {

        //can I ignore copied code?
        int x = index % ip.getWidth();
        int y = index / ip.getWidth();
        int x0 = x - outsize/2;
        int y0 = y - outsize/2;
        int xf = x + outsize/2;
        int yf = y + outsize/2;

        int xm0 = x - size/2;
        int ym0 = y - size/2;
        int xmf = x + size/2;
        int ymf = y + size/2;
        float[] pixels = (float[])ip.getPixels();

        double sum = 0.;
        for (int j = y0; j <= yf; j++) {
            if (j < ym0 || j > ymf) {
                for (int i = x0; i <= xf; i++) {
                    if (i < xm0 || i > xmf) {
                        int idx = i + j * ip.getWidth();
                        sum += pixels[idx];
                    }
                }
            }
        }

        return sum;

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
