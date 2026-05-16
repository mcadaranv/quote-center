"""Mock API for metals pricing.

Install [uv](https://github.com/astral-sh/uv) and run with: uv run --with 'fastapi[standard]' -- fastapi dev metals_pricing_api.py  --host localhost --port 8888
Visit http://<ip>:<port>/docs for API documentation
"""

import random
from fastapi import FastAPI, HTTPException, Query
from enum import Enum
from typing import Dict, Tuple
from pydantic import BaseModel

app = FastAPI(title="Metals Pricing API")

class Metal(str, Enum):
    nickel = "nickel"
    cobalt = "cobalt"
    lithium = "lithium"
    copper = "copper"

# Price ranges for each metal in USD per kg (min, max).
METAL_PRICE_RANGES: Dict[str, Tuple[float, float]] = {
    "nickel": (10.0, 25.0),
    "cobalt": (30.0, 60.0),
    "lithium": (8.0, 15.0),
    "copper": (5.0, 12.0)
}

class PriceResponse(BaseModel):
    metal: str
    price: float
    currency: str = "USD"
    unit: str = "kg"

@app.get("/price")
async def get_metal_price(metal: Metal = Query(..., description="Metal to get price for")) -> PriceResponse:
    # Simulate random failures.
    if random.randint(1, 4) == 1:
        raise HTTPException(status_code=503, detail="Service Temporarily Unavailable")
    
    price_range = METAL_PRICE_RANGES[metal]
    price = round(random.uniform(price_range[0], price_range[1]), 2)
    return PriceResponse(
        metal=metal,
        price=price
    )
