<?php
require __DIR__ . '/vendor/autoload.php';

use Prometheus\CollectorRegistry;
use Prometheus\RenderTextFormat;
use Prometheus\Storage\InMemory;

// Create registry
$registry = new CollectorRegistry(new InMemory());

// Example metrics (add your own counters/gauges as needed)
$counter = $registry->getOrRegisterCounter('php_app', 'http_requests_total', 'Total HTTP requests');
$counter->inc(); // example increment

// Render Prometheus format
$renderer = new RenderTextFormat();

// Run basic socket server
$host = '0.0.0.0';
$port = 9113;
$server = stream_socket_server("tcp://$host:$port", $errno, $errstr);
if (!$server) {
    echo "$errstr ($errno)\n";
    exit(1);
}

while ($conn = stream_socket_accept($server)) {
    $request = '';
    // Read incoming request (headers)
    while (($line = fgets($conn)) !== false) {
        $request .= $line;
        if (rtrim($line) === '') {
            break;
        }
    }
    // Output metrics
    $out = $renderer->render($registry->getMetricFamilySamples());
    fwrite($conn, "HTTP/1.1 200 OK\r\n");
    fwrite($conn, "Content-Type: " . RenderTextFormat::MIME_TYPE . "\r\n");
    fwrite($conn, "Content-Length: " . strlen($out) . "\r\n");
    fwrite($conn, "\r\n");
    fwrite($conn, $out);
    fclose($conn);
}
