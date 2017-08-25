package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImageStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
/**
 * Created by cjw on 8/1/17.
 */
public class FindPeaks3DThread {

    int x;
    int y;
    int z;
    int w;
    int h;
    int d;

    float tol;
    float threshmin;
    float minsep;
    float zscale;

    String name = "None";

    ImageStack stack;

    List<Long> maxima;
    List<FindPeaks3DLocalMax> peaks;

    public FindPeaks3DThread(ImageStack stack, int x, int y, int z, int w, int h, int d,
                             float tol, float threshmin, float minsep, float zscale) {

        this.stack = stack;
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.h = h;
        this.d = d;
        this.tol = tol;
        this.threshmin = threshmin;
        this.minsep = minsep;
        this.zscale = zscale;

    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getNumMaxima() {
        return maxima.size();
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FindPeaks3DLocalMax> process() {

        float[] p1 = stack.getVoxels(x, y, z, w, h, d, null);
        maxima = Collections.synchronizedList(new ArrayList<>());
        peaks = Collections.synchronizedList(new ArrayList<>());
        for (int i = 1; i < p1.length - 1; i++) {
            if (threshmin > p1[i]) continue;
            if (p1[i - 1] > p1[i]) continue;
            if (p1[i + 1] > p1[i]) continue;

            long index = z*w*h + y*w + x + i;  //p1 is a line of pixels in x, so i is the x coordinate
            maxima.add((Long)index);
            FindPeaks3DLocalMax peak = new FindPeaks3DLocalMax(stack, x + i, y, z, p1[i], tol,
                    threshmin, minsep, zscale);
            peak.setName(name);
            peaks.add(peak);
        }

        return peaks;
    }

    public void checkY() {

        if (maxima.size() == 0) return;

        List<Long> toRemove = new ArrayList<>();
        String g = Integer.toString(maxima.size()) + " ";
        if (y > (stack.getHeight() - 2)) {
            maxima.clear();
            //System.out.println(y + " " + z + " " + g + maxima.size());
            return;
        }

        if (y < 2) {
            maxima.clear();
//            System.out.println(y + " " + z + " " + g + maxima.size());
            return;
        }
        for (long index: maxima) {
            int i = (int)((index % w*h) % w);
            float[] pix = stack.getVoxels(i, y - 1, z, 1, 3, 1, null);
            if ((pix[0] > pix[1]) && (pix[2] > pix[1])) {
                toRemove.add(Long.valueOf(index));
            }

        }

        for (Long index : toRemove) {
            maxima.remove((Long)index);
        }
//        System.out.println(y + " " + z + " " + g + maxima.size());
    }

    public void checkZ() {

        if (maxima.size() == 0) {
            return;
        }

        List<Long> toRemove = new ArrayList<>();
        String g = Integer.toString(maxima.size()) + " ";
        if (z > (stack.getSize() - 2)) {
            maxima.clear();
            //System.out.println(y + " " + z + " " + g + maxima.size());
            return;
        }

        if (z < 2) {
            maxima.clear();
            //System.out.println(y + " " + z + " " + g + maxima.size());
            return;
        }
        for (long index: maxima) {
            int i = (int)((index % w*h) % w);
            float[] pix = stack.getVoxels(i, y, z - 1, 1, 1, 3, null);
            if ((pix[0] > pix[1]) && (pix[2] > pix[1])) {
                toRemove.add(Long.valueOf(i));
            }
        }
        for (Long index : toRemove) {
            maxima.remove((Long)index);
        }
//        System.out.println(y + " " + z + " " + g + maxima.size());
    }
}
