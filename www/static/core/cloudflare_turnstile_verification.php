<?php
    $data = json_decode(file_get_contents('php://input'), true);
    $token = $data['token'] ?? '';

    header('Content-Type: application/json');

    if (empty($token)) {
        return false;
    }

    $url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    $data = [
        'secret'   => getenv('CLOUDFLARE_SECRET_KEY'),
        'response' => $token,
        'remoteip' => $_SERVER['REMOTE_ADDR']
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($data));
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);

    $response = curl_exec($ch);
    $err = curl_error($ch);
    curl_close($ch);

    $success = false;

    if ($err) {
        error_log("Cloudflare Turnstile verification failed: " . $err);
        $success = false;
    } else {
        $result = json_decode($response, true);
        $success =  isset($result['success']) && $result['success'] === true;
    }

    echo json_encode([
        'success' => $success
    ]);
