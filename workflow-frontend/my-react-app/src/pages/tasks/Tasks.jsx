import '../css/Tasks.css'
import { NavLink } from "react-router-dom";
import { api } from "../api/api"
import { useAuth } from "../auth/useAuth"
import { useEffect, useState } from 'react';
import { useNavigate } from "react-router-dom";

export default function Tasks(){

  const {user} = useAuth();
  const [tasks, setTasks] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [taskFilter, setTaskFilter] = useState("");
  const [allStatus, setAllStatus] = useState([]);
  const [status, setStatus] = useState("우선 순위");
  const [statusFilter, setStatusFilter] = useState();
  const [openDropdown, setOpenDropdown] = useState(null); // 드롭다운 다중선택 여부
  const nav = useNavigate();

  const tasksHandle = (e) => {
    setPage(0);
    setTaskFilter(e.target.value);
  }

  useEffect(() => {
  const fetchTasks = async () => {
    try{
      console.log("실행");
      const res = await api.post("/api/tasks",
        {page, size:9, filter:taskFilter, status:statusFilter});
      setTasks(res.data.tasks.content);
      setTotalPages(res.data.totalPages);
      setAllStatus(["ALL", ...res.data.status]);
    } catch (error) {
      console.error("tasks 페이지 : " + error);
    }
  }
  fetchTasks();
  }, [user, page, taskFilter, statusFilter]);;

  
  const selectedStatus = (item) => {
    setStatus(item);
    setOpenDropdown(null);
    setStatusFilter(item === "ALL" ? null : item);
    setPage(0);
  }

  const selectTask = (taskId) => {
    nav(`/taskSelected/${taskId}`)
  }

  return(
    <div className="tasks-container">
      <h2>{user?.name}님의 tasks</h2>

      <button value="all" onClick={tasksHandle}>전체 업무</button>
      <button value="company" onClick={tasksHandle}>전사 업무</button>
      <button value="myDepartment" onClick={tasksHandle}>우리 팀 업무</button>
      <button value="create" onClick={tasksHandle}>내가 만든 업무</button>
      <button value="assignee" onClick={tasksHandle}>담당 업무</button>

      <div className='dropdown'>
        <div className="dropdown-header" onClick={() => setOpenDropdown(openDropdown === "status" ? null : "status")}>
          {status}
          <span className={`arrow ${openDropdown === "status" ? "open" : ""}`}>▼</span>
        </div>
         {openDropdown === "status" && (
        <ul className="dropdown-list">
          {allStatus.map((item) => (
          <li key={item} className="dropdown-item" onClick={() => {
          setStatus(item); setOpenDropdown(null); selectedStatus(item)}}>
            {item}
          </li>
          ))}
        </ul>
      )}
      </div>

      <div className="tasks-grid">
         {tasks.map((tasks, i) => (
           <div key={`${i}`} className="task-card">
              <div onClick={() => selectTask(tasks.id)}>
              <div className="task-title">{tasks.title || "제목 없음"}</div>
              <div className="task-info">결재상태: <span className={`task-status status-${tasks.status}`}>{tasks.status}</span></div>
              <div className="task-info">우선순위: {tasks.priority}</div>
              <div className="task-info">작성자: {tasks.createdBy.name}</div>
              <div className="task-info">담당자: {tasks.assigneeId.name}</div>
              <div className="task-info">작성부서: {tasks.ownerDepartmentId.name}</div>
              <div className="task-info">처리/담당부서: {tasks.workDepartmentId.name}</div>
              <div className="task-info">작성일: {tasks.createdAt.replace("T", " ").split(".")[0]}</div>
              <div className="task-info">수정일: {tasks.updatedAt.replace("T", " ").split(".")[0]}</div>
            </div>
          </div>
        ))}
      </div>
      <div>
        {Array.from({length: totalPages}).map((_,idx) =>
        <button key={idx} disabled={idx === page} onClick={() => setPage(idx)}>
          {idx + 1}
        </button>
        )}
      </div>

      <NavLink to="/taskForm" className="task-link">
        글작성
      </NavLink>
    </div>
  )
}
