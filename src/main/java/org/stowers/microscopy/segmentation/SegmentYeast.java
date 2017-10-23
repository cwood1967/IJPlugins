package org.stowers.microscopy.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import org.stowers.microscopy.threshold.AutoThreshold;

/**
 * Created by cjw on 9/20/17.
 */


public class SegmentYeast {

//    ImageProcessor ip;

    public SegmentYeast() {
//        this.ip = ip.convertToFloatProcessor();
    }

    public static ImageProcessor segmentBlue(ImageProcessor oip) {

        ImageProcessor ip = oip.duplicate().convertToFloatProcessor();
        BackgroundSubtracter bg = new BackgroundSubtracter();
        bg.subtractBackround(ip, 25);
        ip.blurGaussian(1.0);
        ip.findEdges();
        ip.gamma(.5);


        ImageProcessor mask = AutoThreshold.thresholdIp(ip, "Otsu", false, false);
        mask = mask.convertToByteProcessor();
        mask.erode();  //dilate
//        mask.dilate();

        ImageProcessor mask2 = mask.duplicate();
        mask2.invert();
        mask2 = BinaryImages.removeLargestRegion(mask2);
        IJ.run(new ImagePlus("Fill", mask2), "Fill Holes", null);

        mask2.erode(); //dilate
        mask2.erode(); //dilate
//        mask2.dilate();

        EDM edm = new EDM();
        edm.toWatershed(mask2);
        return mask2;
    }

    public static ImageProcessor segmentCyto(ImageProcessor ip) {

        ImageProcessor greenIp = ip.duplicate().convertToFloatProcessor();
        BackgroundSubtracter bg = new BackgroundSubtracter();
        bg.subtractBackround(greenIp, 25);
        greenIp.blurGaussian(3.0);
        ImageProcessor mask = AutoThreshold.thresholdIp(greenIp, "Otsu", false, false);
        mask = mask.convertToByteProcessor();
        mask.erode();
        mask.dilate();
        ImagePlus cellMask = new ImagePlus("cell mask", mask);
        IJ.run(cellMask, "Fill Holes", "");
        EDM edm = new EDM();
        edm.toWatershed(mask);
        return mask;
    }
}
