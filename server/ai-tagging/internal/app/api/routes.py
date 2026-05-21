from fastapi import APIRouter, HTTPException, Depends

from internal.app.schemas.request import TagResponse, TagRequest
from internal.app.services.tagging import TaggingService

router = APIRouter()

_tagging_service: TaggingService = None

async def get_tagging_service() -> TaggingService:
    return _tagging_service

def init_tagging_service(service: TaggingService):
    global _tagging_service
    _tagging_service = service

@router.post("/api/tag", response_model=TagResponse)
async def tag_image(
        request: TagRequest,
        service: TaggingService = Depends(get_tagging_service)
):
    result = await service.tag_image_from_url(request.image_url, request.top_k)
    if not result:
        raise HTTPException(status_code=400, detail="Failed to tag image")
    return TagResponse(
        tags=[r.to_dict() for r in result],
        image_url=request.image_url,
        top_k=len(result),
    )

@router.get("/health")
async def health_check():
    if _tagging_service is None:
        return {"status": "starting", "model": "loading"}
    return {
        "status": "healthy",
        "model": _tagging_service.mode(),
        "model_available": _tagging_service.is_model_available(),
        "model_error": _tagging_service.model_error,
    }