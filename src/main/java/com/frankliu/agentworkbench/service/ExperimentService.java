package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.ExperimentDtos;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.ExperimentDomain;
import com.frankliu.agentworkbench.domain.ExperimentStatus;
import com.frankliu.agentworkbench.repository.ExperimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ExperimentService {

    private final ExperimentRepository repository;

    public ExperimentService(ExperimentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ExperimentDtos.Response> list(ExperimentDomain domain, ExperimentStatus status) {
        List<Experiment> experiments;
        if (domain != null && status != null) {
            experiments = repository.findByDomainAndStatus(domain, status);
        } else if (domain != null) {
            experiments = repository.findByDomain(domain);
        } else if (status != null) {
            experiments = repository.findByStatus(status);
        } else {
            experiments = repository.findAll();
        }
        return experiments.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExperimentDtos.Response get(Long id) {
        return toResponse(getEntity(id));
    }

    public ExperimentDtos.Response create(ExperimentDtos.Request request) {
        Experiment experiment = new Experiment();
        applyRequest(experiment, request);
        return toResponse(repository.save(experiment));
    }

    public ExperimentDtos.Response update(Long id, ExperimentDtos.Request request) {
        Experiment experiment = getEntity(id);
        applyRequest(experiment, request);
        experiment.setUpdatedAt(Instant.now());
        return toResponse(repository.save(experiment));
    }

    public void delete(Long id) {
        Experiment experiment = getEntity(id);
        repository.delete(experiment);
    }

    @Transactional(readOnly = true)
    public Experiment getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Experiment not found: " + id));
    }

    private void applyRequest(Experiment experiment, ExperimentDtos.Request request) {
        experiment.setName(request.name());
        experiment.setDomain(request.domain());
        experiment.setDescription(request.description());
        experiment.setDatasetName(request.datasetName());
        experiment.setBaselineModel(request.baselineModel());
        experiment.setStatus(request.status() == null ? ExperimentStatus.DRAFT : request.status());
    }

    private ExperimentDtos.Response toResponse(Experiment experiment) {
        return new ExperimentDtos.Response(
                experiment.getId(),
                experiment.getName(),
                experiment.getDomain(),
                experiment.getDescription(),
                experiment.getDatasetName(),
                experiment.getBaselineModel(),
                experiment.getStatus(),
                experiment.getCreatedAt(),
                experiment.getUpdatedAt()
        );
    }
}
