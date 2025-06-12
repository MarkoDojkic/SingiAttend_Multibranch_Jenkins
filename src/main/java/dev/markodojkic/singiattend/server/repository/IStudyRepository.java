package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Study;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IStudyRepository extends MongoRepository<Study, String> {}
