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
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ShortProcessor;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;

public class LytroReader {

    byte[] fileBytes;
    short[] rawImage;
    short[] rawImage2;
    Path path;
    int offset;
    int size = 7728*5368;
    int width = 7728;
    int height = 5368;

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
        ImagePlus imp = new ImagePlus("Nice");
        imp.setProcessor(ip);
        imp.show();
        rawImage = image;
        return null;
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
//        for (int bit = 0; bit < 8*fileBytes.length; bit++) {
//
//            iby = bit/8;
//            b = fileBytes[iby];
//            tmp = b >> (7 - bit % 8) & 1;
//            if (index < 8) {
//                pixel += tmp << (7 - index);
//                index++;
//            } else {
//                pixel += tmp << (index - 1);
//                index++;
//            }
//            if (index % 10 == 0) {
//                if (pixel == 0) {
//                    counter++;
//                }
//                image[pixelIndex] = (short)pixel;
//                pixelIndex++;
//                pixel = 0;
//                tmp = 0;
//                index = 0;
//            }
//            if (pixelIndex >= image.length) {
//                iby = bit;
//                break;
//            }
//        }
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

    public void sample(int xoffset, int yoffset) {

        int sx = 14;
        int sy = 13;
        short[] s = new short[(int)(size/sx/sy)];

        int index = 0;
        int rawIndex = 0;

        int xoff = 0;
        int yoff = 0;
        boolean nx = true;
        for (int j = yoffset; j < (height - sy/2); j += sy) {
            if (nx) {
                xoff = 0;
            }
            else {
                xoff = 7;
            }
            nx = !nx;
            for (int i = xoffset; i < (width - sx/2); i += sx) {
                int k = (int)(j*width + i + xoff);
                s[index] = rawImage[k];
//                System.out.println(i + " " + j + " " + k + " " + index);

                if ((i + xoff + offset) >= width) break;
                index++;
                if (index >= s.length) break;

            }
            if (index >= s.length) break;
//            if (j > 1) break;
        }

        ImageProcessor ip = new ShortProcessor((int)(width/sx), (int)(height/sy));
        ip.setPixels(s);
        ImagePlus imp = new ImagePlus("Sample");
        imp.setTitle("Sample");
        imp.setProcessor(ip);
        imp.show();

    }

    public void filterBayer() {

        short[] r = new short[size/4];
        short[] gr = new short[size/4];
        short[] gb = new short[size/4];
        short[] b = new short[size/4];

        int index = 0;
        int rawIndex;
        int irx;

        boolean y = true;

        int counter = 0;

        for (int j = 0; j < height; j += 2) {
            for (int i = 0; i < width; i += 2) {
                    int k = j*width + i;

                    gr[index] = rawImage[k];
                    r[index] = rawImage[k + 1];
                    b[index] = rawImage[k + width];
                    gr[index] = rawImage[k + width + 1];
                    index++;
                }
        }

//        ImageStack stack = new ImageStack(width/2, height/2,3);
//        ImageProcessor rip = new ShortProcessor(width/2, height/2);
//        rip.setPixels(r);
//        ImageProcessor gip = new ShortProcessor(width/2, height/2);
//        gip.setPixels(gr);
//        ImageProcessor bip = new ShortProcessor(width/2, height/2);
//        bip.setPixels(b);
//
//        stack.setProcessor(rip, 1);
//        stack.setProcessor(gip, 2);
//        stack.setProcessor(bip, 3);
//
//        ImagePlus imp = new ImagePlus();
//        imp.setTitle("Working?");
//        imp.setStack(stack);
//        CompositeImage cimp = new CompositeImage(imp);
//
//        cimp.show();
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

    public static void main(String[] args) {

        final ImageJ imagej = net.imagej.Main.launch(args);
        String dirname = "/Volumes/projects/jjl/public/Microfab generic/lytro timelapse";
//        String dirname = "/Volumes/projects/jjl/public/Chris Wood/color picture";
//        String filename = "IMG_0488_9.json";
        String filename = "IMG_0487_9.json";

        String pathname = dirname + "/" + filename;
        LytroReader r = new LytroReader(pathname, 000000);
        r.readFile();
//        r.getImage();
        r.getImage2();
        r.filterBayer();
        r.sample(1,1);

    }

}
