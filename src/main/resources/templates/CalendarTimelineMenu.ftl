<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="${request.contextPath}/plugin/${className}/js/gantt.css" type="text/css"/>
    </head>
    <body>
        <div id="chart">
            <div id="loadingIndicator" style="display: none;">Loading...</div>
        </div>
        <!-- Untuk Halaman -->
        <div style="margin-top:10px;">
            <button id="prevPage">Previous</button>
            <span id="currentPage">Page: 1</span>
            <button id="nextPage">Next</button>
        </div>



        <script src="${request.contextPath}/plugin/${className}/js/gantt.js"></script>
        <#-- <script src="${request.contextPath}/plugin/${className}/js/sample-chart/initialize-gantt.js"></script> -->
    </body>

     <script>
    $(document).ready(function () {
        let currentPage = 0; // Track current page
        let ganttChart; // Store Gantt instance globally

        async function fetchData(page = 0) {
            try {
                const response = await fetch('${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&action=timeline&page=' + page);
                
                if (!response.ok) throw new Error('HTTP error! Status: ' + response.status);

                const data = await response.json();
                console.log("Data fetched for page " + page, data);
                return data; 
            } catch (error) {
                console.error("Error fetching data:", error);
                return [];
            }
        }

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
            groupByAlias: "group,subGroup"
        };

        //Custom function to update Gantt data without full reinitialization
        async function updateData(page = 0) {
            currentPage = page;
            $("#currentPage").text('Page: ' + (currentPage + 1));

            const newData = await fetchData(page);
            if (newData.length > 0) {
                if (ganttChart) {
                    ganttChart.clear(); // Clear existing chart data
                    ganttChart.addData(newData); // Add new data
                } else {
                    params.data = newData;
                    ganttChart = new Gantt("chart", params); // Initialize on first load
                }
            }
        }

        //Load first page on startup
        updateData(currentPage);

        //Pagination Buttons
        $("#prevPage").click(() => {
            if (currentPage > 0) updateData(currentPage - 1);
        });

        $("#nextPage").click(() => {
            updateData(currentPage + 1);
        });
    });
    </script>
</html>