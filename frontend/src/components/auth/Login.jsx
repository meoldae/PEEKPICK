import { useState } from 'react';
import { customAxios } from '../../api/customAxios';
import { useNavigate } from 'react-router-dom';

import classes from './style/Login.module.css';

const Login = () => {
  // 테스트용 상태 관리
  const [tester, setTester] = useState(1);
  const navigate = useNavigate();

  const loginHandler = (service) => {
      window.location.href = `http://localhost:8081/oauth2/authorization/${service}`;
  };

  // 테스트 버튼
  const test = (id) => {
    customAxios.post(`/member/login?id=test${id}`)
      .then(response => {
        if (response.data.code === '200') {
          const token = response.data.data
          localStorage.setItem("jwtToken", token);
          window.location.replace("/")
        }
      })
      .catch(error => {
        console.log(error)
      })
  }

  const testSignUp = () => {
    navigate("/userinfo")
  }

  return (
    <div className={classes.container}>
      <img src="img/eyes.png" alt="eyes" className={classes.eyes} />
      <div className={classes.buttons}>
        {/* 이미지로 변경 예정 */}
        {/* 테스트 버튼 */}
        <input type="text" placeholder='숫자를 입력하세요' onChange={e => setTester(e.target.value)} />
        <button onClick={() => test(tester)}>테스트 버튼</button>
        <button onClick={() => testSignUp()}>테스트 회원가입</button>
        {/* 테스트 버튼 */}
        <img src="img/kakao_login.png" alt="kakao" onClick={() => loginHandler('kakao')} />
        <img src="img/naver_login.png" alt="naver" onClick={() => loginHandler('naver')} />
        <img src="img/google_login.png" alt="google" onClick={() => loginHandler('google')} />
      </div>
    </div>
  );
}

export default Login;
