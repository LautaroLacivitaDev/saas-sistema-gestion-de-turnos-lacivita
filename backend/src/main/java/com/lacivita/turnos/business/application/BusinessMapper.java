package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.business.application.BusinessViews.BranchView;
import com.lacivita.turnos.business.application.BusinessViews.BusinessView;
import com.lacivita.turnos.business.domain.Branch;
import com.lacivita.turnos.business.domain.BranchDetails;
import com.lacivita.turnos.business.domain.Business;
import com.lacivita.turnos.business.domain.BusinessProfile;
import java.time.ZoneId;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface BusinessMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "slug", source = "slug")
    @Mapping(target = "name", source = "profile.name")
    @Mapping(target = "category", source = "profile.category")
    @Mapping(target = "description", source = "profile.description")
    @Mapping(target = "searchable", source = "profile.searchable")
    BusinessView toView(UUID id, String slug, BusinessProfile profile);

    default BusinessView toView(Business business) {
        return toView(business.getId(), business.getSlug(), business.profile());
    }

    default BusinessSummary toSummary(Business business) {
        return new BusinessSummary(business.getId(), business.getName(), business.getSlug());
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "details.name")
    @Mapping(target = "street", source = "details.address.street")
    @Mapping(target = "neighborhood", source = "details.address.neighborhood")
    @Mapping(target = "city", source = "details.address.city")
    @Mapping(target = "latitude", source = "details.coordinates.latitude")
    @Mapping(target = "longitude", source = "details.coordinates.longitude")
    @Mapping(target = "phone", source = "details.phone.value")
    @Mapping(target = "timeZone", source = "details.timeZone")
    BranchView toView(UUID id, BranchDetails details);

    default BranchView toView(Branch branch) {
        return toView(branch.getId(), branch.details());
    }

    default String zoneId(ZoneId zone) {
        return zone.getId();
    }
}
