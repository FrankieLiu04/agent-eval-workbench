package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.AgentConfigDtos;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.domain.ReasoningMode;
import com.frankliu.agentworkbench.repository.AgentConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AgentConfigService {

    private final AgentConfigRepository repository;

    public AgentConfigService(AgentConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AgentConfigDtos.Response> list(AgentProvider provider) {
        List<AgentConfig> configs = provider == null ? repository.findAll() : repository.findByProvider(provider);
        return configs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AgentConfigDtos.Response get(Long id) {
        return toResponse(getEntity(id));
    }

    public AgentConfigDtos.Response create(AgentConfigDtos.Request request) {
        AgentConfig config = new AgentConfig();
        applyRequest(config, request);
        return toResponse(repository.save(config));
    }

    public AgentConfigDtos.Response update(Long id, AgentConfigDtos.Request request) {
        AgentConfig config = getEntity(id);
        applyRequest(config, request);
        config.setUpdatedAt(Instant.now());
        return toResponse(repository.save(config));
    }

    public void delete(Long id) {
        AgentConfig config = getEntity(id);
        repository.delete(config);
    }

    @Transactional(readOnly = true)
    public AgentConfig getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agent config not found: " + id));
    }

    private void applyRequest(AgentConfig config, AgentConfigDtos.Request request) {
        config.setName(request.name());
        config.setProvider(request.provider());
        config.setModelName(request.modelName());
        config.setPromptVersion(request.promptVersion());
        config.setToolExposure(request.toolExposure());
        config.setMaxSteps(request.maxSteps());
        config.setReasoningMode(request.reasoningMode() == null ? ReasoningMode.DEFAULT : request.reasoningMode());
    }

    private AgentConfigDtos.Response toResponse(AgentConfig config) {
        return new AgentConfigDtos.Response(
                config.getId(),
                config.getName(),
                config.getProvider(),
                config.getModelName(),
                config.getPromptVersion(),
                config.getToolExposure(),
                config.getMaxSteps(),
                config.getReasoningMode(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
