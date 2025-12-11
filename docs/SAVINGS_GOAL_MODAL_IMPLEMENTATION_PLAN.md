# 저축 목표 모달 및 테마 선택 기능 개선 계획

> **문서 갱신**: 팀원이 기본 드림홈 설정 기능을 이미 구현했으므로, 이 문서는 **추가 개선 사항**에 집중합니다.

---

## 현재 구현 상태 ✅

| 기능 | 상태 | 파일 |
|------|------|------|
| 드림홈 설정 API | ✅ 구현됨 | `DreamHomeController.java`, `DreamHomeService.java` |
| 저축 기록 API | ✅ 구현됨 | `DreamHomeController.java`, `DreamHomeService.java` |
| 드림홈 설정 모달 (기본) | ✅ 구현됨 | `DreamHomeSetModal.vue` |
| 드림홈 Store/Service | ✅ 구현됨 | `dreamHomeStore.js`, `dreamHomeService.js` |
| PropertyActions 연동 | ✅ 구현됨 | `PropertyActions.vue` |
| 경험치/레벨 시스템 | ✅ 구현됨 | `DreamHomeService.java` |
| 저축 기록 모달 | ✅ 구현됨 | `SavingInputModal.vue` |

---

## 개선 필요 사항 🚀

### 1. DSR 기반 목표 금액 계산 (미구현)

> [!IMPORTANT]
> 현재 모달은 단순히 `매물가 × 30%`로 계약금을 계산합니다. 
> DSR 기반 최대 대출 가능액을 고려한 필요 자기자본 계산이 필요합니다.

**현재 동작:**
```javascript
// DreamHomeSetModal.vue
formData.value.targetAmount = Math.ceil(props.property.price * 0.3)
```

**개선 목표:**
- 필요 자기자본 = 매물가 - 최대 대출 가능액
- DSR 등급(SAFE/CAUTION/DANGER) 표시
- **백엔드에서 DSR 계산 및 검증** (클라이언트 조작 방지)

---

### 2. 테마 선택 기능 (미구현)

> [!IMPORTANT]
> `house_theme`, `theme_asset` 테이블은 DB에 존재하지만, 
> 테마 선택 UI와 API가 구현되지 않았습니다.

**필요 작업:**
- 테마 목록 조회 API (`GET /api/themes`)
- **테마 ID 존재/활성 여부 검증** (잘못된 ID 저장 방지)
- 모달에 테마 선택 카드 UI 추가
- 드림홈 설정 시 선택된 테마 저장

---

### 3. IsometricRoomHero 동적 테마 로딩 (미구현)

> 현재 `/phase7.svg` 고정 로딩 → 사용자 선택 테마 기반 동적 로딩 필요

> [!CAUTION]
> 대시보드 응답에 `themeAssetUrl`이 포함되어야 프론트엔드에서 동적 로딩이 가능합니다.
> 현재 `DashboardResponse`에 이 필드가 없으므로 확장이 필요합니다.

---

## 설계 결정 사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 테마 저장 위치 | `user.selected_theme_id` (사용자 단위) | 복수 드림홈은 당장 요구사항 아님. 필요시 마이그레이션 |
| DSR 정보 출처 | 백엔드가 직접 계산 | 클라이언트 조작 방지, API 콜 감소 |
| DSR 검증 방식 | 경고 표시 후 저장 허용 | 사용자 재량 존중, 강제 차단은 UX 저하 |
| GCS 접근 방식 | 공개 URL (`allUsers:objectViewer`) | 별도 인증 불필요, `fetch()` 직접 호출 |
| API 경로 규칙 | `/themes` (apiClient가 `/api` prefix 처리) | 기존 엔드포인트와 일관성 유지 |

---

## 상세 구현 계획

### 1단계: 테마 API 구현 (Backend)

#### [NEW] `ThemeController.java`
- 경로: `src/main/java/com/jipjung/project/controller/ThemeController.java`

```java
@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
@Tag(name = "테마", description = "하우스 테마 관리 API")
public class ThemeController {
    
    private final HouseThemeMapper houseThemeMapper;
    
    @Operation(summary = "활성 테마 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HouseTheme>>> getActiveThemes() {
        List<HouseTheme> themes = houseThemeMapper.findAllActive();
        return ApiResponse.success(themes);
    }
}
```

#### [NEW] `HouseThemeMapper.java`
- 경로: `src/main/java/com/jipjung/project/repository/HouseThemeMapper.java`

