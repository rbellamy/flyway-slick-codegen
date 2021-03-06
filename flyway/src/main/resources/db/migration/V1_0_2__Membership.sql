SET ROLE test;

CREATE TABLE TEST.MEMBERSHIP (
  ID CHARACTER VARYING (100) NOT NULL,
  PROFILE_ID CHARACTER VARYING (100) NOT NULL,
  CONSTRAINT MEMBERSHIP_PK PRIMARY KEY (ID),
  CONSTRAINT PROFILE_FK FOREIGN KEY (PROFILE_ID) REFERENCES TEST.PROFILE (ID) ON UPDATE RESTRICT ON DELETE CASCADE
);