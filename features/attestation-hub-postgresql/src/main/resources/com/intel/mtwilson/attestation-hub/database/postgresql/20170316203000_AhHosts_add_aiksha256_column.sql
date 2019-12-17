/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
/**
 * Author:  hmgowda
 * Created: Mar 16, 2017
 */


ALTER TABLE AH_HOST DROP COLUMN AIK_SHA1;
ALTER TABLE AH_HOST ADD COLUMN AIK_SHA256 text DEFAULT NULL;
INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20170316203000,NOW(),'Removed AIK_SHA1 column and inserted AIK_SHA256');