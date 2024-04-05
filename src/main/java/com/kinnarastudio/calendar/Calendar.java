package com.kinnarastudio.calendar;

import com.kinnarastudio.commons.Try;
import org.apache.commons.lang3.StringEscapeUtils;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Calendar extends UserviewMenu{

    @Override
    public String getCategory() {
        return "Test";
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public String getRenderPage() {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        PluginManager pluginManager = (PluginManager) appContext.getBean("pluginManager");
        final Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("className", getClassName());
        final String dtId = getPropertyString("dataListId");
        final DataList dataList = getDataList(dtId);
        final DataListCollection rows = dataList.getRows();
        final JSONArray events = generateEvents(rows);
        dataModel.put("events", events.toString());

        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        dataModel.put("appId", appDefinition.getAppId());
        dataModel.put("appVersion", appDefinition.getVersion());

        final String formDefId = getPropertyString("formId");
        dataModel.put("formDefId", formDefId);

        final JSONObject jsonForm = getJsonForm(formDefId);
        dataModel.put("jsonForm", StringEscapeUtils.escapeHtml4(jsonForm.toString()));

        final String nonce = generateNonce(appDefinition, jsonForm.toString());
        dataModel.put("nonce", nonce);

        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClass().getName(), "/Templates/homepage.ftl", null);
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
        return "Calendar";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "Calendar";
    }

    @Override
    public String getClassName() {
        return Calendar.class.getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/Properties/calendar.json");
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

    protected JSONArray generateEvents(DataListCollection dataListCollection){
        JSONArray events = new JSONArray();
        for (Object rows : dataListCollection) {
            Map<String,Object> map = (Map<String, Object>) rows;
            JSONObject event = new JSONObject();

            for(Map<String, String> propmapping : getPropertyGrid("dataListMapping")){

                try{
                    String field = propmapping.get("field");
                    String prop = propmapping.get("prop");
                    String value = (String) map.get(field);
                    DateFormat dateValue = new SimpleDateFormat (getPropertyString("dateFormat"));
                    DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
                    if ((prop.equals("start") || prop.equals("end")) && value != null){

                        try{
                            Date dtListDate = dateValue.parse(value);//tanggal yang diambil dari data list
                            //mengubah value dg tipe data String ke tipe data Date

                            String finalDate = dateTime.format(dtListDate);//memasukan hasil parse dari dtListDate;
                            event.put(prop,finalDate);
                        }
                        catch (ParseException e){
                            LogUtil.error(getClassName(), e, e.getLocalizedMessage());
                        }
                    }
                    else if (value != null){
                        event.put(prop,value);
                    }
                }
                catch (JSONException tes){
                    LogUtil.error(getClassName(), tes, tes.getMessage());
                }
            }
            events.put(event);
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
}
