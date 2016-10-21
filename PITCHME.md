## ImageJ Plugins

* ClickForRoi
* ColumnPlotter
* TiffSaverPlugin
* Threshold (adaptive)
* ImageHistogram
#HSLIDE


## ClickForRoi

Set the size for a rectangular ROI then click the image to place ROIs.
ROIs will be added to the ROI Manager.

```java
//java code block

 public void run() {
        window = imp.getWindow();
        canvas = window.getCanvas();
        canvas.addMouseListener(this);
        manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
        manager.setVisible(true);
    }
    private void drawRoi(double x, double y) {
        int x0 = (int)x - roiSize/2;
        int y0 = (int)y - roiSize/2;
        Roi roi = new Roi(x0, y0, roiSize, roiSize);
        manager.add(imp, roi, 1);
        manager.runCommand(imp, "Show All");
    }
    
 ```

#VSLIDE?gist=1b8670bf4da011b736a26b72837c5f43   