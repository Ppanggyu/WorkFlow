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
  const [tempFiles, setTempFiles] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const quillRef = useRef(null);

  const [priorityOpt, setPriorityOpt] = useState(''); // 우선순위
  const [visibilityOpt, setVisibilityOpt] = useState(''); // 공개범위

  const [uuid] = useState(() => crypto.randomUUID());

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
      // const formData = new FormData();
      // for(const file of selectedFiles){
      //   formData.append('file', file);
      // }
      try {
      await api.post("/api/taskForm",
        { title, description, priority, dueDate, visibility, assigneeId, tempImages, tempFiles/*, status*/ }); // status 확인하기 위한 설정(기본값 TODO)
      // await api.post("/api/attachmentsSave", formData);
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
    input.setAttribute("multiple", true);
    input.click();

    input.onchange = async() => {
      const files = Array.from(input.files);

      const formData = new FormData();
      formData.append("uuid", uuid);
      for(const file of files){
        formData.append('file', file);
      }

      try{
        const res = await api.post("/api/imageUpload", formData);

        const quill = quillRef.current.getEditor(); // Quill 에디터 인스턴스 가져오기
        const range = quill.getSelection(true); // 현재 커서 위치, true = 에디터에 강제로 포커스 주는거
        let cursorIndex = range.index;

        for(const file of res.data){
          quill.insertEmbed(range.index, "image", file.url); // 현재 커서위치에 이미지 삽입
          cursorIndex += 1;
          
          // new URL().pathname -> URL 지우고 뒤에만 남김
          setTempImages(prev => [...prev, {url: file.url, path: new URL(file.url).pathname, originalFileName: file.originalFileName}]);
          console.log(tempImages);
        }
        quill.setSelection(cursorIndex); // 이미지 삽입 후 커서 이미지 앞으로
      }catch(e){
        alert(e.response.data.message);
        // eslint-disable-next-line react-hooks/exhaustive-deps
      }};}, [uuid]);
  
useEffect(() => {
  console.log(selectedFiles);
  console.log(tempFiles);
}, [selectedFiles, tempFiles])

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

  const inputFile = async (item) => {
    item.preventDefault(); // submit 시 새로고침 막음
    // e.persist(); // SyntheticEvent를 비동기로 쓸 때 필요했음 - 17버전 이상부터는 안써도 됨
    if(!item.target.value) return;

    const files = Array.from(item.target.files);
    setSelectedFiles((prev) => [...prev, ...files]);

    const formData = new FormData();
    files.forEach((file) => {
      formData.append("file", file);
    })
    formData.append("uuid", uuid);
    try{
      const res = await api.post("/api/fileUpload", formData);
      
      for(const file of res.data){
      setTempFiles(prev => [...prev, {url: file.url, path: new URL(file.url).pathname, originalFileName: file.originalFileName}]);
      }
      console.log(tempFiles);
    }catch(e){
      console.log(e);
    }
  }

  const deleteFile = async (deleteItem) => {

    const deleted = tempFiles.find(file => file.originalFileName === deleteItem.name);
    setTempFiles(prev => prev.filter(file => file.originalFileName !== deleteItem.name));
    setSelectedFiles(prev => prev.filter(file => file.name !== deleteItem.name));

    try{
      api.post("/api/deleteFile", deleted);
    }catch(e){
      console.log(e);
    }
  }

  // quill 세팅
  const modules = useMemo(() => { // useMemo없으면 매 랜더링마다 modules가 다시 생성
    return {
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ['bold', 'italic', 'underline', 'strike', 'blockquote'],
          ['image']
        ],
        handlers: {
          image: imageHandler,
        },
      },
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className='taskFrom-div'>
      <div className='taskForm-titleAndButton'>
        <input className='taskForm-title' type="text" value={title} onChange={(e) => setTitle(e.target.value) } placeholder="제목" />
        <button className='taskForm-button' onClick={ formHandle }>저장</button>
      </div>
      <div className='taskForm-dropdowns'>
        <div className='taskForm-dropdown'>
          <div className="taskForm-dropdown-header" onClick={() => setOpenDropdown(openDropdown === "priority" ? null : "priority")}>
            {priority}
            <span className={`arrow ${openDropdown === "priority" ? "open" : ""}`}>▼</span>
          </div>
          {openDropdown === "priority" && (
          <ul className="taskForm-dropdown-list">
            {priorityOpt.map((item) => (
            <li key={item} className="taskForm-dropdown-item" onClick={() => {
            setPriority(item); setOpenDropdown(null);}}>
              {item}
            </li>
            ))}
          </ul>
        )}
        </div>

        <div className='taskForm-dropdown'>
          <div className="taskForm-dropdown-header" onClick={() => setOpenDropdown(openDropdown === "visibility" ? null : "visibility")}>
            {visibility}
            <span className={`arrow ${openDropdown === "visibility" ? "open" : ""}`}>▼</span>
          </div>
          {openDropdown === "visibility" && (
          <ul className="taskForm-dropdown-list">
            {visibilityOpt.map((item) => (
            <li key={item} className="taskForm-dropdown-item" onClick={() => {
            setVisibility(item); setOpenDropdown(null);}}>
              {item}
            </li>
            ))}
          </ul>
        )}
        </div>

        <div className='taskForm-dropdown'>
          <div className="taskForm-dropdown-header" onClick={() => setOpenDropdown(openDropdown === "assignee" ? null : "assignee")}>
            {assignee}
            <span className={`arrow ${openDropdown === "assignee" ? "open" : ""}`}>▼</span>
          </div>
          {openDropdown === "assignee" && (
          <ul className="taskForm-dropdown-list">
            {assigneeOpt.map((item) => (
            <li key={item.id} className="taskForm-dropdown-item" onClick={() => {
            setAssigneeId(item.id);setAssignee(`${item.name} (${item.departmentName})`); setOpenDropdown(null);}}>
              {item.name} ({item.departmentName})
            </li>
            ))}
          </ul>
        )}
        </div>

        {/* status 확인하기 위한 설정(기본값 TODO) */}
        {/* <div className='dropdown'>
          <div className="dropdown-header" onClick={() => setOpenDropdown(openDropdown === "status" ? null : "status")}>
          {status}
          <span className={`arrow ${openDropdown === "status" ? "open" : ""}`}>▼</span>
          </div>
          {openDropdown === "status" && (
            <ul className="dropdown-list">
            {statusOpt.map((item) => (
              <li key={item.id} className="dropdown-item" onClick={() => {
                setStatus(item); setOpenDropdown(null);}}>
                {item}
                </li>
                ))}
                </ul>
                )}
                </div> */}
        <input className='taskForm-date' type="date" value = {selectedDate || ""}
          min={formatDate(today)} max={formatDate(maxDate)}
          onChange={(e) => {setSelectedDate(e.target.value);}}
          onKeyDown={(e) => e.preventDefault()}></input>
      </div>

      {selectedFiles.length === 0 ? (
        <p>파일첨부가능</p>
      ) : (
        selectedFiles.map((file, index) => (
          <div key={index}>
              <p>{file.name}</p><button onClick={(() => deleteFile(file))}>X</button>
          </div>
        ))
      )}

      <div className='taskForm-fileUpBtnDIV'>
        <input type='file' multiple id="taskForm-inputFile" onChange={inputFile}></input>
      </div>


        
      <ReactQuill ref={quillRef} theme="snow" value={description} onChange={deleteImageHanlder} modules={modules} />
    </div>
  );
}
