package com.smallerp.account;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndustryPackRepository extends JpaRepository<IndustryPack, String> {

    default Map<String, String> namesByCode() {
        return findAll().stream().collect(Collectors.toMap(IndustryPack::getCode, IndustryPack::getName));
    }
}
