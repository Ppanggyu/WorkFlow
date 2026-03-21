// Task 관련 옵션 상수
// 여러 화면(Tasks, TaskForm, TaskDetail)에서 공통으로 사용하는 값들을 모음

// 조회 범위 옵션
export const SCOPES = [
  "all",      // 전체
  "public",   // 전사
  "team",     // 팀/부서
  "created",  // 내가 생성한 업무
  "assigned", // 내가 담당인 업무
];

// 상태 옵션
export const STATUSES = [
  "TODO",        // 할 일
  "IN_PROGRESS", // 진행 중
  "REVIEW",      // 검토 중
  "DONE",        // 완료
  "ON_HOLD",     // 보류
  "CANCELED",    // 취소
];

export const CHOICE_STATUS = {
  TODO: ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"],
  IN_PROGRESS: ["IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"],
  REVIEW: ["REVIEW", "DONE", "ON_HOLD", "CANCELED"],
  DONE: ["DONE", "ON_HOLD", "CANCELED"],
  ON_HOLD: ["IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"],
  CANCELED: ["IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"]
}

export const GET_CHOICE_STATUS = (status) => {
  return CHOICE_STATUS[status] ?? [];
}

// 우선순위 옵션
export const PRIORITIES = [
  "LOW",    // 낮음
  "MEDIUM", // 보통
  "HIGH",   // 높음
];
