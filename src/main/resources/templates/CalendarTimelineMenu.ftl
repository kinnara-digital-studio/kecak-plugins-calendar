<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="${request.contextPath}/plugin/${className}/js/gantt.css" type="text/css"/>
    </head>
    <body>
         <!-- Untuk Halaman -->
        <div style="margin-bottom:10px;">
            <button id="calendarView">Calendar</button>
            &nbsp;
            <button id="prevPage">Previous</button>
            <span id="currentPage">${.now?date?string["MM/dd/yyyy"]}</span>
            <button id="nextPage">Next</button>
        </div>
        <div id="chart">
            <div id="loadingIndicator" style="display: none;">Loading...</div>
        </div>
       



        <script src="${request.contextPath}/plugin/${className}/js/gantt.js"></script>
        <#-- <script src="${request.contextPath}/plugin/${className}/js/sample-chart/initialize-gantt.js"></script> -->
    </body>

     <script>
         $(document).ready(function () {
        let currentPage = 0; // Track current page
        let ganttChart; // Store Gantt instance

        async function _refreshFunction(page = 0) {
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
            <#-- groupBy: "groupId,subGroupId", -->
            <#-- groupByAlias: "group,subGroup", -->
            refreshFunction: () => _refreshFunction(currentPage)
        };

        ganttChart = new Gantt("chart", params);

        //Custom updateData Function for Pagination
        ganttChart.updateData = async function (page) {
            currentPage = page; // Update current page
            let date = new Date();
            date.setDate(date.getDate() + page);

            $("#currentPage").text(date.toLocaleDateString());

            currentPage = page;
            ganttChart.refreshData();
        };

        //Load first page
       // ganttChart.updateData(currentPage);

        //Pagination Buttons
        $("#prevPage").click(() => {
            ganttChart.updateData(currentPage - 1)
        });

        $("#nextPage").click(() => {
            ganttChart.updateData(currentPage + 1);
        });

        $("#calendarView").click(() => {
            window.location='${request.contextPath}/web/userview/${appId}/${userviewId}/_/${customId!menuId}';
        });
    });
    </script>
</html>