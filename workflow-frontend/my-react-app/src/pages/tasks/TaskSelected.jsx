import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from "../api/api";
import DOMPurify from "dompurify";

export default function TaskSelected() {

    const { taskId } = useParams();
    const [selectedTask, setSelectedTask] = useState(null);

    useEffect(() => {
        const fetchTasks = async () => {
            try{
                const res = await api.get("/api/taskSelected", {params:{taskId}});
                setSelectedTask(res.data);
                console.log(res.data.updateAt);
                console.log(res.data);
            } catch(error){
                console.log(error);
            }
        }; fetchTasks();
    }, [taskId]);

    // 랜더링 순서 
    // 1. const {taskId} = useParams(); 부분 const들 랜더링(컴포넌트 함수 실행)
    // 2. if (!selectedTask) (JSX 평가)
    // 3. useEffect -> API 응답 -> 2번으로 이동 true/false 분기점
    if (!selectedTask) {
        return <div>로딩중...</div>;
    }

    return(
        <div>
        <div>제목 : {selectedTask.title}</div>
        <p>작성자 : {selectedTask.createdBy?.name}</p>
        작성부서 : {selectedTask.ownerDepartmentId?.name}
        <p>담당자 : {selectedTask.assigneeId?.name}</p>
        <p>담당/처리 부서 : {selectedTask.workDepartmentId?.name}</p>
        <p>작성일 : {selectedTask.createdAt && new Date(selectedTask.createdAt).toLocaleString()}</p>
        <p>수정일 : {selectedTask.updatedAt && new Date(selectedTask.updatedAt).toLocaleString()}</p>
        <p>업무 우선 순위 : {selectedTask.priority}</p>
        <p>공개 범위 : {selectedTask.visibility}</p>
        <p>결재 : {selectedTask.status}</p>
        <div
            dangerouslySetInnerHTML={{
                __html: DOMPurify.sanitize(selectedTask.description),
            }}
        />
        </div>
    )
}