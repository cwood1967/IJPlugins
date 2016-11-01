package org.stowers.microscopy.ij1plugins.tableutils;

import ij.text.TextPanel;
import javafx.scene.control.Cell;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

/**
 * Created by cjw on 10/27/16.
 */


/*
id	start	frame	x	y	Avg	mother_id
0	0	0	411	359	101294.234	NaN
 */

public class MotherIdTable {

    TextPanel panel;

    String[] headers;

    int[] cellid[];
    int[] start;
    List<CellTrajectory> cells;

    public MotherIdTable(TextPanel panel) {
        this.panel = panel;
        readHeaders();
        readTable();
    }

    public CellTrajectory getTrajFromPoint(int x, int y, int frame) {

        float dmin = Float.MAX_VALUE - 1.f;
        CellTrajectory ctmin = null;
        for (CellTrajectory ct : cells) {
            float d = ct.distanceFrom(x, y, frame);

            if (d < dmin) {
                dmin = d;
                ctmin = ct;
            }
        }
        System.out.println(dmin);
        if (dmin > 7 || ctmin == null) {
            ctmin = new CellTrajectory(-1, -1, -1);
        }
        return ctmin;
    }

    public CellTrajectory getTrajFromId(int id) {

        CellTrajectory ct = null;

        for (CellTrajectory tempct : cells)  {
            if (tempct.getId() == id) {
                ct = tempct;
                break;
            }
        }

        return ct;
    }

    protected void readHeaders() {

        String tabHeadings = panel.getColumnHeadings();
        headers = tabHeadings.split("\t");

    }

    protected void readTable() {

        int numLines = panel.getLineCount();

        cells = new ArrayList<>();
        ArrayList<Integer> frameList = new ArrayList<>();
        ArrayList<Integer> xList = new ArrayList<>();
        ArrayList<Integer> yList = new ArrayList<>();
        ArrayList<Float> intensityList = new ArrayList<>();

        int id = -3233323;

        CellTrajectory ct = null;
        int startFrame;
        for (int i = 0; i < numLines; i++) {
            String line = panel.getLine(i);
//            line = line.replace(" ", "\t");
            String[] tokens = line.split("\\s+");

            int qid = Integer.parseInt(tokens[1]);
            startFrame = Integer.parseInt(tokens[2]);

            if (qid != id) {
                int motherId;
                if (tokens[7].equals("NaN")) {
                    motherId = -(id + 1);
                } else {
                    motherId = Integer.parseInt(tokens[7]);
                }
                id = qid;
                ct = new CellTrajectory(id, startFrame, motherId);
                cells.add(ct);
                frameList = new ArrayList<>();
                xList = new ArrayList<>();
                yList = new ArrayList<>();
                intensityList = new ArrayList<>();
                ct.setFrame(frameList);
                ct.setX(xList);
                ct.setY(yList);
                ct.setIntensity(intensityList);

            }

            int frame = Integer.parseInt(tokens[3]);
            int x = Integer.parseInt(tokens[4]);
            int y = Integer.parseInt(tokens[5]);
            float g = Float.parseFloat(tokens[6]);

            frameList.add(frame + startFrame);
            xList.add(x);
            yList.add(y);
            intensityList.add(g);

        }
    }
}
