<?php
// Minimal page so the image has something to serve.
header('Content-Type: application/json');

echo json_encode([
    'app' => 'todolist-php',
    'php' => PHP_VERSION,
    'sapi' => PHP_SAPI,
]);
