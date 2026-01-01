<?php
require __DIR__ . '/vendor/autoload.php';

use Prometheus\CollectorRegistry;
use Prometheus\RenderTextFormat;
use Prometheus\Storage\InMemory;

// Create registry
$registry = new CollectorRegistry(new InMemory());

// Safe prefix for metrics
$prefix = getenv('APP_NAME') ?: 'app';

// Helper to sanitize metric names
function sanitize_metric_name($name) {
    return preg_replace('/[^a-zA-Z0-9_]/', '_', $name);
}

// Parse PHP-FPM status
function parse_fpm_status($path, $socket = null) {
    if ($socket) {
        $url = "http://unix:$socket:$path";
    } else {
        $url = "http://localhost$path";
    }

    $ctx = stream_context_create(['http' => ['timeout' => 1]]);
    $content = @file_get_contents($url, false, $ctx);
    if ($content === false) {
        return [];
    }

    $metrics = [];
    $lines = explode("\n", $content);
    foreach ($lines as $line) {
        $line = trim($line);
        if ($line === '' || strpos($line, ':') === false) continue;

        [$key, $value] = array_map('trim', explode(':', $line, 2));

        // Numeric values only
        if (is_numeric($value)) {
            $metrics[sanitize_metric_name($key)] = $value + 0;
        }
    }
    return $metrics;
}

// Function to update all FPM metrics
function update_fpm_metrics($registry, $prefix, $fpm_status_path, $fpm_socket) {
    $metrics = parse_fpm_status($fpm_status_path, $fpm_socket);

    foreach ($metrics as $name => $value) {
        $metric_name = $prefix . '_' . $name;
        if (!$registry->getMetricFamilySamples($metric_name)) {
            $registry->getOrRegisterGauge($prefix, $name, 'PHP-FPM metric ' . $name);
        }
        $registry->getOrRegisterGauge($prefix, $name, 'PHP-FPM metric ' . $name)->set($value);
    }
}

// Expose basic PHP info
$phpInfoGauge = $registry->getOrRegisterGauge($prefix, 'php_info', 'Information about the PHP environment', ['version']);
$phpInfoGauge->set(1, [PHP_VERSION]);

// HTTP server to serve metrics
$host = '0.0.0.0';
$port = getenv('METRICS_PORT') ?: 9113;
$server = stream_socket_server("tcp://$host:$port", $errno, $errstr);
if (!$server) {
    echo "$errstr ($errno)\n";
    exit(1);
}

$renderer = new RenderTextFormat();

while ($conn = stream_socket_accept($server)) {
    // Update FPM metrics before serving
    update_fpm_metrics($registry, $prefix, getenv('FPM_STATUS_PATH'), getenv('FPM_SOCKET'));

    // Read HTTP request headers (ignore content)
    while (($line = fgets($conn)) !== false) {
        if (rtrim($line) === '') break;
    }

    // Render Prometheus format
    $out = $renderer->render($registry->getMetricFamilySamples());

    fwrite($conn, "HTTP/1.1 200 OK\r\n");
    fwrite($conn, "Content-Type: " . RenderTextFormat::MIME_TYPE . "\r\n");
    fwrite($conn, "Content-Length: " . strlen($out) . "\r\n");
    fwrite($conn, "\r\n");
    fwrite($conn, $out);
    fclose($conn);
}

