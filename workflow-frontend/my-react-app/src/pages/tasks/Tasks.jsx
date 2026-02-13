import '../css/Tasks.css'
import { NavLink } from "react-router-dom";
import { api } from "../api/api"
import { useAuth } from "../auth/useAuth"
import { useEffect, useState } from 'react';

export default function Tasks(){

  const {user} = useAuth();
  const [tasks, setTasks] = useState([]);

  useEffect(() => {
    const fetchTasks = async () => {
      try{
        const res = await api.post("/api/tasks");
        setTasks(res.data);
      } catch (error) {
        console.error("tasks 페이지 : " + error);
      }
    };
    fetchTasks();
  }, [user]);

  const tasksHandle = async (e) => {
    const handleFilter = e.target.value;
    try{
      const res = await api.get("/api/tasks?filter=" + handleFilter);
      setTasks(res.data);
    } catch(err){
      console.error(err);
      alert("뭐가 문젠데")
    }
  }

  console.log(tasks);

  return(
    <div className="tasks-container">
      <h2>{user?.name}님의 tasks</h2>

      <button value="all" onClick={tasksHandle}>전체 업무</button>
      <button value="company" onClick={tasksHandle}>전사 업무</button>
      <button value="myDepartment" onClick={tasksHandle}>우리 팀 업무</button>
      <button value="create" onClick={tasksHandle}>내가 만든 업무</button>
      <button value="assignee" onClick={tasksHandle}>담당 업무</button>

      <div className="tasks-grid">
        {tasks.map((taskArray, i) => (
          taskArray.map((task, j) => (
            <div key={`${i}-${j}`} className="task-card">
              <div className="task-title">{task.title || "제목 없음"}</div>
              <div className="task-info">결재상태: <span className={`task-status status-${task.status}`}>{task.status}</span></div>
              <div className="task-info">우선순위: {task.priority}</div>
              <div className="task-info">작성자: {task.createdBy.name}</div>
              <div className="task-info">담당자: {task.assigneeId.name}</div>
              <div className="task-info">작성부서: {task.ownerDepartmentId.name}</div>
              <div className="task-info">처리/담당부서: {task.workDepartmentId.name}</div>
              <div className="task-info">작성일: {task.createdAt.replace("T", " ").split(".")[0]}</div>
              <div className="task-info">수정일: {task.updatedAt.replace("T", " ").split(".")[0]}</div>
            </div>
          ))
        ))}
      </div>

      <NavLink to="/taskForm" className="task-link">
        글작성
      </NavLink>
    </div>
  )
}
