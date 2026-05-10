<?php
    const DIR_ROOT = __DIR__ . "/";
    const DIR_STATIC = "static";
    const DIR_CORE = DIR_STATIC . "/core";
    const DIR_LANGUAGES = DIR_STATIC . "/languages";
    const DIR_MISCELLANEOUS = DIR_STATIC . "/miscellaneous";
    const DIR_TEMPLATES = DIR_STATIC . "/templates";
    const SSL_CERTIFICATE_PATH = "/etc/nginx/ssl/intermediateCA.pem";

    define("SERVER_URL", sprintf('https://%s:%s%s', getenv("BE_SERVICE_NAME"), getenv('BE_PORT'), (getenv("BE_APP_CONTEXT_PATH") ? '/' . trim(getenv("BE_APP_CONTEXT_PATH"), '/') : '')));
    define("SERVER_USERNAME", getenv('BACKEND_USERNAME'));
    define("SERVER_PASSWORD", getenv('BACKEND_PASSWORD'));
