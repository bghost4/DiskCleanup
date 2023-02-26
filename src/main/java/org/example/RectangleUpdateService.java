package org.example;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RectangleUpdateService extends Service<Void>{

        private List<Pair<TreeItem<StatItem>,Bound>> packingOrder = Collections.emptyList();
        private int segmentSize = 1000;
        private int snoozeTime = 100;

        private final SimpleObjectProperty<Function<TreeItem<StatItem>, Paint>> colorPicker = new SimpleObjectProperty<>((i -> Color.WHITE));

        public ObjectProperty<Function<TreeItem<StatItem>,Paint>> colorPickerProperty() { return colorPicker; }

        private Function<TreeItem<StatItem>, Optional<Rectangle>> lookupFunction = (a) -> {
            System.out.println("No Function Set To get Rectangle");
            return Optional.empty();
        };

        //Variables to attempt to optimize snoozeTime and segmentSize
        private long lastRuntime = 0;

        private final int minSegments = 10;
        private final int minSnooze = 5;

        public void reportLastUpdateTime(Long millis) {
            long updateTime = millis;
            if(updateTime > snoozeTime) {
                //we are probably hanging up the application thread
                System.out.println("Stuttering: update("+ updateTime +") snooze("+snoozeTime+")");
                segmentSize = Math.max(segmentSize - (segmentSize/4),minSegments); //decrease segmentsize by 25%
                snoozeTime = Math.max(minSnooze,snoozeTime + (snoozeTime/4)); //increase snooze time by 25%
            }
        }

        private void updateLastRuntime(long currentTimeMillis) {
            long prevLastRunTime = lastRuntime;
            lastRuntime = currentTimeMillis;
        }


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
                                lookupFunction.apply(item.a()).ifPresent(rect -> {
                                    rect.setFill(colorPicker.get().apply(item.a()));
                                    items.add(new Pair<>(rect,item.b()));
                                });
                                i++;
                                if( i > packingOrder.size()) {
                                   segment = segmentSize;
                                }
                            }

                            Thread.sleep(snoozeTime); //give the poor application thread some room to breathe
                            final List<Pair<Rectangle,Bound>> cp = new ArrayList<>(items); //shallow copy
                            Platform.runLater(() -> {
                                updateLastRuntime(System.currentTimeMillis());
                                cp.forEach(p -> {
                                    p.a().setX(p.b().x());
                                    p.a().setY(p.b().y());
                                    p.a().setWidth(p.b().width());
                                    p.a().setHeight(p.b().height());
                                });
                                reportLastUpdateTime(System.currentTimeMillis() - lastRuntime);
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
