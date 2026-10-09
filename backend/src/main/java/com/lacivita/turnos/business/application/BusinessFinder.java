package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.application.BusinessViews.SearchHitView;
import com.lacivita.turnos.business.application.BusinessViews.SearchResultsView;
import com.lacivita.turnos.business.domain.Branch;
import com.lacivita.turnos.business.domain.BranchRepository;
import com.lacivita.turnos.business.domain.BusinessSearch;
import com.lacivita.turnos.business.domain.SearchTerms;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Buscador público de negocios: por nombre, rubro o lugar, tolerante a errores de tipeo y a las tildes. */
@Service
public class BusinessFinder {

    static final int MAX_PAGE_SIZE = 50;

    private final BusinessSearch search;
    private final BranchRepository branches;

    BusinessFinder(BusinessSearch search, BranchRepository branches) {
        this.search = search;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public SearchResultsView search(String text, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        // Uno de más para saber si hay otra página sin contar todos los resultados.
        var hits = search.search(SearchTerms.of(text), safeSize + 1, safePage * safeSize);
        boolean hasMore = hits.size() > safeSize;
        var shown = hasMore ? hits.subList(0, safeSize) : hits;
        var places = placesOf(shown.stream().map(BusinessSearch.Hit::id).toList());
        var items = shown.stream()
                .map(hit -> new SearchHitView(
                        hit.name(),
                        hit.slug(),
                        hit.category(),
                        hit.description(),
                        places.getOrDefault(hit.id(), List.of())))
                .toList();
        return new SearchResultsView(items, safePage, hasMore);
    }

    private Map<UUID, List<String>> placesOf(List<UUID> businessIds) {
        if (businessIds.isEmpty()) {
            return Map.of();
        }
        return branches.findAllByBusinessIdInOrderByCreatedAtAsc(businessIds).stream()
                .collect(Collectors.groupingBy(
                        Branch::getBusinessId,
                        Collectors.mapping(
                                branch -> branch.details().address().place(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> list.stream().distinct().toList()))));
    }
}
