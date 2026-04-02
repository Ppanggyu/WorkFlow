import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/hooks/useAuth";
import { api } from "../../api/api";
import "../../css/dashboard/Dashboard.css";
import { formatRelativeDateTime, ddayLabel } from "../../utils/dateUtils";

export default function Dashboard() {
  const nav = useNavigate(); // 페이지 이동 훅
  const { accessToken } = useAuth(); // 로그인 토큰

  // KPI 상태 목록
  const kpis = ["TODO", "IN_PROGRESS", "REVIEW", "DONE", "ON_HOLD", "CANCELED"];

  // KPI 상태별 카운트
  const [counts, setCounts] = useState({});

  // 스코프 선택: assigned = 내 업무, created = 내가 만든 업무
  const [scope, setScope] = useState("assigned");

  const [size] = useState(10);
  const [taskData, setTaskData] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);


  // KPI 데이터 가져오기 (accessToken 있을 때만)
  useEffect(() => {
    if (!accessToken) return;

    const fetchData = async () => {
      try{
        const kpiRes = await api.get("/api/kpi") // 백엔드 KPI API 호출
        setCounts(kpiRes.data);
  
        const res = await api.get("/api/dashBoardTask", {params : {
          page,
          size,
          scope
        }})
        setTaskData(res.data.content);
        setTotalPages(res.data.totalPages);
      }catch(error){
        console.log(error);
      }
    };
    fetchData();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, page, scope]);

  const handleScopeChange = (newScope) => {
    setScope(newScope);
    setPage(0);
  };

  const getPages = () => {
    const max = 5;

    let start = Math.max(0, page - 2);
    let end = Math.min(totalPages - 1, start + max - 1);

    if (end === totalPages - 1) {
      start = Math.max(0, end - max + 1);
    }

    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  };


  // KPI 카드 클릭 시 Tasks 페이지로 이동 + 필터 전달
  const handleKpiClick = (status) => {
    nav(`/tasks?status=${encodeURIComponent(status)}&scope=${encodeURIComponent(scope)}`);
  };

  return (
    <div className="dashboard__grid">
      
      {/* 스코프 선택 버튼 */}
      <div className="kpi__tabs">
        <button
          className={scope === "assigned" ? "active" : ""}
          onClick={() => handleScopeChange("assigned")}
          type="button"
        >
          내 업무
        </button>

        <button
          className={scope === "created" ? "active" : ""}
          onClick={() => handleScopeChange("created")}
          type="button"
        >
          내가 만든 업무
        </button>
      </div>

      {/* KPI 카드 행 */}
      <section className="kpi__row">
        {kpis.map((k) => (
          <div
            key={k}
            className={`card ${k}`} // 카드 스타일 + 상태별 클래스
            onClick={() => handleKpiClick(k)}
            role="button"
            tabIndex={0} // 키보드 접근 가능
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") handleKpiClick(k);
            }}
            style={{ cursor: "pointer" }}
          >
            <div className="card__title">{k}</div>
            <div className="kpi__value">{counts?.[scope]?.[k] ?? 0}</div> {/* KPI 수치 표시 */}
          </div>
        ))}
      </section>

      {/* 아래 영역: 2열 레이아웃 */}
      <section className="two__col">

        {/* 내 업무 테이블 카드 */}
        <div className="card card__tasks">
          <div className="card__title">My Tasks Table</div>

          {taskData.length !== 0 ? 
          <div className="dashboard__task-table">
            {/* header */}
            <div className="dashboard__task-table__header">
              <div>제목</div>
              <div>마감일</div>
              <div>작성일</div>
              <div>수정일</div>
              <div>중요도</div>
              <div>공개범위</div>
              {scope === "created" ? <div>담당자</div> : <div>작성자</div>}
              <div>첨부파일</div>
            </div>

            {/* body */}
            <div className="dashboard__task-table__body">
              {taskData.map((item) => (
                <div className="dashboard__task-table__row" key={item.id} onClick={() => nav(`/tasks/${item.id}`)}>
                  <div className="dashboard__task-table-rowData dashboard__task-table-title">{item.title}</div>
                  <div className="dashboard__task-table-rowData dashboard__task-table-dueDate">{ddayLabel(item.dueDate)}</div>
                  <div className="dashboard__task-table-rowData dashboard__task-table-createdAt">{formatRelativeDateTime(item.createdAt)}</div>
                  <div className="dashboard__task-table-rowData dashboard__task-table-updatedAt">{formatRelativeDateTime(item.updatedAt)}</div>
                  <div className="dashboard__task-table-rowData dashboard__task-table-priority">{item.priority}</div>
                  <div className="dashboard__task-table-rowData dashboard__task-table-visibility">{item.visibility}</div>

                  <div className="dashboard__task-table-rowData dashboard__task-table-name">
                    {scope === "created"
                      ? (item.assigneeName != null ? `${item.assigneeName} (${item.assigneeDepartmentCode})` : "-")
                      : (item.createdByName != null ? `${item.createdByName} (${item.createdByDepartmentCode})` : "-")}
                  </div>

                  <div className="dashboard__task-table-rowData dashboard__task-table-attachmentsCount">{item.attachmentsCount}</div>
                </div>
              ))}
            </div>
          </div>
          : <div>업무가 없습니다.</div>}

        {taskData.length !== 0 ? 
        <div className="dashboard__pagination">
            {/* first */}
            <button
              disabled={page === 0}
              onClick={() => setPage(0)}
            >
              «
            </button>

            {/* prev */}
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              ‹
            </button>

            {/* pages */}
            {getPages().map((p) => (
              <button
                key={p}
                className={p === page ? "active" : ""}
                onClick={() => setPage(p)}
              >
                {p + 1}
              </button>
            ))}

            {/* next */}
            <button
              disabled={page === totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              ›
            </button>

            {/* last */}
            <button
              disabled={page === totalPages - 1}
              onClick={() => setPage(totalPages - 1)}
            >
              »
            </button>

          </div>
        : <div></div> }
        </div>

        

        {/* 활동 로그 카드 */}
        <div className="card card__activity">
          <div className="card__title">Activity Log</div>
          <div className="muted">
            <p>Activity Log</p>
          </div>
        </div>

      </section>
    </div>
  );
}
