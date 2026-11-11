ALTER TABLE crime_matching_result_device_wearer
    ADD CONSTRAINT uc_crime_matching_result_device_wearer_result_id_id UNIQUE (crime_matching_result_id, id);

CREATE TABLE crime_matching_result_document_request
(
    document_request_id                      UUID NOT NULL PRIMARY KEY
        REFERENCES document_request (id) ON DELETE CASCADE,
    crime_matching_result_id                 UUID NOT NULL
        REFERENCES crime_matching_result (id) ON DELETE CASCADE,
    crime_matching_result_device_wearer_id   UUID NOT NULL,
    CONSTRAINT fk_crime_matching_result_document_request_wearer
        FOREIGN KEY (crime_matching_result_id, crime_matching_result_device_wearer_id)
        REFERENCES crime_matching_result_device_wearer (crime_matching_result_id, id) ON DELETE CASCADE
);

CREATE INDEX idx_crime_matching_result_document_request_wearer
    ON crime_matching_result_document_request (crime_matching_result_device_wearer_id);
