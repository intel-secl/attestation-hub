/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova.identity.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * Authentication request to openstack
 *
 */
@JsonInclude(value = Include.NON_NULL)
@SuppressWarnings("unused") // Public fields are needed by jackson to serialize, alternative is adding getters which is too verbose.
public class AuthRequestV3 {

    @JsonInclude(value = Include.NON_NULL)
    private static class Auth {
        public Identity identity;
        public Scope scope;

        public Auth(String domainName, String projectName, String userName, String password) {
            this.identity = new Identity(domainName, userName, password);
            this.scope = new Scope(domainName, projectName);
        }

        @Override
        public String toString() {
            return "Auth [identity=" + this.identity + ", scope=" + this.scope + "]";
        }
    }

    @JsonInclude(value = Include.NON_NULL)
    private static class Identity {
        public String[] methods;
        public Password password;

        public Identity(String domainName, String userName, String password) {
            this.methods = new String[] { "password" };
            this.password = new Password(domainName, userName, password);
        }

        @Override
        public String toString() {
            return "Identity [methods=" + Arrays.toString(this.methods) + ", password=" + this.password + "]";
        }
    }

    @JsonInclude(value = Include.NON_NULL)
    private static class Password {
        public User user;

        public Password(String domainName, String userName, String password) {
            this.user = new User(domainName, userName, password);
        }

        @Override
        public String toString() {
            return "Password [user=" + this.user + "]";
        }
    }

    @JsonInclude(value = Include.NON_NULL)
    private static class User {
        public String name;
        public String password;
        public Domain domain;

        public User(String domainName, String userName, String password) {
            this.domain = new Domain(domainName);
            this.name = userName;
            this.password = password;
        }

        @Override
        public String toString() {
            return "User [name=" + this.name + ", password=*****hidden*****" + ", domain=" + this.domain + "]";
        }
    }

    @JsonInclude(value = Include.NON_NULL)
    private static class Domain {
        public String name;

        public Domain(String domainName) {
            this.name = domainName;
        }

        @Override
        public String toString() {
            return "Domain [name=" + this.name + "]";
        }
    }

    @JsonInclude(value = Include.NON_NULL)
    private static class Scope {
        public Project project;

        public Scope(String domainName, String projectName) {
            this.project = new Project(domainName, projectName);
        }

        @Override
        public String toString() {
            return "Scope [project=" + this.project + "]";
        }

    }

    @JsonInclude(value = Include.NON_NULL)
    private static class Project {
        public String name;
        public Domain domain;

        public Project(String domainName, String projectName) {
            this.name = projectName;
            this.domain = new Domain(domainName);
        }

        @Override
        public String toString() {
            return "Project [name=" + this.name + ", domain=" + this.domain + "]";
        }
    }

    private Auth auth;

    public AuthRequestV3(String domainName, String projectName, String userName, String password) {
        this.auth = new Auth(domainName, projectName, userName, password);
    }

    public Auth getAuth() {
        return this.auth;
    }

    @Override
    public String toString() {
        return "AuthRequestV3 [auth=" + this.auth + "]";
    }

}
