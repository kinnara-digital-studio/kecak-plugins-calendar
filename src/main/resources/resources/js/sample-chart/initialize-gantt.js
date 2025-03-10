// let data =  [
//         {
//             recordID: 1,
//             row: "Row for ID #1",
//             tooltip: "Tooltips here! Get your tooltips!",
//             start: "Wed Jun 03 2020 14:21:55",
//             end: "Wed Jun 03 2020 20:21:55",
//             group: "This is a group with a new name",
//             groupId: 5
//         },
//         {
//             recordID: 2,
//             row: "Row for ID #2",
//             tooltip: "Tooltip for row 2",
//             start: "Jun 03 2020 11:00:00",
//             end: "Jun 03 2020 15:23:43",
//             group: "empty",
//             groupId: 1
//         },
//         {
//             recordID: 1,
//             row: "Row for ID #1",
//             tooltip: "Tooltip unique to this item",
//             start: "Wed Jun 03 2020 06:00:00",
//             end: "Wed Jun 03 2020 10:00:00",
//             group: "test",
//             groupId: 2,
//             subGroupId: 1,
//             subGroup: "Subgroup #1"
//         },
//         {
//             recordID: 5,
//             row: "Now we have grouping",
//             tooltip: "Tooltip after grouping",
//             start: "Wed Jun 03 2020 06:00:00",
//             end: "Wed Jun 03 2020 10:00:00",
//             group: "test",
//             groupId: 2,
//             subGroupId: 1,
//             subGroup: "Subgroup #1"
//         }
//     ];


//This could be an API call to grab data
async function refreshFunction() {
   
    try {
        const response = await fetch("/web/json/app/MTFRM_HRGA/plugin/com.kinnarastudio.calendar.userview.CalendarMenu/service?action=timeline&datalistId=booking_room&userviewId=mtfrm_hrghview&menuId=form_pinjam_ruangan&action=timeline"); // Replace with actual API URL
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