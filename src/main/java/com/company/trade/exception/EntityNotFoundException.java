package com.company.trade.exception;

/**
 * Deal 서비스에서 특정 엔티티 (예: Deal, Ticket)를 찾을 수 없을 때 발생하는 예외.
 * RuntimeException을 상속받아 Unchecked Exception으로 처리합니다.
 */
public class EntityNotFoundException extends RuntimeException {

    // 💡 에러 메시지를 받는 기본 생성자
    public EntityNotFoundException(String message) {
        super(message);
    }

    // 💡 원인이 된 다른 예외를 포함하는 생성자
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}