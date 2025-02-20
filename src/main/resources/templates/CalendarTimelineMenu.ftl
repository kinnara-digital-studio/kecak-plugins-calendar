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
        <div>
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

            async function refreshFunction(page = 0) {
                try {
                    const response = await fetch('${request.contextPath}/web/json/app/${appId}/${appVersion}/plugin/${className}/service?datalistId=${dataListId}&userviewId=${userviewId}&menuId=${menuId}&action=timeline&page=' + page);
                    
                    if (!response.ok) throw new Error('HTTP error! Status: ' + response.status);

                    const data = await response.json();
                    console.log("Data fetched for page " + page, data);

                    return data; // Return data in its original format
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
                groupByAlias: "group,subGroup",
                refreshFunction: () => refreshFunction(currentPage) // Pass currentPage
            };

            let ganttChart = new Gantt("chart", params);

            ganttChart.refreshData = async function (page = 0) {
                currentPage = page; // Update current page
                $("#currentPage").text('Page: ' + currentPage);

                const newData = await refreshFunction(page);
                if (newData) {
                    ganttChart.updateData(newData); // Update Gantt chart
                }
            };

            // Load first page
            ganttChart.refreshData(currentPage);

            // Pagination Buttons
            $("#prevPage").click(() => {
                if (currentPage > 0) {
                    ganttChart.refreshData(currentPage - 1);
                }
            });

            $("#nextPage").click(() => {
                ganttChart.refreshData(currentPage + 1);
            });
        });
    </script>
</html>