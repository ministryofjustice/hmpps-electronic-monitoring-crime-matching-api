SELECT
    *
FROM (
    SELECT
        police_force_area,
        crime_reference,
        crime_version.id                               as version_id,
        device_serial_number,
        count(*)                                       as num_matched_locs,
        sum(case when precision = 0 then 1 else 0 end) as num_zero_precision_locs
    FROM crime
    INNER JOIN crime_version
    ON crime.id = crime_version.crime_id
    INNER JOIN crime_matching_result
    ON crime_version.id = crime_matching_result.crime_version_id
    INNER JOIN crime_matching_result_device_wearer
    ON crime_matching_result.id = crime_matching_result_device_wearer.crime_matching_result_id
    INNER JOIN crime_matching_result_position
    ON crime_matching_result_device_wearer.id = crime_matching_result_position.crime_matching_result_device_wearer_id
    GROUP BY police_force_area, crime_reference, crime_version.id, device_serial_number
)
WHERE num_zero_precision_locs > 0
ORDER BY num_zero_precision_locs DESC

