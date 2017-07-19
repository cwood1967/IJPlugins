package org.stowers.microscopy.ij1plugins;

import ij.ImageStack;

/**
 * Created by cjw on 7/14/17.
 */
public class LoG3DThread {

    int x;
    int y;
    int z;
    int w;
    int h;
    int d;

    ImageStack stack;
    double[] kernel;

    public LoG3DThread(ImageStack stack, double[] kernel,
                       int x, int y, int z, int w, int h, int d) {

        this.stack = stack;
        this.kernel = kernel;
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.h = h;
        this.d = d;

    }

    public void process() {

        float[] p1 = stack.getVoxels(x, y, z, w, h, d, null);
        float[] f1 = convolve(p1, kernel);
        stack.setVoxels(x, y, z, w, h, d, f1);
    }

    private float[] convolve(float[] signal, double[] kernel) {

        float[] res = new float[signal.length];

        float[] padded = new float[res.length + kernel.length - 1];

        int kstart = -kernel.length/2;
        int padStart = kernel.length/2;

        for (int i = 0; i < signal.length; i++) {
            padded[i + padStart] = signal[i];
        }
        for (int i = 0; i < kernel.length/2; i++) {
            padded[i] = signal[0];
            padded[padded.length - 1 - i] = signal[signal.length - 1];
        }

        for (int i = 0; i < signal.length; i++) {
            float sum = 0.f;
            for (int k = 0; k < kernel.length; k++) {
                sum += kernel[k]*padded[i + k];
            }
            res[i] = sum;
        }
        return res;
    }

}
