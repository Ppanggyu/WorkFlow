import { useNavigate } from "react-router-dom";
import { useAuth } from "./useAuth";
import { api } from "../api/api";

export function useLogout() {
  const navigate = useNavigate();
  const { setAccessToken } = useAuth();

  return async function logout() {
    try {
      await api.post("/api/logout");
    } finally {
      // accessToken 메모리 제거
      setAccessToken(null);
      navigate("/login", { replace: true });
    }
  };
}
