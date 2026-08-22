package com.ghostHoliday.graduationExhibitions.exception;

/**
 * 오류 응답 형식.
 *
 * <p>예전에는 예외 메시지 문자열을 그대로 본문에 실어 보냈다. 클라이언트가 파싱하기도 어렵고,
 * 무엇보다 서버 내부 사정(쿼리·파일 경로·라이브러리 이름)이 그대로 밖으로 나갔다.
 *
 * @param code    클라이언트가 분기에 쓸 수 있는 짧은 식별자
 * @param message 사용자에게 보여도 되는 설명
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
