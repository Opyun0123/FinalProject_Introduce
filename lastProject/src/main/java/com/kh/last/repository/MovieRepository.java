package com.kh.last.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.kh.last.model.vo.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
}