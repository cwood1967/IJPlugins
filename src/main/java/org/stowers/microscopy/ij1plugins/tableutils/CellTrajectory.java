package org.stowers.microscopy.ij1plugins.tableutils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cjw on 10/27/16.
 */
public class CellTrajectory {

    int id;
    int startFrame;
    List<Integer> frame;
    List<Integer> x;
    List<Integer> y;
    List<Float> intensity;
    int motherId;

    int currentFrame;

    public CellTrajectory(int id, int startFrame, int motherId) {
        this.id = id;
        this.startFrame = startFrame;
        this.motherId = motherId;
    }

    public int getId() {
        return id;
    }

    public int getX(int f) {

        int res;
        int index = frame.indexOf(f);
        if ((index >= frame.size()) || (index == -1)) {
            res = getX(currentFrame);
        } else {
            res = x.get(index);
            currentFrame = f;
        }
        System.out.println(currentFrame);
        return res;
    }

    public int getY(int f) {
        int res;

        int index = frame.indexOf(f);
        if ( (index >= frame.size()) ||(index == -1)) {
            res = getY(currentFrame);
        } else {
            res = y.get(index);
            currentFrame = f;
        }
        System.out.println(currentFrame);
        return res;
    }

    public List<Integer> getX() {
        return x;
    }

    public List<Integer> getY() {
        return y;
    }

    public List<Float> getIntensity() {
        return intensity;
    }

    public List<Integer> getFrames() {
        return frame;
    }

    public void setX(ArrayList<Integer> xList) {
        this.x = xList;
    }

    public void setY(ArrayList<Integer> yList) {
        this.y = yList;
    }

    public void setIntensity(ArrayList<Float> intensity) {
        this.intensity = intensity;
    }

    public void setFrame(ArrayList<Integer> frameList) {
        this.frame = frameList;
    }

    public float distanceFrom(int ax, int ay, int t) {

        float d = 0;
        if (frame.contains(t)) {
            int f = frame.indexOf(t);
            int dx = ax - x.get(f);
            int dy = ay - y.get(f);
            d = (float) Math.sqrt(dx * dx + dy * dy);
        } else {
            d = Float.MAX_VALUE;
        }

        return d;
    }

}
