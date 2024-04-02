<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='utf-8' />

    <script src='${request.contextPath}/plugin/${className}/node_modules/fullcalendar/index.global.min.js'></script>
    <script>

        document.addEventListener('DOMContentLoaded', function() {
          var calendarEl = document.getElementById('calendar');
          var calendar = new FullCalendar.Calendar(calendarEl, {
              initialView: 'dayGridWeek',
              selectable: true,
              customButtons: {
                addButton: {
                    text: 'Add Event',
                    click: function() {
                    alert('clicked the add button!');
                    }
                }
              },
              headerToolbar: {
                  left: 'prev,next addButton' //prev next  and add event button
                  center: 'title',
                  right: 'timeGridDay,dayGridWeek,dayGridMonth,multiMonthYear,listMonth' // user can switch calendar between day, week, month, and year
              },

              editable: true,
              dayMaxEvents: true, // allow "more" link when too many events
              navLinks: true,
              events: ${events}
          });
          calendar.render();
        });

    </script>
</head>
<body>
<div id='calendar'></div>
</body>
</html>
