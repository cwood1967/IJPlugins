package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 7/13/17.
 */

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.List;


public class LoG3DFilter {

    ImageStack stack;
    ImagePlus imp;

    float[] voxels;
    Object[] pixels;
    int sizeX;
    int sizeY;
    int sizeZ;

    int ksizeX;
    int ksizeY;
    int ksizeZ;

    double sx;
    double sy;
    double sz;

    double cfactor;
    double PI = Math.PI;
    public LoG3DFilter(ImagePlus imp, double sx, double sy, double sz) {

        this.sx = sx;
        this.sy = sy;
        this.sz = sz;

        ksizeX = calcKernelSize(sx);
        ksizeY = calcKernelSize(sy);
        ksizeZ = calcKernelSize(sz);
        //from https://github.com/mcib3d/TANGO/blob/21e2e4ebb8609cd192e665eff30b6e0f47f2a8ce/src/main/java/tango/plugin/filter/LoG3D.java
        //to determine the size of the kernel


        this.imp = imp;
        stack = imp.getStack().convertToFloat();
        sizeX = stack.getWidth();
        sizeY = stack.getHeight();
        sizeZ = stack.getSize();

        pixels = stack.getImageArray();
        //use this comments line below to get a line of pixels in x y or z
        //voxels = stack.getVoxels(0, 0, 0, sizeX, sizeY, sizeZ, );


    }

    private int calcKernelSize(double s) {
        int p = (int)(s*3.0*2.0 + 1.0);
        if (p % 2 == 0) {
            p++;
        }
        return p;
    }

    public ImageStack runFilter() {

        cfactor = 1./(Math.pow(2*PI, 1.5)*(sx*sy*sz));
        cfactor = 1;
        double[] hfx =  makeKernelF(ksizeX, sx);
        double[] hfy =  makeKernelF(ksizeY, sy);
        double[] hfz =  makeKernelF(ksizeZ, sz);
        double[] hbx =  makeKernelB(ksizeX, sx);
        double[] hby =  makeKernelB(ksizeY, sy);
        double[] hbz =  makeKernelB(ksizeZ, sz);

        /* This is making three copies of the stack

         */
        ImageStack part1 = stack.duplicate();
        ImageStack part2 = stack.duplicate();
        ImageStack part3 = stack.duplicate();

        //filter for the z pixellines

        List<LoG3DThread> zList = new ArrayList<>();
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {

                LoG3DThread g = new LoG3DThread(part1, hbz, i, j, 0, 1, 1, sizeZ);
                zList.add(g);
//                float[] pz1 = part1.getVoxels(i, j, 0, 1, 1, sizeZ, null);
//                float[] f1 = convolve(pz1, hbz);
//                part1.setVoxels(i, j, 0, 1, 1, sizeZ, f1);

                LoG3DThread u = new LoG3DThread(part2, hbz, i, j, 0, 1, 1, sizeZ);
                zList.add(u);
//                float[] pz2 = part2.getVoxels(i, j, 0, 1, 1, sizeZ, null);
//                float[] f2 = convolve(pz2, hbz);
//                part2.setVoxels(i, j, 0, 1, 1, sizeZ, f2);

                LoG3DThread v = new LoG3DThread(part3, hfz, i, j, 0, 1, 1, sizeZ);
                zList.add(v);
//                float[] pz3 = part3.getVoxels(i, j, 0, 1, 1, sizeZ, null);
//                float[] f3 = convolve(pz3, hfz);
//                part3.setVoxels(i, j, 0, 1, 1, sizeZ, f3);
            }
        }

        zList.parallelStream()
                .forEach(s->s.process());

