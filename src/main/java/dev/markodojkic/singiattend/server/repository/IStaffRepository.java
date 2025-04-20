package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Staff;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IStaffRepository extends MongoRepository<Staff, String> {}
