ALTER TABLE crime_matching_result_position
ADD COLUMN caseload_datetime_added TIMESTAMPTZ DEFAULT NULL,
ADD COLUMN position_datetime_added TIMESTAMPTZ DEFAULT NULL;
