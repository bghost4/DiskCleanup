package org.example;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Rectangle;
import org.w3c.dom.css.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RectangleUpdateService extends Service<Void>{

        private List<Pair<TreeItem<StatItem>,Bound>> packingOrder = Collections.emptyList();
        private int segmentSize = 100;
        private Function<TreeItem<StatItem>, Optional<Rectangle>> lookupFunction = (a) -> {
            System.out.println("No Function Set To get Rectangle");
            return Optional.empty();
        };


        public void setLookupFunction(Function<TreeItem<StatItem>,Optional<Rectangle>> newFunction) {
            lookupFunction = newFunction;
        }

        public void setPackingOrder(List<Pair<TreeItem<StatItem>,Bound>> newlist) {
            if(isRunning()) {
                cancel();
            }
            this.packingOrder = newlist;
            restart();
        }

        public void setSegmentSize(int newSize) {
            this.segmentSize = newSize;
            if(isRunning()) { cancel(); }
        }



        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    System.out.println("Rectangle Updater Started");
                    if(packingOrder != null && packingOrder.size() > 0) {
                        //process the list in 100 item chunks
                        //todo set the number of items to break into a property
                        int i = 0;
                        while( i < packingOrder.size() ) {
                            List<Pair<Rectangle,Bound>> items = new ArrayList<>(segmentSize);
                            if(Thread.interrupted()) { return null; }
                            for(int segment = 0; segment < Math.min(segmentSize,(packingOrder.size()-i)); segment++) {
                                if(Thread.interrupted()) { return null; }
                                Pair<TreeItem<StatItem>,Bound> item = packingOrder.get(i);
                                lookupFunction.apply(item.a()).ifPresent(rect -> items.add(new Pair<>(rect,item.b())));
                                i++;
                                if( i > packingOrder.size()) {
                                   segment = segmentSize;
                                }
                            }

                            Thread.sleep(100); //give the poor application thread some room to breathe
                            final List<Pair<Rectangle,Bound>> cp = new ArrayList<>(items); //shallow copy
                            Platform.runLater(() -> {
                                System.out.println("updated "+cp.size()+" Rectangles");
                                cp.stream().forEach(p -> {
                                    p.a().setX(p.b().x());
                                    p.a().setY(p.b().y());
                                    p.a().setWidth(p.b().width());
                                    p.a().setHeight(p.b().height());
                                });
                            });
                        }
                    } else {
                        System.err.println("Packing List was null or empty");
                    }

                    System.out.println("Finished updating Rectangles");

                    return null;
                }
            };

        }

}
