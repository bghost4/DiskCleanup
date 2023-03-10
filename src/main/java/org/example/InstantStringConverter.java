package org.example;

import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class InstantStringConverter extends StringConverter<Instant> {

    public final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String toString(Instant object) {
        return OffsetDateTime.ofInstant(object,ZoneId.systemDefault()).format(dtf);
    }

    @Override
    public Instant fromString(String string) {
        return LocalDateTime.parse(string,dtf).atZone(ZoneId.systemDefault()).toInstant();
    }
}
