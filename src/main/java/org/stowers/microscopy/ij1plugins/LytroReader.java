package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 1/17/17.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.io.ImageWriter;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import ij.plugin.filter.Rotator;

public class LytroReader {

    byte[] fileBytes;
    short[] rawImage;
    short[] rawImage2;
    Path path;
    int offset;
    int size = 7728*5368;
    int width = 7728;
    int height = 5368;
    CompositeImage compImp;
    short[][] debayer;
    short[] rotArray;

    String outPath;

    ImageStack bayerStack;
    ImageStack sampleStack;

    ImagePlus sampleImp;
    ImagePlus rawImp;
    ImagePlus projectedImp;

    public LytroReader(String pathname, int offset) {

        path = Paths.get(pathname);
        this.offset = offset;
    }

    public void swapEnds() {

        int pixel;


    }

    public int[] getImage2() {

        short[] image = new short[size];

        int index = 0;
        for (int i = 0; i < fileBytes.length; i += 5) {

            int s = 2;
            int t0 = (fileBytes[i] & 0x00FF) << s;
            int t1 = (fileBytes[i + 1] & 0x00FF) << s;
            int t2 = (fileBytes[i + 2] & 0x00FF) << s;
            int t3 = (fileBytes[i + 3] & 0x00FF) << s;
            int lsb = (fileBytes[i + 4] & 0x00FF) << s;

            t0 += (lsb & 3);
            t1 += (lsb & 12) >> 2;
            t2 += (lsb & 48) >> 4;
            t3 += (lsb & 192) >> 6;

            image[index] = (short)(t0 << 0);
            image[index + 1] = (short)(t1 << 0);
            image[index + 2] = (short)(t2 << 0);
            image[index + 3] = (short)(t3 << 0);

            index += 4;
        }

        ImageProcessor ip = new ShortProcessor(7728, 5368);
        ip.setPixels(image);
        rawImp = new ImagePlus("Nice");
        rawImp.setProcessor(ip);
//        rawImp.show();
        rawImage = image;
        return null;
    }

    public void rotate(double angle) {

        bayerStack.getProcessor(1).rotate(angle);
        bayerStack.getProcessor(2).rotate(angle);
        bayerStack.getProcessor(3).rotate(angle);

//        rawImp.getProcessor().rotate(angle);
//        rotArray = (short[])rawImp.getProcessor().getPixels();
        ImagePlus tempImp = new ImagePlus("Rotated");
        tempImp.setStack(bayerStack);
        compImp = new CompositeImage(tempImp, CompositeImage.COMPOSITE);
//        rawImp.show();
        //compImp.show();

    }

    public void showComp() {
        compImp.show();
    }
    public int[] getImage() {

        short[] image = new short[size];

        byte b;
        int iby = 0;
        int index = 0;
        int pixelIndex = 0;
        int pixel = 0;
        int tmp = 0;
        int counter = 0;
        System.out.println( "N Bits " + 8*fileBytes.length);

        for (int i = 0; i < fileBytes.length; i += 5) {

            long dumb = 0;
            dumb += (long)(fileBytes[i]) << 32;
            dumb += (long)(fileBytes[i + 1]) << 24;
            dumb += (long)(fileBytes[i + 2]) << 16;
            dumb += (long)(fileBytes[i + 3]) << 8;
            dumb += (long)(fileBytes[i + 4]) << 0;

            image[index + 3] = (short)((dumb >> 0) & 1023);
            image[index + 2] = (short)((dumb >> 10) & 1023);
            image[index + 1] = (short)((dumb >> 20) & 1023);
            image[index + 0] = (short)((dumb >> 30) & 1023);

            index += 4;

        }

        System.out.println(image.length + " " + pixelIndex + " " + iby);
        System.out.println(counter);
        System.out.println(size);

//        ImageProcessor ip = new ShortProcessor(7728, 5368);
//        ip.setPixels(image);
//        ImagePlus imp = new ImagePlus("Nice");
//        imp.setProcessor(ip);
//        imp.show();
        rawImage = image;
        return null;
    }

    public void sampleToHyperStack() {

        sampleImp = HyperStackConverter.toHyperStack(sampleImp, 3, sampleImp.getNSlices()/3, 1);

        return;

    }

    public void showSample() {

        sampleImp.show();
    }

