package com.kinnarastudio.calendar.userview;

import com.kinnarastudio.commons.Try;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.dao.UserviewDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.model.UserviewDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewService;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Calendar Menu
 */
public class CalendarMenu extends UserviewMenu implements PluginWebSupport {

    public final static String LABEL = "Calendar";

    @Override
    public String getCategory() {
        return "Kecak";
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-calendar\"></i>";
    }

    @Override
    public String getRenderPage() {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");
        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("className", getClassName());
        final String dtId = getPropertyString("dataListId");
        dataModel.put("dataListId", dtId);

        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        dataModel.put("appId", appDefinition.getAppId());
        dataModel.put("appVersion", appDefinition.getVersion());

        final String formDefId = getPropertyString("formId");
        dataModel.put("formDefId", formDefId);

        final JSONObject jsonForm = getJsonForm(formDefId);
        dataModel.put("jsonForm", StringEscapeUtils.escapeHtml4(jsonForm.toString()));

        final String nonce = generateNonce(appDefinition, jsonForm.toString());
        dataModel.put("nonce", nonce);

        final String userviewId = getUserview().getPropertyString("id");
        final String userMenuId = getUserview().getCurrent().getPropertyString("id");
        dataModel.put("userviewId", userviewId);
        dataModel.put("menuId", userMenuId);

        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClass().getName(), "/templates/CalendarMenu.ftl", null);
    }

    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDecoratedMenu() {
        return null;
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
        return CalendarMenu.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/CalendarMenu.json");
    }

    protected DataList getDataList(String dataListId) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) applicationContext
                .getBean("datalistDefinitionDao");
        DataListService dataListService = (DataListService) applicationContext.getBean("dataListService");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(dataListId, appDefinition);
        if (datalistDefinition == null) {
            LogUtil.warn(getClassName(), "DataList Definition [" + dataListId + "] not found");
            return null;
        }

        DataList dataList = dataListService.fromJson(datalistDefinition.getJson());
        if (dataList == null) {
            LogUtil.warn(getClassName(), "DataList [" + dataListId + "] not found");
            return null;
        }

        dataList.setPageSize(DataList.MAXIMUM_PAGE_SIZE);
        return dataList;
    }

    protected JSONObject getJsonForm(String formDefId) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) appContext.getBean("formDefinitionDao");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        return Optional.of(formDefId)
                .map(s -> formDefinitionDao.loadById(s, appDef))
                .map(FormDefinition::getJson)
                .map(Try.onFunction(JSONObject::new))
                .orElseGet(JSONObject::new);
    }

    protected JSONArray generateEvents(DataListCollection dataListCollection, UserviewMenu userviewMenu) {
        JSONArray events = new JSONArray();
        for (Object rows : dataListCollection) {
            Map<String, Object> map = (Map<String, Object>) rows;

            try {
                JSONObject event = new JSONObject();

                for (Map<String, String> propmapping : userviewMenu.getPropertyGrid("dataListMapping")) {
                    String field = propmapping.get("field");
                    String prop = propmapping.get("prop");
                    String value = (String) map.get(field);
                    DateFormat dateValue = new SimpleDateFormat(userviewMenu.getPropertyString("dateFormat"));
                    DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
                    if ((prop.equals("start") || prop.equals("end")) && value != null) {
                        try {
                            Date dtListDate = dateValue.parse(value);//tanggal yang diambil dari data list
                            //mengubah value dg tipe data String ke tipe data Date

                            String finalDate = dateTime.format(dtListDate);//memasukan hasil parse dari dtListDate;
                            event.put(prop, finalDate);
                        } catch (ParseException e) {
                            LogUtil.error(getClassName(), e, e.getLocalizedMessage());
                        }
                    } else if (value != null) {
                        event.put(prop, value);
                    }
                }
                events.put(event);
            } catch (JSONException tes) {
                LogUtil.error(getClassName(), tes, tes.getMessage());
            }
        }
        return events;
    }

    protected String generateNonce(AppDefinition appDefinition, String jsonForm) {
        return SecurityUtil.generateNonce(
                new String[]{"EmbedForm", appDefinition.getAppId(), appDefinition.getVersion().toString(), jsonForm},
                1);
    }

    protected String generateNonce(AppDefinition appDefinition, Form form) {
        final FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        final String jsonForm = formService.generateElementJson(form);
        return generateNonce(appDefinition, jsonForm);
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String dataListId = getParameter(request, "datalistId");
        String userviewId = getParameter(request, "userviewId");
        String menuId = getParameter(request, "menuId");
        Userview userview = getUserview(userviewId);
        UserviewMenu userviewMenu = getUserviewMenu(userview, menuId);
        DataList dataList = getDataList(dataListId);
        DataListCollection rows = dataList.getRows();

        String action = getParameter(request, "actions");
        if (action.equals("event")) {
            JSONArray events = generateEvents(rows, userviewMenu);
            response.getWriter().write(events.toString());
        } else if (action.equals("ical")) {
            net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
            calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
            calendar.getProperties().add(net.fortuna.ical4j.model.property.Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);

            Collection<VEvent> events = generateVEvents(rows, userviewMenu);
            calendar.getComponents().addAll(events);

            CalendarOutputter outputter = new CalendarOutputter();
            response.addHeader("Content-Disposition", "attachment; filename=calendar.ics");
            ServletOutputStream outputStream = response.getOutputStream();
            outputter.output(calendar, outputStream);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter action [" + action + "]");
        }
    }

    protected Optional<String> optParameter(HttpServletRequest request, String name) {
        return Optional.ofNullable(request.getParameter(name));
    }

    protected String getParameter(HttpServletRequest request, String name) throws ServletException {
        return optParameter(request, name)
                .orElseThrow(() -> new ServletException("Parameter [" + name + "] is required"));
    }

    protected Collection<VEvent> generateVEvents(DataListCollection dataListCollection, UserviewMenu userviewMenu) {
        Collection<VEvent> vEvents = new ArrayList<>();
        for (Object rows : dataListCollection) {
            Map<String, Object> map = (Map<String, Object>) rows;
            try {
                VEvent event = new VEvent();
                for (Map<String, String> propmapping : userviewMenu.getPropertyGrid("dataListMapping")) {

                    String field = propmapping.get("field");
                    String prop = propmapping.get("prop");
                    String value = (String) map.get(field);
                    DateFormat dateValue = new SimpleDateFormat(userviewMenu.getPropertyString("dateFormat"));
                    DateFormat dateTimeVevent = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
                    if (value == null) {
                        continue;
                    }
                    switch (prop) {
                        case "id":
                            event.getProperties().add(new Uid(value));
                            break;
                        case "start":
                            event.getProperties().add(new DtStart(dateTimeVevent.format(dateValue.parse(value))));
                            break;
                        case "end":
                            event.getProperties().add(new DtEnd(dateTimeVevent.format(dateValue.parse(value))));
                            break;
                        case "title":
                            event.getProperties().add(new Summary(value));
                            break;
                    }
                }
                vEvents.add(event);
            } catch (ParseException tes) {
                LogUtil.error(getClassName(), tes, tes.getMessage());
            }
        }
        return vEvents;
    }

    protected Userview getUserview(String userviewId) {
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        UserviewService userviewService = (UserviewService) applicationContext.getBean("userviewService");
        UserviewDefinitionDao userviewDefinitionDao = (UserviewDefinitionDao) applicationContext.getBean("userviewDefinitionDao");

        return Optional.of(userviewId)
                .map(s -> userviewDefinitionDao.loadById(s, appDefinition))
                .map(UserviewDefinition::getJson)
                .map(s -> AppUtil.processHashVariable(s, null, null, null))
                .map(s -> userviewService.createUserview(s, null, false, AppUtil.getRequestContextPath(), null, null, false))
                .orElse(null);
    }

    protected UserviewMenu getUserviewMenu(Userview userview, String userviewId) {
        return userview.getCategories().stream()
                .flatMap(c -> c.getMenus().stream())
                .filter(m -> !userviewId.isEmpty() && userviewId.equalsIgnoreCase(m.getPropertyString("id")))
                .findFirst()
                .orElse(null);
    }
}
