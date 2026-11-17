<?php
// Minimal page so the image has something to serve.
header('Content-Type: application/json');

echo json_encode([
    'app' => 'todolist-php',
    'php' => PHP_VERSION,
    'sapi' => PHP_SAPI,
], JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT);
