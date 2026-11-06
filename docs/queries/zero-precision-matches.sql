WITH matches AS (
SELECT
  DATE(crime_version.created_at)                    as ingestion_date,
	police_force_area,
	crime_reference,
	crime_version.id                                  as version_id,
	device_serial_number,
	crime_matching_result_position.captured_date_time as captured_date_time,
	crime_matching_result_position.precision          as precision,
	crime_matching_result_position.sequence_label     as sequence_label
FROM crime
INNER JOIN crime_version
ON crime.id = crime_version.crime_id
INNER JOIN crime_matching_result
ON crime_version.id = crime_matching_result.crime_version_id
INNER JOIN crime_matching_result_device_wearer
ON crime_matching_result.id = crime_matching_result_device_wearer.crime_matching_result_id
INNER JOIN crime_matching_result_position
ON crime_matching_result_device_wearer.id = crime_matching_result_position.crime_matching_result_device_wearer_id
),

match_summary As (
  SELECT 
    police_force_area, 
	  crime_reference, 
	  version_id, 
	  device_serial_number,
	  count(*)                                       as num_matched_locs,
	  sum(case when precision = 0 then 1 else 0 end) as num_zero_precision_locs
  FROM matches
  GROUP BY police_force_area, crime_reference, version_id, device_serial_number, ingestion_date
  ORDER BY ingestion_date
),

zero_prec_matches AS (
SELECT *
FROM match_summary
WHERE num_zero_precision_locs > 0
),

labelled AS (
    SELECT
        crime_reference,
        device_serial_number,
        regexp_replace(sequence_label, '[0-9]', '', 'g') AS seq_group,
        CAST(substring(sequence_label FROM '[0-9]+') AS integer) AS seq_num
    FROM matches
	WHERE precision > 0
),
numbered AS (
    SELECT
        *,
        LAG(seq_num) OVER (
            PARTITION BY crime_reference,
                         device_serial_number,
                         seq_group
            ORDER BY seq_num
        ) AS prev_seq_num
    FROM labelled
),

numbered_grouped as (
SELECT
    crime_reference,
    device_serial_number,
    BOOL_OR(seq_num = prev_seq_num + 1) AS has_consecutive_numbers
FROM numbered
GROUP BY
    crime_reference,
    device_serial_number
)

SELECT *
FROM numbered_grouped
Where has_consecutive_numbers = false
