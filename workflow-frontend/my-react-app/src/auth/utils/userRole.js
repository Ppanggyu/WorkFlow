import { api } from "../../api/api.js";

export const showDepteSelected = (user) => {
    return user?.role == "ADMIN";
}

// 게시글 권한 확인
export const userRole = async (user, task) => {

    const isAdmin = user?.role === "ADMIN";
    const isManager = user?.role === "MANAGER";
    const isUser = user?.role === "USER";

    let canAccess = false;

    if(isAdmin){
        canAccess = true;
    }else if(isManager){
        canAccess = managerTaskBtnRole(user, task);
    }else if(isUser){
        canAccess = userTaskBtnRole(user, task);
    }
    return canAccess;
}

// 매니저 일 시 서버에서 비교
export const managerTaskBtnRole = async(user, task) => {
    let isDept = false;
    try{
        const res = await api.post(`/api/user/dept`, task);
        isDept = res.data;
    }catch(error){
        console.log(error);
    }
    return isDept;
}

// 유저일 시 그냥 비교
export const userTaskBtnRole = async(user, task) => {
    return (user?.id == task?.createdById) || (user?.id == task?.assigneeId);
}