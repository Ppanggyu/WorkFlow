import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./useAuth";

export default function ProtectedRoute() {
  const { accessToken, authReady } = useAuth();

  if (!authReady) return null;

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

// 라우터 권한 설정
// 엑세트 토큰이 없으면 로그인 페이지로 돌아감.