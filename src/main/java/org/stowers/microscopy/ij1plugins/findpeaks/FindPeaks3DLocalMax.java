package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImageStack;

import java.util.ArrayList;
import java.util.List;

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
    float tolerance;
    float threshold;
    float minsep;
    float zscale = 1.f;

    ImageStack stack;

    List<Long> neighborsList;  //will include  the index of this peak

    public FindPeaks3DLocalMax(ImageStack stack, int x, int y, int z, float value,
                               float tolerance, float threshold, float minsep, float zscale) {
        this.stack = stack;
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
        this.tolerance = tolerance;
        this.threshold = threshold;
        this.minsep = minsep;
        this.zscale = zscale;

        w = stack.getWidth();
        h = stack.getHeight();
        d = stack.getSize();

        voxelIndex = z*w*h + w*y + x;
        neighborsList = new ArrayList<>();

    }

    public List<Long> getNeighborsList() {
        return neighborsList;
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

    public List<Long> findRegion() {
        System.out.print("working on: " + x + " " + y + " " + z + " : " + value + " -- ");
        neighborsList.add(voxelIndex);
//        searchNeighborhoodx, y, z);
        searchForNeighbors();
        System.out.println(neighborsList.size());
        return neighborsList;
    }


    private void searchForNeighbors() {

        List<Long> searchList = new ArrayList<>();
        Long index = (long)(z*w*h + y*w + x);
        searchList.add(index);

        while (searchList.size() > 0) {
            Long i = searchList.get(0);
            int nx = (int)(i % (w*h))  % w;
            int ny = (int)(i % (w*h))  / w;
            int nz = (int)(i / (w*h));
            float dd = (nx - x)*(nx - x) + (ny - y)*(ny - y) + 1.f*(nz - z)*(nz - z);
            if (dd < minsep*minsep/2.0) {
                List<Long> tmplist = searchNeighborhoodIterative(nx, ny, nz);
                searchList.addAll(tmplist);
            }
            searchList.remove(0);
        }
    }

    private List<Long> searchNeighborhoodIterative(int nx, int ny, int nz) {

        List<Long> resList = new ArrayList<>();
        if ((nx < 1) || (ny < 1) || (nz < 1)) return resList;
        if ((nx > w - 2) || (ny > h - 2) || (nz > d - 2)) return resList;

        float[] pixels = stack.getVoxels(nx - 1, ny -1, nz -1 , 3, 3, 3, null);

        //System.out.println("working on: " + nx + " " + ny + " " + nz + " : " + pixels[13]);

        for (int i = 0; i < pixels.length; i++) {
            if (i == pixels.length/2) continue; //do check the center pixel

            int rx = (i % 9) % 3 - 1;
            int ry = (i % 9) / 3 - 1;
            int rz = i / 9 - 1;

            int sx = nx + rx;
            int sy = ny + ry;
            int sz = nz + rz;

            if (((pixels[i] > .5*value) && pixels[i] > threshold) ||
                    (pixels[i] > (value - tolerance)) && (pixels[i] > threshold)) {

                Long index = (long)(sz*w*h + sy*w + sx);
                if (!neighborsList.contains((Long)index)) {
                    neighborsList.add(index);
                    //System.out.println("Added: " + sx + " " + sy + " " + sz + " : " + pixels[i]);
                    float dd = (nx - sx)*(nx - sx) + (ny - sy)*(ny - sy) + (nz - sz)*(nz - sz);
                    //take care of this in searchForNeighbors()
//                    if (dd < 5*5) {
//                        resList.add(index);
//                    }
                }
            }
            else {
                //System.out.println("Rejected: " + sx + " " + sy + " " + sz + " : " + pixels[i]);
            }

        }
        return resList;
    }

    /*
    This was causing stack overflow errors due to the recursion,
    so I switched to an iterative version. See searchNeighborhoodIterative
     */
    private void searchNeighborhood(int nx, int ny, int nz) {

        if ((nx < 1) || (ny < 1) || (nz < 1)) return;
        if ((nx > w - 2) || (ny > h - 2) || (nz > d - 2)) return;

        float[] pixels = stack.getVoxels(nx - 1, ny -1, nz -1 , 3, 3, 3, null);

        //System.out.println("working on: " + nx + " " + ny + " " + nz + " : " + pixels[13]);

        for (int i = 0; i < pixels.length; i++) {
            if (i == pixels.length/2) continue; //do check the center pixel

            int rx = (i % 9) % 3 - 1;
            int ry = (i % 9) / 3 - 1;
            int rz = i / 9 - 1;

            int sx = nx + rx;
            int sy = ny + ry;
            int sz = nz + rz;

            if (((pixels[i] > .5*value) && pixels[i] > threshold) ||
                    (pixels[i] > (value - tolerance)) && (pixels[i] > threshold)) {

                Long index = (long)(sz*w*h + sy*w + sx);
                if (!neighborsList.contains((Long)index)) {
                    neighborsList.add(index);
                    //System.out.println("Added: " + sx + " " + sy + " " + sz + " : " + pixels[i]);
                    float d = (nx - sx)*(nx - sx) + (ny - sy)*(ny - sy) + (nz - sz)*(nz - sz);
                    if (d < 15*15) {
                        searchNeighborhood(sx, sy, sz);
                    }
                }
            }
            else {
                //System.out.println("Rejected: " + sx + " " + sy + " " + sz + " : " + pixels[i]);
            }

        }
    }

    @Override
    public String toString() {
        String res = voxelIndex + ", " + x + ", " + y + ", " + z + ", " + value;
        return res;
    }

}