    public void sample(float xoffset, float yoffset) {


        double sx = 14.283;
        double sy = 12.369;


        int wx = (int)(width/sx);
        int wy = (int)(height/sy);



//        System.out.println(wx + " " + wy + " " + wx*wy);
        short[] sr = new short[wx*wy];
        short[] sg = new short[wx*wy];
        short[] sb = new short[wx*wy];
        int index = 0;
        int rawIndex = 0;

        double xoff = 0;
        int yoff = 0;
        boolean nx = true;

        for(int j = 0; j < wy; j++) {

            if (nx) {
                xoff = 0;
            }
            else {
                xoff = sx/2.;
            }
            nx = !nx;
            double ychip = Math.round(yoffset + j*sy);

            for (int i = 0; i < wx; i++) {

                double xchip = Math.round(xoff + xoffset + i*sx);
                int k = (int)(ychip*width + xchip);
                index = j*wx + i;
                if (index >= sr.length) break;
                sr[index] = ((debayer[0][k]));
                sg[index] = ((debayer[1][k]));
                sb[index] = ((debayer[2][k]));
//                s[index] = (short)((rotArray[k]));

//                sa[index] = (short)((debayer[0][k - 4]));
//                sb[index] = (short)((debayer[0][k + 4]));
//                s[index] += (short)((debayer[0][k - 1]));
//                s[index] += (short)((debayer[0][k + 1]));
//                s[index] += (short)((debayer[0][k + width]));
//                s[index] += (short)((debayer[0][k - width]));
//                s[index] += (short)((debayer[0][k + width + 1]));
//                s[index] += (short)((debayer[0][k + width - 1]));
//                s[index] += (short)((debayer[0][k - width + 1]));
//                s[index] += (short)((debayer[0][k - width - 1]));

//                index++;
                if ((i + xoff + offset) >= width) {
                    System.out.println(i + " " + j + " " + k + " " + index + " " + xchip + " " + ychip);

                    break;
                }

                if (index >= sr.length) break;

            }
            if (index >= sr.length) break;
//            if (j > 1) break;
        }

        ImageProcessor ipr = new ShortProcessor((int)(width/sx), (int)(height/sy));
        ipr.setPixels(sr);
        ImageProcessor ipg = new ShortProcessor((int)(width/sx), (int)(height/sy));
        ipg.setPixels(sg);
        ImageProcessor ipb = new ShortProcessor((int)(width/sx), (int)(height/sy));
        ipb.setPixels(sb);
//        aip.setPixels(sa);
//        bip.setPixels(sb);

        if (sampleStack == null) {
            sampleStack = new ImageStack(wx, wy);
            sampleImp = new ImagePlus("Sample");

        }
        sampleStack.addSlice(ipr);
        sampleStack.addSlice(ipg);
        sampleStack.addSlice(ipb);

        //if (sampleImp.getStack() == null) {
        sampleImp.setStack(sampleStack);
        //}
        sampleImp.setTitle("Sample" + Float.toString(yoffset));
        //sampleImp.show();
        sampleImp.setTitle("Sample" + Float.toString(yoffset));
        sampleImp.updateAndRepaintWindow();


    }

    public void filterBayer() {

        short[] r = new short[size];
        short[] gr = new short[size];
        short[] gb = new short[size];
        short[] b = new short[size];

        debayer = new short[3][size];
        int rawIndex;
        int irx;

        boolean y = true;

        int counter = 0;

        // work on gr (top left pixel)
        int index = 0;
        for (int j = 0; j < height - 2; j += 2) {
            for (int i = 0; i < width - 2; i += 2) {
                int k = j*width + i;

                gr[k] = rawImage[k];

                if (i < (width -2) || j < (height - 2)) {
                    gr[k + 1] = gr[k]; //(short) ((rawImage[k] + rawImage[k + 2]) / 2);
                    gr[k + width] = gr[k]; //(short) ((rawImage[k] + rawImage[k + 2 * width]) / 2);
                    gr[k + width + 1] = gr[k]; //(short) ((rawImage[k] + rawImage[k + 2 * width] +
//                            rawImage[k + 2] + rawImage[k + 2 * width + 2]) / 4);
                } else {
                    gr[k + 1] = gr[k]; //rawImage[k];
                    gr[k + width] = gr[k]; //rawImage[k];
                    gr[k + width + 1] = gr[k]; //rawImage[k];
                }
                index++;
                }
        }

        // work on r (top right pixel)
//        int index = 0;

        for (int j = 0; j < height - 2; j += 2) {

            for (int i = 1; i < width - 1; i += 2) {
                int k = j*width + i;

                r[k] = rawImage[k];
                r[k + width] = r[k]; //(short)((rawImage[k] + rawImage[k + 2*width])/2);
                if (i > 1 || j > 1) {
                    r[k - 1] = r[k]; //(short) ((rawImage[k] + rawImage[k - 2]) / 2);
                    r[k + width -1] = r[k]; //(short)((rawImage[k] + rawImage[k - 2] +
//                                rawImage[k + 2*width - 2] + rawImage[k + 2*width])/4);
                } else {
                    r[k - 1] = r[k]; //rawImage[k];
                    r[k + width - 1] = r[k]; //rawImage[k];
                }
            }
        }

        // work on b (bottom left pixel)
//        int index = 0;
        for (int j = 1; j < height - 2; j += 2) {
            for (int i = 0; i < width - 2; i += 2) {
                int k = j*width + i;

                b[k] = rawImage[k];
                if ((i > (width - 2)) || (j > 1)) {
                    b[k - width] = b[k]; //(short)((rawImage[k] + rawImage[k - 2*width])/2);
                    b[k + 1] =  b[k]; //(short) ((rawImage[k] + rawImage[k + 2]) / 2);
                    b[k - width + 1] =  b[k]; //(short)((rawImage[k] + rawImage[k + 2] +
//                            rawImage[k + 2*width + 2] + rawImage[k + 2*width])/4);
                } else {
                    b[k + 1] =  b[k]; //rawImage[k];
                    b[k - width + 1] =  b[k]; //rawImage[k];
                    b[k - width] =  b[k]; //rawImage[k];
                }
            }
        }

        // work on gb (top right pixel)
//        int index = 0;
        for (int j = 1; j < height - 2; j += 2) {
            for (int i = 1; i < width - 2; i += 2) {
                int k = j*width + i;

                gr[k] = rawImage[k];
                if (i > 1 || j > 1) {
                    gr[k + width] = gr[k]; //(short)((rawImage[k] + rawImage[k + 2*width])/2);
                    gr[k - 1] = gr[k]; //(short) ((rawImage[k] + rawImage[k - 2]) / 2);
                    gr[k + width -1] = gr[k]; //(short)((rawImage[k] + rawImage[k - 2] +
//                            rawImage[k + 2*width - 2] + rawImage[k + 2*width])/4);
                } else {
                    gr[k - 1] = gr[k]; //rawImage[k];
                    gr[k + width - 1] = gr[k]; //rawImage[k];
                    gr[k + width] = gr[k]; //rawImage[k];
                }
            }
        }

        short[] g = new short[gb.length];

        for (int i = 0; i < g.length; i++) {
            g[i] = (short)((gb[i] + gr[i])/2);
        }

        debayer[0] = r;
        debayer[1] = g;
        debayer[2] = b;

        ImageStack stack = new ImageStack(width, height,3);
        ImageProcessor rip = new ShortProcessor(width, height);
        rip.setPixels(r);
        ImageProcessor gip = new ShortProcessor(width, height);
        gip.setPixels(g);

        ImageProcessor bip = new ShortProcessor(width, height);
        bip.setPixels(b);
//
        stack.setProcessor(rip, 1);
        stack.setProcessor(gip, 2);
        stack.setProcessor(bip, 3);
//
        bayerStack = stack;

    }

