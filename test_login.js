const axios = require('axios');

async function testLogin() {
  try {
    console.log('测试登录API...');
    
    const response = await axios.post('http://localhost:8080/api/user/login', {
      username: 'admin',
      password: '123456'
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    console.log('登录成功！');
    console.log('响应数据:', response.data);
  } catch (error) {
    console.error('登录失败！');
    if (error.response) {
      console.error('状态码:', error.response.status);
      console.error('响应数据:', error.response.data);
    } else {
      console.error('错误信息:', error.message);
    }
  }
}

testLogin(); 