package org.stowers.microscopy.ijplugins.utils;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

/**
 * Created by cjw on 6/19/17.
 */
public class BinaryPatch extends AbstractPatch {

    int label;

    public BinaryPatch(ImageProcessor oip, int x, int y, int width, int height, int label) {
        super(oip, x, y, width, height);
        this.label = label;
        ip = makePatch();
    }

    @Override
    public ImageProcessor makePatch() {

        byte a = (byte)255;
        ImageProcessor bip = new ByteProcessor(width, height);
        oip.setRoi(x, y,  width, height);
        ip = oip.crop();

        short[] zp = (short[])ip.getPixels();
        byte[] bp = (byte[])bip.getPixels();
        for (int i = 0; i < zp.length; i++) {
            if (zp[i] == label) {
                bp[i] = a;
            }
            else {
                bp[i] = 0;
            }
        }
            bip.setPixels(bp);

        return bip;
    }


}
