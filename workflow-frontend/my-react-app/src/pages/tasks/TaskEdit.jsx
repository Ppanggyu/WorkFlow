import TaskForm from "../../components/tasks/TaskForm";
import { useParams } from "react-router-dom";

export default function TaskEdit() {

  const { id } = useParams();

  // TaskForm 컴포넌트를 edit 모드로 렌더링
  // - mode="edit" : 기존 Task 데이터를 불러와 수정 가능
  // - TaskForm 내부에서 API 호출, 검증, 제출 처리 모두 담당
  return (
    <TaskForm mode="edit" taskId = {id}/>
  );
}
