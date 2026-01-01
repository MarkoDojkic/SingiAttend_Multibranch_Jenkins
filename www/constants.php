<?php
    define("DIR_ROOT", __DIR__ . "/");
    define("DIR_STATIC", "static");
    define("DIR_CORE", DIR_STATIC . "/core");
    define("DIR_LANGUAGES", DIR_STATIC . "/languages");
    define("DIR_MISCELLANEOUS", DIR_STATIC . "/miscellaneous");
    define("DIR_TEMPLATES", DIR_STATIC . "/templates");
    define("SSL_CERTIFICATE_PATH", DIR_ROOT . DIR_MISCELLANEOUS . "/fullchain_ssl.pem");

    $beHost = getenv("BE_SERVICE_NAME");
    $bePort = getenv('BE_PORT');

    define("SERVER_URL", sprintf('https://%s:%s', $beHost, $bePort));
    define("SERVER_USERNAME", "singiattend-admin");
    define("SERVER_PASSWORD", getenv('SERVER_SSL_KEY_PASSWORD'));
