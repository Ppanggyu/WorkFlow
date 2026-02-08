import { useEffect, useState } from "react";
import { AuthCtx } from "./AuthContext";
import { refreshClient, setApiAccessToken } from "../api/api";

export default function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    setApiAccessToken(accessToken);
  }, [accessToken]);

useEffect(() => {
  const boot = async () => {
    try {

      const res = await refreshClient.post("/api/refresh");

      // 204면 비로그인(정상) → 아무 것도 안 함
      if (res.status === 204) return;

      // 성공 시 액세스 토큰 저장
      setAccessToken(res.data.accessToken);

    } catch (e) {

      // 쿠키는 있는데 토큰이 죽었으면 401 (이것도 조용히 처리 가능)
      if (e.response?.status === 401) return;

      console.error("[BOOT REFRESH ERROR]", e);

    } finally {
      setAuthReady(true);
    }
  };
  boot();
}, []);

  if (!authReady) return null;

  return (
    <AuthCtx.Provider value={{ accessToken, setAccessToken }}>
      {children}
    </AuthCtx.Provider>
  );
}


// AuthProvider 작성된 코드 역할
// 앱 시작 시 /api/refresh로 로그인 상태를 복구하고 accessToken을 전역 상태로 관리하며,
// accessToken이 바뀔 때마다 API 클라이언트에 자동 반영하고 인증 준비가 끝나기 전에는 화면을 렌더링하지 않는다.
