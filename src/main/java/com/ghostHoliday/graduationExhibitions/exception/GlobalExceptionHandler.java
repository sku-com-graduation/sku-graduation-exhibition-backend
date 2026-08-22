package com.ghostHoliday.graduationExhibitions.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.NoSuchElementException;

/**
 * 처리하지 못한 예외를 응답으로 바꾸는 곳.
 *
 * <p>고친 점은 두 가지다.
 *
 * <p><b>상태 코드를 실제 원인에 맞춘다.</b> 예전에는 {@code Exception} 하나만 잡아 전부 500 으로
 * 내려보냈다. 없는 데이터를 조회해도, 잘못된 값을 보내도, 권한이 없어도 전부 500 이라
 * 클라이언트는 "서버가 죽었다"와 "내가 잘못 보냈다"를 구분할 수 없었다.
 *
 * <p><b>내부 사정을 밖으로 내보내지 않는다.</b> 예전에는 {@code ex.getMessage()} 를 그대로 본문에
 * 실었다. 그 안에는 쿼리·파일 경로·라이브러리 이름이 섞여 나올 수 있다. 이제 상세 내용은 로그로만
 * 남기고, 응답에는 사용자에게 보여도 되는 문장만 담는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("UNAUTHORIZED", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", "권한이 없습니다."));
    }

    /** 조회 대상이 없는 경우. {@code Optional.get()} 이 터지는 경로도 여기로 모인다. */
    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        log.info("조회 대상 없음: {}", ex.toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "요청한 데이터를 찾을 수 없습니다."));
    }

    /** 클라이언트가 잘못 보낸 경우 — 이건 서버 잘못이 아니므로 400 이어야 한다. */
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        log.info("잘못된 요청: {}", ex.toString());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("BAD_REQUEST", "요청 값을 확인해 주세요."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of("FILE_TOO_LARGE", "업로드할 수 있는 크기를 넘었습니다."));
    }

    /**
     * 위에서 걸러지지 않은 것들. 여기까지 왔다는 건 우리가 예상하지 못한 상황이라는 뜻이므로
     * 스택 트레이스를 <b>로그에</b> 남긴다 — 응답에는 담지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("처리하지 못한 예외", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "예기치 않은 오류가 발생했습니다."));
    }
}
