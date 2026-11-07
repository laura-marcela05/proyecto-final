package com.corhuila.gymtracker.controller;

import com.corhuila.gymtracker.dto.CheckOutRequest;
import com.corhuila.gymtracker.dto.SessionResponse;
import com.corhuila.gymtracker.security.AuthenticatedUser;
import com.corhuila.gymtracker.service.SessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/students/{studentId}/sessions")
    public ResponseEntity<SessionResponse> checkIn(@PathVariable UUID studentId,
                                                   @AuthenticationPrincipal AuthenticatedUser caller) {
        SessionResponse created = sessionService.checkIn(studentId, caller.id(), caller.role());
        return ResponseEntity.created(URI.create("/api/v1/sessions/" + created.id())).body(created);
    }

    @GetMapping("/students/{studentId}/sessions")
    public List<SessionResponse> list(@PathVariable UUID studentId,
                                      @RequestParam(required = false) String status,
                                      @AuthenticationPrincipal AuthenticatedUser caller) {
        return sessionService.list(studentId, caller.id(), caller.role(), status);
    }

    @PutMapping("/sessions/{sessionId}/muscle-groups/{muscleGroup}")
    public SessionResponse logMuscleGroup(@PathVariable UUID sessionId, @PathVariable String muscleGroup,
                                          @AuthenticationPrincipal AuthenticatedUser caller) {
        return sessionService.logMuscleGroup(sessionId, muscleGroup, caller.id(), caller.role());
    }

    @PatchMapping("/sessions/{sessionId}")
    public SessionResponse checkOut(@PathVariable UUID sessionId, @Valid @RequestBody CheckOutRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser caller) {
        return sessionService.checkOut(sessionId, caller.id(), caller.role());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID sessionId, @AuthenticationPrincipal AuthenticatedUser caller) {
        sessionService.delete(sessionId, caller.id(), caller.role());
        return ResponseEntity.noContent().build();
    }
}
