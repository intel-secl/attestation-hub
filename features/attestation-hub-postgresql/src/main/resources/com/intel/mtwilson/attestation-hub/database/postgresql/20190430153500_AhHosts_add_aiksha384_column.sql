/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
/**
 * Author:
 * Created: Apr 30, 2019
 */


ALTER TABLE AH_HOST DROP COLUMN AIK_SHA256;
ALTER TABLE AH_HOST ADD COLUMN AIK_SHA384 text DEFAULT NULL;
INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20190430153500,NOW(),'Removed AIK_SHA256 column and inserted AIK_SHA384');