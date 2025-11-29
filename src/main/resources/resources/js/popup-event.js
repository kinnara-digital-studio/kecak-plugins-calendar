function dayEventsPopup(info, events){
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
                editEvent({
                    id: ev.id,
                    formId: ev.extendedProps.formId,
                    jsonForm: ev.extendedProps.jsonForm,
                    nonce: ev.extendedProps.nonce
                });
            };

            const deleteBtn = document.createElement("button");
            deleteBtn.className = "event_btn_delete";
            deleteBtn.textContent = "Delete";
            deleteBtn.onclick = function () {
                if (confirm(`Delete event "${ev.title}" ?`)) {
                    deleteEvent({
                        id: ev.id,
                        formId: ev.extendedProps.formId,
                        datalistId: ev.extendedProps.datalistId
                    });
                    eventItem.remove();
                }
            };

            btnWrap.appendChild(editBtn);
            btnWrap.appendChild(deleteBtn);

            eventItem.appendChild(dot);
            eventItem.appendChild(infoWrap);
            if(ev.extendedProps.isEditable){
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
    debugger;
    const dat = data;
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
    desc.innerText = data.description != "null" ? data.description : "No Description";

    // BUTTON AREA
    const btnWrap = document.createElement("div");
    btnWrap.className = "popup-event-btn-row";

    // Edit Button
    const editBtn = document.createElement("button");
    editBtn.className = "event-btn edit-btn";
    editBtn.innerText = "Edit";
    editBtn.onclick = () => {
        editEvent({
            id: data.id,
            formId: data.formId,
            jsonForm: data.jsonForm,
            nonce: data.nonce
        });
        container.remove();
    };

    // Delete Button
    const deleteBtn = document.createElement("button");
    deleteBtn.className = "event-btn delete-btn";
    deleteBtn.innerText = "Delete";
    deleteBtn.onclick = () => {
        if (confirm("Delete this event?")) {
            deleteEvent({
                id: data.id,
                formId: data.formId,
                datalistId: data.datalistId
            });
            container.remove();
        }
    };

    btnWrap.appendChild(editBtn);
    btnWrap.appendChild(deleteBtn);

    // Append all
    box.appendChild(closeBtn);
    box.appendChild(header);
    box.appendChild(card);
    box.appendChild(descTitle);
    box.appendChild(desc);


    if(data.isEditable){
        box.appendChild(btnWrap);
    }

    overlay.appendChild(box);
    container.appendChild(overlay);
    containerCalendar.appendChild(container);
}