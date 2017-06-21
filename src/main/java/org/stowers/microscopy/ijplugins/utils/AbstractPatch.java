package org.stowers.microscopy.ijplugins.utils;

import ij.process.ImageProcessor;

/**
 * Created by cjw on 6/19/17.
 */
public abstract class AbstractPatch {

    int x;
    int y;
    int width;
    int height;

    ImageProcessor oip = null;
    ImageProcessor ip = null;

    public AbstractPatch(ImageProcessor oip, int x, int y, int width, int height) {
        this.oip = oip;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

    }

    public abstract ImageProcessor makePatch();
//    {
//
//        ImageProcessor zip = null;
//        if (!isBinary) {
//            oip.setRoi(x, y, width, height);
//            zip = oip.crop().convertToFloatProcessor();
//            zip = zip.convertToFloatProcessor();
////            ImagePlus zImp = new ImagePlus("patch", zip);
//
//        } else {
//            byte a = (byte)255;
//            ImageProcessor pip = oip.duplicate();
//            pip = pip.convertToByteProcessor();
//            pip.setRoi(x, y,  width, height);
//            zip = pip.crop();
//            pip.resetRoi();
//            byte[] zp = (byte[])zip.getPixels();
//            for (int i = 0; i < zp.length; i++) {
//                if (zp[i] != 0) {
//                    zp[i] = a;
//                }
//            }
//            zip.setPixels(zp);
//        }
//        return zip;
//    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public ImageProcessor getPatch() {
        return ip;
    }
}
