package com.kinnarastudio.calendar.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Event;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public static JSONArray getGoogleEvents(String startDate, String endDate, String calendarId, String serviceAccountJson) throws Exception {
        JSONArray arr = new JSONArray();
        // Init Google Calendar Service
        Calendar calendar = GoogleCalendarService.build(serviceAccountJson);

        // Load events (paging)
        List<Event> events = listGoogleEvents(
                calendar,
                calendarId,
                startDate,
                endDate,
                300);

        try {
            for (Event e : events) {
                JSONObject o = new JSONObject();
                String title = e.getSummary() != null ? e.getSummary() : "(No Title)";
                o.put("id", e.getId());
                o.put("title", title);

                if (e.getStart().getDateTime() != null)
                    o.put("start", GoogleCalendarService.removeTimezone(e.getStart().getDateTime().toString()));
                else
                    o.put("start", e.getStart().getDate().toString());

                if (e.getEnd().getDateTime() != null)
                    o.put("end", GoogleCalendarService.removeTimezone(e.getEnd().getDateTime().toString()));
                else
                    o.put("end", e.getEnd().getDate().toString());

                final String digest = StringUtil.md5(e.getId());
                final String color = digest.substring(0, 6);
                o.put("color", "#" + color);

                o.put("isPublicCalendar", true);
                if (StringUtils.isAllBlank(e.getDescription())){
                    o.put("description", "No Description");
                }else {
                    o.put("description", e.getDescription());
                }
                arr.put(o);
            }

        } catch (Exception ex) {
            LogUtil.error("GoogleCalendarService", ex, ex.getMessage());
        }
        return arr;
    }

    private static List<Event> listGoogleEvents(
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

            LogUtil.info("Holiday Event List: ", holidayEvents.getItems().toString());
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

    private static String removeTimezone(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // Hilangkan timezone format: +07:00 atau -05:00
        return input.replaceAll("(\\+|-)\\d{2}:\\d{2}$", "");
    }
}
