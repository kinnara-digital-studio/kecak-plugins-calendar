function dayEventsPopup(info, events, editable){
    const containerCalendar = document.getElementById("calendar");

    // Container
    const container = document.createElement("div");
    container.id = "popup-event-container";

    // Overlay
    const overlay = document.createElement("div");
    overlay.className = "popup-event-overlay";

    // Popup box
    const box = document.createElement("div");
    box.className = "popup-event-box";

    // Close button (X)
    const closeBtn = document.createElement("span");
    closeBtn.className = "close-event-btn";
    closeBtn.innerHTML = "&times;";
    closeBtn.onclick = () => container.remove();

    // Header
    const header = document.createElement("div");
    header.className = "popup-event-header";

    const title = document.createElement("div");
    title.className = "popup-event-title";
    title.innerText = "Calendar Events";

    header.appendChild(title);

    // Card
    const card = document.createElement("div");
    card.className = "popup-event-card";

    const datePicked = formatDateTime(info.date);
    const h2 = document.createElement("h2");
    h2.innerText = datePicked.date;

    card.appendChild(h2);

    // Event List Section
    const eventsContainer = document.createElement("div");
    const eventsTitle = document.createElement("div");
    eventsTitle.className = "section-event-title";
    eventsTitle.innerText = "Upcoming Events";

    eventsContainer.appendChild(eventsTitle);
    if (events.length === 0){
        const eventItem = document.createElement("div");
        eventItem.className = "event_item";
        const dot = document.createElement("div");
        dot.className = "event_item_dot";

        const title = document.createElement("div");
        title.className = "event_item_title";
        title.textContent = "No Events";

        eventItem.appendChild(dot);
        eventItem.appendChild(title);

        eventsContainer.appendChild(eventItem);
    }else {
        // Loop event
        events.sort((a, b) => new Date(a.startStr) - new Date(b.startStr));
        events.forEach(ev => {
            const eventItem = document.createElement("div");
            eventItem.className = "event_item";

            const dot = document.createElement("div");
            dot.className = "event_item_dot";

            // WRAP TITLE + TIME
            const infoWrap = document.createElement("div");
            infoWrap.className = "event_item_info";

            const start = formatDateTime(ev.startStr);
            const end = formatDateTime(ev.endStr);

            const title = document.createElement("div");
            title.className = "event_item_title";
            title.textContent = ev.title;

            const copy = document.createElement("div");
            copy.className = "event_item_time";
            copy.innerHTML = `${start.time} - ${end.time}`;

            infoWrap.appendChild(title);
            infoWrap.appendChild(copy);

            // BUTTON WRAPPER
            const btnWrap = document.createElement("div");
            btnWrap.className = "event_item_actions";

            const editBtn = document.createElement("button");
            editBtn.className = "event_btn_edit";
            editBtn.textContent = "Edit";
            editBtn.onclick = function() {
                editEvent(ev.id);
            };

            const deleteBtn = document.createElement("button");
            deleteBtn.className = "event_btn_delete";
            deleteBtn.textContent = "Delete";
            deleteBtn.onclick = function () {
                if (confirm(`Delete event "${ev.title}" ?`)) {
                    deleteEvent(ev.id);
                    eventItem.remove();
                }
            };

            btnWrap.appendChild(editBtn);
            btnWrap.appendChild(deleteBtn);

            eventItem.appendChild(dot);
            eventItem.appendChild(infoWrap);
            if(editable && !ev.extendedProps.isPublicCalendar){
                eventItem.appendChild(btnWrap);
            }
            eventsContainer.appendChild(eventItem);
        });
    }

    // Append all
    box.appendChild(closeBtn);
    box.appendChild(header);
    box.appendChild(card);
    box.appendChild(eventsContainer);

    overlay.appendChild(box);
    container.appendChild(overlay);
    containerCalendar.appendChild(container);
}

function formatDateTime(stringDate) {
    const date = new Date(stringDate);

    // Format Tanggal: EEE, dd MMM yyyy
    const formattedDate = date.toLocaleDateString("en-GB", {
        weekday: "long", // EEE
        day: "2-digit", // dd
        month: "long", // MMM
        year: "numeric" // yyyy
    });

    // Format Jam: HH:mm
    const formattedTime = date.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });

    return {
        date: formattedDate,
        time: formattedTime
    };
}

