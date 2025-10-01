<?php
    session_start();
    require "../../constants.php";

    $temp = $_SESSION["loggedInAs"] == "professor" ? "assistant"  : "professor";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));

    $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getSubject/" . $_POST['subjectId']);
                
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
    
    echo "
        {$xml->professorPage->subject_name[0]}: <br><b> {$data["title"]}<br>{$data["titleEnglish"]}</b> <br>
    " . ($data["assistantId"] == null);
?>