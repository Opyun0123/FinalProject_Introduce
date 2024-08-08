package com.kh.last.service;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.kh.last.model.vo.Movie;
import com.kh.last.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final AmazonS3 amazonS3;
    private final MovieRepository movieRepository;

    @Value("${aws.s3.bucketName}")
    private String awsS3BucketName;

    public Movie uploadMovie(MultipartFile file, MultipartFile thumbnail, String title, String director, String cast,
            int releaseYear, String synopsis, float rating, String tags) throws IOException {
        String videoKey = file.getOriginalFilename();
        String thumbnailKey = thumbnail.getOriginalFilename();

        amazonS3.putObject(new PutObjectRequest(awsS3BucketName, videoKey, file.getInputStream(), null));
        amazonS3.putObject(new PutObjectRequest(awsS3BucketName, thumbnailKey, thumbnail.getInputStream(), null));

        String videoUrl = amazonS3.getUrl(awsS3BucketName, videoKey).toString();
        String thumbnailUrl = amazonS3.getUrl(awsS3BucketName, thumbnailKey).toString();
        
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDirector(director);
        movie.setReleaseYear(releaseYear);
        movie.setUrl(videoUrl);
        movie.setThumbnailUrl(thumbnailUrl);
        movie.setRating(rating); // 기본값으로 0.0 설정
        movie.setGenre(""); // 기본값으로 빈 문자열 설정

        // JSON 문자열로 변환하여 설정
        movie.setTagList(Arrays.asList(tags.split(",")));
        movie.setCastList(Arrays.asList(cast.split(",")));

        return movieRepository.save(movie);
    }

    public List<Movie> findAllMovies() {
        return movieRepository.findAll();
    }

    public List<Movie> findMoviesByGenre(String genre) {
        return movieRepository.findByGenre(genre);
    }
    
    public List<Movie> findMoviesByTag(String tag) {
        return movieRepository.findByTagsContaining(tag);
    }

    public List<Movie> findMoviesByCast(String cast) {
        return movieRepository.findByCastContaining(cast);
    }
}
