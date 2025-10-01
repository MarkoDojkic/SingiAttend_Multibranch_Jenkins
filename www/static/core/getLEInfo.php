<?php

    session_start();
    require "../../constants.php";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));

    $table = $_POST["exercise_id"] === null ? 'lecture' : 'exercise';
    $primaryKey = $table . '_id';

    if($table === 'lecture'){
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getLecture/" . $_POST["$primaryKey"]);
                
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $data = json_decode(curl_exec($server_request), true);

        curl_close($server_request);
    } else {
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getExercise/" . $_POST["$primaryKey"]);
                
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $data = json_decode(curl_exec($server_request), true);

        curl_close($server_request);
    }

    $dateLocale = $_SESSION["language"] === "english" ? "Y-m-d g:i a" : "d.m.Y H:i";
    $startedAt = new DateTime($data['startedAt']);
    $endedAt = new DateTime($data['endedAt']);
    $attendance = $xml->{($table === 'lecture' ? "professor" : "assistant") . "Page"}->notOverYet[0];

    if(new DateTime() >= $endedAt) {
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/totalStudents/" . $data['subjectId']);
            
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $totalStudents = json_decode(curl_exec($server_request));

        curl_close($server_request);

        $percetage = round( sizeof($data['attendedStudents']) /$totalStudents *100);
        
        $attendance = $percetage . "% (" . sizeof($data['attendedStudents']) . "/" . $totalStudents . ")";
    }

    $startedAt->setTimezone(new DateTimeZone($_SESSION['user_timezone'] ?? 'UTC'));
    $endedAt->setTimezone(new DateTimeZone($_SESSION['user_timezone'] ?? 'UTC'));
    $startTime = $startedAt->format($dateLocale);
    $endTime = $endedAt->format($dateLocale);

    echo "<table class='table table-bordered table-inverse table-responsive' style='font-size: 13px;'>
            <tbody>
                <tr>
                    <td style='text-align: right;'>{$xml->assistantPage->startTime[0]}</td>
                    <td style='text-align: left;'>{$startTime}</td>
                </tr>
                <tr>
                    <td style='text-align: right;'>{$xml->assistantPage->endTime[0]}</td>
                    <td style='text-align: left;'>{$endTime}</td>
                </tr>
                <tr>
                    <td style='text-align: right;'>{$xml->assistantPage->attendance[0]}</td>
                    <td style='text-align: left;'>{$attendance}</td>
                </tr>
            </tbody>
        </table>";
?>