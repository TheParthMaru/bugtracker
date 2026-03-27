package com.pbm5.bugtracker.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight endpoints for load balancers (e.g. Render defaults to /healthz).
 * Does not touch the database so the port can become healthy as soon as the web tier is up.
 */
@RestController
public class PlatformHealthController {

    @GetMapping({"/healthz", "/health"})
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("ok");
    }
}
