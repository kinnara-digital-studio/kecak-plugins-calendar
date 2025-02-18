<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="${request.contextPath}/plugin/${className}/js/gantt.css" type="text/css"/>
    </head>
    <body>
        <div id="chart"></div>
        <script src="${request.contextPath}/plugin/${className}/js/gantt.js"></script>
       
        <script>
        
        async function refreshFunction() {
   
            try {
                const response = await fetch("https://sandbox.kecak.org/web/json/app/MTFRM_HRGA/plugin/com.kinnarastudio.calendar.userview.CalendarMenu/service?action=timeline&datalistId=booking_room&userviewId=mtfrm_hrghview&menuId=form_pinjam_ruangan&actions=timeline"); // Replace with actual API URL
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                const data = await response.json();
                console.log("Data fetched:", data);
                return data;
            } catch (error) {
                console.error("Error fetching data:", error);
                console.log();
                return [];
            }
        }

        //Parameters that the chart expects
        let params = {
            sidebarHeader: "Unused right now",
            noDataFoundMessage: "No data found",
            startTimeAlias: "start",
            endTimeAlias: "end",
            idAlias: "recordID",
            rowAlias: "row",
            linkAlias: null,
            tooltipAlias: "tooltip",
            groupBy: "groupId,subGroupId",
            groupByAlias: "group,subGroup",
            refreshFunction: refreshFunction
        }

        //Create the chart.
        //On first render the chart will call its refreshData function on its own.
        // let ganttChart = new Gantt("chart", params);

        //To refresh the chart's data
        // ganttChart.refreshData();

        // Initialize the Gantt chart
        let ganttChart = new Gantt("chart", params);

        // Override refreshData to include API call
        ganttChart.refreshData = async function () {
            const newData = await refreshFunction();
            if (newData) {
                ganttChart.updateData(newData); // Assuming updateData is the method to refresh chart data
            }
        };

        // Call refreshData manually if needed
        ganttChart.refreshData();
        
        </script>
    </body>
</html>