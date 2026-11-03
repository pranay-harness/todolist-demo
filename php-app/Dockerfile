# PHP companion app. Pinned to a PHP build fixed for PHP2Shell (CVE-2026-109618 / CVE-2026-109619).
FROM php:8.3.36-apache-bookworm@sha256:cbd3b3d9011668443d54ab40ec31db56c73d91b4c8198935d639aa80d2d815c1

RUN docker-php-ext-install pdo_mysql

COPY public/ /var/www/html/

EXPOSE 80
