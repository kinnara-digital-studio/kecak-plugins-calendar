package com.kinnarastudio.calendar.datalist;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.kinnarastudio.calendar.service.GoogleCalendarService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Google Calendar Datalist Binder
 */
public class GoogleCalendarDatalistBinder extends DataListBinderDefault {
    public final static String LABEL = "Google Calendar DataList Binder";

    @Override
    public DataListColumn[] getColumns() {
        return new DataListColumn[]{
                new DataListColumn("id", "ID", true),
                new DataListColumn("title", "Title", true),
                new DataListColumn("start", "Start", true),
                new DataListColumn("end", "End", true),
                new DataListColumn("description", "Description", true),
                new DataListColumn("location", "Location", true)
        };
    }

    @Override
    public String getPrimaryKeyColumnName() {
        final String value = getPropertyString("primaryKeyColumn");
        return value.isEmpty() ? "id" : value;
    }

    @Override
    public DataListCollection getData(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects, String s, Boolean aBoolean, Integer integer, Integer integer1) {
        try {
            LogUtil.info("GoogleCalendarDatalistBinder", "CONNECT TO GOOGLE CALENDAR");
            /** ======== GET DATE RANGE FROM REQUEST ======== */
            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            String startParam = request.getParameter("start");
            String endParam = request.getParameter("end");

            String calendarId = getPropertyString("calendarId");
            String serviceAccountJson = getPropertyString("serviceAccountJson");

            /** ======== CONNECT TO GOOGLE CALENDAR ======== */
            Calendar calendar = GoogleCalendarService.build(serviceAccountJson);

            List<Event> events = GoogleCalendarService.listGoogleEvents(calendar, calendarId, startParam, endParam, 250);
            LogUtil.info("GoogleCalendarDatalistBinder - Event", String.valueOf(events.stream().count()));
            DataListCollection rows = new DataListCollection();

            if (events.stream().count()  > 0) {
                for (Event event : events) {
                    Map<String, Object> row = new HashMap<>();

                    row.put("id", event.getId());
                    row.put("title", event.getSummary());

                    if (event.getStart().getDateTime() != null)
                        row.put("start", GoogleCalendarService.removeTimezone(event.getStart().getDateTime().toString()));
                    else
                        row.put("start", event.getStart().getDate().toString());

                    if (event.getEnd().getDateTime() != null)
                        row.put("end", GoogleCalendarService.removeTimezone(event.getEnd().getDateTime().toString()));
                    else
                        row.put("end", event.getEnd().getDate().toString());

                    row.put("location", event.getLocation());
                    row.put("description", event.getDescription());

                    rows.add(row);
                }
            }

            return rows;
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
        }
        return new DataListCollection();
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map map, DataListFilterQueryObject[] dataListFilterQueryObjects) {
        try {
            /** ======== GET DATE RANGE FROM REQUEST ======== */
            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            String startParam = request.getParameter("start");
            String endParam = request.getParameter("end");

            String calendarId = getPropertyString("calendarId");
            String serviceAccountJson = getPropertyString("serviceAccountJson");

            /** ======== CONNECT TO GOOGLE CALENDAR ======== */
            Calendar calendar = GoogleCalendarService.build(serviceAccountJson);

            List<Event> events = GoogleCalendarService.listGoogleEvents(calendar, calendarId, startParam, endParam, 250);
            return events.size();
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
        }
        return 0;
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return GoogleCalendarDatalistBinder.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/GoogleCalendarDatalistBinder.json");
    }
}
