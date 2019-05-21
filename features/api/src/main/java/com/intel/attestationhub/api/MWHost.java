/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.api;

import com.intel.mtwilson.flavor.rest.v2.model.Host;
import com.intel.mtwilson.flavor.rest.v2.model.Report;
import com.intel.mtwilson.supplemental.saml.TrustAssertion;

public class MWHost {
    private Host host;
    private Report mwHostReport;
    private String samlValidTo;
    private Boolean trusted;
    private TrustAssertion trustAssertion;

    public TrustAssertion getTrustAssertion() {
        return trustAssertion;
    }

    public void setTrustAssertion(TrustAssertion trustAssertion) {
        this.trustAssertion = trustAssertion;
    }

    public Host getHost() {
	return host;
    }

    public void setHost(Host host) {
	this.host = host;
    }

    public Report getMwHostReport() {
	return mwHostReport;
    }

    public void setMwHostReport(Report mwHostReport) {
	this.mwHostReport = mwHostReport;
    }

    public String getSamlValidTo() {
	return samlValidTo;
    }

    public void setSamlValidTo(String samlValidTo) {
	this.samlValidTo = samlValidTo;
    }

    public Boolean getTrusted() {
	return trusted;
    }

    public void setTrusted(Boolean trusted) {
	this.trusted = trusted;
    }

}
