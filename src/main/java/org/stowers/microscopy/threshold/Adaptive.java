package org.stowers.microscopy.threshold;

/**
 * Created by cjw on 9/7/16.
 */

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.filter.GaussianBlur;


//import static net.imglib2.type.label.BasePairBitType.Base.T;

public class Adaptive {

    ImageProcessor ip;
//    int blockSize;
//
//  float offset;

    public Adaptive() {

    }

    public ImagePlus threshold(ImagePlus imp, int blockSize, float offset) {

        ImageProcessor ip = imp.getProcessor();
        FloatProcessor fp = null;
        fp = ip.convertToFloatProcessor();

        FloatProcessor fp0 = (FloatProcessor)fp.duplicate();
        double max  = fp.getStatistics().max;
        float[] pixels = (float[])fp.getPixels();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = pixels[i]/((float)max);
        }

        float[] p0 = new float[pixels.length];
        System.arraycopy(pixels, 0, p0, 0, pixels.length);

        GaussianBlur gb = new GaussianBlur();
        float s = (blockSize - 1)/6.f;
        gb.blurGaussian(fp, s, s , 0.01);



        byte[] resPix = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {

            if (p0[i] > (pixels[i] - offset)) {
                resPix[i] = (byte)255;
            }
            else {
                resPix[i] = 0;
            }
        }

        ByteProcessor bp = new ByteProcessor(imp.getWidth(), imp.getHeight(), resPix);
        ImagePlus resImp = new ImagePlus("AT", bp);
//        resImp.setProcessor(fp);

        return resImp;
    }


}
