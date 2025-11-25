<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='utf-8' />

    <script src='${request.contextPath}/plugin/${className}/node_modules/fullcalendar/index.global.min.js'></script>
    <!-- Custom Popup Event -->
    <script src='${request.contextPath}/plugin/${className}/js/popup-event.js'></script>
    <link rel="stylesheet" href="${request.contextPath}/plugin/${className}/css/popup-event.css" type="text/css"/>
    <!-- Loading JS -->
    <script src='${request.contextPath}/plugin/${className}/js/loading-event.js'></script>
    <!-- Custom Fullcalendar CSS  -->
    <link rel="stylesheet" href="${request.contextPath}/plugin/${className}/css/custom-fullcalendar.css" type="text/css"/>
    <script>
        var kecakCalendar;
        function popupForm(elementId, appId, appVersion, jsonForm, nonce, args, data, height, width) {
            let formUrl = '${request.contextPath}/web/app/' + appId + '/' + appVersion + '/form/embed?_submitButtonLabel=<#if editable>Submit<#else>Back</#if>';
            let frameId = args.frameId = 'Frame_' + elementId;

            // ADD vs EDIT
            if (data && data.id) {
                formUrl += (formUrl.includes("?") ? "&" : "?") + "id=" + data.id;
            } else {
                formUrl += (formUrl.includes("?") ? "&" : "?") + "_mode=add";
            }

            formUrl += UI.userviewThemeParams();

            let params = {
                _json: JSON.stringify(jsonForm || {}),
                _callback: 'onSubmitted',
                _setting: JSON.stringify(args || {}).replace(/"/g, "'"),
                _jsonrow: JSON.stringify(data || {}),
                _nonce: nonce
            };

            JPopup.show(frameId, formUrl, params, "", width, height);
        }

        function onSubmitted(args) {
            let result = JSON.parse(args.result);
            let frameId = args.frameId;
            JPopup.hide(frameId);
            //refresh
            kecakCalendar.refetchEvents();
        }

        function deleteEvent(id){
            $.get(
                '${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&action=delete&formId=${formDefId}&id='+id,
                 {
                    datalistId: '${dataListId}',
                    userviewId: '${userviewId}',
                    menuId: '${menuId}',
                    action: 'delete',
                    formId: '${formDefId}',
                    id: id
                 },
                 function(data, status){
                    successCallback(
                        data
                    );
                 })
                 .always(function() {
                    kecakCalendar.refetchEvents();
                 });
        }

        function editEvent(id){
            const popupContainer = document.getElementById("popup-event-container");
            popupContainer.remove();

            var jsonForm = $('input#jsonForm').val() ? JSON.parse($('input#jsonForm').val()) : {};
            var nonce = '${nonce!}';
            popupForm('${formDefId}', '${appId}', '${appVersion}', jsonForm, nonce, {}, {id: id}, 370, 900);
        }

        function toggleMenuDropdown() {
            const existing = document.getElementById("calendar-menu-dropdown");
            if (existing) {
                existing.remove();
                return;
            }

            const btn = document.querySelector(".fc-myMenuButton-button");
            const rect = btn.getBoundingClientRect();

            const menu = document.createElement("div");
            menu.id = "calendar-menu-dropdown";
            menu.className = "calendar-menu-dropdown";
            menu.innerHTML = `
                <div class="menu-item" id="menu-export-ics">Export ICS</div>
                <div class="menu-item" id="menu-gantt">Gantt</div>
            `;

            document.body.appendChild(menu);

            menu.style.top = (rect.bottom + 5) + "px";
            menu.style.left = (rect.left - 20) + "px";

            // Close menu ketika klik luar
            document.addEventListener("click", function handler(e) {
                if (!menu.contains(e.target) && e.target !== btn) {
                    menu.remove();
                    document.removeEventListener("click", handler);
                }
            });
        }
        // ACTION MENU
        document.addEventListener("click", function(e) {
            if (e.target.id === "menu-export-ics") {
                window.location='${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&actions=ical';
            }
            if (e.target.id === "menu-gantt") {
                window.location='${request.contextPath}/web/userview/${appId}/${userviewId}/_/${customId!menuId}?view=timeline';
            }
        });

        function enableScrollNavigation() {
            const calendarEl = document.getElementById("calendar");
            if (!calendarEl) return;

            let scrollTimeout = null;
            calendarEl.addEventListener("wheel", function (e) {
                e.preventDefault();
                clearTimeout(scrollTimeout);
                scrollTimeout = setTimeout(() => {
                    if (e.deltaY > 0) {
                        // Scroll Down → NEXT
                        document.querySelector(".fc-next-button")?.click();
                    } else if (e.deltaY < 0) {
                        // Scroll Up → PREV
                        document.querySelector(".fc-prev-button")?.click();
                    }
                }, 80);
            }, { passive: false });
        }

        document.addEventListener('DOMContentLoaded', function() {
            showLoading();
            var calendarEl = document.getElementById('calendar');
            var jsonForm = $('input#jsonForm').val() ? JSON.parse($('input#jsonForm').val()) : {};
            var nonce = '${nonce!}';
            kecakCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'dayGridMonth',

                dayHeaderFormat: { weekday: 'long' },
                expandRows: true,
                handleWindowResize: true,

                selectable: true,
                customButtons: {
                    <#if editable>
                        addButton: {
                            text: 'Add Event',
                            click: function() {
                                popupForm('${formDefId}', '${appId}', '${appVersion}', jsonForm, nonce, {}, {}, 370, 900);
                            }
                        },
                    </#if>
                    viewDrodown: {
                        text: 'View',
                        click: function() {

                        }
                    },
                    myMenuButton: {
                        text: 'Menu', // change with icon grid with CSS
                        click: function() {
                            toggleMenuDropdown();
                        }
                    }
                },
                headerToolbar: {
                    left: 'myMenuButton prev,next <#if editable>addButton</#if>', //prev next  and add event add button
                    center: 'title',
                    right: 'viewDropdown' // user can switch calendar between day, week, month, and year (timeGridDay,dayGridWeek,dayGridMonth,multiMonthYear,listMonth)
                },
                displayEventTime: true,
                eventTimeFormat: { hour: "numeric", minute: "2-digit", meridiem: "short" },
                editable: ${editable?string},
                dayMaxEvents: true, // allow "more" link when too many events
                navLinks: false,
                events: function(fetchInfo, successCallback, failureCallback){
                    showLoading();
                    // Request 1 (Main Event)
                    let mainEvent = $.get(
                        '${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&action=event'
                    );

                    // Request 2 (Holiday Event) If Enable
                    const enableCalHoliday = ${activateCalendarHoliday?string};
                    let holidayEvent = null;
                    if (enableCalHoliday) {
                        holidayEvent = $.get(
                            '${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service',
                            {
                                datalistId: '${dataListId}',
                                userviewId: '${userviewId}',
                                menuId: '${menuId}',
                                action: 'holidayEvent',
                                start: fetchInfo.startStr,
                                end: fetchInfo.endStr
                            }
                        );
                    }

                    // Join All Request
                    $.when(mainEvent, holidayEvent).done(function(r1, r2) {
                        let events = [];
                        // r1 Format: [data, status, xhr]
                        if (r1) {
                            events = events.concat(r1[0]);
                        }

                        if (enableCalHoliday && r2) {
                            events = events.concat(r2[0]);
                        }

                        successCallback(events);

                    }).always(function() {
                        hideLoading();
                    });
                },
                dateClick:  function(info) {
                    const clickedDate = info.dateStr;
                    const events = kecakCalendar.getEvents().filter(e =>
                        e.startStr.substring(0,10) === clickedDate
                    );
                    dayEventsPopup(info, events, ${editable?string});
                },
                dayCellDidMount: function(info) {
                    const day = info.date.getDay();  // 0=Sunday, 6=Saturday
                    // IF WEEKEND
                    if (day === 0 || day === 6) {
                        // Warna background opsional
                        info.el.style.backgroundColor = "#ffecec";

                        // CHANGE DATE COLOR
                        const dayNumberEl = info.el.querySelector(".fc-daygrid-day-number");
                        if (dayNumberEl) {
                            dayNumberEl.style.color = "red";
                            dayNumberEl.style.fontWeight = "bold";
                        }
                    }
                },
                eventClick: function(info) {
                    showEventInfoPopup({
                        id: info.event.id,
                        title: info.event.title,
                        start: info.event.startStr,
                        end: info.event.endStr,
                        location: info.event.extendedProps.location,
                        description: info.event.extendedProps.description,
                        isPublicCalendar: info.event.extendedProps.isPublicCalendar,
                        editable: ${editable?string}
                    });
                }

            });
            kecakCalendar.render();
            enableScrollNavigation();
            // ===============================================
            // Remove Button View Dropdown Generate By FC
            // ===============================================
            const toolbar = document.querySelector(".fc-header-toolbar");

            if (toolbar) {
                const observer = new MutationObserver(() => {
                    const btn = toolbar.querySelector(".fc-viewDropdown-button");
                    if (btn) btn.remove();
                });

                observer.observe(toolbar, {
                    childList: true,
                    subtree: true
                });
            }
            setTimeout(() => {
                let headerRight = document.querySelector(".fc-toolbar.fc-header-toolbar .fc-toolbar-chunk:last-child");

                // Delete default button viewDropdown
                let oldBtn = document.querySelector(".fc-viewDropdown-button");
                if (oldBtn) oldBtn.remove();

                // ==== CREATE SELECT DROPDOWN ====
                let select = document.createElement("select");
                select.className = "fc-view-select";

                select.innerHTML = `
                    <option value="timeGridDay">Day</option>
                    <option value="dayGridWeek">Week</option>
                    <option value="dayGridMonth" selected>Month</option>
                    <option value="multiMonthYear">Year</option>
                    <option value="listMonth">List</option>
                `;

                // ==== EVENT CHANGE ====
                select.addEventListener("change", function() {
                    kecakCalendar.changeView(this.value);
                });

                // INSERT TO HEADER
                headerRight.appendChild(select);
            }, 300);
        });
    </script>
</head>
<body>
    <input type='hidden' id='jsonForm' value="${jsonForm!}" >
    <div id='calendar'></div>
</body>
</html>