```java
@Mapper
public interface HouseThemeMapper {
    
    /**
     * 활성 테마 목록 조회
     */
    List<HouseTheme> findAllActive();
    
    /**
     * 테마 ID로 조회 (활성 여부 포함)
     */
    HouseTheme findById(@Param("themeId") Integer themeId);
    
    /**
     * 테마 ID가 존재하고 활성 상태인지 확인
     */
    boolean existsAndActive(@Param("themeId") Integer themeId);
}
```

#### [NEW] `HouseThemeMapper.xml`
- 경로: `src/main/resources/mapper/HouseThemeMapper.xml`

```xml
<mapper namespace="com.jipjung.project.repository.HouseThemeMapper">
    <select id="findAllActive" resultType="com.jipjung.project.domain.HouseTheme">
        SELECT theme_id, theme_code, theme_name, is_active, created_at, updated_at
        FROM house_theme
        WHERE is_active = TRUE AND is_deleted = FALSE
        ORDER BY theme_id
    </select>
    
    <select id="findById" resultType="com.jipjung.project.domain.HouseTheme">
        SELECT theme_id, theme_code, theme_name, is_active, created_at, updated_at
        FROM house_theme
        WHERE theme_id = #{themeId} AND is_deleted = FALSE
    </select>
    
    <select id="existsAndActive" resultType="boolean">
        SELECT COUNT(*) > 0
        FROM house_theme
        WHERE theme_id = #{themeId} AND is_active = TRUE AND is_deleted = FALSE
    </select>
</mapper>
```

---

### 2단계: 테마 선택 통합 + 검증 (Backend)

#### [MODIFY] `DreamHomeSetRequest.java`
**변경 내용:** 테마 ID 필드 추가 + 양수 검증

```diff
+import jakarta.validation.constraints.Positive;

 public record DreamHomeSetRequest(
         @Schema(description = "아파트 고유 ID", example = "11410-61", requiredMode = Schema.RequiredMode.REQUIRED)
         @NotBlank(message = "아파트 코드는 필수입니다")
         String aptSeq,

         @Schema(description = "목표 금액 (원 단위)", example = "300000000", requiredMode = Schema.RequiredMode.REQUIRED)
         @NotNull(message = "목표 금액은 필수입니다")
         @Min(value = 1, message = "목표 금액은 1원 이상이어야 합니다")
         Long targetAmount,

         @Schema(description = "목표 달성일 (YYYY-MM-DD)", example = "2028-12-31", requiredMode = Schema.RequiredMode.REQUIRED)
         @NotNull(message = "목표 달성일은 필수입니다")
         @Future(message = "목표 달성일은 미래 날짜여야 합니다")
         LocalDate targetDate,

         @Schema(description = "월 목표 저축액 (원 단위, 선택)", example = "2500000")
         @Min(value = 0, message = "월 목표 저축액은 0 이상이어야 합니다")
-        Long monthlyGoal
+        Long monthlyGoal,
+
+        @Schema(description = "선택한 테마 ID (선택, 양수만 허용)", example = "1")
+        @Positive(message = "테마 ID는 양수여야 합니다")
+        Integer themeId
 ) {}
```

#### [MODIFY] `DreamHomeService.java`
**변경 내용:** 테마 존재/활성 검증 로직 추가

```java
// 필드 추가
private final HouseThemeMapper houseThemeMapper;

// setDreamHome 메서드 내부에 추가
@Transactional
public DreamHomeSetResponse setDreamHome(Long userId, DreamHomeSetRequest request) {
    // 기존 로직...
    
    // 테마 선택 시 존재/활성 여부 검증
    if (request.themeId() != null) {
        validateAndSaveTheme(userId, request.themeId());
    }
    
    // 기존 로직...
}

/**
 * 테마 ID 검증 후 사용자에게 저장
 * @throws BusinessException 테마가 존재하지 않거나 비활성 상태인 경우
 */
private void validateAndSaveTheme(Long userId, Integer themeId) {
    HouseTheme theme = houseThemeMapper.findById(themeId);
    
    if (theme == null) {
        throw new BusinessException(ErrorCode.THEME_NOT_FOUND, 
            "테마를 찾을 수 없습니다: " + themeId);
    }
    
    if (!Boolean.TRUE.equals(theme.getIsActive())) {
        throw new BusinessException(ErrorCode.THEME_NOT_ACTIVE, 
            "비활성화된 테마입니다: " + themeId);
    }
    
    userMapper.updateSelectedTheme(userId, themeId);
}
```

