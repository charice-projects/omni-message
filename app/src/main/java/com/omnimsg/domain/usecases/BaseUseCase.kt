package com.omnimsg.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基础UseCase抽象类
 * P - 参数类型
 * R - 返回类型
 */
abstract class BaseUseCase<in P, R> {
    
    /**
     * 执行UseCase的主方法
     */
    abstract suspend fun execute(params: P): Result<R>
    
    /**
     * 在指定调度器上执行UseCase
     */
    suspend operator fun invoke(
        params: P,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Result<R> {
        return try {
            withContext(dispatcher) {
                execute(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 无参数的UseCase
 */
abstract class NoParamsUseCase<R> : BaseUseCase<Unit, R>() {
    
    abstract suspend fun execute(): Result<R>
    
    override suspend fun execute(params: Unit): Result<R> {
        return execute()
    }
    
    suspend operator fun invoke(dispatcher: CoroutineDispatcher = Dispatchers.IO): Result<R> {
        return invoke(Unit, dispatcher)
    }
}

/**
 * 流式UseCase
 */
abstract class FlowUseCase<in P, R> {
    
    abstract fun execute(params: P): kotlinx.coroutines.flow.Flow<Result<R>>
    
    operator fun invoke(params: P): kotlinx.coroutines.flow.Flow<Result<R>> {
        return execute(params)
    }
}