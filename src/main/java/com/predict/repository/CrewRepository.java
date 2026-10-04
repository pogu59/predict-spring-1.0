package com.predict.repository;

import com.predict.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    List<Crew> findByActiveTrueOrderByNameAsc();

    List<Crew> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
