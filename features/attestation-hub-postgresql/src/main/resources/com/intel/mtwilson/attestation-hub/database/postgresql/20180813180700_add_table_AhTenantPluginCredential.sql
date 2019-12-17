/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
/**
 * Author:  rawatar
 * Created: Aug 13, 2018
 */


CREATE  TABLE ah_tenant_plugin_credential (
  id CHAR(36) NOT NULL,
  tenant_id VARCHAR(36),
  plugin_name VARCHAR(255),
  tenant_name VARCHAR(255),
  credential TEXT DEFAULT NULL ,
  created_ts timestamp NULL DEFAULT now(),
  PRIMARY KEY (id)
);

CREATE INDEX idx_tenant_plugin_credential_tenant_id ON ah_tenant_plugin_credential (tenant_id ASC);

INSERT INTO changelog (ID, APPLIED_AT, DESCRIPTION) VALUES (20180813180700,NOW(),'added table ah_tenant_plugin_credential for storing plugin credentials in encrypted manner');