    public void get10(int pos) {
        for (int i = 0; i < 10; i++) {

            System.out.println(fileBytes[pos + i] & 0xFF);
        }
    }
    public void readFile() {

        try {
            byte[] bt = Files.readAllBytes(path);
            fileBytes = new byte[bt.length];
            int counter = 0;
            int index = 0;
            int i = 0;
            while (index < bt.length) {

                fileBytes[i] = bt[index + offset];
                i++;
                index++;

            }
            System.out.println(counter + " " + size + index + " " + i);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sumProject() {

        ZProjector zp = new ZProjector(sampleImp);
        zp.setStartSlice(1);
        zp.setStopSlice(sampleImp.getStack().getSize()/3);
        zp.setMethod(zp.SUM_METHOD);
        zp.doHyperStackProjection(true);
        projectedImp = zp.getProjection();
        projectedImp.show();

    }

    protected void saveProjectedImage() {

        FileSaver saver = new FileSaver(projectedImp);
        String fname = path.getFileName().toString();
        fname = fname.substring(0,8);
        System.out.println(fname);
        System.out.println(outPath + "/" + fname);

        saver.saveAsTiff(outPath + "/" + fname + ".tiff");
    }

    protected void setOutPath(String outpath) {
        this.outPath = outpath;
    }

    protected void run() {
        readFile();
        getImage2();
        filterBayer();
        rotate(0.115);

        for (float j = .4f*14.283f; j < .75f*14.283f ; j+= .4) {
            for (float i = .6f*14.283f + 0; i < 1.3f*14.283f; i+= .4){
                float ix = i - 0;
                float iy = j + 0;
                sample(ix, iy);
            }
        }

        sampleToHyperStack();
        sumProject();
        saveProjectedImage();

    }

    public static void main(String[] args) {

        final ImageJ imagej = net.imagej.Main.launch(args);

//        String dirname = "/Volumes/projects/jjl/public/Chris Wood/color picture";
//        String filename = "IMG_0488_9.json";
//        String dirname = "/Volumes/projects/jjl/public/Microfab generic/lytro timelapse";
//        String filename = "IMG_0487_9.json";
//
//        String dirname = "/Volumes/projects/cjw/LytroWhite";
//        String filename = "IMG_3308_9.json";

        String dirname = "/Users/cjw/Desktop";
        String filename = "IMG_0540_9.json";
        String outpath = "/Users/cjw/Desktop/";
        String pathname = dirname + "/" + filename;
        LytroReader r = new LytroReader(pathname, 000000);
        r.readFile();
//        r.getImage();
        r.getImage2();
        r.filterBayer();

        r.rotate(0.115); //"rotation": -0.0020000615622848272,  radians
//        r.showComp();
        for (float j = .4f*14.283f; j < .75f*14.283f ; j+= .5) {     //.49, .5
            for (float i = .6f*14.283f + 0; i < 1.3f*14.283f; i+= .5){
                float ix = i - 0;
                float iy = j + 0;
                r.sample(ix, iy);
            }
        }

        r.sampleToHyperStack();
//        r.showSample();
        r.sumProject();
        r.saveProjectedImage();
        r.saveProjectedImage();
    }

}
