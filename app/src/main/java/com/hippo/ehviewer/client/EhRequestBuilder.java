/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client;

import com.hippo.ehviewer.Settings;
import com.hippo.okhttp.ChromeRequestBuilder;

public class EhRequestBuilder extends ChromeRequestBuilder {

    public EhRequestBuilder(String url) {
        this(url, Settings.getEhConfig());
    }

    public EhRequestBuilder(String url, EhConfig ehConfig) {
        super(url);
        tag(ehConfig);
    }
}
