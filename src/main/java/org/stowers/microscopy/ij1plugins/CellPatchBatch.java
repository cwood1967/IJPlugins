package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import loci.common.DebugTools;
import loci.plugins.BF;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.stowers.microscopy.ijplugins.utils.NucleusPixels;
import org.stowers.microscopy.threshold.AutoThreshold;

import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.Map;

/**
 * Created by cjw on 5/2/17.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>NotReady>Batch Patch Cells")
public class CellPatchBatch implements Previewable, Command {

    @Parameter(label="Pick a folder for images", style="directory")
    File inputDir;

    @Parameter(label="Pick an output", style="directory")
    File outputDir;

    @Parameter(label="What do the filenames begin with?")
    String startsWith;

    @Parameter(label="What channel to save")
    int saveChannel = 3;  //one based for imagej

    @Parameter(label="What is the segmentation channel?")
    int segChannel = 4;  //one based for imagej

    @Override
    public void run() {
        DebugTools.setRootLevel("WARN");

        String[] files = inputDir.list();

        int count = 0;
        for (int i = 0; i < files.length; i++) {
            String f = files[i];
            if (f.startsWith(startsWith)) {
                String filename = inputDir + "/" + f;
                ImagePlus imp = null;
                System.out.println(f);
                try {
                    ImagePlus[] impArray = BF.openImagePlus(filename);
                    imp = impArray[0];
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                imp.setC(segChannel);
                ImageProcessor ip = imp.getProcessor().convertToFloatProcessor();

                imp.setC(saveChannel);
                ImageProcessor sip = imp.getProcessor().convertToFloatProcessor();

                ImageProcessor bip = guassianBlur(ip, 8.0);
                ImageProcessor mip = AutoThreshold.thresholdIp(bip, "Li", true);
                mip.setBackgroundValue(0.);

                ImageProcessor rip = AutoThreshold.labelRegions(mip);
                HashMap<Integer, List<Integer>> map = AutoThreshold.labelsToMap(rip);

                for (Map.Entry<Integer, List<Integer>> entry : map.entrySet()) {
                    if (entry.getValue().size() > 100) {
                        NucleusPixels np = new NucleusPixels(sip, entry.getValue());
                        np.savePatch(outputDir.getAbsolutePath(), f);
                    }
                }
            }
            count++;
//            if (count > 4) {
//                break;
//            }
        }

    }

    protected ImageProcessor guassianBlur(ImageProcessor ip, double radius) {
        ImageProcessor bip = ip.duplicate();
        bip = bip.convertToFloatProcessor();
        GaussianBlur blur =new GaussianBlur();
        blur.blurGaussian(bip, radius);
        return bip;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
