package com.kinnarastudio.calendar.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Event;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.joget.commons.util.LogUtil;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleCalendarService {
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static Calendar build(String serviceAccountJson) throws Exception {
        GoogleCredentials credentials =
                ServiceAccountCredentials.fromStream(
                        new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
                ).createScoped(
                        Collections.singleton("https://www.googleapis.com/auth/calendar")
                );

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName("Kecak Google Calendar Sync")
                .build();
    }

    public static List<Event> listGoogleEvents(
            Calendar calendar,
            String calendarId,
            String startDate,
            String endDate,
            int maxEvents
    ) throws Exception {
        List<Event> allEvents = new ArrayList<>();
        // ============================
        //      HOLIDAY CALENDAR
        // ============================

        String holidayToken = null;

        do {
            Events holidayEvents = calendar.events()
                    .list(calendarId)
                    .setMaxResults(100)
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .setTimeMin(new com.google.api.client.util.DateTime(startDate))
                    .setTimeMax(new com.google.api.client.util.DateTime(endDate))
                    .setPageToken(holidayToken)
                    .execute();

            if (holidayEvents.getItems() != null) {
                allEvents.addAll(holidayEvents.getItems());
            }

            holidayToken = holidayEvents.getNextPageToken();

        } while (holidayToken != null && allEvents.size() < maxEvents);

        // ============================
        // BATAS maxEvents & SORT
        // ============================

        // Sort by start time
        allEvents.sort((a, b) -> {
            long t1 = (a.getStart().getDateTime() != null)
                    ? a.getStart().getDateTime().getValue()
                    : a.getStart().getDate().getValue();

            long t2 = (b.getStart().getDateTime() != null)
                    ? b.getStart().getDateTime().getValue()
                    : b.getStart().getDate().getValue();

            return Long.compare(t1, t2);
        });

        // Jika maxEvents lebih kecil, lakukan trim
        if (allEvents.size() > maxEvents) {
            return allEvents.subList(0, maxEvents);
        }

        return allEvents;
    }

    public static String removeTimezone(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // Hilangkan timezone format: +07:00 atau -05:00
        return input.replaceAll("(\\+|-)\\d{2}:\\d{2}$", "");
    }
}
