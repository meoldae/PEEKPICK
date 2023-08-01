import { customAxios } from '../../api/customAxios';

import classes from './style/Login.module.css';

const Login = () => {
  const loginHandler = (service) => {
      window.location.href = `http://localhost:8081/oauth2/authorization/${service}`;
  };

  // 테스트 버튼
  const test = (id) => {
    const dummy = {
      id: 1,
      content: '더미입니다.'
    }

    customAxios.post(`/member/login?id=test${id}`, dummy)
      .then(response => {
        console.log(response)
      })
      .catch(error => {
        console.log(error)
      })
  }

  return (
    <div className={classes.container}>
      <div className={classes.buttons}>
        {/* 이미지로 변경 예정 */}
        <button onClick={() => test(1)}>테스트 버튼</button>
        <button onClick={() => loginHandler('kakao')}>카카오 로그인</button>
        <button onClick={() => loginHandler('naver')}>네이버 로그인</button>
        <button onClick={() => loginHandler('google')}>구글 로그인</button>
      </div>
    </div>
  );
}

export default Login;
