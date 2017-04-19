package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;

/**
 * Created by cjw on 4/18/17.
 */


@Plugin(type = Command.class, name = "FindMaxPlane",  menuPath="Plugins>Chris>NotReady>FindMaxPlane")
public class FindMaxPlane implements Previewable, Command {

    @Parameter
    ImagePlus imp;

    int channel;
    int frame;

    int sizeT;
    int sizeZ;
    int sizeC;

    @Override
    public void run() {

        channel = imp.getC();
        sizeZ = imp.getNSlices();
        sizeT = imp.getNFrames();
        sizeC = imp.getNChannels();
        System.out.println(channel + " " + sizeZ + " "+  sizeT);

        System.out.println(channel);
        for (int i = 0; i < sizeT; i++) {
            ArrayList<Float> maxList = new ArrayList<>();
            float maxz = Float.MIN_VALUE;
            float zargmax = 0;
            for (int j = 0; j < sizeZ; j++) {
                int n = imp.getStackIndex(channel, j + 1, i + 1);
                ImageProcessor ip = imp.getStack().getProcessor(n);
                float[] pixels = (float[])ip.convertToFloatProcessor().getPixels();
                int index = getPlaneMaxIndex(pixels);
                float p = pixels[index];
                if (p > maxz) {
                    maxz = p;
                    zargmax = j + 1;
                }

            }
            System.out.println(i + " " + zargmax + " " + maxz);
        }
    }

    protected int getPlaneMaxIndex(float[] pixels) {

        float max = Float.MIN_VALUE;
        int argmax = 0;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > max) {
                max = pixels[i];
                argmax = i;
            }
        }

        return argmax;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
