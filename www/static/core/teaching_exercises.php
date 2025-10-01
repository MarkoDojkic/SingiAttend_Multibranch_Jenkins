<?php
    session_start();
    require "../../constants.php";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));

    foreach(array_keys($_POST) as $key){
        switch(explode('_',$key)[0]){
            case 'details': showDetails(explode('_',$key)[1],$xml); break 2;
            case 'viewStudents': viewAttendingStudents(explode('_',$key)[1],$xml); break 2;
            case 'exercises': viewExercises(explode('_',$key)[1],$xml); break 2;
            case 'startNE': startNewExercise(explode('_',$key)[1],$xml); break 2;
            case 'cancel': exit;
            default: continue 2;
        }
    }

    function showDetails($id,$xml){
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getSubject/" . $id);
                
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

        $action = "/" . DIR_CORE . "/teaching_exercises.php";

        echo "
        <link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css'>
        <script src='https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js'></script>
        <script src='https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js'></script>
        
        <form action='{$action}' method='post' style='text-align: center;margin:0px auto; background-color: #cdeaff; height: 100%;'>
            <div class='form-group'>
                <label for='subject_name'>{$xml->professorPage->subject_name[0]}:</label><br><br>
                <input type='text' class='form-control' name='subject_name' id='subject_name' autocomplete='subject_name' placeholder='{$data["title"]}' readonly>/
                <input type='text' class='form-control' name='subject_nameEng' id='subject_nameEng' autocomplete='subject_nameEng' placeholder='{$data["titleEnglish"]}' readonly>
            </div>
            <div class='form-group'>
                <label for='professor'>{$xml->registrationPage->professor[0]}:</label>
                <input type='text' class='form-control' name='professor_name' id='professor_name_$id' readonly>
            </div>
            <br>		
            <div class='form-group'>
                <button type='submit' class='btn btn-info' id='viewStudents_$id' name='viewStudents_$id'>{$xml->assistantPage->viewStudentsBtn[0]}</button>
                <button type='submit' class='btn btn-danger' id='cancel' name='cancel'>{$xml->professorPage->cancelBtn[0]}</button>
            </div>
        </form>
        
        <script>
            var rows = window.top.document.querySelectorAll('tbody tr');
            var i;
            for (i = 0; i < rows.length; i++) {
                if(rows[i].id != 'tr_{$id}') rows[i].style.backgroundColor = '#00000000';
                else {
                    rows[i].style.backgroundColor = 'lightblue';
                    document.querySelector('#professor_name_$id').placeholder = rows[i].querySelector('td:nth-child(1)').innerHTML;
                }
            }
        </script>
        ";
    }  

    function viewAttendingStudents($id,$xml){
        $alert = "{$xml->assistantPage->viewStudentsText[0]}";
        $localization = $_SESSION["language"] === "english" ? "titleEnglish" : "title";
        
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/allStudentBySubjectId/" . $id);
                
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

        foreach($data as $attendingStudent){
            $lang = $attendingStudent["study"]["taughtIn"] === "engleski" ? $xml->professorPage->englishStudent[0] : $xml->professorPage->serbianStudent[0];
            $study = $_SESSION["language"] === "english" ? 
            $attendingStudent["study"]["facultyTitleEnglish"] . ", " . $attendingStudent["study"]["titleEnglish"] . ", " . $lang:
            $attendingStudent["study"]["facultyTitle"] . ", " . $attendingStudent["study"]["title"] . ", " . $lang;

            $alert .= "\\n - {$attendingStudent["nameSurname"]} ({$attendingStudent["index"]}) - {$attendingStudent["faculty"]} ({$study})";
        }

        if(!strpos($alert,'-')) $alert = $xml->assistantPage->schoolYearOver[0];
        //returns 1 (true) if - does not exist in the string (i.e. in the case if there are no students or if the subject is inactive - subject.is_inactive = '1'
        
        echo "<script>alert('{$alert}');</script>";
        showDetails($id,$xml);
    }

    function viewExercises($id,$xml){

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/subjectIsInactiveById/" . $id);
                
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

        if($data === 'true') 
            die ("
            <script>
            alert('{$xml->assistantPage->schoolYearOver[0]}');
            
            setTimeout(function(){
                window.top.location.reload();
            }, 1000);</script>");

        $exercises = "<option value=''>-</option>";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllExercises/" . $id);
                
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
        
        foreach($data as $exercise){
            $exercises .= "<option value='{$exercise['id']}'>ID: {$exercise['id']}</option>";
        }

        $dateLocal = $_SESSION["language"] === "english" ? date("Y-m-d") : date("d.m.Y");

        $label1 = $dateLocal . ' ' . $xml->assistantPage->startTime[0];
        $label2 = $dateLocal . ' ' . $xml->assistantPage->endTime[0];

        $action = "/" . DIR_CORE . "/teaching_exercises.php";
        $ajax_url = "getLEInfo.php";

        echo "
        <link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css'>
        <script src='https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js'></script>
        <script src='https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js'></script>
        <script src='https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js'></script>
        <script>
            var rows = window.top.document.querySelectorAll('tbody tr');
            var i;
            for (i = 0; i < rows.length; i++) {
                if(rows[i].id != 'tr_{$id}') rows[i].style.backgroundColor = '#00000000';
                else rows[i].style.backgroundColor = 'lightblue';
            }

            function updateExerciseInfo(selections){
                if(selections[selections.selectedIndex].value === '') {
                    document.querySelector('#exerciseInfo').innerHTML = '';
                    return;
                }
                $.ajax({
                    type: 'POST',
                    url: '{$ajax_url}',
                    data: {'exercise_id':selections[selections.selectedIndex].value}, 
                    success: function(result){
                        document.querySelector('#exerciseInfo').innerHTML = result;
                    }
                });
            }
        </script>
        
        <form target='phpIframe' action='{$action}' method='post' style='text-align: center;margin:0px auto; background-color: #cdeaff; height: 100%;'>
            <br>
            <div class='form-group'>
                <label for='exerciseSelection'>{$xml->assistantPage->exerciseSelection[0]}:</label>
                <select class='form-control' name='exerciseSelection' id='exerciseSelection' onchange='updateExerciseInfo(this)'>
                    {$exercises}
                </select>
            </div>
            <div id='exerciseInfo'></div>
            <div class='form-group'>
                <label for='start_time'>{$label1}</label>
                <input type='time' id='start_time' name='start_time'>
            </div>
            <div class='form-group'>
                <label for='end_time'>{$label2}</label>
                <input type='time' id='end_time' name='end_time'>
            </div>			
            <div class='form-group'>
                <button type='submit' class='btn btn-success' id='startNE_$id' name='startNE_$id'>{$xml->assistantPage->exerciseStartBtn[0]}</button>
                <button type='submit' class='btn btn-danger' id='cancel' name='cancel'>{$xml->professorPage->cancelBtn[0]}</button>
            </div>
        </form>
        ";
    }

    function startNewExercise($id,$xml){

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getLastExercise/" . $id);
                
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $lastExerciseData = json_decode(curl_exec($server_request), true);
        curl_close($server_request);

        $exerciseLength = date_diff(new DateTime($_POST['start_time']),new DateTime($_POST['end_time']));
  
        if($lastExerciseData != null){
            $lengthFromLastExercise = date_diff(new DateTime($lastExerciseData["endedAt"]),new DateTime($_POST['start_time'])); 

            if($exerciseLength->i < 45 && $exerciseLength->h == 0 // incorrect: 12:00->12:44;12:00->11:59;12:00->18:01
                || $exerciseLength->invert === 1 || $exerciseLength->h > 6 || $exerciseLength->h == 6 && $exerciseLength->i != 0 || $lengthFromLastExercise->invert === 1) {
                echo "<script>alert('{$xml->errors->LELengthInvalid[0]}');</script>";
                viewExercises($id,$xml);
                exit;
            }
        }

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/insert/exercise/" . $id . "/" . $_POST['start_time'] . "/" . $_POST['end_time']);
            
        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, false);
        curl_setopt($server_request, CURLOPT_CUSTOMREQUEST, "PUT");
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        curl_exec($server_request);

        echo("<script>alert('{$xml->assistantPage->startNewExerciseSuccessfull[0]}');</script>");

        viewExercises($id,$xml);
    }
?>