function showEventInfoPopup(data) {
    const containerCalendar = document.getElementById("calendar");

    // Container
    const container = document.createElement("div");
    container.id = "popup-event-container";

    // Overlay
    const overlay = document.createElement("div");
    overlay.className = "popup-event-overlay";

    // Popup box
    const box = document.createElement("div");
    box.className = "popup-event-box";

    // Close button (X)
    const closeBtn = document.createElement("span");
    closeBtn.className = "close-event-btn";
    closeBtn.innerHTML = "&times;";
    closeBtn.onclick = () => container.remove();

    // Header
    const header = document.createElement("div");
    header.className = "popup-event-header";

    const title = document.createElement("div");
    title.className = "popup-event-title";
    title.innerText = "Event Info";

    header.appendChild(title);

    // Card
    const card = document.createElement("div");
    card.className = "popup-event-card";

    const h2 = document.createElement("h2");
    h2.innerText = data.title;

    const startDateTime = formatDateTime(data.start);
    const start = document.createElement("div");
    start.className = "event-time-row";
    start.innerHTML = "⏱ Start: " + startDateTime.date + " - " + startDateTime.time;

    const endDateTime = formatDateTime(data.end);
    const end = document.createElement("div");
    end.className = "event-time-row";
    end.innerHTML = "⏱ End: " + endDateTime.date + " - " + endDateTime.time;

    card.appendChild(h2);
    card.appendChild(start);
    card.appendChild(end);

    // Description
    const descTitle = document.createElement("div");
    descTitle.className = "section-event-title";
    descTitle.innerText = "Description";

    const desc = document.createElement("pre");
    desc.className = "description-event-box";
    desc.innerText = data.description;

    // BUTTON AREA
    const btnWrap = document.createElement("div");
    btnWrap.className = "popup-event-btn-row";

    // Edit Button
    const editBtn = document.createElement("button");
    editBtn.className = "event-btn edit-btn";
    editBtn.innerText = "Edit";
    editBtn.onclick = () => {
        editEvent(data.id);
        container.remove();
    };

    // Delete Button
    const deleteBtn = document.createElement("button");
    deleteBtn.className = "event-btn delete-btn";
    deleteBtn.innerText = "Delete";
    deleteBtn.onclick = () => {
        if (confirm("Delete this event?")) {
            deleteEvent(data.id);
            container.remove();
        }
    };

    btnWrap.appendChild(editBtn);
    btnWrap.appendChild(deleteBtn);

    // Append all
    box.appendChild(closeBtn);
    box.appendChild(header);
    box.appendChild(card);

    if(data.isPublicCalendar){
        box.appendChild(descTitle);
        box.appendChild(desc);
    }

    if(data.editable && !data.isPublicCalendar){
        box.appendChild(btnWrap);
    }

    overlay.appendChild(box);
    container.appendChild(overlay);
    containerCalendar.appendChild(container);
}

function popupFormWithoutJPopup(elementId, appId, appVersion, jsonForm, nonce, args, data, height = "600px", width = "90%") {

    const containerCalendar = document.getElementById("calendar");

    // Hapus popup jika ada
    const existing = document.getElementById("popup-form-container");
    if (existing) existing.remove();

    // Container popup
    const container = document.createElement("div");
    container.id = "popup-form-container";
    container.className = "popup-event-overlay";

    // Box popup
    const box = document.createElement("div");
    box.className = "popup-event-box";
    box.style.width = width;
    box.style.height = height;
    box.style.maxWidth = "900px";
    box.style.maxHeight = "90vh";
    box.style.overflow = "hidden";
    box.style.position = "relative";

    // Tombol close
    const closeBtn = document.createElement("span");
    closeBtn.className = "close-event-btn";
    closeBtn.innerHTML = "&times;";
    closeBtn.onclick = () => container.remove();

    // ===============================
    // BUILD URL
    // ===============================
    debugger;
    let formUrl = '/web/app/' + appId + '/' + appVersion + '/form/embed?_submitButtonLabel=Submit';

    const frameId = args.frameId = "Frame_" + elementId;

    if (data && data.id) {
        formUrl += (formUrl.includes("?") ? "&" : "?") + "id=" + data.id;
    } else {
        formUrl += (formUrl.includes("?") ? "&" : "?") + "_mode=add";
    }

    if (typeof UI !== "undefined" && UI.userviewThemeParams) {
        formUrl += UI.userviewThemeParams();
    }

    // ADD OWASP
    formUrl += "&" + getOwaspCsrfToken();

    // ===============================
    // PARAMETER FORM
    // ===============================
    const params = {
        _json: JSON.stringify(jsonForm || {}),
        _callback: "onSubmitted",
        _setting: JSON.stringify(args || {}).replace(/"/g, "'"),
        _jsonrow: JSON.stringify(data || {}),
        _nonce: nonce
    };

    // Convert params → hidden form sender
    const form = document.createElement("form");
    form.style.display = "none";
    form.method = "POST";
    form.target = frameId;
    form.action = formUrl;

    Object.keys(params).forEach(k => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = k;
        input.value = params[k];
        form.appendChild(input);
    });
    document.body.appendChild(form);
    // ===============================
    // IFRAME
    // ===============================
    const iframe = document.createElement("iframe");
    iframe.id = frameId;
    iframe.name = frameId;
    iframe.style.width = "100%";
    iframe.style.height = "100%";
    iframe.style.border = "0";

    // Append
    box.appendChild(closeBtn);
    box.appendChild(iframe);
    container.appendChild(box);
    container.appendChild(form);
    containerCalendar.appendChild(container);

    // Submit otomatis → load form Joget ke iframe
    form.submit();
}

function getOwaspCsrfToken(){
    let owasp = ConnectionManager.tokenName+"="+ConnectionManager.tokenValue;
    return owasp;
}