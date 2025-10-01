<!DOCTYPE html>
<html>
    <head>
        <meta charset='utf-8'>
        <meta http-equiv='X-UA-Compatible' content='IE=edge'>
        <title>SingiAttend</title>
        <meta name='viewport' content='width=device-width, initial-scale=1'>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>
    </head>
    <body style="background-color: #d3fff8;">
        <?php
            session_start();          
            
            require "constants.php";

            if(@$_SESSION["language"] !== "english" && 
                    @$_SESSION["language"] !== "serbianCyrilic" 
                            && @$_SESSION["language"] !== "serbianLatin") 
                $_SESSION["language"] = "serbianCyrilic";
            else if(isset($_GET["language"])) @$_SESSION["language"] = $_GET["language"];

            $xml = @simplexml_load_file(DIR_LANGUAGES . "/{$_SESSION["language"]}.xml") or die(file_get_contents("error404.html"));

            if(@$_SESSION["loggedInAs"] !== "admin"  && @$_SESSION["loggedInAs"] !== "professor"  
                && @$_SESSION["loggedInAs"] !== "assistant") {
                        $_SESSION["page"] = "login";
                        @$_SESSION["loggedInAs"] = null;
                    }

            if(@$_GET["page"] !== null) $_SESSION["page"] = $_GET["page"];
            else header("Location:index.php?language={$_SESSION["language"]}&page={$_SESSION["page"]}", true, 307);
            
            echo initiateHeader($xml);
                
            if(@$_SESSION["loggedInAs"] === "professor"){
                switch($_SESSION["page"]){
                    case "login": case "admin_access": header("Location:index.php?language={$_SESSION["language"]}&page=teaching_subject_management", true, 307);
                    case "teaching_subject_management": echo initiateProfessorPage1($xml); break;
                    case "add_new_subject": echo initiateProfessorPage2($xml); break;
                    case "professor_reports": echo initiateProfessorPage3($xml); break;
                    default: echo file_get_contents("error404.html"); break;
                }
            }

            else if(@$_SESSION["loggedInAs"] === "assistant"){
                switch($_SESSION["page"]){
                    case "login": case "admin_access":
                    case "teaching_exercises": echo initiateAssistantPage1($xml); break;
                    case "exercises_reports": echo initiateAssistantPage2($xml); break;
                    default: echo file_get_contents("error404.html"); break;
                }
            }

            else if(@$_SESSION["loggedInAs"] === "admin"){
                switch($_SESSION["page"]){
                    case "login": case "admin_access":
                    case "staff_registration": echo initiateAdminPage1($xml); break;
                    case "staff_management": echo initiateAdminPage2($xml); break;
                    case "students_management": echo initiateAdminPage3($xml); break;
                    case "admin_reports": echo initiateAdminPage4($xml); break;
                    default: echo file_get_contents("error404.html"); break;
                }
            }

            else {
                switch(@$_SESSION["page"]){
                    case "login": echo initiateLoginPage($xml); break;
                    case "admin_access": echo authenticateAdmin($xml); break;
                    default: echo file_get_contents("error404.html"); break;
                }
            }   
    
            echo initiateFooter($xml);
        ?>    
    </body>
</html>

