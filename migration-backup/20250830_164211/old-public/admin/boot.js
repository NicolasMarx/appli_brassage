// public/admin/boot.js
(function () {
    'use strict';

    var mounted = false;

    function mount() {
        if (mounted) return;

        var node = document.getElementById('elm-admin');
        var metaCsrf = document.querySelector('meta[name="csrf-token"]');
        var metaAuth = document.querySelector('meta[name="admin-auth"]');

        var csrf = metaCsrf ? metaCsrf.getAttribute('content') : null;
        var auth = metaAuth ? metaAuth.getAttribute('content') === 'true' : false;

        if (!node || !window.Elm || !window.Elm.Main || typeof window.Elm.Main.init !== 'function') {
            // Elm pas encore chargé ? on retente au prochain tick
            return setTimeout(mount, 0);
        }

        try {
            window.Elm.Main.init({
                node: node,
                flags: { csrf: csrf, auth: auth }
            });
            mounted = true;
        } catch (err) {
            // Log utile en dev si l'init échoue
            // (n'a aucun impact en prod si la console est fermée)
            console.error('Failed to init Elm Main:', err);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mount, { once: true });
    } else {
        mount();
    }
})();
