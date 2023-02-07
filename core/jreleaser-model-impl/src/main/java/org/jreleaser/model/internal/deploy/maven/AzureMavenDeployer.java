/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model.internal.deploy.maven;

import org.jreleaser.model.Active;
import org.jreleaser.model.Http;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * @author Andres Almiray
 * @since 1.5.0
 */
public final class AzureMavenDeployer extends AbstractMavenDeployer<AzureMavenDeployer, org.jreleaser.model.api.deploy.maven.AzureMavenDeployer> {
    private static final long serialVersionUID = 4799445800117349009L;

    private final org.jreleaser.model.api.deploy.maven.AzureMavenDeployer immutable = new org.jreleaser.model.api.deploy.maven.AzureMavenDeployer() {
        private static final long serialVersionUID = -7887921802377680250L;

        @Override
        public String getGroup() {
            return org.jreleaser.model.api.deploy.maven.MavenDeployer.GROUP;
        }

        @Override
        public String getUrl() {
            return AzureMavenDeployer.this.getUrl();
        }

        @Override
        public String getUsername() {
            return AzureMavenDeployer.this.getUsername();
        }

        @Override
        public String getPassword() {
            return AzureMavenDeployer.this.getPassword();
        }

        @Override
        public Http.Authorization getAuthorization() {
            return AzureMavenDeployer.this.getAuthorization();
        }

        @Override
        public boolean isSign() {
            return AzureMavenDeployer.this.isSign();
        }

        @Override
        public boolean isVerifyPom() {
            return AzureMavenDeployer.this.isVerifyPom();
        }

        @Override
        public boolean isApplyMavenCentralRules() {
            return AzureMavenDeployer.this.isApplyMavenCentralRules();
        }

        @Override
        public List<String> getStagingRepositories() {
            return unmodifiableList(AzureMavenDeployer.this.getStagingRepositories());
        }

        @Override
        public String getType() {
            return AzureMavenDeployer.this.getType();
        }

        @Override
        public String getName() {
            return AzureMavenDeployer.this.getName();
        }

        @Override
        public Active getActive() {
            return AzureMavenDeployer.this.getActive();
        }

        @Override
        public boolean isEnabled() {
            return AzureMavenDeployer.this.isEnabled();
        }

        @Override
        public Map<String, Object> asMap(boolean full) {
            return unmodifiableMap(AzureMavenDeployer.this.asMap(full));
        }

        @Override
        public String getPrefix() {
            return AzureMavenDeployer.this.getPrefix();
        }

        @Override
        public Map<String, Object> getExtraProperties() {
            return unmodifiableMap(AzureMavenDeployer.this.getExtraProperties());
        }

        @Override
        public Integer getConnectTimeout() {
            return AzureMavenDeployer.this.getConnectTimeout();
        }

        @Override
        public Integer getReadTimeout() {
            return AzureMavenDeployer.this.getReadTimeout();
        }
    };

    public AzureMavenDeployer() {
        super(org.jreleaser.model.api.deploy.maven.AzureMavenDeployer.TYPE);
    }

    @Override
    public org.jreleaser.model.api.deploy.maven.AzureMavenDeployer asImmutable() {
        return immutable;
    }

    @Override
    protected void asMap(boolean full, Map<String, Object> props) {
        // noop
    }
}
