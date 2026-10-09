package com.predict.reward;

import java.util.List;

/**
 * 교환 상품 목록(임시 고정값). 기프티콘 발송 대행사 계약 전이라 운영자가 수동으로 보내는 MVP 기준이며,
 * 상품·가격이 정해지면 테이블로 옮겨 관리자 화면에서 바꾸게 한다. 신청 기록에는 상품명·포인트를
 * 복사해 두므로(RewardExchangeRequest) 여기 값을 바꿔도 과거 기록은 그대로다.
 */
public final class RewardCatalog {

    public static final List<RewardProduct> PRODUCTS = List.of(
            new RewardProduct("ICECREAM_1000", "아이스크림 쿠폰", 1_000),
            new RewardProduct("CVS_3000", "편의점 상품권", 3_000),
            new RewardProduct("COFFEE_4500", "커피 쿠폰", 4_500),
            new RewardProduct("BAKERY_5000", "베이커리 쿠폰", 5_000));

    private RewardCatalog() {
    }

    public static RewardProduct require(String code) {
        return PRODUCTS.stream()
                .filter(product -> product.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("없는 상품이에요: " + code));
    }
}
