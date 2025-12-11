// src/main/java/com/company/trade/dto/NicepayWebhookRequest.java (권장 수정안)

package com.company.trade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
// ... (나머지 import)

@Getter
@Setter
@ToString
public class NicepayWebhookRequest {

    // ** 결과 상태 **
    @JsonProperty("ResultCode")
    private String resultCode;
    @JsonProperty("ResultMsg")
    private String resultMsg;

    // ** 거래 정보 **
    @JsonProperty("MID")
    private String mid;
    @JsonProperty("TID")
    private String tid;

    @JsonProperty("Moid") // DTO에서는 orderId로 사용하지만, NICEPAY에서는 Moid임.
    private String orderId;

    @JsonProperty("Amt")
    private String amount;          // NICEPAY는 금액을 String으로 보내는 경우가 많음

    // ** 결제 상세 정보 **
    @JsonProperty("ApprovalNum")
    private String approvalNum;
    @JsonProperty("PayMethod")
    private String payMethod;
    // ...
}