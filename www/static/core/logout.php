<?php
    session_start();
    require_once "../../constants.php";

    if($_SESSION['isAdminLoggedIn']){
        foreach(["SingidunumBG", "SingidunumNS", "SingidunumNIS"] as $proxyIdentifier){
            $server_request = curl_init(SERVER_URL . "/api/v1/csrfLogout");

            curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($server_request, CURLOPT_HEADER, true);
            curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                "X-Tenant-ID: " . $proxyIdentifier
            ));
            curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);
            curl_close($server_request);
        }
    } else {
        $server_request = curl_init(SERVER_URL . "/api/v1/csrfLogout");

        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HEADER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "X-Tenant-ID: " . $_SESSION['proxyIdentifier']
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);
        curl_close($server_request);
    }

    $redirectHostURL = (isset($_SERVER['HTTPS']) ? 'https://' : 'http://') . $_SERVER['HTTP_HOST'] . "/index.php?language=" . $_SESSION['language'] . "&page=login";

    $redirect = $_SESSION['isSAMLogin'] ? ((isset($_SERVER['HTTPS']) ? 'https://' : 'http://') . $_SERVER['HTTP_HOST'] . '/iam/samlLogout?postLogoutRedirect=' . rawurlencode($redirectHostURL)) : $redirectHostURL;
    session_unset();
    session_destroy();
    header('Location: ' . $redirect, true, 307);
