import axios from 'axios'
import { message } from 'ant-design-vue'

const myAxios = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 60000,
  withCredentials: true,
})

myAxios.interceptors.request.use(
  function (config) {
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)

let warned = false

myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        if (!warned) {
          warned = true
          message.warning('请先登录')
          setTimeout(() => {
            warned = false
          }, 1000)
        }
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`
      }
    }
    return response
  },
  function (error) {
    if (error?.response?.data?.message) {
      message.error(error.response.data.message)
    }
    return Promise.reject(error)
  },
)

export default myAxios
