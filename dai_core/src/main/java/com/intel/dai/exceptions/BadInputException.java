/* Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

*/
package com.intel.dai.exceptions;

/** Main Exception class thrown for the bad input provided by different DAI parts.
 *
 */
@SuppressWarnings("serial")
public class BadInputException extends Exception{

    public BadInputException(String message) {
        super(message);
    }

    public BadInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

