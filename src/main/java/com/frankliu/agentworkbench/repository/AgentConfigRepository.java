package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.AgentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentConfigRepository extends JpaRepository<AgentConfig, Long> {

    List<AgentConfig> findByProvider(AgentProvider provider);
}
