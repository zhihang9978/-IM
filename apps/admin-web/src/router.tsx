import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import UserManagement from './pages/UserManagement'
import MessageManagement from './pages/MessageManagement'
import GroupManagement from './pages/GroupManagement'
import FileManagement from './pages/FileManagement'
import DataAnalysis from './pages/DataAnalysis'
import SystemSettings from './pages/SystemSettings'
import DataBackup from './pages/DataBackup'
import Profile from './pages/Profile'
import MainLayout from './components/Layout/MainLayout'
import authService from './services/authService'

// 私有路由保护
const PrivateRoute = ({ children }: { children: JSX.Element }) => {
  return authService.isAuthenticated() ? children : <Navigate to="/login" replace />
}

function Router() {
  return (
    <Routes>
      {/* 公开路由 */}
      <Route path="/login" element={<Login />} />

      {/* 受保护的路由 */}
      <Route path="/" element={<PrivateRoute><MainLayout /></PrivateRoute>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="users" element={<UserManagement />} />
        <Route path="messages" element={<MessageManagement />} />
        <Route path="groups" element={<GroupManagement />} />
        <Route path="files" element={<FileManagement />} />
        <Route path="analytics" element={<DataAnalysis />} />
        <Route path="settings" element={<SystemSettings />} />
        <Route path="backup" element={<DataBackup />} />
        <Route path="profile" element={<Profile />} />
      </Route>

      {/* 404页面 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default Router

