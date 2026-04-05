
// AuditLog 출력 시 한글 번역 용

export const auditLogLabel = (v) => {
  return fieldLabel[v] 
      ?? actionType[v]
      ?? "-";
};

export const fieldLabel = {
  title: "제목",
  description: "내용",
  priority: "우선순위",
  visibility: "공개범위",
  assignee: "담당자",
  dueDate: "마감일",
  Attachment: "첨부파일",
  status: "상태",
};

export const actionType = {
  TASK_UPDATE: "업무수정",
  TASK_DELETED: "업무삭제",
  TASK_STATUS: "상태변경",
  TASK_RESOTRE: "업무복구",
};
