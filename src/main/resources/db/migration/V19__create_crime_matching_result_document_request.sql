CREATE TABLE crime_matching_result_document_request
(
    document_request_id UUID NOT NULL,
    crime_matching_result_device_wearer_id UUID NOT NULL,
    CONSTRAINT pk_crime_matching_result_document_request PRIMARY KEY (document_request_id)
);

ALTER TABLE crime_matching_result_document_request
    ADD CONSTRAINT FK_CRIME_MATCHING_RESULT_DOCUMENT_REQUEST_DOCUMENT_REQUEST
        FOREIGN KEY (document_request_id) REFERENCES document_request (id)
            ON DELETE CASCADE;

ALTER TABLE crime_matching_result_document_request
    ADD CONSTRAINT FK_RESULT_DOCUMENT_REQUEST_WEARER
        FOREIGN KEY (crime_matching_result_device_wearer_id)
            REFERENCES crime_matching_result_device_wearer (id)
            ON DELETE CASCADE;
