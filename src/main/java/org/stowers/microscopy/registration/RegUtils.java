package org.stowers.microscopy.registration;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import jguis.TurboRegJ_;

public class RegUtils {


    public static ImageProcessor getImpProcessor(final ImagePlus imp, int channel, int slice, int frame){

        int slices=imp.getNSlices();
        int channels=imp.getNChannels();
        int frames=imp.getNFrames();

        if(frames==1){
            frames=slices;
            slices=1;
            //targetFrame=targetSlice;
            //targetSlice=1;
            //frames2=slices2;
            //slices2=1;
        }

        int index=(channel-1)+(slice-1)*channels+(frame-1)*slices*channels+1;
        return imp.getStack().getProcessor(index).convertToFloat();
    }


    //this version doesn't rely on swapping the target and source every time
    public static double[][] getLocalTransform(ImagePlus imp,int width,int height,int transformation,
                                               int targetChannel, int targetSlice,
                                               int f,int prevf){
        double[][] sourcePoints = null;
        double[][] targetPoints = null;
        double[][] localTransform = null;
        ImagePlus target=new ImagePlus("StackRegTarget",getImpProcessor(imp,targetChannel,targetSlice,prevf));
        ImagePlus source=new ImagePlus("StackRegSource",getImpProcessor(imp,targetChannel,targetSlice,f));
        TurboRegJ_ trj=gettrj();
        switch (transformation) {
            case 0: {
                //simple translation
                trj.setTargetPoints(new double[][]{{width/2,height/2}});
                trj.setSourcePoints(new double[][]{{width/2,height/2}});
                trj.initAlignment(source, target, TurboRegJ_.TRANSLATION);
                break;
            }
            case 1: {
                //rigid body
                trj.setSourcePoints(new double[][]{{width/2,height/2},{width/2,height/4},{width/2,3*height/4}});
                trj.setTargetPoints(new double[][]{{width/2,height/2},{width/2,height/4},{width/2,3*height/4}});
                trj.initAlignment(source, target, TurboRegJ_.RIGID_BODY);
                break;
            }
            case 2: {
                //scaled rotation
                trj.setSourcePoints(new double[][]{{width/4,height/2},{3*width/4,height/2}});
                trj.setTargetPoints(new double[][]{{width/4,height/2},{3*width/4,height/2}});
                trj.initAlignment(source, target, TurboRegJ_.SCALED_ROTATION);
                break;
            }
            case 3: {
                //affine
                trj.setSourcePoints(new double[][]{{width/2,height/4},{width/4,3*height/4},{3*width/4,3*height/4}});
                trj.setTargetPoints(new double[][]{{width/2,height/4},{width/4,3*height/4},{3*width/4,3*height/4}});
                trj.initAlignment(source, target, TurboRegJ_.AFFINE);
                break;
            }
            default: {
                IJ.error("Unexpected transformation");
                return null;
            }
        }
        //target.setProcessor(null, source.getProcessor()); //here we put the source in the target imp
        sourcePoints = trj.getSourcePoints();
        targetPoints = trj.getTargetPoints();
        localTransform = getTransformationMatrix(targetPoints,sourcePoints,transformation);
        return localTransform;
    }

