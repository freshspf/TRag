import axios from 'axios'

const API_BASE_URL = '/api'

class ChatService {
  /**
   * 发送聊天消息
   * @param {string} conversationId - 会话ID（可选）
   * @param {string} message - 用户消息
   * @returns {Promise<{conversationId: string, answer: string}>}
   */
  static async chat(conversationId, message) {
    try {
      const response = await axios.post(`${API_BASE_URL}/chat/memory`, {
        conversationId: conversationId || null,
        message: message.trim()
      })
      return response.data
    } catch (error) {
      if (error.response) {
        // 服务器返回错误响应
        throw new Error(error.response.data?.message || '请求失败')
      } else if (error.request) {
        // 请求发送但没有收到响应
        throw new Error('无法连接到服务器，请确保后端服务已启动')
      } else {
        // 其他错误
        throw new Error('请求发生错误：' + error.message)
      }
    }
  }
}

export default ChatService
