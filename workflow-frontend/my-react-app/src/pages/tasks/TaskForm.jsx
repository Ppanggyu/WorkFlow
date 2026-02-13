import React, { useEffect, useState } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import { useNavigate } from 'react-router-dom'
import { useAuth } from "../auth/useAuth";
import { api } from "../api/api";
import "../css/TaskForm.css";

export default function TaskInput() {
  const { accessToken } = useAuth();
  const [description, setDescription] = useState(''); // 내용
  const [title, setTitle] = useState(''); // 제목
  const navigate = useNavigate();
  const [openDropdown, setOpenDropdown] = useState(null); // 드롭다운 다중선택 여부
  const [priority, setPriority] = useState("우선순위 선택");
  const [visibility, setVisibility] = useState("공개범위 선택");
  const [selectedDate, setSelectedDate] = useState();
  const [assignee, setAssignee] = useState("담당자 선택");
  const [assigneeOpt, setAssigneeOpt] = useState([]);
  const [assigneeId, setAssigneeId] = useState('');

  const [priorityOpt, setPriorityOpt] = useState(''); // 우선순위
  const [visibilityOpt, setVisibilityOpt] = useState(''); // 공개범위

  const today = new Date();
  const maxDate = new Date();
  maxDate.setFullYear(today.getFullYear() + 10);
  const formatDate = (date) => date.toISOString().split("T")[0];
  const dueDate = selectedDate;

const formHandle = async () => {
    if(!accessToken) {
        alert("asdasd")
        return;
    }
    if(priority == "우선순위 선택"){
      alert("업무 우선 순위를 정해주세요.");
      return;
    }
    if(assignee == "담당자 선택"){
      alert("담당자를 선택해 주세요.")
      return;
    }
    try {
    await api.post("/api/taskForm", { title, description, priority, dueDate, visibility, assigneeId });
    navigate("/tasks");
  } catch (err) {
    console.error(err);
    alert("작성실패");
  }
}

useEffect(() => {
  const fetchAssignees = async () => {
      try{
        const res = await api.post("/api/allDepartment");
        setAssigneeOpt(res.data.allDepartment);
        setVisibilityOpt(res.data.visibility)
        setPriorityOpt(res.data.priority)
      } catch (error) {
        console.error("담당자 불러오기 실패 : " + error);
      }
    };
    fetchAssignees();
  }, []);

  console.log(assigneeOpt);

  return (
    <div>
      <input type="text" value={title} onChange={(e) => setTitle(e.target.value) } placeholder="제목" />
      <div className='dropdown'>
        <div className="dropdown-header" onClick={() => setOpenDropdown(openDropdown === "priority" ? null : "priority")}>
          {priority}
          <span className={`arrow ${openDropdown === "priority" ? "open" : ""}`}>▼</span>
        </div>
         {openDropdown === "priority" && (
        <ul className="dropdown-list">
          {priorityOpt.map((item) => (
          <li key={item} className="dropdown-item" onClick={() => {
          setPriority(item); setOpenDropdown(null);}}>
            {item}
          </li>
          ))}
        </ul>
      )}
      </div>

      <div className='dropdown'>
        <div className="dropdown-header" onClick={() => setOpenDropdown(openDropdown === "visibility" ? null : "visibility")}>
          {visibility}
          <span className={`arrow ${openDropdown === "visibility" ? "open" : ""}`}>▼</span>
        </div>
         {openDropdown === "visibility" && (
        <ul className="dropdown-list">
          {visibilityOpt.map((item) => (
          <li key={item} className="dropdown-item" onClick={() => {
          setVisibility(item); setOpenDropdown(null);}}>
            {item}
          </li>
          ))}
        </ul>
      )}
      </div>

      <div className='dropdown'>
        <div className="dropdown-header" onClick={() => setOpenDropdown(openDropdown === "assignee" ? null : "assignee")}>
          {assignee}
          <span className={`arrow ${openDropdown === "assignee" ? "open" : ""}`}>▼</span>
        </div>
         {openDropdown === "assignee" && (
        <ul className="dropdown-list">
          {assigneeOpt.map((item) => (
          <li key={item.id} className="dropdown-item" onClick={() => {
          setAssigneeId(item.id);setAssignee(`${item.name} (${item.departmentName})`); setOpenDropdown(null);}}>
            {item.name} ({item.departmentName})
          </li>
          ))}
        </ul>
      )}
      </div>



      <input type="date" value = {selectedDate || ""}
          min={formatDate(today)} max={formatDate(maxDate)}
          onChange={(e) => {setSelectedDate(e.target.value);}}
          onKeyDown={(e) => e.preventDefault()}></input>

        
      <ReactQuill theme="snow" value={description} onChange={setDescription} />
      <button className='button' onClick={ formHandle }>저장</button>
    </div>
  );
}
