package com.restaurant.saas.storage.repository;

import com.restaurant.saas.storage.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByStorageKey(String storageKey);
}
