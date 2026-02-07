import './Login.css'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from './useAuth';
import { api } from "../api/api";

function Login()  {

    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const {setAccessToken} = useAuth();

const loginHandle = async () => {
  try {
    const res = await api.post("/api/login", { email, password });
    setAccessToken(res.data.accessToken);
    navigate("/");
  } catch (err) {
    console.error(err);
    alert("아이디 또는 비밀번호가 틀렸습니다.");
  }
};

    return (
        <div>
            <h2>로그인</h2>

            <input
            type='email'
            placeholder='이메일'
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            />

            <input
            type='password'
            placeholder='비밀번호'
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            />

            <button onClick={loginHandle}>로그인</button>
        </div>
    ); 
}

export default Login;