CREATE TABLE hub_manager
(
    id                              UUID         NOT NULL,
    name                            VARCHAR(255) NOT NULL,
    signature_image                 BYTEA,
    signature_image_content_type    VARCHAR(255),
    CONSTRAINT pk_hub_manager PRIMARY KEY (id)
);