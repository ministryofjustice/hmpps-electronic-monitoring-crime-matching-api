ALTER TABLE crime_matching_result_device_wearer
ALTER COLUMN device_serial_number TYPE varchar(255)
USING device_serial_number::varchar;
