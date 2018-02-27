package org.stowers.microscopy.registration;

import ij.IJ;
import ij.ImagePlus;
import jguis.TurboRegJ_;

public class FrameTransformer {

    int frames;
    int slices;
    int channels;

    int width;
    int height;

    int transformation;
    int f;
    ImagePlus imp;
    double[][] globalTransform;
    double[][] anchorPoints;

    public FrameTransformer(ImagePlus imp, int width, int height, int transformation,
                            double[][] globalTransform, double[][] anchorPoints, int f) {


        this.imp = imp;
        this.globalTransform = globalTransform;
        this.anchorPoints = anchorPoints;
        this.transformation = transformation;
        this.f = f;
        frames = imp.getNFrames();
        slices = imp.getNSlices();
        channels = imp.getNChannels();

        this.width = imp.getWidth();
        this.height = imp.getHeight();


        if(frames==1){
            frames=slices;
            slices=1;
        }
    }

    public boolean transformFrame() { //ImagePlus imp, int width, int height, int transformation, double[][] globalTransform, double[][] anchorPoints, int f){
        //this just uses the globalTransform to transform the indicated frame
        double[][] sourcePoints=null;
        TurboRegJ_ trj=null;
        switch (transformation) {
            case 0: {
                //transform a new set of source and anchor points
                sourcePoints = new double[1][3];
                for (int i = 0; (i < 3); i++) {
                    sourcePoints[0][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
                    }
                }
                //and transform the entire hyperstack frame according to those points
                for(int i=1;i<=slices;i++){
                    for(int j=1;j<=channels;j++){
                        trj=RegUtils.gettrj();
                        trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]}});
                        trj.setTargetPoints(new double[][]{{width/2,height/2}});
                        ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
                        ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.TRANSLATION);

                        if(transformed==null) {
                            System.out.println("Why is this happening?");
                            return false;
                        }
                        RegUtils.setImpProcessor(imp,transformed,j,i,f);
                    }
                }
                break;
            }
            case 1: {
                sourcePoints = new double[3][3];
                for (int i = 0; (i < 3); i++) {
                    sourcePoints[0][i] = 0.0;
                    sourcePoints[1][i] = 0.0;
                    sourcePoints[2][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
                        sourcePoints[1][i] += globalTransform[i][j]* anchorPoints[1][j];
                        sourcePoints[2][i] += globalTransform[i][j]* anchorPoints[2][j];
                    }
                }

                for(int i=1;i<=slices;i++){
                    for(int j=1;j<=channels;j++){
                        trj=RegUtils.gettrj();
                        trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]},{sourcePoints[1][0],
                                sourcePoints[1][1]},{sourcePoints[2][0],sourcePoints[2][1]}});
                        trj.setTargetPoints(new double[][]{{width/2,height/2},{width/2,height/4},{width/2,3*height/4}});
                        ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
                        ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.RIGID_BODY);
                        if(transformed==null) return false;
                        RegUtils.setImpProcessor(imp,transformed,j,i,f);
                    }
                }
                break;
            }
            case 2: {
                sourcePoints = new double[2][3];
                for (int i = 0; (i < 3); i++) {
                    sourcePoints[0][i] = 0.0;
                    sourcePoints[1][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
                        sourcePoints[1][i] += globalTransform[i][j]* anchorPoints[1][j];
                    }
                }
                for(int i=1;i<=slices;i++){
                    for(int j=1;j<=channels;j++){
                        trj=RegUtils.gettrj();
                        trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]},{sourcePoints[1][0],sourcePoints[1][1]}});
                        trj.setTargetPoints(new double[][]{{width/4,height/2},{3*width/4,height/2}});
                        ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
                        ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.SCALED_ROTATION);
                        if(transformed==null) return false;
                        RegUtils.setImpProcessor(imp,transformed,j,i,f);
                    }
                }
                break;
            }
            case 3: {
                sourcePoints = new double[3][3];
                for (int i = 0; (i < 3); i++) {
                    sourcePoints[0][i] = 0.0;
                    sourcePoints[1][i] = 0.0;
                    sourcePoints[2][i] = 0.0;
                    for (int j = 0; (j < 3); j++) {
                        sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
                        sourcePoints[1][i] += globalTransform[i][j]* anchorPoints[1][j];
                        sourcePoints[2][i] += globalTransform[i][j]* anchorPoints[2][j];
                    }
                }
                for(int i=1;i<=slices;i++){
                    for(int j=1;j<=channels;j++){
                        trj=RegUtils.gettrj();
                        trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]},{sourcePoints[1][0],sourcePoints[1][1]},{sourcePoints[2][0],sourcePoints[2][1]}});
                        trj.setTargetPoints(new double[][]{{width/2,height/4},{width/4,3*height/4},{3*width/4,3*height/4}});
                        ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
                        ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.AFFINE);
                        if(transformed==null) return false;
                        RegUtils.setImpProcessor(imp,transformed,j,i,f);
                    }
                }
                break;
            }
        }

        return true;
    }
}
