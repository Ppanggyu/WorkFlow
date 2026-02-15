import React, { useEffect, useState, useMemo, useRef, useCallback } from 'react';
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
  const [tempImages, setTempImages] = useState([]);
  const quillRef = useRef(null);

  const [priorityOpt, setPriorityOpt] = useState(''); // 우선순위
  const [visibilityOpt, setVisibilityOpt] = useState(''); // 공개범위

  const uuid = crypto.randomUUID();

  const today = new Date();
  const maxDate = new Date();
  maxDate.setFullYear(today.getFullYear() + 10);
  const formatDate = (date) => date.toISOString().split("T")[0];
  const dueDate = selectedDate;


  // 저장
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
      await api.post("/api/taskForm",
        { title, description, priority, dueDate, visibility, assigneeId, tempImages });
      navigate("/tasks");
    } catch (err) {
      console.error(err);
      alert("작성실패");
    }
  }

  // 우선순위, 공개범위, 담당자
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

  // 이미지 업로드
  const imageHandler = useCallback(() => {
    const input = document.createElement('input');
    input.setAttribute('type', 'file');
    input.setAttribute('accept', 
      '.jpg,.jpeg,.png,.gif,.webp');
    input.click();

    input.onchange = async() => {
      const file = input.files[0];
      const formData = new FormData();
      formData.append('file', file);
      formData.append("uuid", uuid);

      try{
        const res = await api.post("/api/imageUpload", formData);
        const fullUrl = res.data.imageURL;

        const quill = quillRef.current.getEditor(); // Quill 에디터 인스턴스 가져오기
        const range = quill.getSelection(true); // 현재 커서 위치, true = 에디터에 강제로 포커스 주는거

        quill.insertEmbed(range.index, "image", fullUrl); // 현재 커서위치에 이미지 삽입
        quill.setSelection(range.index + 1); // 이미지 삽입 후 커서 이미지 앞으로

        // new URL().pathname -> URL 지우고 뒤에만 남김
        setTempImages(prev => [...prev, {url: fullUrl, path: new URL(fullUrl).pathname}]);
      }catch(e){
        alert(e.response.data.message);
      }};}, [uuid]);
  
  // 이미지 삭제
  function deleteImageHanlder(content, delta, source, editor) {
    setDescription(content);

    // content: 에디터의 현재 HTML 문자열
    // delta: 이번 변경 사항(삽입/삭제 등)을 담은 Quill Delta 객체
    // source: 변경을 누가 일으켰는지 ('user', 'api', 'silent')
    // editor: Quill 인스턴스, 현재 상태 가져오거나 조작 가능

    if (source == 'user'){
      const currentImages = editor.getContents().ops
      .filter(op => op.insert && op.insert.image) // 이미지만 추출
      .map(op => op.insert.image); // 이미지 URL 배열로 변환

      // 모든 이미지에서 에디터에 없는 이미지 목록 추출
      const deleted = tempImages.filter(img => !currentImages.includes(img.url));
      // 삭제
      deleted.forEach(({path}) => {
        api.post("/api/deleteImage", {path});
      });
      // 남아 있는 이미지만 유지
      setTempImages(prev => prev.filter(img => currentImages.includes(img.url)));
    }
  };

  // quill 세팅
  const modules = useMemo(() => { // useMemo없으면 매 랜더링마다 modules가 다시 생성
    return {
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ['bold', 'italic', 'underline', 'strike', 'blockquote'],
          ['image'],
        ],
        handlers: {
          image: imageHandler,
        },
      },
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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

        
      <ReactQuill ref={quillRef} theme="snow" value={description} onChange={deleteImageHanlder} modules={modules} />
      <button className='button' onClick={ formHandle }>저장</button>
    </div>
  );
}
