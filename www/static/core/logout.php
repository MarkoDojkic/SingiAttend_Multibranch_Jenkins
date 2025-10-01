<?php
    session_start();
    require "../../constants.php";

    if($_SESSION['isAdminLoggedIn']){
        foreach(["SingidunumBG", "SingidunumNS", "SingidunumNIS"] as $proxyIdentifier){
            $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/csrfLogout");

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
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/csrfLogout");

        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HEADER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "X-Tenant-ID: " . $_SESSION['proxyIdentifier']
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);
        curl_close($server_request);
    }

    session_unset();
    session_destroy();
    header("Location: /index.php", true, 307);
?>