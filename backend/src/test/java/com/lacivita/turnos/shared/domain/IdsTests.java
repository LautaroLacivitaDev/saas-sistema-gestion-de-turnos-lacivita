package com.lacivita.turnos.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTests {

    @Test
    void generatesVersion7Uuids() {
        UUID id = Ids.newId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void idsCreatedLaterSortAfterEarlierOnes() {
        UUID earlier = Ids.newId(Instant.parse("2026-01-01T10:00:00Z"));
        UUID later = Ids.newId(Instant.parse("2026-01-01T10:00:01Z"));

        assertThat(earlier.toString()).isLessThan(later.toString());
    }

    @Test
    void idsAreUnique() {
        var ids = new HashSet<UUID>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(Ids.newId());
        }

        assertThat(ids).hasSize(10_000);
    }
}
