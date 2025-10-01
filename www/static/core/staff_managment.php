<?php
    session_start();
    require "../../constants.php";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));
    
    foreach(array_keys($_POST) as $key){
        switch(explode('_',$key)[0]){
            case "switchRole": switchRole(explode('_',$key)[1],$xml); break;
            case 'edit': editStaffMember(explode('_',$key)[1],$xml); break;
            case 'delete': deleteStaffMember(explode('_',$key)[1],$xml); break;
            default: continue 2;
        }
    }

    function switchRole($id,$xml){
        $newRole = $_POST["oldRole_$id"] === "professor" ? "assistant" : "professor";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/checkIfStaffHasSubjectAssigned/" . $id);
     
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $data = curl_exec($server_request);

        curl_close($server_request);

        if(!$data){
            $staffMember = $newRole === "professor" ? $xml->registrationPage->assistant[0] : $xml->registrationPage->professor[0];
            showErrorAlert($xml->errors->switchRoleError1[0] . " " . mb_strtolower($staffMember) . " " . $xml->errors->switchRoleError2[0]);
        }
        else {
            $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/update/staff/" . $id);
                
            curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "PATCH");
            curl_setopt($server_request, CURLOPT_POSTFIELDS, json_encode(array("role"=>$newRole)));
            curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                "Content-Type: application/json",
                "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
                $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
                "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
            ));
            curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

            $response = curl_exec($server_request);
            curl_close($server_request);

            if(json_decode($response)->{"error"} != null) showErrorAlert($xml->errors->switchRoleError0[0]);
        }

        reloadPage();
    }

    function editStaffMember($id,$xml){

        $nameSurname_pattern = "/^([\x{0410}-\x{0418}\x{0402}\x{0408}\x{041A}-\x{041F}\x{0409}\x{040A}\x{0420}-\x{0428}\x{040B}\x{040F}A-Z\x{0110}\x{017D}\x{0106}\x{010C}\x{0160}]{1}[\x{0430}-\x{0438}\x{0452}\x{043A}-\x{043F}\x{045A}\x{0459}\x{0440}-\x{0448}\x{0458}\x{045B}\x{045F}a-z\x{0111}\x{017E}\x{0107}\x{010D}\x{0161}]+(\s|\-)?)+$/u";
        $password_pattern = "/^(?=.*[a-z])(?=.*\d)[a-z\d]{8,}$/";

        if(preg_match_all($nameSurname_pattern, $_POST["newNS_$id"])){
            $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/update/staff/" . $id);

            curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "PATCH");
            curl_setopt($server_request, CURLOPT_POSTFIELDS, json_encode(array("nameSurname"=>$_POST["newNS_$id"])));
            curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                "Content-Type: application/json",
                "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
                $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
                "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
            ));
            curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);
            
            $response = curl_exec($server_request);
            curl_close($server_request);

            if(json_decode($response)->{"error"} != null) showErrorAlert($xml->errors->wrong_nS[0]);
        }

        if(!empty($_POST["newUE_$id"])){
            $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/update/staff/" . $id);
                
            curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "PATCH");
            curl_setopt($server_request, CURLOPT_POSTFIELDS, json_encode(array("email"=>$_POST["newUE_$id"] . "@singidunum.ac.rs")));
            curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                "Content-Type: application/json",
                "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
                $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
                "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
            ));
            curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

            $response = curl_exec($server_request);
            curl_close($server_request);

            if(json_decode($response)->{"error"} != null) showErrorAlert($xml->errors->wrong_email[0]);
        }

        if(preg_match_all($password_pattern, $_POST["newPASS_$id"])){
            $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/update/staff/" . $id);
                
            curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "PATCH");
            curl_setopt($server_request, CURLOPT_POSTFIELDS, json_encode(array("passwordHash"=>$_POST["newPASS_$id"])));
            curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                "Content-Type: application/json",
                "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
                $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
                "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
            ));
            curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

            $response = curl_exec($server_request);
            curl_close($server_request);

            if(json_decode($response)->{"error"} != null) showErrorAlert($xml->errors->wrong_pass[0]);
            else {
                $date = date('d.m.Y H:i:s', time()); //H - 24, h - 12
                $newPass = $_POST["newPASS_$id"];
                $editData = "
                    ------PASSWORD_CHANGED_STAFF------
                                ID: {$id}
                         NEW PASSWORD: $newPass
                            CHANGE AT: {$date}
                    ------PASSWORD_CHANGED_STAFF------                  
                ";

                file_put_contents(DIR_ROOT . DIR_MISCELLANEOUS . "/editedPasswords.rtf", $editData, FILE_APPEND | LOCK_EX);
            }
        }

        reloadPage();   
    }

    function deleteStaffMember($id,$xml){

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/delete/staff/" . $id . "/" . ($_POST["oldRole_$id"] === "professor" ? "0" : "1"));

        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "DELETE");
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $response = curl_exec($server_request);
        curl_close($server_request);
        
        if(json_decode($response)->{"error"} != null) showErrorAlert($xml->errors->deleteError[0]);
        else reloadPage();
    }

    function reloadPage(){
        echo "<script>setTimeout(function(){
            window.top.location.reload();
        }, 1000);</script>";
    }

    function showErrorAlert($message){
        echo "<script>alert('$message');</script>";
        reloadPage();
    }
?>