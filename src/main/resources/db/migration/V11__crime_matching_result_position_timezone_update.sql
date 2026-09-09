ALTER TABLE crime_matching_result_position
    ALTER COLUMN captured_date_time
    TYPE TIMESTAMPTZ
    USING captured_date_time AT TIME ZONE 'UTC';