    public static double[][] getTransformationMatrix (final double[][] fromCoord,final double[][] toCoord,final int transformation) {
        //this was copied essentially as is from StackReg
        double[][] matrix = new double[3][3];
        switch (transformation) {
            case 0: {
                matrix[0][0] = 1.0;
                matrix[0][1] = 0.0;
                matrix[0][2] = toCoord[0][0] - fromCoord[0][0];
                matrix[1][0] = 0.0;
                matrix[1][1] = 1.0;
                matrix[1][2] = toCoord[0][1] - fromCoord[0][1];
                break;
            }
            case 1: {
                final double angle = Math.atan2(fromCoord[2][0] - fromCoord[1][0],
                        fromCoord[2][1] - fromCoord[1][1]) - Math.atan2(toCoord[2][0] - toCoord[1][0],
                        toCoord[2][1] - toCoord[1][1]);
                final double c = Math.cos(angle);
                final double s = Math.sin(angle);
                matrix[0][0] = c;
                matrix[0][1] = -s;
                matrix[0][2] = toCoord[0][0] - c * fromCoord[0][0] + s * fromCoord[0][1];
                matrix[1][0] = s;
                matrix[1][1] = c;
                matrix[1][2] = toCoord[0][1] - s * fromCoord[0][0] - c * fromCoord[0][1];
                break;
            }
            case 2: {
                double[][] a = new double[3][3];
                double[] v = new double[3];
                a[0][0] = fromCoord[0][0];
                a[0][1] = fromCoord[0][1];
                a[0][2] = 1.0;
                a[1][0] = fromCoord[1][0];
                a[1][1] = fromCoord[1][1];
                a[1][2] = 1.0;
                a[2][0] = fromCoord[0][1] - fromCoord[1][1] + fromCoord[1][0];
                a[2][1] = fromCoord[1][0] + fromCoord[1][1] - fromCoord[0][0];
                a[2][2] = 1.0;
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[0][1] - toCoord[1][1] + toCoord[1][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[1][0] + toCoord[1][1] - toCoord[0][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
            case 3: {
                double[][] a = new double[3][3];
                double[] v = new double[3];
                a[0][0] = fromCoord[0][0];
                a[0][1] = fromCoord[0][1];
                a[0][2] = 1.0;
                a[1][0] = fromCoord[1][0];
                a[1][1] = fromCoord[1][1];
                a[1][2] = 1.0;
                a[2][0] = fromCoord[2][0];
                a[2][1] = fromCoord[2][1];
                a[2][2] = 1.0;
                invertGauss(a);
                v[0] = toCoord[0][0];
                v[1] = toCoord[1][0];
                v[2] = toCoord[2][0];
                for (int i = 0; (i < 3); i++) {
                    matrix[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[0][i] += a[i][j] * v[j];
                    }
                }
                v[0] = toCoord[0][1];
                v[1] = toCoord[1][1];
                v[2] = toCoord[2][1];
                for (int i = 0; (i < 3); i++) {
                    matrix[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        matrix[1][i] += a[i][j] * v[j];
                    }
                }
                break;
            }
            default: {
                IJ.error("Unexpected transformation");
            }
        }
        matrix[2][0] = 0.0;
        matrix[2][1] = 0.0;
        matrix[2][2] = 1.0;
        return(matrix);
    } /* end getTransformationMatrix */


    private static void invertGauss (final double[][] matrix) {
        //also copied from StackReg
        final int n = matrix.length;
        final double[][] inverse = new double[n][n];
        for (int i = 0; (i < n); i++) {
            double max = matrix[i][0];
            double absMax = Math.abs(max);
            for (int j = 0; (j < n); j++) {
                inverse[i][j] = 0.0;
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                }
            }
            inverse[i][i] = 1.0 / max;
            for (int j = 0; (j < n); j++) {
                matrix[i][j] /= max;
            }
        }
        for (int j = 0; (j < n); j++) {
            double max = matrix[j][j];
            double absMax = Math.abs(max);
            int k = j;
            for (int i = j + 1; (i < n); i++) {
                if (absMax < Math.abs(matrix[i][j])) {
                    max = matrix[i][j];
                    absMax = Math.abs(max);
                    k = i;
                }
            }
            if (k != j) {
                final double[] partialLine = new double[n - j];
                final double[] fullLine = new double[n];
                System.arraycopy(matrix[j], j, partialLine, 0, n - j);
                System.arraycopy(matrix[k], j, matrix[j], j, n - j);
                System.arraycopy(partialLine, 0, matrix[k], j, n - j);
                System.arraycopy(inverse[j], 0, fullLine, 0, n);
                System.arraycopy(inverse[k], 0, inverse[j], 0, n);
                System.arraycopy(fullLine, 0, inverse[k], 0, n);
            }
            for (k = 0; (k <= j); k++) {
                inverse[j][k] /= max;
            }
            for (k = j + 1; (k < n); k++) {
                matrix[j][k] /= max;
                inverse[j][k] /= max;
            }
            for (int i = j + 1; (i < n); i++) {
                for (k = 0; (k <= j); k++) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for (k = j + 1; (k < n); k++) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }
        for (int j = n - 1; (1 <= j); j--) {
            for (int i = j - 1; (0 <= i); i--) {
                for (int k = 0; (k <= j); k++) {
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
                for (int k = j + 1; (k < n); k++) {
                    matrix[i][k] -= matrix[i][j] * matrix[j][k];
                    inverse[i][k] -= matrix[i][j] * inverse[j][k];
                }
            }
        }
        for (int i = 0; (i < n); i++) {
            System.arraycopy(inverse[i], 0, matrix[i], 0, n);
        }
    } /* end invertGauss */


    public static TurboRegJ_ gettrj(){
        try{
            Class c=Class.forName("TurboReg_");
            Object tr=c.newInstance();
            return new TurboRegJ_(tr);
        } catch(Throwable e){IJ.log(e.toString());}
        return null;
    }
}