#### [NEW] `ErrorCode.java` 추가 항목

```java
// 테마 관련 에러 코드 추가
THEME_NOT_FOUND(HttpStatus.NOT_FOUND, "THEME_001", "테마를 찾을 수 없습니다"),
THEME_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "THEME_002", "비활성화된 테마입니다"),
```

#### [MODIFY] `UserMapper.java`
**변경 내용:** 테마 업데이트 메서드 추가

```java
/**
 * 사용자의 선택 테마 업데이트
 */
@Update("UPDATE user SET selected_theme_id = #{themeId}, updated_at = NOW() WHERE user_id = #{userId}")
int updateSelectedTheme(@Param("userId") Long userId, @Param("themeId") Integer themeId);
```

---

### 3단계: 대시보드 응답에 테마 에셋 URL 포함 (Backend)

> [!IMPORTANT]
> 프론트엔드의 `IsometricRoomHero.vue`가 동적으로 테마 SVG를 로드하려면,
> 대시보드 응답에 현재 레벨에 해당하는 테마 에셋 URL이 포함되어야 합니다.

#### [MODIFY] `DashboardResponse.java`
**변경 내용:** 테마 에셋 정보 추가

```diff
 public record DashboardResponse(
         // 기존 필드들...
         UserInfo user,
         GoalSection goal,
-        GrowthSection growth
+        GrowthSection growth,
+        ThemeAssetInfo themeAsset
 ) {
+    /**
+     * 현재 적용 중인 테마 에셋 정보
+     */
+    @Schema(description = "현재 테마 에셋")
+    public record ThemeAssetInfo(
+            @Schema(description = "선택된 테마 ID", example = "1")
+            Integer themeId,
+            
+            @Schema(description = "테마 코드", example = "MODERN")
+            String themeCode,
+            
+            @Schema(description = "테마명", example = "모던 하우스")
+            String themeName,
+            
+            @Schema(description = "현재 레벨에 해당하는 이미지 URL", example = "https://storage.googleapis.com/jipjung-assets/themes/modern/phase3.svg")
+            String imageUrl
+    ) {}
 }
```

#### [MODIFY] `DashboardService.java` (또는 해당 서비스)
**변경 내용:** 테마 에셋 조회 로직 추가

```java
// 필드 추가
private final ThemeAssetMapper themeAssetMapper;

// getDashboard 메서드 내부
public DashboardResponse getDashboard(Long userId) {
    User user = userMapper.findById(userId);
    // 기존 로직...
    
    // 테마 에셋 조회
    ThemeAssetInfo themeAsset = resolveThemeAsset(user);
    
    return new DashboardResponse(
        userInfo,
        goalSection,
        growthSection,
        themeAsset  // 추가
    );
}

/**
 * 사용자의 현재 레벨에 해당하는 테마 에셋 조회
 */
private ThemeAssetInfo resolveThemeAsset(User user) {
    Integer themeId = user.getSelectedThemeId();
    Integer level = user.getCurrentLevel() != null ? user.getCurrentLevel() : 1;
    
    // 선택된 테마가 없으면 기본 테마(1) 사용
    if (themeId == null) {
        themeId = 1;
    }
    
    ThemeAsset asset = themeAssetMapper.findByThemeIdAndLevel(themeId, level);
    HouseTheme theme = houseThemeMapper.findById(themeId);
    
    if (asset == null || theme == null) {
        // 기본 에셋 반환
        return new ThemeAssetInfo(1, "MODERN", "모던 하우스", "/phase7.svg");
    }
    
    return new ThemeAssetInfo(
        theme.getThemeId(),
        theme.getThemeCode(),
        theme.getThemeName(),
        asset.getImageUrl()
    );
}
```

#### [MODIFY] `ThemeAssetMapper.java`
**변경 내용:** 레벨별 에셋 조회 메서드 (이미 존재할 수 있음, 확인 필요)

```java
@Mapper
public interface ThemeAssetMapper {
    // 기존 메서드...
    
    /**
     * 테마 ID와 레벨로 에셋 조회
     */
    ThemeAsset findByThemeIdAndLevel(@Param("themeId") Integer themeId, @Param("level") Integer level);
}
```

#### [MODIFY] `ThemeAssetMapper.xml`

