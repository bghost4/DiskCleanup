package org.example;

import javafx.scene.control.SpinnerValueFactory;

public class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
    @Override
    public void decrement(int steps) {
        valueProperty().set(valueProperty().getValue()-steps);
    }

    @Override
    public void increment(int steps) {
        valueProperty().set(valueProperty().getValue()+steps);
    }

}
