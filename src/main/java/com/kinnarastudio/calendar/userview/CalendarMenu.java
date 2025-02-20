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
import org.joget.apps.datalist.model.DataListBinder;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.model.UserviewPermission;
import org.joget.apps.userview.service.UserviewService;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();

        final boolean isTimelineView = optParameter(request, "view")
                .map("timeline"::equalsIgnoreCase)
                .orElse(false);


        final String template;
        if (isTimelineView) {
            template = "/templates/CalendarTimelineMenu.ftl";
        } else {
            template = "/templates/CalendarMenu.ftl";
        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");

        final Map<String, Object> dataModel = new HashMap<>();

        final boolean hasPermissionToEdit = optPermission()
                .map(UserviewPermission::isAuthorize)
                .orElse(false);

        dataModel.put("className", getClassName());

        final String dtId = getPropertyString("dataListId");
        dataModel.put("dataListId", dtId);

        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        dataModel.put("appId", appDefinition.getAppId());
        dataModel.put("appVersion", appDefinition.getVersion());

        final String formDefId = getPropertyString("formId");
        dataModel.put("formDefId", formDefId);

        if (formDefId.isEmpty()) {
            dataModel.put("editable", false);
        } else {
            final JSONObject jsonForm = getJsonForm(formDefId, !hasPermissionToEdit);
            dataModel.put("jsonForm", StringEscapeUtils.escapeHtml4(jsonForm.toString()));

            final String nonce = generateNonce(appDefinition, jsonForm.toString());
            dataModel.put("nonce", nonce);

            dataModel.put("editable", hasPermissionToEdit);
        }

        final String userviewId = getUserview().getPropertyString("id");
        dataModel.put("userviewId", userviewId);

        final String userMenuId = getUserview().getCurrent().getPropertyString("id");
        dataModel.put("menuId", userMenuId);

        final String customId = getUserview().getCurrent().getPropertyString("customId");
        if(customId != null && !customId.isEmpty()) {
            dataModel.put("customId", customId);
        }

        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClass().getName(), template, null);
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

    protected JSONObject getJsonForm(String formDefId, boolean readonly) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) appContext.getBean("formDefinitionDao");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        return Optional.of(formDefId)
                .map(s -> formDefinitionDao.loadById(s, appDef))
                .map(FormDefinition::getJson)
                .map(formService::createElementFromJson)
                .map(Try.toPeek(e -> FormUtil.setReadOnlyProperty(e, readonly, readonly)))
                .map(formService::generateElementJson)
                .map(Try.onFunction(JSONObject::new))
                .orElseGet(JSONObject::new);
    }

    protected JSONArray generateEvents(DataListCollection<Map<String, Object>> dataListCollection, UserviewMenu userviewMenu) {
        JSONArray events = new JSONArray();
        for (Map<String, Object> map : dataListCollection) {
            try {
                JSONObject event = new JSONObject();

                for (Map<String, String> propmapping : userviewMenu.getPropertyGrid("dataListMapping")) {
                    String field = propmapping.get("field");
                    String prop = propmapping.get("prop");
                    String value = String.valueOf(map.get(field));
                    final DateFormat dateValue = new SimpleDateFormat(userviewMenu.getPropertyString("dateFormat"));
                    final DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
                    if ((prop.equals("start") || prop.equals("end")) && value != null) {
                        try {
                            final Date dtListDate = dateValue.parse(value);//tanggal yang diambil dari data list
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

                final boolean randomColor = randomColorByTitle();
                if (randomColor) {
                    final String title = event.getString("title");
                    final String digest = StringUtil.md5(title);
                    final String color = digest.substring(0, 6);

                    event.put("color", "#" + color);
                }
                events.put(event);
            } catch (JSONException tes) {
                LogUtil.error(getClassName(), tes, tes.getMessage());
            }
        }
        return events;
    }

    protected JSONArray getTimelineData(DataListCollection<Map<String, Object>> dataListCollection, UserviewMenu userviewMenu, int page) {
        final Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.DATE, page);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        final Date early = calendar.getTime();

        calendar.set(Calendar.HOUR, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        final Date late = calendar.getTime();

        final DateFormat dateValue = new SimpleDateFormat(userviewMenu.getPropertyString("dateFormat"));
        final DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
        final Map<String, String>[] dataListMapping = userviewMenu.getPropertyGrid("dataListMapping");

        return new JSONArray() {{
            for (Map<String, Object> map : dataListCollection) {
                try {
                    final JSONObject jsonRow = new JSONObject() {{
                        for (Map<String, String> propmapping : dataListMapping) {
                            final String field = propmapping.get("field");
                            final String prop = propmapping.get("prop");
                            final String value = String.valueOf(map.get(field));

                            if (value == null) continue;

                            switch (prop) {
                                case "start":
                                case "end":
                                    try {
                                        final Date dtListDate = dateValue.parse(value);
                                        if (early.before(dtListDate) && dtListDate.before(late)) {
                                            final String finalDate = dateTime.format(dtListDate);
                                            put(prop, finalDate);
                                        }

                                    } catch (ParseException e) {
                                        LogUtil.error(getClassName(), e, e.getLocalizedMessage());
                                    }
                                    break;
                                case "title":
                                    put("row", value);
                                    break;
                                case "id":
                                    put("recordID", value);
                                    break;
                                default:
                                    put(prop, value);
                                    break;
                            }
                        }
                    }};

                    if (jsonRow.has("start") && jsonRow.has("end")) {
                        put(jsonRow);
                    }
                } catch (JSONException e) {
                    LogUtil.error(getClassName(), e, e.getMessage());
                }
            }
        }};
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

        if (userviewMenu == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        DataList dataList = getDataList(dataListId);
        DataListCollection<Map<String, Object>> rows = Optional.ofNullable((DataListCollection<Map<String, Object>>) dataList.getRows())
                .stream()
                .flatMap(Collection::stream)

                // reformat content value
                .map(row -> formatRow(dataList, row))
                .collect(Collectors.toCollection(DataListCollection::new));


        final String action = getParameter(request, "action");
        if ("event".equals(action)) {
            final JSONArray events = generateEvents(rows, userviewMenu);
            response.getWriter().write(events.toString());
        } else if ("timeline".equals(action)) {
//                final JSONArray events = new JSONArray(AppUtil.readPluginResource(getClassName(), "/resources/mock-data.json"));
            final int page = optParameter(request, "page")
                    .map(Try.onFunction(Integer::parseInt, (NumberFormatException ignored) -> 0))
                    .orElse(0);
            final JSONArray events = getTimelineData(rows, userviewMenu, page);

            response.getWriter().write(events.toString());
        } else if ("ical".equals(action)) {
            net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
            calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
            calendar.getProperties().add(net.fortuna.ical4j.model.property.Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);

            final Collection<VEvent> events = generateVEvents(rows, userviewMenu);
            calendar.getComponents().addAll(events);

            final CalendarOutputter outputter = new CalendarOutputter();
            response.addHeader("Content-Disposition", "attachment; filename=calendar.ics");

            final ServletOutputStream outputStream = response.getOutputStream();
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

    protected Collection<VEvent> generateVEvents(DataListCollection<Map<String, Object>> dataListCollection, UserviewMenu userviewMenu) {
        final Collection<VEvent> vEvents = new ArrayList<>();
        for (final Map<String, Object> map : dataListCollection) {
            try {
                final DateFormat dateTimeVevent = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
                final DateFormat dateValue = new SimpleDateFormat(userviewMenu.getPropertyString("dateFormat"));

                final VEvent event = new VEvent();
                for (Map<String, String> propmapping : userviewMenu.getPropertyGrid("dataListMapping")) {

                    final String field = propmapping.get("field");
                    final String prop = propmapping.get("prop");
                    final String value = String.valueOf(map.get(field));
                    if ("null".equalsIgnoreCase(value) || value.isEmpty()) {
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

    protected boolean randomColorByTitle() {
        return true;
    }

    protected Optional<UserviewPermission> optPermission() {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final PluginManager pluginManager = (PluginManager) applicationContext.getBean("pluginManager");
        return Optional.of("permission")
                .map(this::getProperty)
                .map(o -> (Map<String, Object>) o)
                .map(pluginManager::getPlugin);
    }

    @Nonnull
    protected Map<String, Object> formatRow(@Nonnull DataList dataList, @Nonnull Map<String, Object> row) {
        return Optional.of(dataList)
                .map(DataList::getColumns)
                .stream()
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .map(DataListColumn::getName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(s -> s, s -> formatCell(dataList, row, s)));
    }

    /**
     * Format
     *
     * @param dataList DataList
     * @param row      Row
     * @param field    Field
     * @return
     */
    @Nonnull
    protected Object formatCell(@Nonnull final DataList dataList, @Nonnull final Map<String, Object> row, String field) {
        Object value = row.get(field);

        return Optional.of(dataList)
                .map(DataList::getColumns)
                .stream()
                .flatMap(Arrays::stream)
                .filter(c -> field.equals(c.getName()))
                .findFirst()
                .flatMap(column -> Optional.of(column)
                        .map(DataListColumn::getFormats)
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(Objects::nonNull)
                        .map(f -> (Object) f.format(dataList, column, row, value))
                        .filter(Objects::nonNull)
                        .findFirst()
                )
                .orElse(value);
    }

    /**
     * Get Primary Key
     *
     * @param dataList
     * @return
     */
    @Nonnull
    protected String getPrimaryKeyColumn(@Nonnull final DataList dataList) {
        return Optional.of(dataList)
                .map(DataList::getBinder)
                .map(DataListBinder::getPrimaryKeyColumnName)
                .orElse("id");
    }

    protected String getTimelineRenderPage(Map<String, Object> dataModel) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");

        final boolean hasPermissionToEdit = optPermission()
                .map(UserviewPermission::isAuthorize)
                .orElse(false);

        dataModel.put("className", getClassName());

        final String dtId = getPropertyString("dataListId");
        dataModel.put("dataListId", dtId);

        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        dataModel.put("appId", appDefinition.getAppId());
        dataModel.put("appVersion", appDefinition.getVersion());

        final String formDefId = getPropertyString("formId");
        dataModel.put("formDefId", formDefId);

        if (formDefId.isEmpty()) {
            dataModel.put("editable", false);
        } else {
            final JSONObject jsonForm = getJsonForm(formDefId, !hasPermissionToEdit);
            dataModel.put("jsonForm", StringEscapeUtils.escapeHtml4(jsonForm.toString()));

            final String nonce = generateNonce(appDefinition, jsonForm.toString());
            dataModel.put("nonce", nonce);

            dataModel.put("editable", hasPermissionToEdit);
        }

        final String userviewId = getUserview().getPropertyString("id");
        dataModel.put("userviewId", userviewId);

        final String userMenuId = getUserview().getCurrent().getPropertyString("id");
        dataModel.put("menuId", userMenuId);


        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClass().getName(), "/templates/CalendarTimelineMenu.ftl", null);
    }
}