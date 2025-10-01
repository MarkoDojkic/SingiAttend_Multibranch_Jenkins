#!/bin/bash

for DB_NAME in "SingiAttend_BG" "SingiAttend_NS" "SingiAttend_NIS"; do
    if ! mongo --quiet --eval "db.getMongo().getDBNames().indexOf('$DB_NAME') >= 0" > /dev/null; then
        echo "Database $DB_NAME does not exist, restoring..."
        mongorestore --db $DB_NAME "/var/tmp/$DB_NAME/"
    else
        echo "Database $DB_NAME already exists, skipping restore."
    fi
done