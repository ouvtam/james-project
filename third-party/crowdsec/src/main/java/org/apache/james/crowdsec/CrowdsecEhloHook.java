/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.crowdsec;

import static org.apache.james.crowdsec.CrowdsecUtils.isBanned;

import java.util.List;

import javax.inject.Inject;

import org.apache.james.crowdsec.client.CrowdsecClientConfiguration;
import org.apache.james.crowdsec.client.CrowdsecHttpClient;
import org.apache.james.crowdsec.model.CrowdsecDecision;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HeloHook;
import org.apache.james.protocols.smtp.hook.HookResult;

public class CrowdsecEhloHook implements HeloHook {
    private final CrowdsecHttpClient crowdsecHttpClient;

    @Inject
    public CrowdsecEhloHook(CrowdsecClientConfiguration configuration) {
        this.crowdsecHttpClient = new CrowdsecHttpClient(configuration);
    }

    @Override
    public HookResult doHelo(SMTPSession session, String helo) {
        String ip = session.getRemoteAddress().getAddress().getHostAddress();
        return crowdsecHttpClient.getCrowdsecDecisions()
            .map(decisions -> apply(decisions, ip)).block();
    }

    private HookResult apply(List<CrowdsecDecision> decisions, String ip) {
        return decisions.stream()
            .filter(decision -> isBanned(decision, ip))
            .findFirst()
            .map(banned -> HookResult.DENY)
            .orElse(HookResult.DECLINED);
    }
}
