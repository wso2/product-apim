/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.tenantsync.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to communicate tenant management related information.
 * It is designed to be immutable and is constructed using the Builder pattern.
 */
public final class TenantManagementEvent {

    @SerializedName("iss")
    private final String iss;

    @SerializedName("jti")
    private final String jti;

    @SerializedName("iat")
    private final long iat;

    @SerializedName("events")
    private final Map<String, EventDetail> events;

    private TenantManagementEvent(Builder builder) {

        this.iss = builder.iss;
        this.jti = builder.jti;
        this.iat = builder.iat;
        this.events = builder.events;
    }

    public String getIss() {

        return iss;
    }

    public String getJti() {

        return jti;
    }

    public long getIat() {

        return iat;
    }

    public Map<String, EventDetail> getEvents() {

        // Return an unmodifiable copy to maintain immutability.
        return (events != null) ? Collections.unmodifiableMap(events) : null;
    }

    /**
     * Builder for {@link TenantManagementEvent}.
     */
    public static class Builder {

        private String iss;
        private String jti;
        private long iat;
        private Map<String, EventDetail> events;

        public Builder iss(String iss) {

            this.iss = iss;
            return this;
        }

        public Builder jti(String jti) {

            this.jti = jti;
            return this;
        }

        public Builder iat(long iat) {

            this.iat = iat;
            return this;
        }

        public Builder events(Map<String, EventDetail> events) {

            if (events != null) {
                this.events = new HashMap<>(events);
            }
            return this;
        }

        public TenantManagementEvent build() {

            return new TenantManagementEvent(this);
        }
    }

    /**
     * Represents the detailed information contained within a specific event type.
     */
    public static final class EventDetail {

        @SerializedName("initiatorType")
        private final String initiatorType;

        @SerializedName("tenant")
        private final Tenant tenant;

        @SerializedName("action")
        private final String action;

        private EventDetail(Builder builder) {

            this.initiatorType = builder.initiatorType;
            this.tenant = builder.tenant;
            this.action = builder.action;
        }

        public String getInitiatorType() {

            return initiatorType;
        }

        public Tenant getTenant() {

            return tenant;
        }

        public String getAction() {

            return action;
        }

        /**
         * Builder for {@link EventDetail}.
         */
        public static class Builder {

            private String initiatorType;
            private Tenant tenant;
            private String action;

            public Builder initiatorType(String initiatorType) {

                this.initiatorType = initiatorType;
                return this;
            }

            public Builder tenant(Tenant tenant) {

                this.tenant = tenant;
                return this;
            }

            public Builder action(String action) {

                this.action = action;
                return this;
            }

            public EventDetail build() {

                return new EventDetail(this);
            }
        }
    }

    /**
     * Represents the tenant object. This class includes all possible fields from all
     * event types. Fields not present in a specific JSON will be null.
     */
    public static final class Tenant {

        @SerializedName("id")
        private final String id;

        @SerializedName("domain")
        private final String domain;

        @SerializedName("owners")
        private final List<Owner> owners;

        @SerializedName("lifecycleStatus")
        private final LifecycleStatus lifecycleStatus;

        @SerializedName("ref")
        private final String ref;

        private Tenant(Builder builder) {

            this.id = builder.id;
            this.domain = builder.domain;
            this.owners = builder.owners;
            this.lifecycleStatus = builder.lifecycleStatus;
            this.ref = builder.ref;
        }

        public String getId() {

            return id;
        }

        public String getDomain() {

            return domain;
        }

        public List<Owner> getOwners() {

            // Return an unmodifiable copy to maintain immutability.
            return (owners != null) ? Collections.unmodifiableList(owners) : null;
        }

        public LifecycleStatus getLifecycleStatus() {

            return lifecycleStatus;
        }

        public String getRef() {

            return ref;
        }

        /**
         * Builder for {@link Tenant}.
         */
        public static class Builder {

            private String id;
            private String domain;
            private List<Owner> owners;
            private LifecycleStatus lifecycleStatus;
            private String ref;

            public Builder id(String id) {

                this.id = id;
                return this;
            }

            public Builder domain(String domain) {

                this.domain = domain;
                return this;
            }

            public Builder owners(List<Owner> owners) {

                if (owners != null) {
                    this.owners = new ArrayList<>(owners);
                }
                return this;
            }

            public Builder lifecycleStatus(LifecycleStatus lifecycleStatus) {

                this.lifecycleStatus = lifecycleStatus;
                return this;
            }

            public Builder ref(String ref) {

                this.ref = ref;
                return this;
            }

            public Tenant build() {

                return new Tenant(this);
            }
        }
    }

    /**
     * Represents an owner of a tenant.
     */
    public static final class Owner {

        @SerializedName("username")
        private final String username;

        @SerializedName("password")
        private final String password;

        @SerializedName("email")
        private final String email;

        @SerializedName("firstname")
        private final String firstname;

        @SerializedName("lastname")
        private final String lastname;

        private Owner(Builder builder) {

            this.username = builder.username;
            this.password = builder.password;
            this.email = builder.email;
            this.firstname = builder.firstname;
            this.lastname = builder.lastname;
        }

        public String getUsername() {

            return username;
        }

        public String getPassword() {

            return password;
        }

        public String getEmail() {

            return email;
        }

        public String getFirstname() {

            return firstname;
        }

        public String getLastname() {

            return lastname;
        }

        /**
         * Builder for {@link Owner}.
         */
        public static class Builder {

            private String username;
            private String password;
            private String email;
            private String firstname;
            private String lastname;

            public Builder username(String username) {

                this.username = username;
                return this;
            }

            public Builder password(String password) {

                this.password = password;
                return this;
            }

            public Builder email(String email) {

                this.email = email;
                return this;
            }

            public Builder firstname(String firstname) {

                this.firstname = firstname;
                return this;
            }

            public Builder lastname(String lastname) {

                this.lastname = lastname;
                return this;
            }

            public Owner build() {

                return new Owner(this);
            }
        }
    }

    /**
     * Represents the lifecycle status of a tenant, used in activation/deactivation events.
     */
    public static final class LifecycleStatus {

        @SerializedName("activated")
        private final boolean activated;

        private LifecycleStatus(Builder builder) {

            this.activated = builder.activated;
        }

        public boolean isActivated() {

            return activated;
        }

        /**
         * Builder for {@link LifecycleStatus}.
         */
        public static class Builder {

            private boolean activated;

            public Builder activated(boolean activated) {

                this.activated = activated;
                return this;
            }

            public LifecycleStatus build() {

                return new LifecycleStatus(this);
            }
        }
    }
}