        List<LoG3DThread> yList = new ArrayList<>();
        //filter for the y pixle lines
        for (int i = 0; i < sizeX; i++) {
            for (int k = 0; k < sizeZ; k++) {

                LoG3DThread g = new LoG3DThread(part1, hby, i, 0, k, 1, sizeY, 1);
                yList.add(g);
//                float[] py1 = part1.getVoxels(i, 0, k, 1, sizeY, 1, null);
//                float[] f1 = convolve(py1, hby);
//                part1.setVoxels(i, 0, k, 1, sizeY, 1, f1);

                LoG3DThread u = new LoG3DThread(part2, hfy, i, 0, k, 1, sizeY, 1);
                yList.add(u);
//                float[] py2 = part2.getVoxels(i, 0, k, 1, sizeY, 1, null);
//                float[] f2 = convolve(py2, hfy);
//                part2.setVoxels(i, 0, k, 1, sizeY, 1, f2);

                LoG3DThread v = new LoG3DThread(part3, hby, i, 0, k, 1, sizeY, 1);
                yList.add(v);
//                float[] py3 = part3.getVoxels(i, 0, k, 1, sizeY, 1, null);
//                float[] f3 = convolve(py3, hby);
//                part3.setVoxels(i, 0, k, 1, sizeY, 1, f3);

            }
        }

        yList.parallelStream()
                .forEach(s->s.process());


        List<LoG3DThread> xList = new ArrayList<>();
        for (int j = 0; j < sizeY; j++) {
            for (int k = 0; k < sizeZ; k++) {

                LoG3DThread g = new LoG3DThread(part1, hfx, 0, j, k, sizeX, 1, 1);
                xList.add(g);
//                float[] px1 = part1.getVoxels(0, j, k, sizeX, 1, 1, null);
//                float[] f1 = convolve(px1, hfx);
//                part1.setVoxels(0, j, k, sizeX, 1, 1, f1);

                LoG3DThread u = new LoG3DThread(part2, hbx, 0, j, k, sizeX, 1, 1);
                xList.add(u);
//                float[] px2 = part2.getVoxels(0, j, k, sizeX, 1, 1, null);
//                float[] f2 = convolve(px2, hbx);
//                part2.setVoxels(0, j, k, sizeX, 1, 1, f2);

                LoG3DThread v = new LoG3DThread(part3, hbx, 0, j, k, sizeX, 1, 1);
                xList.add(v);
//                float[] px3 = part3.getVoxels(0, j, k, sizeX, 1, 1, null);
//                float[] f3 = convolve(px3, hbx);
//                part3.setVoxels(0, j, k, sizeX, 1, 1, f3);

            }
        }

        xList.parallelStream()
                .forEach(s->s.process());


        double t1 = System.currentTimeMillis();
        ImageStack res = addStacks(part1, part2);
        res = addStacks(res, part3);
        part1 = null;
        part2 = null;
        part3 = null;
        System.out.println("Add time: " + (System.currentTimeMillis() - t1));

        int bb = 1;
        return res;
    }

    private double[] makeKernelF(int ksize, double s) {
        double[] hf = new double[ksize];

        double s2 = s*s;
        double s4 = s2*s2;
        for (int i = 0; i < ksize; i++) {
            double x = i - ksize/2;
            double f = x*x/s4 - 1./s2;
            f = f*Math.exp(-x*x/(2.*s2));
            hf[i] = -cfactor*f;
        }

        return hf;
    }

    private double[] makeKernelB(int ksize, double s) {

        double[] hb = new double[ksize];

        double s2 = s*s;

        for (int i = 0; i < ksize; i++) {
            double x = i - ksize/2;
            double b = Math.exp(-x*x/(2.*s2));
            hb[i] = b;
        }
        return hb;
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

    private ImageStack addStacks(ImageStack stack1, ImageStack stack2) {

        ImageStack res = new ImageStack(sizeX, sizeY, sizeZ);

        ImageProcessor ip1;
        ImageProcessor ip2;
        for (int k =0; k < sizeZ; k++) {

            //System.out.println(stack1.getProcessor(k + 1).getStatistics().max);
            //System.out.println(stack2.getProcessor(k + 1).getStatistics().max);
            float[] pix1 = (float[])stack1.getProcessor(k + 1).getPixels();
            float[] pix2 = (float[])stack2.getProcessor(k + 1).getPixels();

            float[] pixres = new float[pix1.length];
            for (int i = 0; i < pix1.length; i++) {
                pixres[i] = pix1[i] + pix2[i];
            }

            res.setPixels(pixres, k + 1);
        }

        return res;
    }


}
