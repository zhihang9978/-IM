package com.lanxin.im.utils

/**
 * 统一的结果封装类
 * IM知识库推荐：分类错误处理，提供更好的用户体验
 * 
 * 参考：Kotlin Result模式 + Clean Architecture
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * 应用异常分类
 * 便于针对不同类型的错误提供不同的处理策略
 */
sealed class AppException(
    override val message: String,
    open val code: Int = -1,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * 网络异常
     */
    data class NetworkException(
        override val message: String = "网络连接失败，请检查网络设置",
        override val code: Int = ERROR_NETWORK
    ) : AppException(message, code)
    
    /**
     * 认证异常（Token过期/无效）
     */
    data class AuthException(
        override val message: String = "登录已过期，请重新登录",
        override val code: Int = ERROR_AUTH
    ) : AppException(message, code)
    
    /**
     * 服务器异常
     */
    data class ServerException(
        override val message: String = "服务器错误，请稍后再试",
        override val code: Int = ERROR_SERVER,
        val httpCode: Int = 500
    ) : AppException(message, code)
    
    /**
     * 业务异常（服务器返回的业务错误）
     */
    data class BusinessException(
        override val message: String,
        override val code: Int
    ) : AppException(message, code)
    
    /**
     * 本地数据异常
     */
    data class LocalDataException(
        override val message: String = "本地数据读取失败",
        override val code: Int = ERROR_LOCAL_DATA
    ) : AppException(message, code)
    
    /**
     * 文件异常
     */
    data class FileException(
        override val message: String = "文件处理失败",
        override val code: Int = ERROR_FILE
    ) : AppException(message, code)
    
    /**
     * 权限异常
     */
    data class PermissionException(
        override val message: String = "缺少必要权限",
        override val code: Int = ERROR_PERMISSION
    ) : AppException(message, code)
    
    /**
     * 未知异常
     */
    data class UnknownException(
        override val message: String = "未知错误",
        val throwable: Throwable? = null
    ) : AppException(message, ERROR_UNKNOWN, throwable)
    
    companion object {
        const val ERROR_NETWORK = 1001
        const val ERROR_AUTH = 1002
        const val ERROR_SERVER = 1003
        const val ERROR_LOCAL_DATA = 1004
        const val ERROR_FILE = 1005
        const val ERROR_PERMISSION = 1006
        const val ERROR_UNKNOWN = 9999
    }
}

/**
 * Extension functions for Result
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T> Result<T>.onError(action: (AppException) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}

/**
 * Map Result<T> to Result<R>
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading
    }
}

/**
 * FlatMap Result<T> to Result<R>
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading
    }
}

/**
 * Get data or null
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        else -> null
    }
}

/**
 * Get data or default value
 */
fun <T> Result<T>.getOrDefault(default: T): T {
    return when (this) {
        is Result.Success -> data
        else -> default
    }
}

/**
 * Convert throwable to AppException
 */
fun Throwable.toAppException(): AppException {
    return when (this) {
        is java.net.UnknownHostException,
        is java.net.SocketTimeoutException,
        is java.net.ConnectException -> AppException.NetworkException()
        is java.io.IOException -> AppException.FileException(message ?: "文件处理失败")
        is SecurityException -> AppException.PermissionException(message ?: "缺少必要权限")
        is AppException -> this
        else -> AppException.UnknownException(
            message ?: "未知错误",
            this
        )
    }
}
