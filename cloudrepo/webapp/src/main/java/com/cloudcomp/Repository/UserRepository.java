package com.cloudcomp.Repository;

import com.cloudcomp.Pojo.User;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

@Transactional
public interface UserRepository extends CrudRepository<User, Long> {
    User findByEmail(String email);
}
