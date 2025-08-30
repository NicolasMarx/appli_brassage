(function () {
    function mount() {
        var node = document.getElementById('app');
        if (!node || !window.Elm || !window.Elm.Main || !window.Elm.Main.init) {
            return setTimeout(mount, 0);
        }
        window.Elm.Main.init({ node: node, flags: null });
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mount);
    } else {
        mount();
    }
})();
