import { useState } from 'react'
import ChatService from './services/ChatService'
import './App.css'

function App() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [conversationId, setConversationId] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!input.trim() || isLoading) return

    const userMessage = input.trim()
    setInput('')

    // 添加用户消息到界面
    setMessages(prev => [...prev, { role: 'user', content: userMessage }])
    setIsLoading(true)

    try {
      const response = await ChatService.chat(conversationId, userMessage)

      // 保存会话ID
      setConversationId(response.conversationId)

      // 添加AI回复到界面
      setMessages(prev => [...prev, { role: 'assistant', content: response.answer }])
    } catch (error) {
      console.error('Chat error:', error)
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: '抱歉，发生了错误：' + (error.response?.data?.message || error.message)
      }])
    } finally {
      setIsLoading(false)
    }
  }

  const handleNewChat = () => {
    setMessages([])
    setConversationId('')
    setInput('')
  }

  return (
    <div className="app">
      <div className="chat-container">
        {/* 头部 */}
        <div className="chat-header">
          <h1>AI 对话助手</h1>
          <button onClick={handleNewChat} className="new-chat-btn">
            新对话
          </button>
        </div>

        {/* 消息列表 */}
        <div className="messages-container">
          {messages.length === 0 ? (
            <div className="welcome-message">
              <p>你好！我是AI助手，有什么可以帮助你的吗？</p>
            </div>
          ) : (
            messages.map((msg, index) => (
              <div
                key={index}
                className={`message ${msg.role === 'user' ? 'user-message' : 'assistant-message'}`}
              >
                <div className="message-avatar">
                  {msg.role === 'user' ? '👤' : '🤖'}
                </div>
                <div className="message-content">
                  <div className="message-text">{msg.content}</div>
                </div>
              </div>
            ))
          )}
          {isLoading && (
            <div className="message assistant-message">
              <div className="message-avatar">🤖</div>
              <div className="message-content">
                <div className="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 输入框 */}
        <form onSubmit={handleSubmit} className="input-form">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="输入消息..."
            disabled={isLoading}
            className="message-input"
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="send-button"
          >
            {isLoading ? '发送中...' : '发送'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default App
