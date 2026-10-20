package io.github.student.agile.utils;

import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@UtilityClass
public class DateUtils {

    public static ZonedDateTime reduceTimeFromZonedDateTime(ZonedDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault());
    }
}
