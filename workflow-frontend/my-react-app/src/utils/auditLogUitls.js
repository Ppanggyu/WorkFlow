
// AuditLog 출력 시 한글 번역 용

export const auditLogtFieldnameLabel = (v) => {
  if (v === "title") return "제목";
  if (v === "description") return "내용";
  if (v === "priority") return "우선순위";
  if (v === "visibility") return "공개범위";
  if (v === "assignee") return "담당자";
  if (v === "dueDate") return "마감일";
  if (v === "Attachment") return "첨부파일";
  if (v === "status") return "상태";
  return v ?? "-";
};
