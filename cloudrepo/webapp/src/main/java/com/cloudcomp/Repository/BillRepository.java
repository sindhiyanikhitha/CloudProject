package com.cloudcomp.Repository;

import com.cloudcomp.Pojo.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Transactional
public interface BillRepository extends JpaRepository<Bill,Long> {

    List<Bill> findByOwnerId(UUID id);
    Long deleteById(UUID id);
    Bill findById(UUID id);
}
