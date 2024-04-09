<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='utf-8' />

    <script src='${request.contextPath}/plugin/${className}/node_modules/fullcalendar/index.global.min.js'></script>
    <script>
        var kecakCalendar;
        function popupForm(elementId, appId, appVersion, jsonForm, nonce, args, data, height, width) {
            let formUrl = '${request.contextPath}/web/app/' + appId + '/' + appVersion + '/form/embed?_submitButtonLabel=Submit';
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
            var jsonForm = JSON.parse($('input#jsonForm').val());
            var nonce = '${nonce}';
            kecakCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'dayGridWeek',
                selectable: true,
                customButtons: {
                    addButton: {
                        text: 'Add Event',
                        click: function() {
                            //alert('clicked the add button!');
                            popupForm('${formDefId}', '${appId}', '${appVersion}', jsonForm, nonce, {}, {}, 800, 900);
                        }
                    }
                },
                headerToolbar: {
                    left: 'prev,next addButton', //prev next  and add event add button
                    center: 'title',
                    right: 'timeGridDay,dayGridWeek,dayGridMonth,multiMonthYear,listMonth' // user can switch calendar between day, week, month, and year
                },
                editable: true,
                dayMaxEvents: true, // allow "more" link when too many events
                navLinks: true,
                events: function(fetchInfo, successCallback, failureCallback){
                    $.get('${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}', function(data, status){
                        successCallback(
                            data
                        );
                    });
                },
                eventClick: function(info) {
                    debugger;
                    let id = info.event.id;
                    popupForm('${formDefId}', '${appId}', '${appVersion}', jsonForm, nonce, {}, {id: id}, 800, 900);
                }
            });
            kecakCalendar.render();

        });

    </script>
</head>
<body>
    <input type='hidden' id='jsonForm' value="${jsonForm}" >
    <div id='calendar'></div>
</body>
</html>
