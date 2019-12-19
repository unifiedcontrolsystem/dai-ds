// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.properties;

/**
 * Exception thrown when The Property* classes fail to convert from one format to another.
 */
@SuppressWarnings("serial")
public class PropertyNotExpectedType extends Exception {
    PropertyNotExpectedType(Class<?> expected, Class<?> found) {
        super(String.format("Was expecting a '%s' type but got a '%s' type!", expected.getSimpleName(),
                found.getSimpleName()));
    }
    PropertyNotExpectedType(Class<?> expected) {
        super(String.format("Was expecting a '%s' type but got a null instead!", expected.getSimpleName()));
    }
}
