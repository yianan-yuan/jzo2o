import {
  baseUrl,
  notToLoginApiUrl
} from './env'
// 参数： url:请求地址  param：请求参数  method：请求方式 callBack：回调函数
const requestQueue = [];
export function request ({
  url = '/api',
  params = {},
  method = 'GET'
}) {
  // uni.getStorage({
  // 	key: ''
  // })
  // 获取token
  // 请求队列，保存正在进行的请求
  const token = uni.getStorageSync('token')
  let header = {
    // 'Accept': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Content-Type': 'application/json;charset=UTF-8',
    'Authorization': token
  }
  // 构造请求对象
  const requestConfig = {
    url: baseUrl + url,
    data: params,
    header: header,
    method: method
  };
  // 判断是否有相同的请求正在进行，如果有则返回正在进行的请求
  // const existingRequest = requestQueue.find(
  //   req => JSON.stringify(req.config) === JSON.stringify(requestConfig)
  // );
  // if (existingRequest) {
  //   throw new Warning('请求正在进行中，请勿重复请求！')
  // }

  // // 创建一个延迟函数，用于设置一定时间后从请求队列中移除请求
  // const delayRemove = (requestConfig, delay) => {
  //   setTimeout(() => {
  //     const index = requestQueue.findIndex(req => JSON.stringify(req.config) === JSON.stringify(requestConfig));
  //     if (index !== -1) {
  //       requestQueue.splice(index, 1);
  //     }
  //   }, delay);
  // };
  const requestRes = new Promise((resolve, reject) => {
    uni.request({
      ...requestConfig,
      success: (res) => {
        const {
          data
        } = res
        // 始终 resolve 外层 res 对象（保持与项目里其他接口调用方一致：res.data.code）
        resolve(res)
        // 业务码为 0 或 200：继续正常
        if (data && (data.code == 0 || data.code == 200)) {
          return
        }
        // 业务码 605：需要登录
        if (data && data.code === 605) {
          if (getCurrentPages()[getCurrentPages().length - 1].route !== 'pages/login/index') {
            setTimeout(() => {
              uni.navigateTo({
                url: `/pages/login/index?isLogin=1&reason=${data.msg}`
              });
            }, 2000)
          }
          return
        }
        // 其它业务码（非 0/200/605）：打印日志，调用方会在 .then 中看到非 200 自行处理
        console.warn('request biz error:', data)
      },
      fail: (err) => {
        const error = {
          data: {
            msg: err && err.errMsg
          }
        }
        console.log('request fail:', err);
        reject(error)
      }
    })
  })
  const handleError = (error, resolve, url) => {
    var errorCode = error.statusCode;
    if (errorCode == 401) {
      uni.removeStorageSync('token');
      uni.removeStorageSync('nickName')
      if (!notToLoginApiUrl.includes(url)) {
        uni.showToast({
          title: "请先登录",
          icon: "none",
          duration: 10000,
          success: () => {
            setTimeout(() => {
              uni.navigateTo({
                url: '/pages/login/index'
              });
            }, 2000)

          },
          fail: () => {
          }
        });
      }

    } else if (errorCode == 500) {
      if (uni.getStorageSync("token") == "") {
        uni.showToast({
          title: "请先登录",
          icon: "none",
          duration: 2000,
          success: () => {

          },
          fail: () => {

          }
        });
      } else {
        uni.showToast({
          title: error.data.error.message + "",
          icon: "none",
          duration: 10000
        });
      }
    } else if (typeof resolve === 'function') {
      resolve(error)
    }
  }
  return requestRes
}
