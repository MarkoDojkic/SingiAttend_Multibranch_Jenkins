<?php
    define("DIR_ROOT", __DIR__ . "/");
    define("DIR_STATIC", "static");
    define("DIR_CORE", DIR_STATIC . "/core");
    define("DIR_LANGUAGES", DIR_STATIC . "/languages");
    define("DIR_MISCELLANEOUS", DIR_STATIC . "/miscellaneous");
    define("DIR_TEMPLATES", DIR_STATIC . "/templates");
    define("SSL_CERTIFICATE_PATH", DIR_ROOT . DIR_MISCELLANEOUS . "/fullchain_ssl.pem");
    define("SERVER_URL", "localhost:");
    define("SERVER_PORT", "62811"); #Proxy app port
    define("SERVER_USERNAME", "singiattend-admin");
    define("SERVER_PASSWORD", "singiattend-server2021");
?>