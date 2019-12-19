// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2017-2018 Intel(r) Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.intel.xdg;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("serial")
public class StringArrayListSet extends ArrayList<String> {
    @Override
    public boolean add(String s) {
        if(!contains(s)) {
            super.add(s);
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends String> collection) {
        boolean rv = true;
        for(String value: collection)
            rv = this.add(value) && rv;
        return rv;
    }
}
