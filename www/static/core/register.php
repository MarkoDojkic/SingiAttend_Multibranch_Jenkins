<?php

    session_start();
    require "../../constants.php";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));
    $errors = array();
    $nameSurname = trim($_POST["nameSurname"]);
    $password = trim($_POST["password"]);
    $email = trim($_POST["singidunumMail"]);

    $nameSurname_pattern = "/^([\x{0410}-\x{0418}\x{0402}\x{0408}\x{041A}-\x{041F}\x{0409}\x{040A}\x{0420}-\x{0428}\x{040B}\x{040F}A-Z\x{0110}\x{017D}\x{0106}\x{010C}\x{0160}]{1}[\x{0430}-\x{0438}\x{0452}\x{043A}-\x{043F}\x{045A}\x{0459}\x{0440}-\x{0448}\x{0458}\x{045B}\x{045F}a-z\x{0111}\x{017E}\x{0107}\x{010D}\x{0161}]+(\s|\-)?)+$/u";
    //name (1-15 letters)_surname (1-15 letters) - unicode
    $password_pattern = "/^(?=.*[a-z])(?=.*\d)[a-z\d]{8,}$/";
    //lowercase and digit (length of min 8)

    if($_FILES["csv_newUsersList"]["error"] === 0){

        if ($_POST["captcha"] !== $_SESSION['captcha_text']) die("<i style='color:red;font-size:14px;'> - " . $xml->errors->invalid_captcha[0] . "</i><br><br>");

        $data_csv = array_map('str_getcsv', file($_FILES['csv_newUsersList']['tmp_name']));
        $invalid_csv_path = DIR_ROOT . DIR_MISCELLANEOUS . "/invalidCsvRows.rtf";

        if(file_exists($invalid_csv_path)) 
            unlink($invalid_csv_path); //izbrisati predhodni fajl ukoliko on postoji

        for($i = 0; $i < sizeof($data_csv); $i++){
            if(!preg_match_all($password_pattern, $data_csv[$i][1])
                  || !preg_match_all("/@singidunum.ac.rs$/", $data_csv[$i][2])
                    || ($data_csv[$i][3] !== "professor" && $data_csv[$i][3] !== "assistant")
            ) file_put_contents($invalid_csv_path, $data_csv[$i][2] . " - " . $xml->errors->csvInvalidUser[0] . "<br>\n", FILE_APPEND | LOCK_EX);
            else {
                $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/insert/staff");
                
                $user_data = array("nameSurname" => $data_csv[$i][0], "email" => $data_csv[$i][2], "passwordHash" => $data_csv[$i][1], "role" => $data_csv[$i][3]);

                curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
                curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "POST");
                curl_setopt($server_request, CURLOPT_POSTFIELDS, json_encode($user_data));
                curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                    "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                    "Content-Type: application/json",
                    "X-Tenant-ID: " . $data_csv[$i][4],
                    $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $data_csv[$i][4]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $data_csv[$i][4]],
                    "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $data_csv[$i][4]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $data_csv[$i][4]]
                ));
                curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

                $response = json_decode(curl_exec($server_request), true);

                curl_close($server_request);

                if($response->{"error"} != null){
                    file_put_contents($invalid_csv_path, $data_csv[$i][2] . " - " . $xml->errors->registrationError[0] . "<br>\n", FILE_APPEND | LOCK_EX);
                    continue;
                }

                $newUserHeader = $response->id . ':' . strtoupper($data_csv[$i][3]);

                $newUser = "
                    ------{$newUserHeader}------
                        {$data_csv[$i][0]}
                        {$data_csv[$i][2]}
                        {$data_csv[$i][1]}
                        {$xml->registrationPage->mb_strtolower(str_replace('Singidunum', '', $data_csv[$i][4]))[0]}
                    ------{$newUserHeader}------
                ";

                file_put_contents(DIR_ROOT . DIR_MISCELLANEOUS . "/accounts.rtf", $newUser, FILE_APPEND | LOCK_EX);
            }
        }

        if(!file_exists($invalid_csv_path))
           echo "<i style='color:green;font-size:14px;'> + " . $xml->registrationPage->csvAllSuccessfull[0] . "</i><br><br>
           <script>setTimeout(function(){
                window.top.location.reload();
            }, 5000);</script>";
        else {
            echo "<i style='color:purple;font-size:14px;'> * " . $xml->errors->csvInvalidUsers[0] . "<br><br>";
            echo file_get_contents($invalid_csv_path);
            echo "</i>";
        }

        return;
    }
    else if(isset($_POST["csv_newUsersList"])) //print invalid csv error only if some file is selected
        echo "<i style='color:red;font-size:14px;'> - " . $xml->errors->csvInvalidError[0] . "</i><br><br>";

    if (!preg_match_all($nameSurname_pattern, $nameSurname)) $errors[] = "wrong_nS";

    if (!preg_match_all("/@singidunum.ac.rs$/", $email)) $errors[] = "wrong_email";
    
    if (!preg_match_all($password_pattern, $password)) $errors[] = "wrong_pass";
    
    if ($password !== trim($_POST["passwordConfirm"]) || !isset($_POST["passwordConfirm"]) || $_POST["passwordConfirm"] === "") $errors[] = "missmatched_pass";
    
    if (!isset($_POST["registerAs"])) $errors[] = "notSelected_registerAs";
    
    if ($_POST["captcha"] !== $_SESSION['captcha_text']) $errors[] = "invalid_captcha";

    
    if(sizeof($errors) !== 0){
        foreach ($errors as $errorName){
            echo "<i style='color:red;font-size:14px;'> - " . $xml->errors->{$errorName}[0] . "</i><br><br>";
        }
    }
    else {
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/insert/staff");
                
        $user_data = array("nameSurname" => $nameSurname, "email" => $email, "passwordHash" => $password, "role" => $_POST['registerAs']);

        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "POST");
        curl_setopt($server_request, CURLOPT_POSTFIELDS, json_encode($user_data));
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_POST["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_POST["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN-' . $_POST["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_POST["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $response = json_decode(curl_exec($server_request), true);

        curl_close($server_request);

        if($response->{"error"} != null) die($xml->errors->registrationError[0] . "<br>");
        else echo "<i style='color:green;font-size:14px;'> + {$xml->registrationPage->successfullRegistration[0]} $response->id . {$xml->registrationPage->pageReload[0]}</i><br><br>";
        
        $newUserHeader = $response->id . ':' . strtoupper($nameSurname);

        $newUser = "
            ------{$newUserHeader}------
                {$email}
                {$password}
                {$_POST['registerAs']}
            ------{$newUserHeader}------
        ";

        file_put_contents(DIR_ROOT . DIR_MISCELLANEOUS . "/accounts.rtf", $newUser, FILE_APPEND | LOCK_EX);

        echo "<script>setTimeout(function(){
            window.top.location.reload();
         }, 5000);</script>";
    }

    unset($_SESSION['captcha_text']);
    echo '<script>parent.reloadCaptcha();</script>';
?>