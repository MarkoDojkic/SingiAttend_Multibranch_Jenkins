package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Staff;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface IStaffRepository extends MongoRepository<Staff, String> {
    @Query(value="{'email': {$regex : ?0, $options: 'i'}}")
    Optional<Staff> getByEmail(String email);
}
