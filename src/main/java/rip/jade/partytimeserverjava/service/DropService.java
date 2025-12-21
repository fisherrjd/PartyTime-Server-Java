package rip.jade.partytimeserverjava.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rip.jade.partytimeserverjava.dto.DropPartyResponse;
import rip.jade.partytimeserverjava.dto.DropRequest;
import rip.jade.partytimeserverjava.dto.DropResponse;
import rip.jade.partytimeserverjava.entity.Drop;
import rip.jade.partytimeserverjava.entity.DropParty;
import rip.jade.partytimeserverjava.repository.DropPartyRepository;
import rip.jade.partytimeserverjava.repository.DropRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DropService {
    private final DropRepository dropRepository;
    private final DropPartyRepository dropPartyRepository;

    @Value("${party.duplicate-window-millis}")
    private long duplicateWindowMillis;

    @Value("${party.timeout-minutes}")
    private long partyTimeoutMinutes;

    @Value("${party.cleanup-interval-millis}")
    private long cleanupIntervalMillis;

    @Transactional
    public DropResponse handleDrop(DropRequest request) {
        int world = request.getWorld();
        Instant now = Instant.now();

        // Check for existing active party
        Optional<DropParty> existingParty = dropPartyRepository.findByWorldAndIsActiveTrue(world);

        if (existingParty.isPresent()) {
            DropParty party = existingParty.get();

            // Check if party has timed out (no drops in 5 minutes)
            if (hasPartyTimedOut(party, now)) {
                log.info("Drop party {} on world {} has timed out. Ending party.", party.getId(), world);
                endParty(party);
                // Create new party and add drop
                return createPartyAndAddDrop(world, request, now);
            }

            // Check for duplicate (within 0.5 seconds)
            if (isDuplicateDrop(party, now)) {
                log.info("Duplicate drop detected on world {} - ignoring (within 0.5s window)", world);
                return DropResponse.builder()
                        .status("duplicate")
                        .world(world)
                        .dropPartyId(party.getId())
                        .message("Drop ignored - likely duplicate from another client")
                        .build();
            }

            // Add drop to existing party
            return addDropToParty(party, request, now);
        } else {
            // No active party exists - create new one
            log.info("No active party on world {} - creating new drop party", world);
            return createPartyAndAddDrop(world, request, now);
        }
    }

    private boolean hasPartyTimedOut(DropParty party, Instant now) {
        Duration timeSinceLastDrop = Duration.between(party.getLastDropAt(), now);
        return timeSinceLastDrop.toMinutes() >= partyTimeoutMinutes;
    }

    private boolean isDuplicateDrop(DropParty party, Instant now) {
        Duration timeSinceLastDrop = Duration.between(party.getLastDropAt(), now);
        return timeSinceLastDrop.toMillis() < duplicateWindowMillis;
    }

    private DropResponse createPartyAndAddDrop(int world, DropRequest request, Instant now) {
        // Create new drop party
        DropParty party = createNewParty(world);

        // Create drop entity
        Drop drop = buildDrop(request);

        // Add drop to party
        party.addDrop(drop);
        party.setLastDropAt(now);

        // Update average drop value
        updateAverageDrop(party);

        // Save to database
        dropPartyRepository.save(party);

        log.info("Created new drop party {} on world {} with first drop: {} x{}",
                party.getId(), world, request.getItemName(), request.getQuantity());

        return DropResponse.builder()
                .status("party_created")
                .world(world)
                .dropPartyId(party.getId())
                .message("New drop party started on world " + world)
                .build();
    }

    private DropResponse addDropToParty(DropParty party, DropRequest request, Instant now) {
        // Create drop entity
        Drop drop = buildDrop(request);

        // Add drop to party
        party.addDrop(drop);
        party.setLastDropAt(now);

        // Update average drop value
        updateAverageDrop(party);

        // Save to database
        dropPartyRepository.save(party);

        log.info("Added drop to party {}: {} x{} (value: {}gp)",
                party.getId(), request.getItemName(), request.getQuantity(), request.getValue());

        return DropResponse.builder()
                .status("drop_added")
                .world(party.getWorld())
                .dropPartyId(party.getId())
                .message("Drop added to existing party")
                .build();
    }

    private Drop buildDrop(DropRequest request) {
        return Drop.builder()
                .itemId(request.getItemId())
                .itemName(request.getItemName())
                .quantity(request.getQuantity())
                .value(request.getValue())
                .build();
    }

    private DropParty createNewParty(int world) {
        return DropParty.builder()
                .world(world)
                .avgDrop(0)
                .isActive(true)
                .lastDropAt(Instant.now())
                .build();
    }

    private void updateAverageDrop(DropParty party) {
        if (party.getDrops().isEmpty()) {
            party.setAvgDrop(0);
            return;
        }

        int totalValue = party.getDrops().stream()
                .mapToInt(drop -> drop.getValue() * drop.getQuantity())
                .sum();

        int avgValue = totalValue / party.getDrops().size();
        party.setAvgDrop(avgValue);
    }

    private void endParty(DropParty party) {
        party.setIsActive(false);
        dropPartyRepository.save(party);
        log.info("Ended drop party {} on world {}", party.getId(), party.getWorld());
    }

    /**
     * Get all active drop parties
     */
    public List<DropPartyResponse> getAllActiveParties() {
        List<DropParty> activeParties = dropPartyRepository.findByIsActiveTrue();

        return activeParties.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Map DropParty entity to DropPartyResponse DTO
     */
    private DropPartyResponse mapToResponse(DropParty party) {
        return DropPartyResponse.builder()
                .id(party.getId())
                .world(party.getWorld())
                .avgDrop(party.getAvgDrop())
                .isActive(party.getIsActive())
                .createdAt(party.getCreatedAt())
                .lastDropAt(party.getLastDropAt())
                .dropCount(party.getDrops().size())
                .build();
    }

    /**
     * Cleanup inactive drop parties
     * Runs automatically at configured interval (default: every 2 minutes)
     * Can also be called manually via the cleanup endpoint
     */
    @Transactional
    @Scheduled(fixedRateString = "${party.cleanup-interval-millis}")
    public int cleanupInactiveParties() {
        Instant now = Instant.now();
        Instant cutoffTime = now.minus(Duration.ofMinutes(partyTimeoutMinutes));

        // Find all active parties
        List<DropParty> activeParties = dropPartyRepository.findByIsActiveTrue();

        int cleanedCount = 0;
        for (DropParty party : activeParties) {
            // Check if party has timed out
            if (party.getLastDropAt().isBefore(cutoffTime)) {
                endParty(party);
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("Cleaned up {} inactive drop parties", cleanedCount);
        }

        return cleanedCount;
    }
}