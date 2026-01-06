<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='utf-8' />

    <script src='${request.contextPath}/plugin/${className}/node_modules/fullcalendar/index.global.min.js'></script>
    <script>
        var kecakCalendar;
        function popupForm(elementId, appId, appVersion, jsonForm, nonce, args, data, height, width) {
            let formUrl = '${request.contextPath}/web/app/' + appId + '/' + appVersion + '/form/embed?_submitButtonLabel=<#if editable>Submit<#else>Back</#if>';
            let frameId = args.frameId = 'Frame_' + elementId;

            if (data && data.id) {
                if (formUrl.indexOf("?") != -1) {
                    formUrl += "&";
                } else {
                    formUrl += "?";
                }
                formUrl += "id=" + data.id;
            }
            formUrl += UI.userviewThemeParams();

            var params = {
                _json : JSON.stringify(jsonForm ? jsonForm : {}),
                _callback : 'onSubmitted',
                _setting : JSON.stringify(args ? args : {}).replace(/"/g, "'"),
                _jsonrow : JSON.stringify(data ? data : {}),
                _nonce : nonce
            };

            JPopup.show(frameId, formUrl, params, "", width, height);
        }

        function onSubmitted(args) {
            debugger;
            let result = JSON.parse(args.result);
            let frameId = args.frameId;
            JPopup.hide(frameId);
            //refresh
            kecakCalendar.refetchEvents();
        }

        document.addEventListener('DOMContentLoaded', function() {
            var calendarEl = document.getElementById('calendar');
            var jsonForm = $('input#jsonForm').val() ? JSON.parse($('input#jsonForm').val()) : {};
            var nonce = '${nonce!}';
            kecakCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'dayGridMonth',
                selectable: true,
                customButtons: {
                    <#if editable>
                        addButton: {
                            text: 'Add Event',
                            click: function() {
                                popupForm('${formDefId}', '${appId}', '${appVersion}', jsonForm, nonce, {}, {}, 800, 900);
                            }
                        },
                    </#if>
                    exportButtons: {
                        text: 'Export ICS',
                        click: function() {
                            window.location='${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&actions=ical';
                        }
                    },
                    timelineButton: {
                        text: 'Gantt',
                        click: function() {
                            window.location='${request.contextPath}/web/userview/${appId}/${userviewId}/_/${customId!menuId}?view=timeline';
                        }
                    }
                },
                headerToolbar: {
                    left: 'prev,next<#if editable> addButton</#if><#if exportable> exportButtons</#if><#if timelineView> timelineButton</#if>', //prev next  and add event add button
                    center: 'title',
                    right: 'timeGridDay,dayGridWeek,dayGridMonth,multiMonthYear,listMonth' // user can switch calendar between day, week, month, and year
                },
                editable: ${editable?string},
                dayMaxEvents: true, // allow "more" link when too many events
                navLinks: true,
                events: function(fetchInfo, successCallback, failureCallback){
                    let url = '${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&action=event';
                    let menuObj = JSON.stringify(${menuObj!});

                    $.ajax({
                        type: "POST",
                        url: url,
                        data: menuObj,
                        success: (data, status) => successCallback(data),
                        contentType: "application/json; charset=utf-8",
                        dataType: 'json'
                    });
                }
                <#if jsonForm?? >
                    ,eventClick: function(info) {
                        let id = info.event.id;
                        popupForm('${formDefId}', '${appId}', '${appVersion}', jsonForm, nonce, {}, {id: id}, 800, 900);
                    }
                </#if>
            });
            kecakCalendar.render();

        });

    </script>
</head>
<body>
    <input type='hidden' id='jsonForm' value="${jsonForm!}" >
    <div id='calendar'></div>
</body>
</html>
