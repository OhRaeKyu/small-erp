package com.smallerp.tenant;

import com.smallerp.account.BusinessHour;
import com.smallerp.account.BusinessHour.DayOfWeekCode;
import com.smallerp.account.IndustryPack;
import com.smallerp.account.IndustryPackRepository;
import com.smallerp.account.Tenant;
import com.smallerp.account.TenantRepository;
import com.smallerp.common.ApiException;
import com.smallerp.common.ErrorCode;
import com.smallerp.common.FieldErrorsException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSettingsService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final TenantRepository tenants;
    private final IndustryPackRepository industryPacks;

    public TenantSettingsService(TenantRepository tenants, IndustryPackRepository industryPacks) {
        this.tenants = tenants;
        this.industryPacks = industryPacks;
    }

    @Transactional(readOnly = true)
    public TenantProfile get(Long tenantId) {
        return toProfile(find(tenantId));
    }

    @Transactional
    public TenantProfile update(Long tenantId, TenantSettingsController.UpdateRequest req) {
        List<BusinessHour> hours = validateHours(req.businessHours());
        Tenant tenant = find(tenantId);
        tenant.updateProfile(req.name().trim(), blankToNull(req.address()), blankToNull(req.phone()), hours,
                req.defaultDailyCapacity(), req.orderLeadDays());
        return toProfile(tenant);
    }

    /** 저장된 값이 없거나 일부 요일만 있어도 화면에는 월~일 7개를 보여 준다 (없는 요일은 쉬는 날) */
    static List<BusinessHour> withAllDays(List<BusinessHour> saved) {
        Map<DayOfWeekCode, BusinessHour> byDay = new EnumMap<>(DayOfWeekCode.class);
        saved.forEach(h -> byDay.put(h.day(), h));
        List<BusinessHour> all = new ArrayList<>();
        for (DayOfWeekCode d : DayOfWeekCode.values()) {
            all.add(byDay.getOrDefault(d, new BusinessHour(d, true, null, null)));
        }
        return all;
    }

    private List<BusinessHour> validateHours(List<BusinessHour> input) {
        Map<String, String> errors = new LinkedHashMap<>();
        Map<DayOfWeekCode, BusinessHour> byDay = new EnumMap<>(DayOfWeekCode.class);
        for (BusinessHour h : input) {
            if (h == null || h.day() == null) {
                errors.putIfAbsent("businessHours", "요일을 확인해 주세요.");
                continue;
            }
            if (byDay.containsKey(h.day())) {
                errors.putIfAbsent("businessHours", "같은 요일이 두 번 들어 있습니다.");
                continue;
            }
            if (h.closed()) {
                byDay.put(h.day(), new BusinessHour(h.day(), true, null, null));
                continue;
            }
            LocalTime open = parse(h.open());
            LocalTime close = parse(h.close());
            String field = "businessHours." + h.day();
            if (open == null || close == null) {
                errors.putIfAbsent(field, "시간을 00:00 형식으로 입력해 주세요.");
            } else if (!open.isBefore(close)) {
                errors.putIfAbsent(field, "마감 시간은 시작 시간보다 늦어야 합니다.");
            }
            byDay.put(h.day(), h);
        }
        if (!errors.isEmpty()) {
            throw new FieldErrorsException(errors);
        }
        return withAllDays(new ArrayList<>(byDay.values()));
    }

    private static LocalTime parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value, HH_MM);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Tenant find(Long tenantId) {
        return tenants.findById(tenantId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private TenantProfile toProfile(Tenant t) {
        String industryName = industryPacks.findById(t.getIndustryCode()).map(IndustryPack::getName).orElse(t.getIndustryCode());
        return TenantProfile.of(t, industryName);
    }
}
