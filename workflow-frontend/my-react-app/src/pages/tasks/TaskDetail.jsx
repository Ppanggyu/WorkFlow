import "../../css/tasks/TaskDetail.css";
import { useEffect, useState, useMemo, useRef } from "react";
import { useParams, useNavigate, NavLink } from "react-router-dom";
import { api } from "../../api/api.js";

import { visibilityLabel } from "../../utils/taskUtils";
import { formatRelativeDateTime, ddayLabel } from "../../utils/dateUtils";

import { useAuth } from "../../auth/hooks/useAuth.js"
import { userFromToken } from "../../auth/utils/userFromToken.js"
import { userRole } from "../../auth/utils/userRole.js"
import { auditLogLabel } from "../../utils/auditLogUitls.js"

import ImageModal from "../../components/common/ImageModal";

// 첨부 모듈 분리
import AttachmentList from "../../components/attachments/AttachmentList";

export default function TaskDetail() {

  const { id } = useParams();
  const { scope } = useParams();
  const nav = useNavigate();

  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const {accessToken} = useAuth(); // JWT Decode용
  const user = userFromToken(accessToken); // JWT Deocde
  const [canAccess, setCanAcess] = useState(false);
  const [auditLog, setAuditLog] = useState([]);
  const [expended, setExpended] = useState(false); // 더보기, 접기 용

  // 모달 기능
  const descRef = useRef(null);

  const [imgModal, setImgModal] = useState({
    open: false,
    src: "",
    alt: "",
  });

  const closeModal = () => {
    setImgModal({ open: false, src: "", alt: "" });
  };

  // 이미지 클릭 시 모달 오픈
  useEffect(() => {
    const el = descRef.current;
    if (!el) return;

    const onClick = (e) => {
      const target = e.target;
      if (target && target.tagName === "IMG") {
        setImgModal({
          open: true,
          src: target.getAttribute("src") || "",
          alt: target.getAttribute("alt") || "",
        });
      }
    };

    el.addEventListener("click", onClick);
    return () => el.removeEventListener("click", onClick);
  }, [task?.description]);

  // useEffect(() => {...},[id]);
  // 최초 렌더링 시 1번 실행
  // id 변경 시마다 실행
  // 실행 전에 이전 effect cleanup 먼저 실행
  // 마지막에 호출안하면 실행안됨(useEffect은 타이밍 트리거 일뿐)
  // 여기선 Task API 호출에 사용하며 AbortController로 요청 취소 가능
  useEffect(() => {
    const controller = new AbortController();

    (async () => {
      setLoading(true);
      setErr("");

      try {
        const res = await api.get(`/api/tasks/${id}/${scope}`, {
          signal: controller.signal, // 스위치 연결
        });
        setTask(res.data);
        const access = await userRole(user, res.data);
        setCanAcess(access);
      } catch (e) {
        if (e.name === "CanceledError" || e.code === "ERR_CANCELED") return;

        // 상태코드별 사용자 친화적 에러 메시지
        const msg =
          e?.response?.status === 404 ? "업무를 찾을 수 없습니다."
            : e?.response?.status === 401 ? "로그인이 필요합니다."
              : "조회에 실패했습니다.";

        setErr(msg);
      } finally {
        setLoading(false);
      }
    })();

    // 페이지 이동/언마운트 시 요청 취소
    return () => controller.abort();
  }, [id]);

  // 작성일 라벨
  const createdAtLabel = useMemo(
    () => formatRelativeDateTime(task?.createdAt),
    [task?.createdAt]
  );

  // DONE 상태면 D-Day 라벨 숨김
  const dday = useMemo(() => {
    if (!task?.dueDate) return null;

    // 완료/취소 업무는 마감 강조 불필요
    if (task?.status === "DONE" || task?.status === "CANCELED") return null;

    return ddayLabel(task.dueDate);
  }, [task?.dueDate, task?.status]);

  // 첨부 목록 (백엔드가 task.attachments로 내려준다는 전제)
  const attachments = useMemo(() => {
    const arr = task?.attachments || [];
    return Array.isArray(arr) ? arr : [];
  }, [task?.attachments]);

  // 수정 내용 출력용
  useEffect(() => {
    const logFetch = async () => {
      try{
        const res = await api.get(`/api/auditLog/${id}`)
        const grouped = Object.values(
          // AuditLog DB에 추가한 updateGroupId 기준으로 배열 묶기
          // 정렬은 Repository에서 OrderBy
          res.data.reduce((acc, log) => {
            const key = log.updateGroupId;

            if(!acc[key]) {
              acc[key] = [];
            }
            
            acc[key].push(log);

            return acc;
          }, {})
        );
        setAuditLog(grouped);
      }catch(error){
        console.log(error);
      }
    };
    logFetch();
  }, [id])

  const formatStatus = (log) => {
    const statusLogs = log.filter(l => l.fieldName === "status");

    if(statusLogs.length === 0) return null;

    const flow = [
      statusLogs[0].beforeValue,
      ...statusLogs.map(l => l.afterValue)
    ];

    return `상태변경 (${flow.join(" → ")})`;
  }

 const deleteTask = async (item) => {
  const reason = prompt('삭제 사유를 입력하세요.');
  if(!reason) return;

  const ok = window.confirm('정말 삭제하시겠습니까?');
  if(!ok) return;

    try{
      await api.delete(`/api/tasks/${item}`, {data: reason});
      nav("/tasks");
    }catch(error){
      console.log(error);
    }
 }

 const resotre = async (taskId) => {

  const reason = prompt('업무 복구 사유를 입력하세요.');
  if(!reason) return;

  try{
    await api.post(`/api/tasks/resotre/${taskId}`, {data: reason});
    nav("/tasks");
  }catch(error){
    console.loog(error);
    alert("업무 복구 실패")
  }
 }

  const likedHandler = async (bool, taskId) => {
    // 즐겨찾기 활성화 비활성화 -> bool = true or false
    const prevTasks = task;

    setTask(prev => ({
      ...prev,
      liked: !bool
    }));

    try{
        if(bool){
          await api.delete(`/api/likes/${taskId}`);
        }else{
          await api.post(`/api/likes/${taskId}`);
        }
      }catch(error){
        console.log(error);

        setTask(prevTasks);
      }

  };

  if (loading)
    return <div className="taskdetail__state">불러오는 중...</div>;

  if (err)
    return (
      <div className="taskdetail__state">
        <div className="taskdetail__error">{err}</div>
        <button
          className="taskdetail__btn taskdetail__btn--ghost"
          onClick={() => nav(-1)}
        >
          뒤로가기
        </button>
      </div>
    );

  if (!task) return null;

  const priorityKey = (task?.priority || "").toLowerCase();

  return (
    <div className="taskdetail">

      {/* 헤더 카드 */}
      <div className="taskdetail__card taskdetail__card--header">

        <div className="taskdetail__topRow">

          <div>
            <div className="taskdetail__eyebrow">
              {createdAtLabel}
            </div>

            <h2 className="taskdetail__title">
              <span>{task.liked ? 
              <button className="like__onBtn" 
                onClick={() => {likedHandler(true, task.id)
              }}>⭐</button>
              : 
              <button className="like__offBtn" 
                onClick={() => {likedHandler(false, task.id)
              }}>☆</button>}</span>  {task.title}
            </h2>
          </div>

          <div className="taskdetail__actions">
            <NavLink
              className="taskdetail__btn taskdetail__btn--ghost"
              to="/tasks"
            >
              목록으로
            </NavLink>
            
            {scope !== "deleted" ? (
              (canAccess) && (
                <>
                <NavLink
                  className="taskdetail__btn"
                  to={`/tasks/${id}/edit`}
                >
                  수정
                </NavLink>

                <button
                  type="button"
                  className="taskdetail__btn taskdetail__btn--danger"
                  onClick={() => deleteTask(task.id)}
                >
                  삭제
                </button>
                </>
              ))
              :
              (
              <button 
              type="button"
              className="taskdetail__btn taskdetail__btn--danger"
              onClick={() => resotre(task.id)}>
                복구
              </button>
              )}
          </div>

        </div>

        {/* 상태, 범위, 중요도, 마감, D-Day */}
        <div className="taskdetail__badges">

          <span className={`taskdetail__badge taskdetail__badge--${(task.status || "").toLowerCase()}`}>
            {task.status ?? "-"}
          </span>

          <span className="taskdetail__badge taskdetail__badge--visibility">
            {visibilityLabel(task.visibility)}
          </span>

          <span className={`taskdetail__badge taskdetail__badge--priority taskdetail__badge--priority-${priorityKey}`}>
            중요도: {task.priority ?? "-"}
          </span>

          {task.dueDate && (
            <span className="taskdetail__badge taskdetail__badge--due">
              마감: {task.dueDate}
            </span>
          )}

          {/* DONE이면 자동으로 안 뜸 */}
          {dday && (
            <span className="taskdetail__badge taskdetail__badge--dday">
              {dday}
            </span>
          )}

        </div>

      </div>

      {/* 내용 카드 */}
      <div className="taskdetail__grid">

        {/* 왼쪽: 설명 + 첨부 */}
        <div className="taskdetail__card">

          {task.description ? (
            <div
              ref={descRef}
              className="taskdetail__desc"
              dangerouslySetInnerHTML={{ __html: task.description }}
            />
          ) : (
            <div className="taskdetail__empty">설명이 없습니다.</div>
          )}

          {/* 첨부파일 (모듈 분리 컴포넌트) */}
          <AttachmentList
            attachments={attachments}
          />

        </div>

        {/* 모달 (공용 컴포넌트) */}
        <ImageModal
          open={imgModal.open}
          src={imgModal.src}
          alt={imgModal.alt}
          onClose={closeModal}
        />

        {/* 오른쪽: 정보 */}
        <div>
        <div className="taskdetail__card taskdetail__card--meta">

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">작성자</div>
            <div className="taskdetail__metaVal">
              {(() => {
                const name = task.createdByName ?? task.creatorName;
                const dept = task.createdByDepartmentName;

                return name ?
                `${name}${dept ? `(${dept})` : ""}`
                : "-";
              }) ()}
            </div>
          </div>

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">담당자</div>
            <div className="taskdetail__metaVal">
              {(() => {
                const name = task.assigneeName;
                const dept = task.assigneeDepartmentName;

                return name ?
                `${name}${dept ? `(${dept})` : ""}`
                : "-";
              }) ()}
            </div>
          </div>

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">마감일</div>
            <div className="taskdetail__metaVal">
              {task.dueDate ?? "-"} {dday && `(${dday})`}
            </div>
          </div>

          <div className="taskdetail__metaRow">
            <div className="taskdetail__metaKey">작성일</div>
            <div className="taskdetail__metaVal">
              {createdAtLabel}
            </div>
          </div>

        </div>

        {/* 오른쪽: 수정 정보 */}
        {auditLog.length === 0 ? (<div></div>) :
          (<>
          <div className="taskdetail__updateTitle">수정 내용</div>
            {auditLog.slice(0, expended ? auditLog.length : 3).map((log, index) => (
            <div key={index} className="taskdetail__card taskdetail__card--meta">
              <div className="taskdetail__metaRow">
                <div className="taskdetail__metaKey">수정자</div>
                <div className="taskdetail__metaVal">
                  {log[0].actor.name} ({log[0].actor.department})
                </div>
              </div>
                <div className="taskdetail__metaRow">
                  <div className="taskdetail__metaKey">수정필드</div>
                  <div className="taskdetail__metaVal">
                    {[formatStatus(log),
                      ...log.filter(a => a.fieldName !== "status")
                      .map(a => auditLogLabel(a.fieldName))]
                    .filter(Boolean)
                    .join(", ")
                    }
                    {/* {log.map(a => auditLogLabel(a.fieldName)).join(", ")} */}
                  </div>
                </div>

              <div className="taskdetail__metaRow">
                <div className="taskdetail__metaKey">수정사유</div>
                <div className="taskdetail__metaVal">
                  {log[0].reason ?? "-"}
                </div>
              </div>

              <div className="taskdetail__metaRow">
                <div className="taskdetail__metaKey">수정일</div>
                <div className="taskdetail__metaVal">
                  {formatRelativeDateTime(log[0].createdAt)}
                </div>
              </div>
            </div>
          ))}
          </>
        )}
          {auditLog.length > 3 && (
            <button className="taskdetail__expended" onClick={() => setExpended(prev => !prev)}>
              {expended ? "접기" : "더보기"}
            </button>
          )}

      </div>
      </div>

    </div>
  );
}