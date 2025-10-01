<?php

    session_start();
    require "../../constants.php";

    $xml = @simplexml_load_file(DIR_ROOT . DIR_LANGUAGES . "/{$_SESSION["language"]}.xml")  or die(file_get_contents(DIR_ROOT . "/error404.html"));
    $attended = "";
    $notAttended = "";
    $attendancesData = "";
    $datesData = "[";

    $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/totalStudents/" . $_POST["subjectSelection"]);
                
    curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
        "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
        "Content-Type: application/json",
        "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
        $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
        "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
    ));
    curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

    $totalStudents = curl_exec($server_request);

    curl_close($server_request);

    if($_POST["graphType"] === "barGraphLectures"){
        $attendanceData = getAttendanceData($xml, $totalStudents, "Lectures");

        if(!is_array($attendanceData) || count($attendanceData) == 0) die("<i style='color:red;font-size:28px;'>" . $xml->errors->graphNotGenerated[0] . "</i>");

        echo "
            <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js' integrity='sha512-CQBWl4fJHWbryGE+Pc7UAxWMUMNMWzWxF4SQo9CgkJIN1kx6djDQZjh3Y8SZ1d+6I+1zze6Z7kHXO7q3UyZAWw==' crossorigin='anonymous' referrerpolicy='no-referrer'></script>
            
            <canvas id='lecturesAttendanceChart'></canvas>

            <script>
                new Chart(document.getElementById('lecturesAttendanceChart').getContext('2d'), {
                    type: 'bar',
                    data: {
                        labels: {$attendanceData[0]},
                        datasets: {$attendanceData[1]}
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            title: {
                                display: true,
                                text: '{$xml->professorPage->barChartTitle[0]}'
                            },
                            scales: {
                                yAxes: [{
                                    ticks: {
                                        beginAtZero: true
                                    }
                                }]
                            }
                        }
                    }
                });
            </script>
        ";
    } else if($_POST["graphType"] === "barGraphExercises"){
        $attendanceData = getAttendanceData($xml, $totalStudents, "Exercises");

        if(!is_array($attendanceData) || count($attendanceData) == 0) die("<i style='color:red;font-size:28px;'>" . $xml->errors->graphNotGenerated[0] . "</i>");

        echo "
            <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js' integrity='sha512-CQBWl4fJHWbryGE+Pc7UAxWMUMNMWzWxF4SQo9CgkJIN1kx6djDQZjh3Y8SZ1d+6I+1zze6Z7kHXO7q3UyZAWw==' crossorigin='anonymous' referrerpolicy='no-referrer'></script>
            
            <canvas id='exercisesAttendanceChart'></canvas>

            <script>
                new Chart(document.getElementById('exercisesAttendanceChart').getContext('2d'), {
                    type: 'bar',
                    data: {
                        labels: {$attendanceData[0]},
                        datasets: {$attendanceData[1]}
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            title: {
                                display: true,
                                text: '{$xml->assistantPage->barChartTitle[0]}'
                            },
                            scales: {
                                yAxes: [{
                                    ticks: {
                                        beginAtZero: true
                                    }
                                }]
                            }
                        }
                    }
                });
            </script>
        ";
    } else if($_POST["graphType"] === "pieGraph"){
        $attendanceDataLectures = getAttendanceData($xml, $totalStudents, "Lectures");
        $attendanceDataExercises = getAttendanceData($xml, $totalStudents, "Exercises");

        if(!is_array($attendanceDataLectures) || count($attendanceDataLectures) == 0 || !is_array($attendanceDataExercises) || count($attendanceDataExercises) == 0) die("<i style='color:red;font-size:28px;'>" . $xml->errors->graphNotGenerated[0] . "</i>");

        $lecturesAttended = $xml->professorPage->attended[0] . ": - " . mb_strtolower($xml->professorPage->lecturesBtn[0]);
        $lecturesNotAttended = $xml->professorPage->notAttended[0] . ": - " . mb_strtolower($xml->professorPage->lecturesBtn[0]);
        $exercisesAttended = $xml->professorPage->attended[0] . ": - " . mb_strtolower($xml->assistantPage->exercisesBtn[0]);
        $exercisesNotAttended = $xml->professorPage->notAttended[0] . ": - " . mb_strtolower($xml->assistantPage->exercisesBtn[0]);

        $notAttendedLectures = 100-$attendanceDataLectures[2];
        $notAttendedExercises = 100-$attendanceDataExercises[2];

        echo "
                <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js' integrity='sha512-CQBWl4fJHWbryGE+Pc7UAxWMUMNMWzWxF4SQo9CgkJIN1kx6djDQZjh3Y8SZ1d+6I+1zze6Z7kHXO7q3UyZAWw==' crossorigin='anonymous' referrerpolicy='no-referrer'></script>
                
                <div style='display: flex; gap: 20%; width: 45%; height: 100%;'>
                    <canvas id='lecturesAttendanceChart' style='flex: 1;'></canvas>
                    <canvas id='exercisesAttendanceChart' style='flex: 1;'></canvas>
                </div>

                <script>
                    new Chart(document.getElementById('lecturesAttendanceChart').getContext('2d'), {
                        type: 'pie',
                        data: {
                            labels: ['{$lecturesAttended}', '{$lecturesNotAttended}'],
                            datasets: [{
                                backgroundColor: ['#33FF3F', '#FF3333'],
                                data: [{$attendanceDataLectures[2]}, {$notAttendedLectures}]
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                title: {
                                    display: true,
                                    text: '{$xml->professorPage->pieChartTitle[0]} {$attendanceDataLectures[2]}%'
                                },
                                tooltip: {
                                    callbacks: {
                                        label: function(context) {
                                            return context.raw + '%';
                                        }
                                    }
                                }
                            }
                        }
                    });

                    new Chart(document.getElementById('exercisesAttendanceChart').getContext('2d'), {
                        type: 'pie',
                        data: {
                            labels: ['{$exercisesAttended}', '{$exercisesNotAttended}'],
                            datasets: [{
                                backgroundColor: ['#99ffa0', '#ff8080'],
                                data: [{$attendanceDataExercises[2]}, {$notAttendedExercises}]
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                title: {
                                    display: true,
                                    text: '{$xml->assistantPage->pieChartTitle[0]} {$attendanceDataExercises[2]}%'
                                },
                                tooltip: {
                                    callbacks: {
                                        label: function(context) {
                                            return context.raw + '%';
                                        }
                                    }
                                }
                            }
                        }
                    });
                </script>
            ";
    }

    function getAttendanceData($xml, $totalStudents, $attendanceFor){
        $totalAttendedStudentsSum = 0;
        $attended = "";
        $notAttended = "";
        $datesData = "[";
        
        $server_request = curl_init("https://" . SERVER_URL . SERVER_PORT . "/api/getAll" . $attendanceFor . "/" . $_POST["subjectSelection"]);

        curl_setopt($server_request, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($server_request, CURLOPT_HTTPHEADER, array(
            "Authorization: Basic " . base64_encode(SERVER_USERNAME . ":" . SERVER_PASSWORD),
            "Content-Type: application/json",
            "X-Tenant-ID: " . $_SESSION["proxyIdentifier"],
            $_SESSION['CSRF_TOKEN_HEADER_NAME-' . $_SESSION["proxyIdentifier"]] . ": " . $_SESSION['CSRF_TOKEN_SECRET-' . $_SESSION["proxyIdentifier"]],
            "Cookie: JSESSIONID=" . $_SESSION['JSESSIONID-' . $_SESSION["proxyIdentifier"]] . "; XSRF-TOKEN=" . $_SESSION['CSRF_TOKEN-' . $_SESSION["proxyIdentifier"]]
        ));
        curl_setopt($server_request, CURLOPT_CAINFO, SSL_CERTIFICATE_PATH);

        $response = json_decode(curl_exec($server_request), true);

        curl_close($server_request);

        if(!is_array($response) || sizeof($response) == 0) return "<i style='color:red;font-size:28px;'>" . $xml->errors->graphNotGenerated[0] . "</i>";
        
        foreach($response as $classInstance){
            $attended .= "\"" . sizeof($classInstance["attendedStudents"]) . "\",";
            $notAttended .= "\"" . ($totalStudents - sizeof($classInstance["attendedStudents"])) . "\",";

            $dateOfLecture = new DateTime($classInstance['startedAt']);
            $dateOfLecture->setTimezone(new DateTimeZone($_SESSION['user_timezone'] ?? 'UTC'));
            $dateOfLecture = $dateOfLecture->format($_SESSION["language"] === "english" ? "Y-m-d" : "d.m.Y");

            $datesData .= "\"" . $dateOfLecture .  "\",";
            $totalAttendedStudentsSum += sizeof($classInstance["attendedStudents"]);
        }

        $attended = substr($attended,0,strlen($attended)-1); //remove last ,
        $notAttended = substr($notAttended,0,strlen($notAttended)-1);

        $attendancesData = "[{
            label: '{$xml->professorPage->attended[0]}',
            backgroundColor: '#33FF3F',
            data: [$attended]
        },{
            label: '{$xml->professorPage->notAttended[0]}',
            backgroundColor: '#FF3333',
            data: [$notAttended]
        }]";

        $datesData = substr($datesData,0,strlen($datesData)-1); //remove last ,
        $datesData .= "]";

        return array($datesData, $attendancesData, round(($totalAttendedStudentsSum / ($totalStudents*sizeof($response))) * 100));
    }
?>