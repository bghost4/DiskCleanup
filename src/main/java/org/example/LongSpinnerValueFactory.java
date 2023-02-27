package org.example;

import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

public class LongSpinnerValueFactory extends SpinnerValueFactory<Long>  {

    public LongSpinnerValueFactory() {
        super();

        setConverter(new StringConverter<Long>() {
            @Override
            public String toString(Long object) {
                return String.format("%d",object);
            }

            @Override
            public Long fromString(String string) {
                return Long.parseLong(string);
            }
        });

    }

    @Override
    public void decrement(int steps) {
        valueProperty().set(valueProperty().getValue()-steps);
    }

    @Override
    public void increment(int steps) {
        valueProperty().set(valueProperty().getValue()+steps);
    }

}
