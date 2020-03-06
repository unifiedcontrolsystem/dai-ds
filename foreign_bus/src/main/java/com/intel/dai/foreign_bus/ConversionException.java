// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.foreign_bus;

/**
 * Description of class ConversionException.
 */
@SuppressWarnings("serial")
public class ConversionException extends Exception {
    public ConversionException(String message) { super(message); }
    public ConversionException(String message, Throwable cause) { super(message, cause); }
}
