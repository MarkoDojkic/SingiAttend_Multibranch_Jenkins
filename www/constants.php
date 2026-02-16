<?php
    const DIR_ROOT = __DIR__ . "/";
    const DIR_STATIC = "static";
    const DIR_CORE = DIR_STATIC . "/core";
    const DIR_LANGUAGES = DIR_STATIC . "/languages";
    const DIR_MISCELLANEOUS = DIR_STATIC . "/miscellaneous";
    const DIR_TEMPLATES = DIR_STATIC . "/templates";
    const SSL_CERTIFICATE_PATH = "/etc/nginx/ssl/intermediateCA.pem";

    $beHost = getenv("BE_SERVICE_NAME");
    $bePort = getenv('BE_PORT');
    $beAppContextPath = getenv("BE_APP_CONTEXT_PATH");

    define("SERVER_URL", sprintf('https://%s:%s%s', $beHost, $bePort, ($beAppContextPath ? '/' . trim($beAppContextPath, '/') : '')));
    const SERVER_USERNAME = "singiattend-admin";
    define("SERVER_PASSWORD", getenv('SERVER_SSL_KEY_PASSWORD'));
