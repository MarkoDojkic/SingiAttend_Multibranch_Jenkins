<?php
    session_start();
    require_once "../../constants.php";

    if (!isset($_GET["username"]) || trim($_GET["username"]) === "") {
        header("Location: ../index.php");
        exit;
    }

    $username = trim($_GET["username"]);

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));

    $server_request = curl_init(SERVER_URL . "/api/v1/invalidateSessionForUser/" . rawurlencode($username));

    curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "OPTIONS");
    curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

    curl_exec($server_request);

    $httpCode = curl_getinfo($server_request, CURLINFO_HTTP_CODE);

    if ($httpCode >= 200 && $httpCode < 300) {
        $_SESSION["SUCCESS"] = "Session for '{$username}' has been invalidated.";
    } else {
        $_SESSION["ERROR"] = "Failed to invalidate session for '{$username}' (HTTP {$httpCode}).";
    }

    header("Location: /index.php?language={$_SESSION["language"]}&page=login");
    exit;