```xml
<select id="findByThemeIdAndLevel" resultType="com.jipjung.project.domain.ThemeAsset">
    SELECT asset_id, theme_id, level, image_url, created_at, updated_at
    FROM theme_asset
    WHERE theme_id = #{themeId} AND level = #{level} AND is_deleted = FALSE
</select>
```

---

### 4단계: DSR 백엔드 계산 및 검증 (Backend)

> [!NOTE]
> DSR 정보는 백엔드에서 직접 계산합니다. 클라이언트가 조작할 수 없도록 하되,
> 사용자가 원하는 금액을 입력할 수 있도록 **경고만 표시**하고 저장은 허용합니다.

#### [MODIFY] `DreamHomeSetResponse.java`
**변경 내용:** DSR 기반 권장 금액 + 경고 메시지 추가

```diff
 public record DreamHomeSetResponse(
-        DreamHomeInfo dreamHome
+        DreamHomeInfo dreamHome,
+        DsrGuidance dsrGuidance
 ) {
+    /**
+     * DSR 기반 가이드
+     */
+    @Schema(description = "DSR 기반 안내 (목표 금액이 권장 금액보다 낮을 경우 경고)")
+    public record DsrGuidance(
+            @Schema(description = "DSR 등급 (SAFE/CAUTION/DANGER)", example = "SAFE")
+            String dsrGrade,
+            
+            @Schema(description = "최대 대출 가능액 (원)", example = "500000000")
+            Long maxLoanAmount,
+            
+            @Schema(description = "권장 목표 금액 (필요 자기자본, 원)", example = "350000000")
+            Long recommendedTargetAmount,
+            
+            @Schema(description = "경고 메시지 (목표 금액이 권장보다 낮을 경우)", example = "목표 금액이 권장 금액보다 낮습니다.")
+            String warningMessage
+    ) {}
 }
```

#### [MODIFY] `DreamHomeService.java`
**변경 내용:** DSR 계산 + 경고 로직 추가

```java
// 필드 추가
private final DsrService dsrService;

@Transactional
public DreamHomeSetResponse setDreamHome(Long userId, DreamHomeSetRequest request) {
    // 아파트 조회
    Apartment apartment = findApartmentOrThrow(request.aptSeq());
    Long propertyPrice = resolveLatestDealPrice(apartment);
    
    // DSR 계산
    DsrGuidance dsrGuidance = calculateDsrGuidance(userId, propertyPrice, request.targetAmount());
    
    // 테마 검증 및 저장
    if (request.themeId() != null) {
        validateAndSaveTheme(userId, request.themeId());
    }
    
    // 기존 드림홈 처리 로직...
    
    return DreamHomeSetResponse.from(dreamHome, apartment, propertyPrice, dsrGuidance);
}

/**
 * DSR 기반 가이드 계산
 */
private DsrGuidance calculateDsrGuidance(Long userId, Long propertyPrice, Long userTargetAmount) {
    // DSR 정보 조회
    DsrResult dsr = dsrService.calculateDsr(userId);
    
    String dsrGrade = dsr.getGrade(); // SAFE, CAUTION, DANGER
    Long maxLoanAmount = dsr.getMaxLoanAmount();
    
    // 필요 자기자본 = 매물가 - 최대 대출 가능액
    Long recommendedTarget = Math.max(0, propertyPrice - maxLoanAmount);
    
    // 경고 메시지 (목표 금액 < 권장 금액인 경우)
    String warning = null;
    if (userTargetAmount < recommendedTarget) {
        warning = String.format(
            "입력하신 목표 금액(%,d원)이 DSR 기반 권장 금액(%,d원)보다 낮습니다. " +
            "대출 한도를 초과할 수 있습니다.",
            userTargetAmount, recommendedTarget
        );
    }
    
    return new DsrGuidance(dsrGrade, maxLoanAmount, recommendedTarget, warning);
}
```

---

### 5단계: 프론트엔드 테마 서비스 (Frontend)

#### [NEW] `themeService.js`
- 경로: `src/api/services/themeService.js`

```javascript
/**
 * Theme Service
 * 
 * 테마 관련 API 호출을 담당하는 서비스 레이어.
 * 
 * @module api/services/themeService
 */

import apiClient from '@/api/client'
import { THEME_ENDPOINTS } from '@/api/endpoints'

export const themeService = {
    /**
     * 활성 테마 목록 조회
     * @returns {Promise<Array>} 테마 목록
     */
    async getActiveThemes() {
        const response = await apiClient.get(THEME_ENDPOINTS.LIST)
        return response.data.data
    }
}
```

