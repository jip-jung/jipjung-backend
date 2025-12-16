package com.jipjung.project.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지출 카테고리 enum
 */
@Getter
@RequiredArgsConstructor
public enum SpendingCategory {
    FOOD("식비"),
    TRANSPORT("교통비"),
    SHOPPING("쇼핑"),
    ENTERTAINMENT("여가/문화"),
    LIVING("생활비"),
    ETC("기타");

    private final String label;

    public static SpendingCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return ETC;
        }
        try {
            return SpendingCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ETC;
        }
    }
}
