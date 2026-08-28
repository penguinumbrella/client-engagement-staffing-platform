package com.skillstorm.api_gateway.Filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaleCacheSupportTest {

    @Test
    void isCacheable_trueForGetOnCacheableRoutes() {
        assertThat(StaleCacheSupport.isCacheable("GET", "/staffing/api/consultants")).isTrue();
        assertThat(StaleCacheSupport.isCacheable("GET", "/client/clients")).isTrue();
        assertThat(StaleCacheSupport.isCacheable("GET", "/engagement/api/engagements")).isTrue();
    }

    @Test
    void isCacheable_falseForNonGetMethods() {
        assertThat(StaleCacheSupport.isCacheable("POST", "/staffing/api/consultants")).isFalse();
        assertThat(StaleCacheSupport.isCacheable("PUT", "/client/clients")).isFalse();
        assertThat(StaleCacheSupport.isCacheable("DELETE", "/client/clients/1")).isFalse();
    }

    @Test
    void isCacheable_falseForRoutesOutsideAllowlist() {
        assertThat(StaleCacheSupport.isCacheable("GET", "/auth/api/auth/me")).isFalse();
        assertThat(StaleCacheSupport.isCacheable("GET", "/notification/api/notifications")).isFalse();
    }

    @Test
    void isCacheable_falseForSelfScopedMePaths() {
        assertThat(StaleCacheSupport.isCacheable("GET", "/staffing/api/assignments/me/engagement-ids")).isFalse();
        assertThat(StaleCacheSupport.isCacheable("GET", "/staffing/api/assignments/me/engagements/1/exists")).isFalse();
    }

    @Test
    void buildKey_isOrderIndependentForQueryParams() {
        String keyA = StaleCacheSupport.buildKey("GET", "/client/clients", "page=0&size=100");
        String keyB = StaleCacheSupport.buildKey("GET", "/client/clients", "size=100&page=0");

        assertThat(keyA).isEqualTo(keyB);
    }

    @Test
    void buildKey_differsByMethodPathAndQuery() {
        String base = StaleCacheSupport.buildKey("GET", "/client/clients", null);

        assertThat(base).isNotEqualTo(StaleCacheSupport.buildKey("GET", "/client/clients/1", null));
        assertThat(base).isNotEqualTo(StaleCacheSupport.buildKey("GET", "/client/clients", "page=1"));
        assertThat(base).isNotEqualTo(StaleCacheSupport.buildKey("POST", "/client/clients", null));
    }
}
