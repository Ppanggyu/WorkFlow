import { useEffect, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { api } from "../api/api.js";

export default function Dashboard() {

  const kpis = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'ON_HOLD', 'CANCELED']
  const [counts, setCounts] = useState({})
  const { accessToken } = useAuth();

// accessToken 값이 바뀔때 마다 안에 있는 코드를 실행하라. 
useEffect(() => {
  console.log("Dashboard accessToken =", accessToken);
  if (!accessToken) return; // 로그인 전엔 호출 X

  // 겟 포스트 구분
  api.get("/api/kpi")
  // 요청 성공하면 200(ok) 실행
    .then((res) => setCounts(res.data))
    // 실패하면 에러 표시
    // ex) 토큰 만료 401, 서버 오류 500, 네트워크 오류 등
    .catch((e) => console.error("KPI 불러오기 실패", e));
}, [accessToken]);

  return (
    <div className="dashboardGrid">
      {/* KPI Cards */}
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