#### [MODIFY] `endpoints.js`
**변경 내용:** 테마 엔드포인트 추가

```javascript
/**
 * 테마 API 엔드포인트
 * apiClient.baseURL = '/api' 이므로 prefix 없이 정의
 */
export const THEME_ENDPOINTS = {
    LIST: '/themes'
}
```

---

### 6단계: 모달 UI 개선 (Frontend)

#### [MODIFY] `DreamHomeSetModal.vue`
**주요 변경 사항:**

1. 테마 선택 카드 UI 추가
2. 테마 ID를 request에 포함
3. DSR 경고 메시지 표시 (응답에서 수신)

```vue
<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="isOpen" class="modal-overlay" @click="handleOverlayClick">
        <div class="modal-container" @click.stop>
          <!-- Header -->
          <div class="modal-header">
            <h2 class="modal-title">🏠 드림홈 설정</h2>
            <button class="close-button" @click="closeModal" :disabled="isSubmitting">✕</button>
          </div>

          <!-- Property Info -->
          <div class="property-info">
            <h3 class="property-name">{{ property?.title || '아파트' }}</h3>
            <p class="property-location">{{ property?.sido }} {{ property?.sigungu }}</p>
            <p class="property-price">최신 거래가: {{ formatPrice(property?.price || 0) }}</p>
          </div>

          <!-- Form -->
          <form class="modal-form" @submit.prevent="handleSubmit">
            <!-- Target Amount -->
            <div class="form-group">
              <label class="form-label">목표 금액 (필요 계약금)</label>
              <div class="input-with-calc">
                <div class="input-wrapper">
                  <input
                    v-model.number="formData.targetAmount"
                    type="number"
                    class="form-input"
                    placeholder="목표 금액 입력"
                    min="1"
                    required
                    :disabled="isSubmitting"
                  />
                  <span class="input-suffix">만원</span>
                </div>
                <button type="button" class="calc-button" @click="calcDownPayment" :disabled="isSubmitting">
                  30% 자동계산
                </button>
              </div>
            </div>

            <!-- Target Date -->
            <div class="form-group">
              <label class="form-label">목표 달성일</label>
              <input
                v-model="formData.targetDate"
                type="date"
                class="form-input"
                :min="minDate"
                required
                :disabled="isSubmitting"
              />
            </div>

            <!-- Monthly Goal -->
            <div class="form-group">
              <label class="form-label">월 목표 저축액</label>
              <div class="input-wrapper">
                <input
                  v-model.number="formData.monthlyGoal"
                  type="number"
                  class="form-input"
                  placeholder="월 저축 목표"
                  min="1"
                  required
                  :disabled="isSubmitting"
                />
                <span class="input-suffix">만원</span>
              </div>
            </div>

            <!-- 🆕 Theme Selection -->
            <div class="form-group" v-if="themes.length > 0">
              <label class="form-label">집 테마 선택</label>
              <div class="theme-grid">
                <button
                  v-for="theme in themes"
                  :key="theme.themeId"
                  type="button"
                  class="theme-card"
                  :class="{ selected: formData.themeId === theme.themeId }"
                  @click="selectTheme(theme.themeId)"
                  :disabled="isSubmitting"
                >
                  <img :src="getThemeThumbnail(theme.themeCode)" :alt="theme.themeName" />
                  <span class="theme-name">{{ theme.themeName }}</span>
                </button>
              </div>
            </div>

            <!-- DSR Warning (shown after submit if applicable) -->
            <div v-if="dsrWarning" class="dsr-warning">
              ⚠️ {{ dsrWarning }}
            </div>

            <!-- Submit Button -->
            <button type="submit" class="submit-button" :disabled="isSubmitting || !isFormValid">
              <span v-if="isSubmitting" class="spinner"></span>
              {{ isSubmitting ? '설정 중...' : '드림홈 설정하기' }}
            </button>
          </form>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDreamHomeStore } from '@/stores/dreamHomeStore'
import { useToast } from '@/composables/useToast'
import { themeService } from '@/api/services/themeService'

// ... 기존 props, emits ...

// 테마 관련 state
const themes = ref([])
const dsrWarning = ref(null)

// formData에 themeId 추가
const formData = ref({
  targetAmount: null,
  targetDate: '',
  monthlyGoal: null,
  themeId: null  // 🆕
})

// 테마 목록 로드
onMounted(async () => {
  try {
    themes.value = await themeService.getActiveThemes()
    // 기본 테마 선택 (첫 번째)
    if (themes.value.length > 0) {
      formData.value.themeId = themes.value[0].themeId
    }
  } catch (error) {
    console.error('테마 목록 로드 실패:', error)
  }
})

// 테마 선택
const selectTheme = (themeId) => {
  formData.value.themeId = themeId
}

// 테마 썸네일 URL
const getThemeThumbnail = (themeCode) => {
  // 로컬 또는 GCS URL
  return `/themes/${themeCode.toLowerCase()}/thumbnail.png`
}

// 폼 제출 수정
const handleSubmit = async () => {
  if (!isFormValid.value) {
    showError('모든 필드를 입력해주세요')
    return
  }

  isSubmitting.value = true
  dsrWarning.value = null

  try {
    const response = await dreamHomeStore.setDreamHome({
      aptSeq: props.property?.aptSeq || props.property?.id,
      targetAmount: formData.value.targetAmount,
      targetDate: formData.value.targetDate,
      monthlyGoal: formData.value.monthlyGoal,
      themeId: formData.value.themeId  // 🆕
    })

    // DSR 경고 메시지 표시
    if (response.dsrGuidance?.warningMessage) {
      dsrWarning.value = response.dsrGuidance.warningMessage
      // 경고만 표시하고 저장은 완료됨
    }

    showSuccess(`"${props.property?.title}"을(를) 드림홈으로 설정했습니다!`)
    emit('success', response)
    closeModal()
    router.push('/')
  } catch (error) {
    showError(error.message || '드림홈 설정에 실패했습니다')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
/* 🆕 테마 선택 스타일 */
.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}

.theme-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.75rem;
  border-radius: 12px;
  border: 2px solid transparent;
  background: rgba(var(--brand-accent-rgb, 255, 107, 61), 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-card.selected {
  border-color: var(--brand-accent, #ff6b3d);
  background: rgba(var(--brand-accent-rgb, 255, 107, 61), 0.12);
}

.theme-card img {
  width: 60px;
  height: 60px;
  object-fit: contain;
  border-radius: 8px;
}

.theme-name {
  margin-top: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--showroom-text-day, #5D4037);
}

/* DSR 경고 */
.dsr-warning {
  padding: 0.75rem 1rem;
  background: rgba(255, 193, 7, 0.15);
  border: 1px solid rgba(255, 193, 7, 0.4);
  border-radius: 12px;
  color: #856404;
  font-size: 0.875rem;
  line-height: 1.4;
}

html[data-theme="night"] .dsr-warning {
  background: rgba(255, 193, 7, 0.1);
  color: #ffc107;
}
</style>
```

