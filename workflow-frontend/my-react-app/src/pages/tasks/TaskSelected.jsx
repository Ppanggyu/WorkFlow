import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from "../api/api";
import DOMPurify from "dompurify";
import "../css/TaskSelected.css"

export default function TaskSelected() {

    const { taskId } = useParams();
    const [selectedTask, setSelectedTask] = useState(null);
    const [attachments, setAttachments] = useState([]);

    useEffect(() => {
        const fetchTasks = async () => {
            try{
                const res = await api.get("/api/taskSelected", {params:{taskId}});
                setSelectedTask(res.data.task);
                setAttachments(res.data.taskAtList);
                console.log(res.data);
            } catch(error){
                console.log(error);
            }
        }; fetchTasks();
    }, [taskId]);

    const downloadHandle = async (item) =>{
        const res = await api.get(`/api/${selectedTask.id}/file/${item.id}`, {responseType: "blob"})
        const url = window.URL.createObjectURL(new Blob([res.data]));
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", item.originalFilename);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    }

    // 랜더링 순서 
    // 1. const {taskId} = useParams(); 부분 const들 랜더링(컴포넌트 함수 실행)
    // 2. if (!selectedTask) (JSX 평가)
    // 3. useEffect -> API 응답 -> 2번으로 이동 true/false 분기점
    if (!selectedTask) {
        return <div>로딩중...</div>;
    }

    return (
    <div className="task-selected-container">
        <div className="task-selected-title">
            <span className="title-text">{selectedTask.title}</span>
            <div className="task-badges">
                <span className={`task-badge priority-${selectedTask.priority}`}>{selectedTask.priority}</span>
                <span className={`task-badge visibility-${selectedTask.visibility}`}>{selectedTask.visibility}</span>
                <span className={`task-badge status-${selectedTask.status}`}>{selectedTask.status}</span>
                </div>
            </div>

            <div className="task-selected-info">
                <span className="task-selected-label">작성자:</span> {selectedTask.createdBy?.name}
            </div>
            <div className="task-selected-info">
                <span className="task-selected-label">작성부서:</span> {selectedTask.ownerDepartmentId?.name}
            </div>
            <div className="task-selected-info">
                <span className="task-selected-label">담당자:</span> {selectedTask.assigneeId?.name}
            </div>
            <div className="task-selected-info">
                <span className="task-selected-label">담당/처리 부서:</span> {selectedTask.workDepartmentId?.name}
            </div>
            <div className="task-selected-info">
                <span className="task-selected-label">작성일:</span> {selectedTask.createdAt && new Date(selectedTask.createdAt).toLocaleString()}
            </div>
            <div className="task-selected-info">
                <span className="task-selected-label">수정일:</span> {selectedTask.updatedAt && new Date(selectedTask.updatedAt).toLocaleString()}
            </div>

            <div className="task-selected-info">
                <span className="task-selected-label">첨부 파일:</span>
                {attachments.length === 0 ? (
                    <div className="task-noFile">파일이 존재하지 않습니다.</div>
                ) :
                <div className='task-selected-content'>
                    {attachments.map((item) => (
                    <div key={`${item.id}`} className="task-selected-card" onClick={() => downloadHandle(item)}>
                        {item.originalFilename}
                        <span className='task-selected-fileSize'> {item.formatFileSize}</span>
                    </div>))}
                </div>}
            </div>

            <div
                className="task-selected-description"
                dangerouslySetInnerHTML={{
                    __html: DOMPurify.sanitize(selectedTask.description),
                }}
            />
        </div>
    );
}