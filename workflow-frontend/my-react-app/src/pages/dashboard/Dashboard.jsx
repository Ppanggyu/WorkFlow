import { useEffect, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { api } from "../api/api.js";
import "../css/Dashboard.css"

export default function Dashboard() {

  const kpis = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'ON_HOLD', 'CANCELED']
  const [counts, setCounts] = useState({})
  const { accessToken } = useAuth();

const fetchKpi = async (type = "created") => {
  try {
    const res = await api.get(`/api/kpi?type=${type}`);
    setCounts(res.data);
  } catch (e) {
    console.log(e);
  }
};

useEffect(() => {
  if (!accessToken) return;

    const fetchKpiActive = async () => {
      await fetchKpi(); // 초기 호출, type 기본값 사용
    };
  fetchKpiActive();
}, [accessToken]);

  return (
    <div className="dashboardGrid">
      {/* KPI Cards */}
      <div className="taskBtns">
        <button className="taskBtn" onClick={() => fetchKpi("created")}>내가 만든 업무</button>
        <button className="taskBtn" onClick={() => fetchKpi("assignee")}>담당 업무</button>
      </div>
      <section className="kpiRow">
        {kpis.map((k) => (
          <div key={k} className="card">
            <div className="cardTitle">{k}</div>
            <div className="muted">{counts[k] ?? 0}</div>
          </div>
        ))}
      </section>

      {/* 아래 2컬럼 */}
      <section className="twoCol">
        <div className="card bigCard">
          <div className="cardTitle">My Tasks Table</div>
          <div className="muted">(필터: 상태/담당자/기간/키워드)
          </div>
        </div>

        <div className="card bigCard2">
          <div className="cardTitle">Activity Log</div>
          <div className="muted">(상태/담당자/마감일 변경 이력)
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
            <p>확인용</p>
          </div>
        </div>
      </section>
    </div>
  )
}
