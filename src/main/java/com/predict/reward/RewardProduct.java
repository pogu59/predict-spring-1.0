package com.predict.reward;

/** 교환할 수 있는 상품 하나. points만큼 포인트를 차감하고 운영자가 기프티콘을 보낸다. */
public record RewardProduct(String code, String name, int points) {
}
