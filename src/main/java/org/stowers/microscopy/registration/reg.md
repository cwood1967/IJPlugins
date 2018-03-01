
###  Original

```java
    double[][][] localtransforms=new double[frames][][];
        for(int f= (targetFrame - 1); f>0; f--) {
            //localtransforms[f-1]=getLocalTransform(target,imp,width,height,transformation,f);
            localtransforms[f-1]=getLocalTransform(imp,width,height,transformation,f,f+1);
            IJ.showStatus("Frame "+f+" Registered");
        }
        for (int f=(targetFrame+1); f<=frames; f++) {
            //localtransforms[f-1]=getLocalTransform(target,imp,width,height,transformation,f);
            localtransforms[f-1]=getLocalTransform(imp,width,height,transformation,f,f-1);
            IJ.showStatus("Frame "+f+" Registered");
        }

```


### Multi-threaded
```java
double[][][] localtransforms=new double[frames][][];

        long t1 = System.currentTimeMillis();

        List<RegThread> threads = new ArrayList<>();
        for(int f= (targetFrame - 1); f>0; f--) {
            threads.add(new RegThread(imp, transformation, f, f +1 , targetChannel, targetSlice));
        }

        for (int f=(targetFrame+1); f<=frames; f++) {
            threads.add(new RegThread(imp, transformation, f, f - 1 , targetChannel, targetSlice));
        }

        threads.parallelStream()
                .forEach(s -> s.calcLocalTransform(localtransforms));

```

### Thread registration class

```java
public class RegThread {

    int targetframe;
    int sourceframe;
    int targetChannel;
    int targetSlice;
    int transformation;
    double[][] localTransform;

    ImagePlus imp;
    public RegThread(ImagePlus imp, int transformation, int sourceframe, int targetframe,
                     int targetChannel, int targetSlice) {

        this.imp = imp;
        this.targetframe = targetframe;
        this.sourceframe = sourceframe;
        this.targetChannel = targetChannel;
        this.targetSlice = targetSlice;
        this.transformation = transformation;

    }

    public void calcLocalTransform(double[][][] matrix) {

//        System.out.println("Frame " + sourceframe + " + registered to " + targetframe);
        double[][] transform = RegUtils.getLocalTransform(imp, imp.getWidth(), imp.getHeight(), transformation,
                targetChannel, targetSlice, sourceframe, targetframe);

        localTransform = transform;
        matrix[sourceframe - 1] = transform;
        IJ.showStatus("Frame " + sourceframe + " + registered to " + targetframe);
    }

}
```

### Original Transformation
```java
for(int f=1;f<=frames;f++){
            if(!transformFrame(imp, width, height,transformation, transforms[f-1], anchorPoints, f)) return;
            IJ.showStatus("Frame "+f+" Aligned");
        }
```

### Multi-threaded transformation
```java
        List<FrameTransformer> transformers = new ArrayList<>();
        for(int f=1;f<=frames;f++){
            transformers.add(new FrameTransformer(imp, width, height,transformation,
                    transforms[f-1], anchorPoints, f));

        }

        transformers.parallelStream()
                .forEach(s -> s.transformFrame());
```
### Thread transform class
```java
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
```