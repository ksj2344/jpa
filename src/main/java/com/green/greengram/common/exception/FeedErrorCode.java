package com.green.greengram.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {
    FAIL_TO_REG(HttpStatus.INTERNAL_SERVER_ERROR,"피드 등록 에러")
    , FAIL_TO_DEL(HttpStatus.BAD_REQUEST, "피드 삭제에 실패하였습니다.")
    , FAIL_TO_DEL_COMMENT(HttpStatus.BAD_REQUEST, "피드 댓글삭제에 실패하셨습니다.")
    ;
    
    private final HttpStatus httpStatus;
    private final String message;
}