---

### 7단계: 동적 테마 로딩 (Frontend)

#### [MODIFY] `IsometricRoomHero.vue`
**변경 내용:** authStore에서 테마 에셋 URL 사용

```diff
+import { useAuthStore } from '@/stores/authStore'

+const authStore = useAuthStore()

 async function loadHouseSvg() {
   if (houseSvgMarkup.value || svgError.value) return
   try {
-    const res = await fetch('/phase7.svg')
+    // 대시보드 응답에서 받은 테마 에셋 URL 사용
+    const themeUrl = authStore.themeAsset?.imageUrl || '/phase7.svg'
+    const res = await fetch(themeUrl)
     if (!res.ok) throw new Error('집 SVG를 불러오지 못했습니다')
     houseSvgMarkup.value = await res.text()
```

#### [MODIFY] `authStore.js`
**변경 내용:** 테마 에셋 정보 저장

```javascript
// state 추가
const themeAsset = ref(null)

// 대시보드 데이터 수신 시 업데이트
function setDashboardData(data) {
    // 기존 로직...
    
    // 테마 에셋 저장
    if (data.themeAsset) {
        themeAsset.value = data.themeAsset
    }
}

// return에 추가
return {
    // ...
    themeAsset,
    // ...
}
```

---

## 파일 변경 요약

