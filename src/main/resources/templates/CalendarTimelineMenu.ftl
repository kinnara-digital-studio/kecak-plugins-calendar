<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="${request.contextPath}/plugin/${className}/js/gantt.css" type="text/css"/>
    </head>
    <body>
        <div id="chart">
            <div id="loadingIndicator" style="display: none;">Loading...</div>
        </div>
        <script src="${request.contextPath}/plugin/${className}/js/gantt.js"></script>
        <#-- <script src="${request.contextPath}/plugin/${className}/js/sample-chart/initialize-gantt.js"></script> -->
    </body>

    <script>
        $(document).ready(function() {
            async function refreshFunction() {
                try {
                    const response = await fetch("${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&action=timeline"); // Replace with actual API URL
                    if (!response.ok) {
                        throw new Error('HTTP error! Status: '+ response.status);
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
        });
    </script>
</html>