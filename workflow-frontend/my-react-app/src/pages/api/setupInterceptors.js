import { api } from "./api";

export function setupInterceptors(getToken, setToken) {

  // 요청마다 토큰 붙이기
  api.interceptors.request.use((config) => {
    const token = getToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  // 401이면 refresh 후 재시도
  api.interceptors.response.use(
    (res) => res,
    async (err) => {
      const original = err.config;

      if (err.response?.status === 401 && !original._retry) {
        original._retry = true;

        try {
          const r = await api.post("/api/refresh"); // 쿠키로 refresh
          const newToken = r.data.accessToken;      // 서버가 body로 주는 경우

          setToken(newToken);
          original.headers.Authorization = `Bearer ${newToken}`;
          return api(original);
          
        } catch (e) {
          setToken(null);
          return Promise.reject(e);
        }
      }

      return Promise.reject(err);
    }
  );
}
