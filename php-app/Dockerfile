# Test fixture for PHP2Shell (CVE-2026-109618 / CVE-2026-109619) scanning.
# Intentionally pinned to a vulnerable PHP build. Do not deploy.
FROM php:8.3.12-apache

RUN docker-php-ext-install pdo_mysql

COPY public/ /var/www/html/

EXPOSE 80
