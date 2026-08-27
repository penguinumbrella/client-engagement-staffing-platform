package com.skillstorm.api_gateway.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hit via the CircuitBreaker filter's fallbackUri on each gateway route when
 * the downstream service fails to respond (timeout, connection refused, or
 * the breaker is already open and short-circuiting). Returns a 503 naming
 * the specific service that's down instead of letting the failure surface
 * as a generic 500/timeout.
 *
 * Response shape matches the {"message": "..."} ErrorResponse used by the
 * downstream services themselves, so the frontend can read err.error.message
 * the same way regardless of whether the gateway or the service produced it.
 */
@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback/client", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> client() {
        return unavailable("client");
    }

    @RequestMapping(value = "/fallback/engagement", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> engagement() {
        return unavailable("engagement");
    }

    @RequestMapping(value = "/fallback/staffing", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> staffing() {
        return unavailable("staffing");
    }

    @RequestMapping(value = "/fallback/notification", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> notification() {
        return unavailable("notification");
    }

    @RequestMapping(value = "/fallback/auth", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public ResponseEntity<Map<String, String>> auth() {
        return unavailable("auth");
    }

    private ResponseEntity<Map<String, String>> unavailable(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "The " + service + " service is currently unavailable. Please try again later."));
    }
}
