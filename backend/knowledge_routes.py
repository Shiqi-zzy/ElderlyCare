"""智能体-知识库调试路由。

/debug/bindKnowledge（GET）：手动触发智能体绑定知识库，返回萤石接口完整原始结果。
启动时也会自动执行一次绑定（见 main.py lifespan），本接口用于联调重试/查看返回。
"""
import asyncio
import logging

from fastapi import APIRouter

import knowledge_service

logger = logging.getLogger("knowledge")

router = APIRouter(tags=["智能体知识库"])


@router.get("/debug/bindKnowledge")
async def debug_bind_knowledge():
    """手动触发智能体绑定知识库（任务3 调试接口）。

    返回 {code, message, data}，data = 萤石绑定接口完整原始响应（含 meta.code/message），
    便于核对绑定是否成功；任何异常已由 knowledge_service 兜底降级。
    """
    result = await asyncio.to_thread(knowledge_service.bind_agent_knowledge)
    logger.info("[知识库] /debug/bindKnowledge 返回: %s", result)
    return {"code": 200, "message": "ok", "data": result}
