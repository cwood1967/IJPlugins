package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackStatistics;

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

    float mean3;

    float intensity = 0;
    float cx = 0;
    float cy = 0;
    float cz = 0;

    float scx = 0 ;
    float scy = 0 ;
    float scz = 0 ;

    int xmin;
    int xmax;
    int ymin;
    int ymax;
    int zmin;
    int zmax;

    int nrejected;
    float foundRadius;
    float maxSize;

    String name = "None";
    ImageStack stack;
    StackStatistics stats;

    List<Long> neighborsList;  //will include  the index of this peak'
    List<Long> notAllowed;

    public FindPeaks3DLocalMax(ImageStack stack, StackStatistics stats, int x, int y, int z, float value,
                               float tolerance, float threshold, float minsep, float zscale, float maxSize) {
        this.stack = stack;
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = value;
        this.tolerance = tolerance;
        this.threshold = threshold;
        this.minsep = minsep;
        this.zscale = zscale;
        this.stats = stats;
        this.maxSize = maxSize;

        w = stack.getWidth();
        h = stack.getHeight();
        d = stack.getSize();

        mean3 = measureMean(3);
        voxelIndex = z*w*h + w*y + x;
        neighborsList = new ArrayList<>();

    }

    public double[][] getXYZCoordinates() {

        double[][] res = new double[3][neighborsList.size()];
        for (int i = 0;i < neighborsList.size(); i++) {
            long index = neighborsList.get(i);
            res[0][i] = xFromIndex(index);
            res[1][i] = yFromIndex(index);
            res[2][i] =  zFromIndex(index);
        }
        return res;
    }

    private int xFromIndex(long index) {

        long az = index/(w*h);
        long p = index  - az*w*h;
        long ax = p % w;
        return (int)ax;

    }

    private int yFromIndex(long index) {

        long az = index/(w*h);
        long p = index  - az*w*h;
        long ay = p / w;
        return (int)ay;

    }

    private int zFromIndex(long index) {
        long az = index/(w*h);
        return (int)az;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public float getSumIntensity() {
        return intensity;
    }

    public List<Long> getNeighborsList() {
        return neighborsList;
    }

    public List<Long> getObjectVoxels() {
        return neighborsList;
    }

    public float getMeanIntensity() {
        return intensity/neighborsList.size();
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

    public int getDx() {
        return xmax - xmin;
    }
    public int getDy() {
        return ymax - ymin;
    }
    public int getDz() {
        return zmax - zmin;
    }

    public float[] getCOM() {
        return new float[] {cx, cy, cz};
    }

    public int getNRejected() {
        return nrejected;
    }

    public float getFoundRadius() {
        return foundRadius;
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

    public List<Long> findRegion(List<Long> notAllowed) {

        this.notAllowed = notAllowed;
        // 1215, 929, 3
        if ((x == 1215) & (y == 929)) {
            int blah = 1;
        }
        neighborsList.add(voxelIndex);
//        intensity = value;
//        searchNeighborhoodx, y, z);
        scx = value*x;
        scy = value*y;
        scz = value*z;
        intensity = value;

        cx = scx/intensity;
        cy = scy/intensity;
        cz = scz/intensity;
        searchForNeighbors();
//        System.out.println("working on: " + name + " " + x + " " + y + " " + z + " : " +
//                value + " -- " + neighborsList.size() + " " + voxelIndex);
//           System.out.println(neighborsList.size());
        return neighborsList;
    }


    private void searchForNeighbors() {

        xmin = Integer.MAX_VALUE;
        xmax= Integer.MIN_VALUE;
        ymin = Integer.MAX_VALUE;
        ymax= Integer.MIN_VALUE;
        zmin = Integer.MAX_VALUE;
        zmax= Integer.MIN_VALUE;

        List<Long> searchList = new ArrayList<>();
        Long index = (long)(z*w*h + y*w + x);
        searchList.add(index);

        while (searchList.size() > 0 && neighborsList.size() < maxSize) {
            Long i = searchList.get(0);
            int nx = (int)(i % (w*h))  % w;
            int ny = (int)(i % (w*h))  / w;
            int nz = (int)(i / (w*h));
            float dd = (nx - x)*(nx - x) + (ny - y)*(ny - y) + 1.f*(nz - z)*(nz - z);
            float d = (float)Math.sqrt(dd);
            if (dd > foundRadius*foundRadius) {
                foundRadius = (float) Math.sqrt(dd);
            }
            //System.out.println(dd + " " + Math.abs(nx -x) + " " + Math.abs(ny - y) + " " + Math.abs(nz - z));
            //if (d < (.5*minsep)) { //100*minsep*minsep/2.0) {
                List<Long> tmplist = searchNeighborhoodIterative(nx, ny, nz);
                searchList.addAll(tmplist);
            //.}

            searchList.remove(0);
        }

        cx = scx/intensity;
        cy = scy/intensity;
        cz = scz/intensity;
    }

    private List<Long> searchNeighborhoodIterative(int nx, int ny, int nz) {

        List<Long> resList = new ArrayList<>();
        if ((nx < 1) || (ny < 1) || (nz < 1)) return resList;
        if ((nx > w - 2) || (ny > h - 2) || (nz > d - 2)) return resList;

        float[] pixels = stack.getVoxels(nx - 1, ny -1, nz -1 , 3, 3, 3, null);

        //System.out.println("working on: " + nx + " " + ny + " " + nz + " : " + pixels[13]);


        for (int i = 0; i < pixels.length; i++) {
            if (i == pixels.length/2) continue; //don't check the center pixel

            int rx = (i % 9) % 3 - 1;
            int ry = (i % 9) / 3 - 1;
            int rz = i / 9 - 1;

            double dr2 = rx*rx + ry*ry+rz*rz;
            if (dr2 > 1.1) {
                continue;
            }
            int sx = nx + rx;
            int sy = ny + ry;
            int sz = nz + rz;
            Long index = (long)(sz*w*h + sy*w + sx);
            if (!okToUse(index)) {
                continue;
            }
            float cb = (float)(tolerance*(value)); // - stats.mean));
//            float cb = (float)(value - stats.mean) - tolerance;
            float cp = (float)(pixels[i]);// - stats.mean);

            if (pixels[i] > value) {
                continue;
            }

            float dd = (sx - x)*(sx - x) + (sy - y)*(sy - y) + 1.f*(sz - z)*(sz - z);
            float d = (float)Math.sqrt(dd);
//            if (d < (.5*minsep))

            if ((d < (.6*minsep)) && (cp > cb) && (pixels[i] > threshold)) {
//                    ((pixels[i] > (value - tolerance)) && (pixels[i] > threshold)) {
//                    (pixels[i] >` threshold) {

                if (!neighborsList.contains((Long)index)) {
                    neighborsList.add(index);
                    //System.out.println("Added: " + sx + " " + sy + " " + sz + " : " + pixels[i]);
                    //float dd = (nx - sx)*(nx - sx) + (ny - sy)*(ny - sy) + (nz - sz)*(nz - sz);
                    //take care of this in searchForNeighbors()
                    intensity +=  pixels[i];
                    scx += pixels[i]*sx;
                    scy += pixels[i]*sy;
                    scz += pixels[i]*sz;

                    checkextent(sx, sy, sz);
                    resList.add(index);
                }
            }
            else {
                nrejected++;
                //System.out.println("Rejected: " + sx + " " + sy + " " + sz + " : " + pixels[i]);
            }

        }


        return resList;
    }

    private void checkextent(int sx, int sy, int sz) {

        if (sx < xmin) {
            xmin = sx;
        } else if (sx > xmax) {
            xmax = sx;
        }
        if (sy < ymin) {
            ymin = sy;
        } else if (sy > ymax) {
            ymax = sy;
        }
        if (sz < zmin) {
            zmin = sz;
        } else if (sz > zmax) {
            zmax = sz;
        }

    }

    private boolean okToUse(long index) {

        if (notAllowed.contains(index)) {
            return false;
        } else {
            return true;
        }
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

    public float getMean3() {
        return mean3;
    }

    public float measureMean(int k) {

        if (x <= k/2 || x >= (w - (k/2 + 1)) ||  y <= k/2 || y > (h - (k/2 + 1))
                || z <= k/2 || z >= (d - (k/2 + 1))) {
            k = 1;
        }

        float[] pixels= null;

        try {
            pixels = stack.getVoxels(x - k / 2, y - k / 2, z - k / 2, k, k, k, null);
        }
        catch (IndexOutOfBoundsException e) {
            System.out.println(k + " " + toString());
            return value;
        }
        float sum = 0.f;
        for (int i = 0; i < pixels.length; i++) {
            sum += pixels[i];
        }

        return sum/pixels.length;
    }

    @Override
    public String toString() {

        String res = String.format("%10d, (%5d,%5d,%5d),"
                + "%10.1f,%10.1f,%5d, "
                + "%10.1f,%10.1f,%10.1f, "
                + "[%5d,%5d], [%5d,%5d], [%5d,%5d]",
                voxelIndex , x, y , z,
                value, intensity, neighborsList.size(),
                cx, cy, cz,
                xmin, xmax, ymin, ymax, zmin, zmax);
        return res;
    }

}
