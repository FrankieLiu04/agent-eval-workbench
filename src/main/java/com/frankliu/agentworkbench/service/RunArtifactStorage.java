package com.frankliu.agentworkbench.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class RunArtifactStorage {

    private final ObjectMapper objectMapper;
    private final Path root;

    public RunArtifactStorage(ObjectMapper objectMapper, @Value("${app.artifacts.root}") Path root) {
        this.objectMapper = objectMapper;
        this.root = root.toAbsolutePath().normalize();
    }

    public String store(String runId, Object artifact) {
        Path runDirectory = root.resolve(runId).normalize();
        if (!runDirectory.startsWith(root)) {
            throw new IllegalArgumentException("Invalid run id for artifact storage");
        }
        Path target = runDirectory.resolve("run.json");
        Path staged = null;
        try {
            Files.createDirectories(runDirectory);
            if (sameArtifact(target, artifact)) {
                return root.relativize(target).toString();
            }
            if (Files.exists(target)) {
                throw new IllegalStateException("A different artifact already exists for run " + runId);
            }
            staged = Files.createTempFile(runDirectory, "run-", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(staged.toFile(), artifact);
            moveIntoPlace(staged, target);
            return root.relativize(target).toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store run artifact", ex);
        } finally {
            if (staged != null) {
                try {
                    Files.deleteIfExists(staged);
                } catch (IOException ignored) {
                    // A staged file is safe to remove manually after a failed write.
                }
            }
        }
    }

    public JsonNode read(String reference) {
        if (reference == null) {
            return null;
        }
        Path artifact = root.resolve(reference).normalize();
        if (!artifact.startsWith(root) || !Files.isRegularFile(artifact)) {
            return null;
        }
        return objectMapper.readTree(artifact.toFile());
    }

    private boolean sameArtifact(Path target, Object artifact) {
        return Files.isRegularFile(target)
                && objectMapper.readTree(target.toFile()).equals(objectMapper.valueToTree(artifact));
    }

    private void moveIntoPlace(Path staged, Path target) throws IOException {
        try {
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(staged, target);
        } catch (FileAlreadyExistsException ex) {
            throw new IllegalStateException("Run artifact was written concurrently", ex);
        }
    }
}
