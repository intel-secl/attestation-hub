/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:
 * Created: Apr 30, 2019
 */


ALTER TABLE AH_HOST DROP COLUMN AIK_SHA256;
ALTER TABLE AH_HOST ADD COLUMN AIK_SHA384 text DEFAULT NULL;
INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20190430153500,NOW(),'Removed AIK_SHA256 column and inserted AIK_SHA384');