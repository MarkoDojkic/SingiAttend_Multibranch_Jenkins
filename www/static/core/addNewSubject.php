<?php
    session_start();
    require "../../constants.php";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));

    $errors = array();
    $subject_name = trim($_POST["subject_name"]);
    $studies = array();
    $years = array();

    if (empty($subject_name) || substr_count($subject_name, "/") !== 1 || strpos($subject_name, "/") === 0 || strpos($subject_name, "/") === strlen($subject_name) - 1) $errors[] = "wrong_sN"; 
    
    if (@$_POST['studies'] === null) $errors[] = "did_not_selected_any_study";

    if(sizeof($errors) !== 0){
        foreach ($errors as $errorName){
            echo "<i style='color:red;font-size:14px;'> - " . $xml->errors->{$errorName}[0] . "</i><br><br>";
        }
    }
    else {
        $checkingTitle_temp = explode("/",$subject_name)[0];

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/addNewSubject");

        $subjectData = json_encode(array(
            "title" => explode("/",$subject_name)[0],
            "titleEnglish" => explode("/",$subject_name)[1],
            "professorId" => $_SESSION['loggedInId'],
            "assistantId" => !empty($_POST['assistant_selection']) ? $_POST['assistant_selection'] : null,
            "enrolledStudyIds" => $_POST['studies'],
            "isInactive" => false
        ));
                
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "POST");
        curl_setopt($server_request, CURLOPT_POSTFIELDS, $subjectData);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $response = curl_exec($server_request);
        
        if(!$response)
            echo "<i style='color:red;font-size:14px;'> - {$xml->professorPage->addSubjectFailed[0]}</i><br><br>";
        else {
            echo "<i style='color:green;font-size:14px;'> + {$xml->professorPage->addSubjectSuccessfull[0]}</i><br><br>";

            echo "<script>setTimeout(function(){
                window.top.location.reload();
            }, 5000);</script>";
        }

        curl_close($server_request);
    }
?>