// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Description of class PermissiveX509TrustManager.
 */
public class PermissiveX509TrustManager implements X509TrustManager {
    @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException { }

    @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException { }

    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
}
