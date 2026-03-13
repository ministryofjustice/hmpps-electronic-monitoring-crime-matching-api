ALTER TABLE crime_version
    ALTER COLUMN crime_date_time_from
    TYPE TIMESTAMP
    USING crime_date_time_from AT TIME ZONE 'UTC';

ALTER TABLE crime_version
    ALTER COLUMN crime_date_time_to
    TYPE TIMESTAMP
    USING crime_date_time_to AT TIME ZONE 'UTC';
