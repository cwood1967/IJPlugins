package org.stowers.microscopy.ij1plugins;

import ij.ImageStack;

/**
 * Created by cjw on 8/2/17.
 */


public class FindPeaks3DLocalMax {

    int x;
    int y;
    int z;
    int w, h, d;
    long voxelIndex;
    float value;
    ImageStack stack;

    public FindPeaks3DLocalMax(ImageStack stack, int x, int y, int z, float value) {
        this.stack = stack;
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
        w = stack.getWidth();
        h = stack.getHeight();
        d = stack.getSize();

        voxelIndex = z*w*h + w*y + x;

    }

    public float getValue() {
        return value;
    }

    public long getVoxelIndex() {
        return voxelIndex;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean checkY() {

        if (value > 2500) {
            int sdsd = 4;
        }
        float[] pixels;
        try {
            pixels = stack.getVoxels(x - 1, y - 1, z - 1, 3, 3, 3, null);
        }

        catch (Exception e) {
            System.out.println(x + " " +  y + " " + z);
            e.printStackTrace();
            return false;
        }
        float v = pixels[13];
        boolean res = true;
        for (int i = 0; i < pixels.length; i++) {
            if (i == 13) continue;
            if (pixels[i] > v) {
                res = false;
                break;
            }
        }
        return res;
    }

    public boolean checkZ() {

        boolean res = false;
        return res;
    }

    @Override
    public String toString() {
        String res = voxelIndex + ", " + x + ", " + y + ", " + z + ", " + value;
        return res;
    }

}
