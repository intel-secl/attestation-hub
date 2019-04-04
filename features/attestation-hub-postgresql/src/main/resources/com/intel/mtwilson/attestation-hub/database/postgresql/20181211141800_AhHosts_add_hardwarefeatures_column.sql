/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  rawatar
 * Created: Dec 11, 2018
 */


ALTER TABLE AH_HOST ADD COLUMN HARDWARE_FEATURES text DEFAULT NULL;
INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20181211141800,NOW(),'Inserted HARDWARE_FEATURES column');