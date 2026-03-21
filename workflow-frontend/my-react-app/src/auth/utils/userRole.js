// role 확인
export const userRole = (user) => {
    return (user?.role === "ADMIN" || user?.role == "MANAGER");
}