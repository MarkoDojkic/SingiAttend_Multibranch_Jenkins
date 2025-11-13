package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Staff;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
@Repository
public interface IStaffRepository extends MongoRepository<Staff, String> {
    @Query(value="{'email': {$regex : ?0, $options: 'i'}}")
    Optional<Staff> getByEmail(String email);

    List<Staff> findAllByRole(String role);
}
