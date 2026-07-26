package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.domain.ReasoningMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentConfigRepository extends JpaRepository<AgentConfig, Long> {

    List<AgentConfig> findByProvider(AgentProvider provider);

    boolean existsByProviderAndModelName(AgentProvider provider, String modelName);

    Optional<AgentConfig> findFirstByProviderAndModelNameAndReasoningMode(
            AgentProvider provider,
            String modelName,
            ReasoningMode reasoningMode
    );
}
