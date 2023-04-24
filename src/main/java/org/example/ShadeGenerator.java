package org.example;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class ShadeGenerator extends Task<Path> {

    private final Function<TreeItem<StatItem>,Rectangle> lookupFunction;
    private final Stream<TreeItem<StatItem>> items;
    private final double width, height;

    public ShadeGenerator(Function<TreeItem<StatItem>, Rectangle> lookupFunction, Stream<TreeItem<StatItem>> items, double width, double height) {
        this.lookupFunction = lookupFunction;
        this.items = items;
        this.width = width;
        this.height = height;
        updateTitle("Updating Shade Overlay");
    }


    Stream<PathElement> createRemoveRect(Rectangle r) {
        return createRect(r.getX(),r.getY(),r.getWidth(),r.getHeight(),true);
    }

    Stream<PathElement> createRect(double x, double y, double w, double h, boolean remove) {
        if(remove) {
            return Stream.of(
                    new MoveTo(x+w, y+h),
                    new VLineTo(y),
                    new HLineTo(x),
                    new VLineTo(y+h),
                    new HLineTo(x+w) );
        } else {
            return Stream.of(
                    new MoveTo(x, y),
                    new HLineTo(w),
                    new VLineTo(h),
                    new HLineTo(x),
                    new VLineTo(y));
        }
    }

    @Override
    protected Path call() {
        Path p = new Path();
        //Shader Setup
        p.setFill(Color.WHITE);
        p.setStroke(null);
        p.setOpacity(0.75);
        p.setFillRule(FillRule.EVEN_ODD);
        p.setMouseTransparent(true);

        p.getElements().addAll(createRect(0,0,width,height,false).toList());

        p.getElements().addAll(
                items.flatMap(ti -> Optional.ofNullable(lookupFunction.apply(ti)).stream()).flatMap(this::createRemoveRect).toList()
        );

        return p;
    }
}
