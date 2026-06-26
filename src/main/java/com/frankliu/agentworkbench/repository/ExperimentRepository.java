package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.ExperimentDomain;
import com.frankliu.agentworkbench.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByDomain(ExperimentDomain domain);

    List<Experiment> findByStatus(ExperimentStatus status);

    List<Experiment> findByDomainAndStatus(ExperimentDomain domain, ExperimentStatus status);
}
