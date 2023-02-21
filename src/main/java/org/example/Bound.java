package org.example;

import java.util.List;

public record Bound (double x, double y, double width, double height) {
    List<Bound> split(double percent) {
        boolean horizontal = width > height;
        double tgtArea = getArea()*percent;
        if(horizontal) {
            double x2 = tgtArea/height+x;
            Bound b1 = new Bound(x,y,x2-x,height);
            Bound b2 = new Bound(x2,y,width-(x2-x),height);
            return List.of(b1,b2);
        } else {
            double y2 = tgtArea/width+y;
            double height1 = y2-y;
            double height2 = height - height1;
            Bound b1 = new Bound(x,y,width,height1);
            Bound b2 = new Bound(x,y2,width,height2);
            return List.of(b1,b2);
        }
    }
    double getArea() { return width*height; }
}