<?php
    
    function initiateHeader($xml){
        $header = file_get_contents(DIR_TEMPLATES . "/header.html");
        
        $links = "";
        $pageName = "";
        $linkID = 1;
        $logoutHref = DIR_CORE . '/logout.php';

        if($_SESSION["loggedInAs"] === null)
            $pageName = "home";
        else {
            $pageName = $_SESSION["loggedInAs"];
            $links .= "<a class='navbar-brand mr-2 mr-md-2' style='font-size: 25px !important; margin-top: 0.8em; color: red;'
                            href={$logoutHref} class=''>". $_SESSION['loggedInUser'] . "</a>";
        }
        
        do {   
            $links .= "
                <a class='navbar-brand mr-2 mr-md-2' 
                style='font-size: 25px !important; margin-top: 0.8em;' 
                href='index.php?language={$_SESSION["language"]}&page={$xml->navigation->{$pageName . "Page"}->{"link" . $linkID}->href[0]}'>
                {$xml->navigation->{$pageName . "Page"}->{"link" . $linkID}->name[0]}</a>
            ";
            $linkID++;
        } while(isset($xml->navigation->{$pageName . "Page"}->{"link" . $linkID}));

        $header = str_replace("{IMAGE_SRC}", DIR_MISCELLANEOUS . "/logo.png", $header);
        $header = str_replace("{LINKS}", $links, $header);

        return $header;
    }

    function initiateLoginPage($xml){
        unset($_SESSION['captcha_text']);
        $page_context = file_get_contents(DIR_TEMPLATES . "/login.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/login.php", $page_context);
        $page_context = str_replace("{IMAGE_SRC}", DIR_CORE . "/captcha.php", $page_context);
        $page_context = str_replace("{HEADER_TITLE}",$xml->loginPage->headerTitle[0], $page_context);
        $page_context = str_replace("{id}",$xml->loginPage->id[0], $page_context);
        $page_context = str_replace("{password}",$xml->registrationPage->password[0], $page_context);
        $page_context = str_replace("{bg}",$xml->registrationPage->bg[0], $page_context);
        $page_context = str_replace("{ns}",$xml->registrationPage->ns[0], $page_context);
        $page_context = str_replace("{nis}",$xml->registrationPage->nis[0], $page_context);
        $page_context = str_replace("{captcha}",$xml->registrationPage->captcha[0], $page_context);
        $page_context = str_replace("{LOGIN}",$xml->loginPage->login[0], $page_context);
        $page_context = str_replace("{RESET}",$xml->registrationPage->reset[0], $page_context);
        
        return $page_context;
    }

    function initiateProfessorPage1($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/teachingSubjectsManagement.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/teaching_subjects_managment.php", $page_context);
        $page_context = str_replace("{ASSISTENT_TITLE}",$xml->registrationPage->assistant[0], $page_context);
        $page_context = str_replace("{SUBJECT_TITLE}",explode("(",$xml->professorPage->subject_name[0])[0], $page_context);
        
        $tBody = "";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllSubjectsByProfessor/" . $_SESSION['loggedInId']);
        
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
        
        foreach($data as $subject){
            $subjectName = $_SESSION["language"] === "english" ? $subject["titleEnglish"] : $subject["title"];

            $startYearButton = "<input type='submit' id='startSY_{$subject['subjectId']}' name='startSY_{$subject['subjectId']}'class='btn btn-success' value='{$xml->professorPage->startSYBtn[0]}'></input>";
            $endYearButton = "<input type='submit' id='endSY_{$subject['subjectId']}' name='endSY_{$subject['subjectId']}'class='btn btn-danger' value='{$xml->professorPage->endSYBtn[0]}'></input>";
            $execisesButton = empty($subject['nameA']) ? "<input type='submit' id='exercises_{$subject['subjectId']}' name='exercises_{$subject['subjectId']}' class='btn btn-primary' value='{$xml->assistantPage->exercisesBtn[0]}'></input>&nbsp;" : "";

            $sYInput = $subject["isInactive"] ? $startYearButton : $endYearButton;
            $assistantName = empty($subject['nameA']) ? $subject["nameT"] : $subject["nameA"];

            $tBody .= "
                <tr id='tr_{$subject['subjectId']}'>
                    <td>$assistantName</td>
                    <td>$subjectName</td>
                    <td>
                    <input type='submit' id='lectures_{$subject['subjectId']}' name='lectures_{$subject['subjectId']}' class='btn btn-primary' value='{$xml->professorPage->lecturesBtn[0]}'></input>&nbsp;
                    {$execisesButton}
                    <input type='submit' id='details_{$subject['subjectId']}' name='details_{$subject['subjectId']}' class='btn btn-info' value='{$xml->professorPage->detailsBtn[0]}'></input>&nbsp;
                    {$sYInput}
                    </td>
                </tr>
            ";
        }

        $page_context = str_replace("{tBody}",$tBody, $page_context);

        return $page_context;
    }

    function initiateProfessorPage2($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/addNewSubject.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/addNewSubject.php", $page_context);
        $page_context = str_replace("{SUBJECT_NAME_TITLE}",$xml->professorPage->subject_name[0], $page_context);
        $page_context = str_replace("{ASSISTANT_SELECTION_TITLE}",$xml->professorPage->assistant_selection[0], $page_context);
        
        $assistants = "<option value=''>-</option>";
        $studies = "";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllAssistants");
                
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

        foreach($data as $assistant){
            $assistants .= "
                <option value='{$assistant["id"]}'>{$assistant["nameSurname"]}</option>
            ";
        }

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllStudies");
                        
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

        $titleLanguage = $_SESSION["language"] === "english" ? "titleEnglish" : "title";
        $numberOfStudies = 0;

        foreach($data as $study){
            for($i = 1; $i < 5; $i++){
                
                if($_SESSION["language"] === "english")
                    $localization = $study["taughtIn"] === "srpski" ? "Serbian language" : "English language";
                else 
                    $localization = $study["taughtIn"] === "srpski" ? "Српски језик" : "Енглески језик";
                
                $formatedValue = $study["id"] . '_' . $i;
                $formatedName = $study["$titleLanguage"] . " - " . $localization . " ($i)";
                
                $studies .= "
                    <option value='{$formatedValue}'>{$formatedName}</option>
                ";
                $numberOfStudies++;
            }
        }

        $page_context = str_replace("{ASSISTANT_SELECTION_VALUES}",$assistants, $page_context);
        $page_context = str_replace("{STUDY_TITLE}",$xml->professorPage->study[0], $page_context);
        $page_context = str_replace("{STUDY_SIZE}",$numberOfStudies, $page_context);
        $page_context = str_replace("{STUDY_VALUES}",$studies, $page_context);
        $page_context = str_replace("{ADD_NEW_SUBJECT}",$xml->professorPage->add_new_subject[0], $page_context);
        $page_context = str_replace("{RESET}",$xml->registrationPage->reset[0], $page_context);

        return $page_context;
    }

    function initiateProfessorPage3($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/professorReports.html");
        $page_context = str_replace("{LABEL_SUBJECTS}",$xml->professorPage->selectSubject[0], $page_context);
        $page_context = str_replace("{FORM_ACTION}",DIR_CORE . "/getGraphs.php", $page_context);
        $page_context = str_replace("{AJAX_URL}",DIR_CORE . "/getSubjectInfo.php", $page_context);
        $page_context = str_replace("{GENERATE_BAR_GRAPH_LECTURE}",$xml->professorPage->generateBGraphLectures[0], $page_context);
        $page_context = str_replace("{GENERATE_BAR_GRAPH_EXERCISE}",$xml->assistantPage->generateBGraphExercises[0], $page_context);
        $page_context = str_replace("{GENERATE_PIE_GRAPH}",$xml->professorPage->generatePGraph[0], $page_context);

        $subjects = "<option value=''>-</option>";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllSubjectsByProfessor/" . $_SESSION['loggedInId']);
        
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

        foreach($data as $subject){
            $subjects .= "<option value='{$subject["subjectId"]}'>ID: {$subject["subjectId"]}</option>";
        }

        $page_context = str_replace("{SUBJECTS}",$subjects, $page_context);

        return $page_context;
    }

    function initiateAssistantPage1($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/teachingExercises.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/teaching_exercises.php", $page_context);
        $page_context = str_replace("{PROFESSOR_TITLE}",$xml->registrationPage->professor[0], $page_context);
        $page_context = str_replace("{SUBJECT_TITLE}",explode("(",$xml->professorPage->subject_name[0])[0], $page_context);
        
        $tBody = "";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllSubjectsByAssistant/" . $_SESSION['loggedInId']);
                
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

        foreach($data as $subject){
            $subjectName = $_SESSION["language"] === "english" ? $subject["titleEnglish"] : $subject["title"];
        
            $tBody .= "
                <tr id='tr_{$subject['subjectId']}'>
                    <td>{$subject['nameT']}</td>
                    <td>$subjectName</td>
                    <td>
                        <input type='submit' id='exercises_{$subject['subjectId']}' name='exercises_{$subject['subjectId']}' class='btn btn-primary' value='{$xml->assistantPage->exercisesBtn[0]}'></input>&nbsp;
                        <input type='submit' id='details_{$subject['subjectId']}' name='details_{$subject['subjectId']}' class='btn btn-info' value='{$xml->professorPage->detailsBtn[0]}'></input>&nbsp;
                    </td>
                </tr>
            ";
        }

        $page_context = str_replace("{tBody}",$tBody, $page_context);

        return $page_context;
    }

    function initiateAssistantPage2($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/assistantReports.html");
        $page_context = str_replace("{LABEL_SUBJECTS}",$xml->professorPage->selectSubject[0], $page_context);
        $page_context = str_replace("{FORM_ACTION}",DIR_CORE . "/getGraphs.php", $page_context);
        $page_context = str_replace("{AJAX_URL}",DIR_CORE . "/getSubjectInfo.php", $page_context);
        $page_context = str_replace("{GENERATE_BAR_GRAPH_EXERCISE}",$xml->assistantPage->generateBGraphExercises[0], $page_context);

        $subjects = "<option value=''>-</option>";

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllSubjectsByAssistant/" . $_SESSION['loggedInId']);
                
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

        foreach($data as $subject){
            $subjects .= "<option value='{$subject["subjectId"]}'>ID: {$subject["subjectId"]}</option>";
        }

        $page_context = str_replace("{SUBJECTS}",$subjects, $page_context);

        return $page_context;
    }

    function initiateAdminPage1($xml){
        unset($_SESSION['captcha_text']);

        $page_context = file_get_contents(DIR_TEMPLATES . "/registration.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/register.php", $page_context);
        $page_context = str_replace("{IMAGE_SRC}", DIR_CORE . "/captcha.php", $page_context);
        $page_context = str_replace("{HEADER_TITLE}",$xml->registrationPage->headerTitle[0], $page_context);
        $page_context = str_replace("{nameSurname}",$xml->registrationPage->nameSurname[0], $page_context);
        $page_context = str_replace("{email}",$xml->registrationPage->email[0], $page_context);
        $page_context = str_replace("{password}",$xml->registrationPage->password[0], $page_context);
        $page_context = str_replace("{passwordConfirm}",$xml->registrationPage->passwordConfirm[0], $page_context);
        $page_context = str_replace("{professor}",$xml->registrationPage->professor[0], $page_context);
        $page_context = str_replace("{assistant}",$xml->registrationPage->assistant[0], $page_context);
        $page_context = str_replace("{bg}",$xml->registrationPage->bg[0], $page_context);
        $page_context = str_replace("{ns}",$xml->registrationPage->ns[0], $page_context);
        $page_context = str_replace("{nis}",$xml->registrationPage->nis[0], $page_context);
        $page_context = str_replace("{captcha}",$xml->registrationPage->captcha[0], $page_context);
        $page_context = str_replace("{REGISTER}",$xml->registrationPage->register[0], $page_context);
        $page_context = str_replace("{RESET}",$xml->registrationPage->reset[0], $page_context);
        $page_context = str_replace("{CSV_MESSAGE}",$xml->registrationPage->csvMessage[0], $page_context);
        
        return $page_context;
    }

    function initiateAdminPage2($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/staffManagement.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/staff_managment.php", $page_context);
        $page_context = str_replace("{NS_TITLE}",$xml->registrationPage->nameSurname[0], $page_context);
        $page_context = str_replace("{EMAIL_TITLE}",$xml->registrationPage->email[0], $page_context);
        $page_context = str_replace("{PASSWORD_TITLE}",$xml->adminPage->passwordTitle[0], $page_context);
        $page_context = str_replace("{ROLE_TITLE}",$xml->adminPage->roleTitle[0], $page_context);
        
        $tBody = "";

        $_SESSION["proxyIdentifier"] = getNewProxyIdentifier($xml);

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllStaff");
                
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

        foreach($data as $staff_member){
            $email = explode("@",$staff_member["email"])[0];

            $tBody .= "
                <tr>
                    <td><input type='text' id='newNS_{$staff_member['id']}' name='newNS_{$staff_member['id']}' placeholder='{$staff_member['nameSurname']}'></td>
                    <td><input type='text' id='newUE_{$staff_member['id']}' name='newUE_{$staff_member['id']}' placeholder='{$email}'></input>@singidunum.ac.rs</td>
                    <td><input type='password' id='newPASS_{$staff_member['id']}' name='newPASS_{$staff_member['id']}' placeholder='********'></input></td>
                    <td><input type='text' id='oldRole_{$staff_member['id']}' name='oldRole_{$staff_member["id"]}' value='{$staff_member["role"]}' readonly></input></td>
                    <td>
                        <input type='submit' id='switchRole_{$staff_member['id']}' name='switchRole_{$staff_member['id']}' class='btn btn-info' value='{$xml->adminPage->switchRoleBtn[0]}'></input>&nbsp;
                        <input type='submit' id='edit_{$staff_member['id']}' name='edit_{$staff_member['id']}' class='btn btn-warning' value='{$xml->adminPage->editBtn[0]}'></input>&nbsp;
                        <input type='submit' id='delete_{$staff_member['id']}' name='delete_{$staff_member['id']}'class='btn btn-danger' value='{$xml->adminPage->deleteBtn[0]}'></input>
                    </td>
                </tr>
            ";
        }

        $page_context = str_replace("{tBody}",$tBody, $page_context);
        return $page_context;
    }

    function initiateAdminPage3($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/studentsManagment.html");
        $page_context = str_replace("{FORM_ACTION}", DIR_CORE . "/students_managment.php", $page_context);
        $page_context = str_replace("{NS_TITLE}",$xml->registrationPage->nameSurname[0], $page_context);
        $page_context = str_replace("{INDEX_NO}",$xml->adminPage->indexNumber[0], $page_context);
        $page_context = str_replace("{PASSWORD_TITLE}",$xml->adminPage->passwordTitle[0], $page_context);
        $page_context = str_replace("{EMAIL_TITLE}",$xml->registrationPage->email[0], $page_context);
        $page_context = str_replace("{facultyTitle - STUDY_TITLE(YEAR)}",$xml->adminPage->facultySY[0], $page_context);
        
        $tBody = "";
        
        $_SESSION["proxyIdentifier"] = getNewProxyIdentifier($xml);

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllStudents");
                
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

        foreach($data as $student){
            $email = explode("@",$student["email"])[0];
            $studentEnrollment = $student["study"] . "<br>" . explode("-",$studentEnrollment_temp)[1];

            $studentEnrollment = $_SESSION["language"] === "english" ? 
                    $student["study"]["facultyTitleEnglish"] . " <br/> " . $student["study"]["titleEnglish"] . " (" . $student["year"] . ")":
                    $student["study"]["facultyTitle"] . " <br/> " . $student["study"]["title"] . " (" . $student["year"] . ")";

            $tBody .= "
                <tr>
                    <td><input type='text' id='newNS_{$student['id']}' name='newNS_{$student['id']}' placeholder='{$student['nameSurname']}'></td>
                    <td><input type='text' id='newIX_{$student['id']}' name='newIX_{$student['id']}' placeholder='{$student['index']}'></td>
                    <td><input type='password' id='newPASS_{$student['id']}' name='newPASS_{$student['id']}' placeholder='********'></input></td>
                    <td><input type='text' id='newUE_{$student['id']}' name='newUE_{$student['id']}' value='{$email}'></input>@singimail.rs</td>
                    <td>$studentEnrollment</td>
                    <td>
                        <input type='submit' id='edit_{$student['id']}' name='edit_{$student['id']}' class='btn btn-warning' value='{$xml->adminPage->editBtn[0]}'></input>&nbsp;
                        <input type='submit' id='delete_{$student['id']}' name='delete_{$student['id']}'class='btn btn-danger' value='{$xml->adminPage->deleteBtn[0]}'></input>
                    </td>
                </tr>
            ";
        }

        $page_context = str_replace("{tBody}",$tBody, $page_context);
        return $page_context;
    }

    function initiateAdminPage4($xml){
        $page_context = file_get_contents(DIR_TEMPLATES . "/adminReports.html");
        $page_context = str_replace("{LABEL_SUBJECTS}",$xml->professorPage->selectSubject[0], $page_context);
        $page_context = str_replace("{FORM_ACTION}",DIR_CORE . "/getGraphs.php", $page_context);
        $page_context = str_replace("{AJAX_URL}",DIR_CORE . "/getSubjectInfo.php", $page_context);
        $page_context = str_replace("{GENERATE_BAR_GRAPH_LECTURE}",$xml->professorPage->generateBGraphLectures[0], $page_context);
        $page_context = str_replace("{GENERATE_BAR_GRAPH_EXERCISE}",$xml->assistantPage->generateBGraphExercises[0], $page_context);
        $page_context = str_replace("{GENERATE_PIE_GRAPH}",$xml->professorPage->generatePGraph[0], $page_context);

        $subjects = "<option value=''>-</option>";

        $_SESSION["proxyIdentifier"] = getNewProxyIdentifier($xml);

        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAllSubjects");
                
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

        foreach($data as $subject){
            $subjects .= "<option value='{$subject["subjectId"]}'>ID: {$subject["subjectId"]}</option>";
        }

        $page_context = str_replace("{SUBJECTS}",$subjects, $page_context);

        return $page_context;
    }

    function initiateFooter($xml){
        $footer = file_get_contents(DIR_TEMPLATES . "/footer.html");
        $footer = str_replace("{TITLE}",$xml->navigation->title[0], $footer);
        $footer = str_replace("{COPYRIGHT}",$xml->footer->copyright[0], $footer);
        $footer = str_replace("{ENGLISH}",$xml->footer->language1[0], $footer);
        $footer = str_replace("{SERBIAN_CYRILIC}",$xml->footer->language2[0], $footer);
        $footer = str_replace("{SERBIAN_LATIN}",$xml->footer->language3[0], $footer);
        $footer = str_replace("{PAGE}",$_GET["page"], $footer);
        
        return $footer;
    }

    function authenticateAdmin($xml){
        header("WWW-Authenticate: Basic realm=\"Administrator panel\"");
        header("HTTP/1.0 401 Unauthorized");
        if (@$_SERVER['PHP_AUTH_USER'] === 'Administrator' && password_verify(@$_SERVER['PHP_AUTH_PW'], "$2y$10\$zeRF8YO1yIitpNMyuyHMpuYwBFRcPh96L6Bol0AE1wztZpiUfKU9S")){
            foreach(["SingidunumBG", "SingidunumNS", "SingidunumNIS"] as $proxyIdentifier){
                $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/csrfLogin");

                curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
                curl_setopt($server_request, CURLOPT_HEADER, true); //Capture headers
                curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
                    "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
                    "X-Tenant-ID: " . $proxyIdentifier
                ));
                curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);
                $response = curl_exec($server_request);

                // Split headers and body
                $header_size = curl_getinfo($server_request, CURLINFO_HEADER_SIZE);
                $header = substr($response, 0, $header_size);
                $body = json_decode(substr($response, $header_size), true);

                // Extract JSESSIONID
                preg_match('/set-cookie: JSESSIONID=([^;]+)/', $header, $jsessionMatches);
                if (isset($jsessionMatches[1])) $_SESSION['JSESSIONID-' .  $proxyIdentifier] = $jsessionMatches[1];
                else die(file_get_contents("error404.html"));

                // Extract CSRF Token and CSRF secret
                preg_match('/XSRF-TOKEN=([^;]+)/', $header, $xsrfMatches);
                $csrfTokenSecret = $body['token'] ?? null;
                if ($csrfTokenSecret && isset($xsrfMatches[1])) {
                    $_SESSION['CSRF_TOKEN-' .  $proxyIdentifier] = $xsrfMatches[1];
                    $_SESSION['CSRF_TOKEN_SECRET-' . $proxyIdentifier] = $csrfTokenSecret;
                    $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $proxyIdentifier] = $body['headerName'];
                } else die(file_get_contents("error404.html"));

                curl_close($server_request);
            }

            $_SESSION["loggedInAs"] = "admin";
            $_SESSION['loggedInUser'] = "Administrator";
            $_SERVER["PHP_AUTH_USER"] = null;
            $_SERVER["PHP_AUTH_PW"] = null;
            header("Location:index.php?language={$_SESSION["language"]}&page=staff_registration",true, 307);
        }

        echo "<script>window.location = 'index.php?language={$_SESSION["language"]}&page=login';</script>";
    }

    function getNewProxyIdentifier($xml){
        if ($_SERVER["REQUEST_METHOD"] === "POST" && isset($_POST['proxyIdentifier'])) return $_POST['proxyIdentifier'];
        else {
            $modal_context = file_get_contents(DIR_TEMPLATES . "/adminProxyHeaderModal.html");
            $modal_context = str_replace("{proxyIdentifierModalLabel}",$xml->adminPage->proxyIdentifierModalLabel[0], $modal_context);
            $modal_context = str_replace("{bg}",$xml->registrationPage->bg[0], $modal_context);
            $modal_context = str_replace("{ns}",$xml->registrationPage->ns[0], $modal_context);
            $modal_context = str_replace("{nis}",$xml->registrationPage->nis[0], $modal_context);
            $modal_context = str_replace("{confirm}",$xml->registrationPage->confirm[0], $modal_context);

            echo($modal_context);
            echo("<script>$('#proxyIdentifierModal').modal('show');</script>");
            exit;
        }
    }
?>