CREATE TABLE "item"
(
  id       BIGINT PRIMARY KEY  NOT NULL,

  uid      VARCHAR(100) UNIQUE NOT NULL,
  text     VARCHAR(100)        NOT NULL,
  created  TIMESTAMP           NOT NULL,
  updated  TIMESTAMP           NOT NULL
);
CREATE SEQUENCE item_id_s;