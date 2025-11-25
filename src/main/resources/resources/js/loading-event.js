function injectLoading() {
    // Cek jika sudah ada supaya tidak inject dua kali
    if (document.getElementById("loading-progress")) return;

    let container = document.getElementById("calendar");

    let loadingDiv = document.createElement("div");
    loadingDiv.id = "loading-progress";
    loadingDiv.style.position = "absolute";
    loadingDiv.style.inset = "0";
    loadingDiv.style.background = "rgba(255,255,255,0.7)";
    loadingDiv.style.display = "none"; // default hidden
    loadingDiv.style.justifyContent = "center";
    loadingDiv.style.alignItems = "center";
    loadingDiv.style.zIndex = "10";

    loadingDiv.innerHTML = `
                <div class="w-75">
                    <div class="progress">
                        <div id="loading-bar"
                             class="progress-bar progress-bar-striped progress-bar-animated"
                             role="progressbar"
                             style="width: 0%;">
                        </div>
                    </div>
                    <p class="text-center mt-2">Loading data...</p>
                </div>
            `;

    container.appendChild(loadingDiv);
}

function animateProgress() {
    let bar = document.getElementById("loading-bar");
    let width = 0;

    bar.style.width = "0%";

    let interval = setInterval(() => {
        if (width >= 90) {
            clearInterval(interval);
        } else {
            width += 2;
            bar.style.width = width + "%";
        }
    }, 80);
}

function showLoading() {
    injectLoading(); // pastikan loading sudah dibuat
    document.getElementById("loading-progress").style.display = "flex";
    animateProgress();
}

function hideLoading() {
    let loading = document.getElementById("loading-progress");
    if (loading) loading.style.display = "none";
}