from pydantic import BaseModel, Field

class TagRequest(BaseModel):
    image_url: str = Field(..., description="Image url", min_length=1)
    top_k: int = Field(..., description="Number of tags to return", ge=1, le=20)

class TagResponse(BaseModel):
    tags: list[dict] = Field(..., description="List of tags")
    image_url: str = Field(..., description="Image url")
    top_k: int = Field(..., description="Number of tags to return")