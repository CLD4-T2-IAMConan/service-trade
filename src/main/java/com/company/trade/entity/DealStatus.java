package com.company.trade.entity;

// 거래(Deal)의 상태를 정의합니다.
public enum DealStatus {
    /** 거래 요청: 구매자가 판매자에게 거래를 요청한 상태 */
    PENDING,

    /** 거래 수락: 판매자가 요청을 수락하여 거래가 진행 중인 상태 */
    ACCEPTED,

    /** 결제 완료: 구매자가 결제를 완료한 상태 **/
    PAID,

    /** 거래 완료: 최종적으로 거래가 완료된 상태 */
    COMPLETED,

    /** 거래 취소: 거래가 만료되거나 취소된 상태 */
    CANCELED,

    /** 거래 실패: 어떤 이유로든 거래가 실패한 상태 */
    FAILED
}