| 레이어 | 파일 | 변경 유형 | 우선순위 |
|--------|------|-----------|----------|
| Backend | `ThemeController.java` | NEW | 🔴 High |
| Backend | `HouseThemeMapper.java` | NEW | 🔴 High |
| Backend | `HouseThemeMapper.xml` | NEW | 🔴 High |
| Backend | `ErrorCode.java` | MODIFY | 🔴 High |
| Backend | `DreamHomeSetRequest.java` | MODIFY | 🔴 High |
| Backend | `DreamHomeService.java` | MODIFY | 🔴 High |
| Backend | `DreamHomeSetResponse.java` | MODIFY | 🔴 High |
| Backend | `DashboardResponse.java` | MODIFY | 🔴 High |
| Backend | `DashboardService.java` | MODIFY | 🔴 High |
| Backend | `ThemeAssetMapper.java` | MODIFY | 🔴 High |
| Backend | `ThemeAssetMapper.xml` | MODIFY | 🔴 High |
| Backend | `UserMapper.java` | MODIFY | 🔴 High |
| Frontend | `themeService.js` | NEW | 🔴 High |
| Frontend | `endpoints.js` | MODIFY | 🔴 High |
| Frontend | `DreamHomeSetModal.vue` | MODIFY | 🔴 High |
| Frontend | `authStore.js` | MODIFY | 🟡 Medium |
| Frontend | `IsometricRoomHero.vue` | MODIFY | 🟡 Medium |

---

## GCS 테마 이미지 설정 가이드

> 테마 이미지 업로드는 사용자가 직접 수행합니다.
> **공개 읽기 권한**으로 설정하여 별도 인증 없이 `fetch()` 가능하도록 합니다.

### GCS 버킷 설정

```bash
# 버킷 생성 (리전: 서울)
gsutil mb -l asia-northeast3 gs://jipjung-assets

# 공개 읽기 권한 설정
gsutil iam ch allUsers:objectViewer gs://jipjung-assets

# CORS 설정
cat > cors.json << EOF
[
  {
    "origin": ["http://localhost:5173", "https://your-domain.com"],
    "method": ["GET"],
    "responseHeader": ["Content-Type"],
    "maxAgeSeconds": 3600
  }
]
EOF
gsutil cors set cors.json gs://jipjung-assets
```

### 권장 폴더 구조

```
themes/
  modern/
    phase1.svg ~ phase7.svg
    thumbnail.png
  hanok/
    phase1.svg ~ phase7.svg
    thumbnail.png
  castle/
    phase1.svg ~ phase7.svg
    thumbnail.png
```

### theme_asset 테이블 데이터 예시

```sql
INSERT INTO theme_asset (theme_id, level, image_url) VALUES
(1, 1, 'https://storage.googleapis.com/jipjung-assets/themes/modern/phase1.svg'),
(1, 2, 'https://storage.googleapis.com/jipjung-assets/themes/modern/phase2.svg'),
(1, 7, 'https://storage.googleapis.com/jipjung-assets/themes/modern/phase7.svg'),
(2, 1, 'https://storage.googleapis.com/jipjung-assets/themes/hanok/phase1.svg');
```

---

## 검증 계획

### 백엔드 테스트 (Swagger)

1. `GET /api/themes` → 활성 테마 목록 반환 확인
2. `POST /api/dream-home` with `themeId: 1` → 성공, 테마 저장 확인
3. `POST /api/dream-home` with `themeId: 999` → 404 에러 확인
4. `POST /api/dream-home` with `themeId: -1` → 400 에러 확인 (양수 검증)
5. `GET /api/dashboard` → `themeAsset.imageUrl` 반환 확인
6. DSR 경고 테스트: 권장 금액보다 낮은 `targetAmount` 입력 → `dsrGuidance.warningMessage` 확인

### 프론트엔드 테스트

1. 매물 상세 → "내 집으로 설정" → 모달에서 테마 카드 표시 확인
2. 테마 선택 → 선택된 카드 하이라이트 확인
3. 저장 후 → 백엔드 요청에 `themeId` 포함 확인
4. DSR 경고 있을 경우 → 모달에 경고 메시지 표시 확인
5. 대시보드 → `IsometricRoomHero`에서 선택된 테마 SVG 로드 확인

---

## 구현 우선순위

1. **Phase 1**: 테마 API + 테마 선택 UI + 검증 로직 (1-2일)
2. **Phase 2**: 대시보드 응답 확장 + 동적 테마 로딩 (1일)
3. **Phase 3**: DSR 백엔드 계산 + 경고 표시 (1일)
4. **Phase 4**: GCS 이미지 업로드 + 연동 테스트 (0.5일)
