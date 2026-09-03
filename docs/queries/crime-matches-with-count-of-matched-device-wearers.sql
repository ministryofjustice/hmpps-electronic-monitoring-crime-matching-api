SELECT
    police_force_area,
    crime_reference,
    crime_version.id as version_id,
    count(*) as num_matched_device_wearers
FROM crime
INNER JOIN crime_version
ON crime.id = crime_version.crime_id
INNER JOIN crime_matching_result
ON crime_version.id = crime_matching_result.crime_version_id
INNER JOIN crime_matching_result_device_wearer
ON crime_matching_result.id = crime_matching_result_device_wearer.crime_matching_result_id
GROUP BY police_force_area, crime_reference, crime_version.id
ORDER BY num_matched_device_wearers DESC

