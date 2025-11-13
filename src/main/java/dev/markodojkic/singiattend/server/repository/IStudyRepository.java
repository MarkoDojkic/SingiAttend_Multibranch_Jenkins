package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Study;
import org.springframework.data.mongodb.repository.MongoRepository;

@SuppressWarnings("NullableProblems")
public interface IStudyRepository extends MongoRepository<Study, String> {}
