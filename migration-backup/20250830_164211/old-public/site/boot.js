(function () {
    function start() {
        var node = document.getElementById("site-app");
        if (node && window.Elm && Elm.Main && Elm.Main.init) {
            Elm.Main.init({ node: node });
        }
    }
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
