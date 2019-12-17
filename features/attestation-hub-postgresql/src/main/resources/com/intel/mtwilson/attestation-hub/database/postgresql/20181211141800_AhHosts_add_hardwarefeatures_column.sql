/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
/**
 * Author:  rawatar
 * Created: Dec 11, 2018
 */


ALTER TABLE AH_HOST ADD COLUMN HARDWARE_FEATURES text DEFAULT NULL;
INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20181211141800,NOW(),'Inserted HARDWARE_FEATURES column');