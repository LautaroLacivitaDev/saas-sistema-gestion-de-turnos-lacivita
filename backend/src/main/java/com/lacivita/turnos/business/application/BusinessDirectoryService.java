package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.business.domain.BranchRepository;
import com.lacivita.turnos.business.domain.BusinessRepository;
import com.lacivita.turnos.business.domain.SlugClaimRepository;
import com.lacivita.turnos.shared.security.BranchLocator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementa las consultas que el módulo ofrece a otros módulos y a la seguridad. */
@Service
class BusinessDirectoryService implements BusinessDirectory, BranchLocator {

    private final BusinessRepository businesses;
    private final BranchRepository branches;
    private final SlugClaimRepository slugClaims;
    private final BusinessMapper mapper;

    BusinessDirectoryService(
            BusinessRepository businesses,
            BranchRepository branches,
            SlugClaimRepository slugClaims,
            BusinessMapper mapper) {
        this.businesses = businesses;
        this.branches = branches;
        this.slugClaims = slugClaims;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessSummary> find(UUID businessId) {
        return businesses.findById(businessId).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessSummary> findAll(Collection<UUID> businessIds) {
        if (businessIds.isEmpty()) {
            return List.of();
        }
        return businesses.findAllByIdIn(businessIds).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessSummary> findBySlug(String slug) {
        return slugClaims
                .findBusinessBySlug(slug.strip().toLowerCase(Locale.ROOT))
                .map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchSummary> branch(UUID businessId, UUID branchId) {
        return branches.findByIdAndBusinessId(branchId, businessId)
                .map(branch -> new BranchSummary(
                        branch.getId(),
                        branch.getBusinessId(),
                        branch.details().name(),
                        branch.details().address().oneLine(),
                        branch.details().timeZone()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean branchesBelongTo(UUID businessId, Set<UUID> branchIds) {
        return !branchIds.isEmpty() && branches.countByBusinessIdAndIdIn(businessId, branchIds) == branchIds.size();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> businessOf(UUID branchId) {
        return branches.findBusinessIdById(branchId);
    }
}
