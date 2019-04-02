/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.attestationhub.plugin.nova;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

public class TestUtils {

    public static String loadJsonData(String path) {
        String rawJson = "";
        try (InputStream fileAsStream = TestUtils.class.getClassLoader().getResourceAsStream(path)) {
            if(fileAsStream == null) {
                fail("Unable to load Json file from path " + path);
            }
            rawJson = new String(ByteStreams.toByteArray(fileAsStream));
        } catch (IOException e) {
            fail("Unable to load Json file from path " + path);
        }
        return rawJson;
    }
}
