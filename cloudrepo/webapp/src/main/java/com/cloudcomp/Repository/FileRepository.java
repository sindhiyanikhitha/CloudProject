package com.cloudcomp.Repository;

import com.cloudcomp.Pojo.File;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Transactional
public interface FileRepository extends JpaRepository<File,Long> {
    File findById(UUID id);
    Long deleteById(UUID id);
    File findByBillid(UUID id);
    Long deleteByBillid(UUID id);
}
