package com.example.common.repository;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    default T findByIdOrElseThrow(ID id) {
        return findById(id).orElseThrow(() ->
            new com.example.global.exception.BusinessException(
                com.example.global.response.response.ResponseCode.NOT_FOUND, "해당 Entity를 찾을 수 없습니다. id = " + id)
        );
    }

}